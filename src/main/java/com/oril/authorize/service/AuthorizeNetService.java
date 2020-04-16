package com.oril.authorize.service;


import com.oril.authorize.model.*;
import net.authorize.api.contract.v1.TransactionResponse;

public interface AuthorizeNetService {

    /**
     * Processes credit card payment
     *
     * @param requestDTO data needed for request - price and credit card details
     * @return transaction result
     */
    TransactionResponse processCreditCardTransaction(AuthorizeCreditCardRequestDTO requestDTO);

    /**
     * Processes apple pay payment
     *
     * @param requestDTO data needed for request - price and payment token
     * @return transaction result
     */
    TransactionResponse processApplePayTransaction(AuthorizeApplePayRequestDTO requestDTO);

    /**
     * Processes authorize customer payment
     *
     * @param requestDTO data needed for request - price and authorize profile ids
     * @return transaction result
     */
    TransactionResponse processCustomerTransaction(AuthorizeCustomerRequestDTO requestDTO);

    /**
     * Creates authorize customer profile
     *
     * @param billingInfo credit card details
     * @return dto with authorize profile ids
     */
    CustomerProfileDTO saveCustomerProfile(BillingInfo billingInfo);

    /**
     * Deletes authorize customer profile
     *
     * @param customerProfileDTO dto with authorize profile ids
     */
    void deleteCustomerProfile(CustomerProfileDTO customerProfileDTO);

}
