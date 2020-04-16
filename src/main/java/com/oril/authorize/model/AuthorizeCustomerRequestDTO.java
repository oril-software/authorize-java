package com.oril.authorize.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthorizeCustomerRequestDTO {

    private Double price;
    private String authorizeCustomerId;
    private String authorizePaymentProfileId;

}
