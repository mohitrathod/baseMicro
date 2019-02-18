package com.mohitrathod.customer.account.service;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

import com.mohitrathod.customer.account.bean.User;
import com.mohitrathod.customer.account.repository.BasicUserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

@RestController
public class CustomerAccountServiceController {

	private Logger logger = LoggerFactory.getLogger(this.getClass());

	@Autowired
	private CustomerWalletServiceProxy proxy;

	@Autowired
	BasicUserRepository repository;

	@GetMapping("/currency-converter/from/{from}/to/{to}/quantity/{quantity}")
	public CustomerBalanceBean convertCurrency(@PathVariable String from, @PathVariable String to,
											   @PathVariable BigDecimal quantity) {

		Map<String, String> uriVariables = new HashMap<>();
		uriVariables.put("from", from);
		uriVariables.put("to", to);

		ResponseEntity<CustomerBalanceBean> responseEntity = new RestTemplate().getForEntity(
				"http://localhost:8000/currency-exchange/from/{from}/to/{to}", CustomerBalanceBean.class,
				uriVariables);

		CustomerBalanceBean response = responseEntity.getBody();

		return new CustomerBalanceBean(response.getId(), from, to, response.getConversionMultiple(), quantity,
				quantity.multiply(response.getConversionMultiple()), response.getPort());
	}

	@GetMapping("/currency-converter-feign/from/{from}/to/{to}/quantity/{quantity}")
	public CustomerBalanceBean convertCurrencyFeign(@PathVariable String from, @PathVariable String to,
													@PathVariable BigDecimal quantity) {

		CustomerBalanceBean response = proxy.balanceService(from, to);

		logger.info("{}", response);

		return new CustomerBalanceBean(response.getId(), from, to, response.getConversionMultiple(), quantity,
				quantity.multiply(response.getConversionMultiple()), response.getPort());
	}



	@PostMapping("/create-customer")
	public void createCustomer(@RequestParam String name) {

		System.out.println(name + "< customer name");

		User user = new User();
		user.setId(43L);
		user.setUsername("mfoobar");
		user.setFirstname("afirstname");
		user.setLastname("lastname");

		user = repository.save(user);

		System.out.println(repository.findById(user.getId()));

	}

	@GetMapping("/info")
	public String info() {

		return "this is customer account service";
	}

}