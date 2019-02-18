package com.mohitrathod.customer.account.service;

import com.datastax.driver.core.Session;
import com.mohitrathod.customer.account.bean.User;
import com.mohitrathod.customer.account.repository.BasicUserRepository;
import com.mohitrathod.customer.account.util.CassandraKeyspace;
import org.junit.ClassRule;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.data.cassandra.repository.config.EnableCassandraRepositories;
import org.springframework.data.util.Version;


@SpringBootApplication
@EnableFeignClients("com.mohitrathod.customer.account.service")
@EnableDiscoveryClient
@EntityScan(basePackageClasses = User.class)
@EnableCassandraRepositories(basePackageClasses = BasicUserRepository.class)
public class CustomerAccountServiceApp {

	public final static Version CASSANDRA_3_4 = Version.parse("3.4");

	@ClassRule
	public final static CassandraKeyspace CASSANDRA_KEYSPACE = CassandraKeyspace.onLocalhost();

	@Autowired
	BasicUserRepository repository;
	@Autowired
	Session session;


	public static void main(String[] args) {
		SpringApplication.run(CustomerAccountServiceApp.class, args);
	}
}
