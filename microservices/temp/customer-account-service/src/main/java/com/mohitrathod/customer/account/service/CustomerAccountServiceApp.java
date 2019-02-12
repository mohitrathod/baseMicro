package com.mohitrathod.customer.account.service;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication
@EnableFeignClients("com.mohitrathod.customer.account.service")
@EnableDiscoveryClient
public class CustomerAccountServiceApp {

	public static void main(String[] args) {
		SpringApplication.run(CustomerAccountServiceApp.class, args);
	}
}
