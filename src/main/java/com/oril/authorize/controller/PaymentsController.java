package com.oril.authorize.controller;


import com.oril.authorize.model.*;
import com.oril.authorize.service.AuthorizeNetService;
import lombok.AllArgsConstructor;
import net.authorize.api.contract.v1.TransactionResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/payments/authorize")
@AllArgsConstructor(onConstructor = @__(@Autowired))
public class PaymentsController {

    final AuthorizeNetService authorizeNetService;

    @PostMapping("/creditCard")
    public TransactionResponse buyWithAuthorize(@RequestBody AuthorizeCreditCardRequestDTO requestDTO) {
        return authorizeNetService.processCreditCardTransaction(requestDTO);
    }

    @PostMapping("/applePay")
    public TransactionResponse buyWithApplePay(@RequestBody AuthorizeApplePayRequestDTO requestDTO) {
        return authorizeNetService.processApplePayTransaction(requestDTO);
    }

    @PostMapping(value = "/account")
    public CustomerProfileDTO createAuthorizeAccount(@RequestBody BillingInfo billingInfo) {
        return authorizeNetService.saveCustomerProfile(billingInfo);
    }

    @PostMapping("/customer")
    public TransactionResponse buyWithCustomerId(@RequestBody AuthorizeCustomerRequestDTO requestDTO) {
        return authorizeNetService.processCustomerTransaction(requestDTO);
    }

    @DeleteMapping(value = "/account")
    public void deleteAuthorizeAccount(@RequestBody CustomerProfileDTO profileDTO) {
        authorizeNetService.deleteCustomerProfile(profileDTO);
    }

}
