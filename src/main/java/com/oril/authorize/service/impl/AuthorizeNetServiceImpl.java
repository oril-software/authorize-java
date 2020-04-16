package com.oril.authorize.service.impl;

import com.oril.authorize.model.*;
import com.oril.authorize.service.AuthorizeNetService;
import lombok.RequiredArgsConstructor;
import net.authorize.api.contract.v1.*;
import net.authorize.api.controller.CreateCustomerProfileController;
import net.authorize.api.controller.CreateTransactionController;
import net.authorize.api.controller.DeleteCustomerProfileController;
import net.authorize.api.controller.base.ApiOperationBase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.math.BigDecimal;
import java.math.RoundingMode;


@Service
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class AuthorizeNetServiceImpl implements AuthorizeNetService {

    private static final String APPLE_PAY_DATA_DESCRIPTOR = "COMMON.APPLE.INAPP.PAYMENT";
    private static final String DUPLICATE_WINDOW = "duplicateWindow";
    private static final String DUPLICATE_WINDOW_TIME = "0";
    private static Logger logger = LoggerFactory.getLogger(AuthorizeNetServiceImpl.class);
    private MerchantAuthenticationType merchantAuthenticationType;

    @Value("${authorize.apiLoginId}")
    private String apiLoginId;

    @Value("${authorize.transactionKey}")
    private String transactionKey;

    @PostConstruct
    public void init() {
        ApiOperationBase.setEnvironment(net.authorize.Environment.SANDBOX);
        merchantAuthenticationType = new MerchantAuthenticationType();
        merchantAuthenticationType.setName(apiLoginId);
        merchantAuthenticationType.setTransactionKey(transactionKey);
        ApiOperationBase.setMerchantAuthentication(merchantAuthenticationType);
    }

    @Override
    public TransactionResponse processCreditCardTransaction(AuthorizeCreditCardRequestDTO requestDTO) {
        TransactionRequestType txnRequest = createTransactionRequestType(requestDTO);

        TransactionResponse result = makeTransactionRequest(txnRequest);
        logger.info("Successfully created transaction with Transaction ID: {}", result.getTransId());
        return result;
    }

    @Override
    public TransactionResponse processApplePayTransaction(AuthorizeApplePayRequestDTO requestDTO) {
        TransactionRequestType transactionRequest = createTransactionRequestType(requestDTO);

        TransactionResponse result = makeTransactionRequest(transactionRequest);
        logger.info("Successfully created transaction with Transaction ID: {}", result.getTransId());
        return result;
    }

    @Override
    public TransactionResponse processCustomerTransaction(AuthorizeCustomerRequestDTO requestDTO) {
        TransactionRequestType txnRequest = createTransactionRequestType(requestDTO);

        TransactionResponse result = makeTransactionRequest(txnRequest);
        logger.info("Successfully created transaction with Transaction ID: {}", result.getTransId());
        return result;
    }

    @Override
    public CustomerProfileDTO saveCustomerProfile(BillingInfo billingInfo) {
        PaymentType paymentType = new PaymentType();
        paymentType.setCreditCard(buildCreditCardType(billingInfo.getCreditCard()));
        CustomerPaymentProfileType customerPaymentProfileType = buildCustomerPaymentProfile(billingInfo.getAddress(), paymentType);
        CustomerProfileType customerProfileType = buildCustomerProfileType(customerPaymentProfileType);
        CreateCustomerProfileRequest apiRequest = buildCreateCustomerProfileApiRequest(customerProfileType);
        apiRequest.setValidationMode(ValidationModeEnum.LIVE_MODE);
        CreateCustomerProfileController controller = new CreateCustomerProfileController(apiRequest);
        controller.execute();
        CreateCustomerProfileResponse response = controller.getApiResponse();
        if (response == null || response.getMessages().getResultCode() != MessageTypeEnum.OK) {
            throw new RuntimeException(response != null ? response.getMessages().getMessage().get(0).getText() : "Failed");
        }
        String authorizeCustomerId = response.getCustomerProfileId();
        String authorizePaymentProfileId = null;
        if (!response.getCustomerPaymentProfileIdList().getNumericString().isEmpty()) {
            authorizePaymentProfileId = response.getCustomerPaymentProfileIdList().getNumericString().get(0);
        }
        return new CustomerProfileDTO(authorizeCustomerId, authorizePaymentProfileId);
    }

    @Override
    public void deleteCustomerProfile(CustomerProfileDTO customerProfileDTO) {
        String authorizeCustomerId = customerProfileDTO.getAuthorizeCustomerId();
        DeleteCustomerProfileRequest apiRequest = new DeleteCustomerProfileRequest();
        apiRequest.setCustomerProfileId(authorizeCustomerId);
        DeleteCustomerProfileController controller = new DeleteCustomerProfileController(apiRequest);
        controller.execute();
        DeleteCustomerProfileResponse response = controller.getApiResponse();
        if (response.getMessages().getResultCode() != MessageTypeEnum.OK) {
            throw new RuntimeException("Failed to delete Credit Card Info");
        }
        logger.info(authorizeCustomerId + " was deleted");
    }

    private TransactionRequestType createTransactionRequestType(AuthorizeCreditCardRequestDTO requestDTO) {
        BillingInfo billingInfo = requestDTO.getBillingInfo();
        PaymentType paymentType = new PaymentType();
        paymentType.setCreditCard(buildCreditCardType(billingInfo.getCreditCard()));

        TransactionRequestType txnRequest = new TransactionRequestType();
        txnRequest.setTransactionType(TransactionTypeEnum.AUTH_CAPTURE_TRANSACTION.value());
        txnRequest.setPayment(paymentType);
        txnRequest.setAmount(getAmount(requestDTO.getPrice()));
        txnRequest.setBillTo(buildCustomerAddressType(billingInfo.getAddress()));
        setTransactionSettings(txnRequest);
        return txnRequest;
    }

    private TransactionRequestType createTransactionRequestType(AuthorizeApplePayRequestDTO requestDTO) {
        OpaqueDataType op = new OpaqueDataType();
        op.setDataDescriptor(APPLE_PAY_DATA_DESCRIPTOR);
        op.setDataValue(requestDTO.getPaymentToken());
        PaymentType paymentOne = new PaymentType();
        paymentOne.setOpaqueData(op);

        TransactionRequestType transactionRequest = new TransactionRequestType();
        transactionRequest.setTransactionType(TransactionTypeEnum.AUTH_CAPTURE_TRANSACTION.value());
        transactionRequest.setPayment(paymentOne);
        transactionRequest.setAmount(getAmount(requestDTO.getPrice()));
        setTransactionSettings(transactionRequest);
        return transactionRequest;
    }

    private TransactionRequestType createTransactionRequestType(AuthorizeCustomerRequestDTO requestDTO) {
        TransactionRequestType txnRequest = new TransactionRequestType();
        txnRequest.setTransactionType(TransactionTypeEnum.AUTH_CAPTURE_TRANSACTION.value());
        txnRequest.setProfile(buildCustomerProfilePaymentType(requestDTO));
        setTransactionSettings(txnRequest);
        txnRequest.setAmount(getAmount(requestDTO.getPrice()));
        return txnRequest;
    }

    private CustomerProfilePaymentType buildCustomerProfilePaymentType(AuthorizeCustomerRequestDTO requestDTO) {
        CustomerProfilePaymentType profileToCharge = new CustomerProfilePaymentType();
        profileToCharge.setCustomerProfileId(requestDTO.getAuthorizeCustomerId());
        PaymentProfile paymentProfile = new PaymentProfile();
        paymentProfile.setPaymentProfileId(requestDTO.getAuthorizePaymentProfileId());
        profileToCharge.setPaymentProfile(paymentProfile);
        return profileToCharge;
    }

    private void setTransactionSettings(TransactionRequestType transactionRequest) {
        ArrayOfSetting transactionSettings = new ArrayOfSetting();
        SettingType setting = new SettingType();
        setting.setSettingName(DUPLICATE_WINDOW);
        setting.setSettingValue(DUPLICATE_WINDOW_TIME);
        transactionSettings.getSetting().add(setting);
        transactionRequest.setTransactionSettings(transactionSettings);
    }

    private BigDecimal getAmount(Double price) {
        return BigDecimal.valueOf(price).setScale(2, RoundingMode.HALF_EVEN);
    }

    private TransactionResponse makeTransactionRequest(TransactionRequestType txnRequest) {
        CreateTransactionRequest apiRequest = new CreateTransactionRequest();
        apiRequest.setTransactionRequest(txnRequest);
        CreateTransactionController controller = new CreateTransactionController(apiRequest);
        controller.execute();

        CreateTransactionResponse response = controller.getApiResponse();
        if (response == null) {
            logger.info("Failed to get Response from authorize.net transaction request");
            throw new RuntimeException("Something went wrong. Try again later");
        }
        if (response.getMessages().getResultCode() != MessageTypeEnum.OK) {
            logger.error("Failed Transaction in section 2");
            if (response.getTransactionResponse() != null && response.getTransactionResponse().getErrors() != null) {
                logger.error("Error message 1 {}", response.getTransactionResponse().getErrors().getError().get(0).getErrorText());
                logger.error("Error Code: {}", response.getTransactionResponse().getErrors().getError().get(0).getErrorCode());
                throw new RuntimeException(response.getTransactionResponse().getErrors().getError().get(0).getErrorText());
            } else {
                logger.error("Error message 2 {}", response.getMessages().getMessage().get(0).getText());
                throw new RuntimeException(response.getMessages().getMessage().get(0).getText());
            }
        }

        TransactionResponse result = response.getTransactionResponse();
        if (result == null || result.getMessages() == null) {
            logger.error("Failed Transaction in section 1");
            if (response.getTransactionResponse().getErrors() != null) {
                logger.error("Error Code: {}", response.getTransactionResponse().getErrors().getError().get(0).getErrorCode());
                logger.error("Error Message: {}", response.getTransactionResponse().getErrors().getError().get(0).getErrorText());
                throw new RuntimeException(response.getTransactionResponse().getErrors().getError().get(0).getErrorText());
            }
            throw new RuntimeException("Something went wrong. Try again later");
        }

        return result;
    }

    private CreateCustomerProfileRequest buildCreateCustomerProfileApiRequest(CustomerProfileType customerProfileType) {
        CreateCustomerProfileRequest apiRequest = new CreateCustomerProfileRequest();
        apiRequest.setMerchantAuthentication(merchantAuthenticationType);
        apiRequest.setProfile(customerProfileType);
        return apiRequest;
    }

    private CustomerProfileType buildCustomerProfileType(CustomerPaymentProfileType customerPaymentProfileType) {
        CustomerProfileType customerProfileType = new CustomerProfileType();
        customerProfileType.setDescription("Profile description");
        customerProfileType.getPaymentProfiles().add(customerPaymentProfileType);
        return customerProfileType;
    }

    private CustomerPaymentProfileType buildCustomerPaymentProfile(Address billingAddress, PaymentType paymentType) {
        CustomerPaymentProfileType customerPaymentProfileType = new CustomerPaymentProfileType();
        customerPaymentProfileType.setCustomerType(CustomerTypeEnum.INDIVIDUAL);
        customerPaymentProfileType.setPayment(paymentType);
        customerPaymentProfileType.setBillTo(buildCustomerAddressType(billingAddress));
        return customerPaymentProfileType;
    }

    private CustomerAddressType buildCustomerAddressType(Address billingAddress) {
        CustomerAddressType customerAddress = new CustomerAddressType();
        customerAddress.setFirstName(billingAddress.getFirstName());
        customerAddress.setLastName(billingAddress.getLastName());
        customerAddress.setAddress(billingAddress.getAddress());
        customerAddress.setCity(billingAddress.getCity());
        customerAddress.setState(billingAddress.getState());
        customerAddress.setZip(billingAddress.getZip());
        return customerAddress;
    }

    private CreditCardType buildCreditCardType(CreditCard creditCard) {
        CreditCardType creditCardType = new CreditCardType();
        creditCardType.setCardNumber(creditCard.getCardNumber());
        creditCardType.setExpirationDate(creditCard.getExpMonth() + creditCard.getExpYear());
        creditCardType.setCardCode(String.valueOf(creditCard.getCvv()));
        return creditCardType;
    }


}
