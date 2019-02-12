package com.mohitrathod.customer.account.service;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.cloud.netflix.ribbon.RibbonClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name="customer-wallet-service")
@RibbonClient(name="customer-wallet-service")
public interface CustomerWalletServiceProxy {
  @GetMapping("/customer-wallet/getBalance")
  public CustomerBalanceBean balanceService
    (@PathVariable("from") String from, @PathVariable("to") String to);
}