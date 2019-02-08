/*
 * Copyright 2017-2019 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.kafka.listener;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willAnswer;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;

import java.time.Duration;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRebalanceListener;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.OffsetAndMetadata;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.record.TimestampType;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InOrder;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.config.KafkaListenerEndpointRegistry;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.transaction.KafkaTransactionManager;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit4.SpringRunner;

/**
 * @author Gary Russell
 * @since 2.1
 *
 */
@RunWith(SpringRunner.class)
@DirtiesContext
public class SeekToCurrentBatchErrorHandlerTests {

	private static final String CONTAINER_ID = "container";

	@SuppressWarnings("rawtypes")
	@Autowired
	private Consumer consumer;

	@SuppressWarnings("rawtypes")
	@Autowired
	private Producer producer;

	@Autowired
	private Config config;

	@Autowired
	private KafkaListenerEndpointRegistry registry;

	/*
	 * Deliver 6 records from three partitions, fail on the second record second
	 * partition.
	 */
	@SuppressWarnings("unchecked")
	@Test
	public void discardRemainingRecordsFromPollAndSeek() throws Exception {
		assertThat(this.config.deliveryLatch.await(10, TimeUnit.SECONDS)).isTrue();
		assertThat(this.config.pollLatch.await(10, TimeUnit.SECONDS)).isTrue();
		this.registry.stop();
		assertThat(this.config.closeLatch.await(10, TimeUnit.SECONDS)).isTrue();
		InOrder inOrder = inOrder(this.consumer, this.producer);
		inOrder.verify(this.consumer).subscribe(any(Collection.class), any(ConsumerRebalanceListener.class));
		inOrder.verify(this.consumer).poll(Duration.ofMillis(ContainerProperties.DEFAULT_POLL_TIMEOUT));
		inOrder.verify(this.producer).beginTransaction();
		inOrder.verify(this.consumer).seek(new TopicPartition("foo", 0), 0L);
		inOrder.verify(this.consumer).seek(new TopicPartition("foo", 1), 0L);
		inOrder.verify(this.consumer).seek(new TopicPartition("foo", 2), 0L);
		inOrder.verify(this.producer).abortTransaction();
		inOrder.verify(this.consumer).poll(Duration.ofMillis(ContainerProperties.DEFAULT_POLL_TIMEOUT));
		inOrder.verify(this.producer).beginTransaction();
		Map<TopicPartition, OffsetAndMetadata> offsets = new LinkedHashMap<>();
		offsets.put(new TopicPartition("foo", 0), new OffsetAndMetadata(2L));
		offsets.put(new TopicPartition("foo", 1), new OffsetAndMetadata(2L));
		offsets.put(new TopicPartition("foo", 2), new OffsetAndMetadata(2L));
		inOrder.verify(this.producer).sendOffsetsToTransaction(offsets, CONTAINER_ID);
		inOrder.verify(this.producer).commitTransaction();
		assertThat(this.config.ehException).isInstanceOf(ListenerExecutionFailedException.class);
		assertThat(((ListenerExecutionFailedException) this.config.ehException).getGroupId()).isEqualTo(CONTAINER_ID);
	}

	@Configuration
	@EnableKafka
	public static class Config {

		private final CountDownLatch pollLatch = new CountDownLatch(1);

		private final CountDownLatch deliveryLatch = new CountDownLatch(2);

		private final CountDownLatch closeLatch = new CountDownLatch(1);

		private final AtomicBoolean fail = new AtomicBoolean(true);

		private volatile Exception ehException;

		@KafkaListener(id = CONTAINER_ID, topics = "foo")
		public void foo(List<String> in) {
			this.deliveryLatch.countDown();
			if (this.fail.getAndSet(false)) {
				throw new RuntimeException("foo");
			}
		}

		@SuppressWarnings({ "rawtypes" })
		@Bean
		public ConsumerFactory consumerFactory() {
			ConsumerFactory consumerFactory = mock(ConsumerFactory.class);
			final Consumer consumer = consumer();
			given(consumerFactory.createConsumer(CONTAINER_ID, "", "-0")).willReturn(consumer);
			return consumerFactory;
		}

		@SuppressWarnings({ "rawtypes", "unchecked" })
		@Bean
		public Consumer consumer() {
			final Consumer consumer = mock(Consumer.class);
			final TopicPartition topicPartition0 = new TopicPartition("foo", 0);
			final TopicPartition topicPartition1 = new TopicPartition("foo", 1);
			final TopicPartition topicPartition2 = new TopicPartition("foo", 2);
			willAnswer(i -> {
				((ConsumerRebalanceListener) i.getArgument(1)).onPartitionsAssigned(
						Collections.singletonList(topicPartition1));
				return null;
			}).given(consumer).subscribe(any(Collection.class), any(ConsumerRebalanceListener.class));
			Map<TopicPartition, List<ConsumerRecord>> records1 = new LinkedHashMap<>();
			records1.put(topicPartition0, Arrays.asList(
					new ConsumerRecord("foo", 0, 0L, 0L, TimestampType.NO_TIMESTAMP_TYPE, 0, 0, 0, null, "foo"),
					new ConsumerRecord("foo", 0, 1L, 0L, TimestampType.NO_TIMESTAMP_TYPE, 0, 0, 0, null, "bar")));
			records1.put(topicPartition1, Arrays.asList(
					new ConsumerRecord("foo", 1, 0L, 0L, TimestampType.NO_TIMESTAMP_TYPE, 0, 0, 0, null, "baz"),
					new ConsumerRecord("foo", 1, 1L, 0L, TimestampType.NO_TIMESTAMP_TYPE, 0, 0, 0, null, "qux")));
			records1.put(topicPartition2, Arrays.asList(
					new ConsumerRecord("foo", 2, 0L, 0L, TimestampType.NO_TIMESTAMP_TYPE, 0, 0, 0, null, "fiz"),
					new ConsumerRecord("foo", 2, 1L, 0L, TimestampType.NO_TIMESTAMP_TYPE, 0, 0, 0, null, "buz")));
			final AtomicInteger which = new AtomicInteger();
			willAnswer(i -> {
				this.pollLatch.countDown();
				switch (which.getAndIncrement()) {
					case 0:
					case 1:
						return new ConsumerRecords(records1);
					default:
						try {
							Thread.sleep(1000);
						}
						catch (InterruptedException e) {
							Thread.currentThread().interrupt();
						}
						return new ConsumerRecords(Collections.emptyMap());
				}
			}).given(consumer).poll(Duration.ofMillis(ContainerProperties.DEFAULT_POLL_TIMEOUT));
			willAnswer(i -> {
				this.closeLatch.countDown();
				return null;
			}).given(consumer).close();
			return consumer;
		}

		@SuppressWarnings({ "rawtypes", "unchecked" })
		@Bean
		public ConcurrentKafkaListenerContainerFactory kafkaListenerContainerFactory() {
			ConcurrentKafkaListenerContainerFactory factory = new ConcurrentKafkaListenerContainerFactory();
			factory.setConsumerFactory(consumerFactory());
			factory.getContainerProperties().setAckOnError(false);
			factory.setBatchErrorHandler(new SeekToCurrentBatchErrorHandler() {

				@Override
				public void handle(Exception thrownException, ConsumerRecords<?, ?> data, Consumer<?, ?> consumer,
						MessageListenerContainer container) {

					Config.this.ehException = thrownException;
					super.handle(thrownException, data, consumer, container);
				}

			});
			factory.setBatchListener(true);
			factory.getContainerProperties().setTransactionManager(tm());
			return factory;
		}

		@SuppressWarnings({ "unchecked", "rawtypes" })
		@Bean
		public KafkaTransactionManager tm() {
			return new KafkaTransactionManager<>(producerFactory());
		}

		@SuppressWarnings("rawtypes")
		@Bean
		public ProducerFactory producerFactory() {
			ProducerFactory pf = mock(ProducerFactory.class);
			given(pf.createProducer()).willReturn(producer());
			given(pf.transactionCapable()).willReturn(true);
			return pf;
		}

		@SuppressWarnings("rawtypes")
		@Bean
		public Producer producer() {
			return mock(Producer.class);
		}

	}

}
