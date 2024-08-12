package hng_java_boilerplate.payment;

import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.model.*;
import com.stripe.model.checkout.Session;
import com.stripe.net.RequestOptions;
import com.stripe.param.CustomerCreateParams;
import com.stripe.param.CustomerSearchParams;
import com.stripe.param.ProductCreateParams;
import com.stripe.param.checkout.SessionCreateParams;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.view.RedirectView;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class PaymentController {

    private final HttpServletRequest request;

    @PostMapping("/payments")
    public String test(@RequestBody RequestDTO requestDTO) throws StripeException {
        System.out.println(BigDecimal.valueOf(19.99));
        BigDecimal t = BigDecimal.ONE;
        Stripe.apiKey = "sk_test_51PmcZkP5BIJwwIPn3vi9Y26hUyJVYnClJEqmmCQOc8lw2Kj01xthLwEdHRGBZUqlSPlLAGU7G51Vd8AEPbtAgV1R00ixN3DlHS";
        Product product = new Product();
        Price price = new Price();
        price.setCurrency("usd");
        price.setUnitAmountDecimal(BigDecimal.valueOf(1000_00));
        product.setName("Playstation");
        product.setId("23");
        product.setDefaultPriceObject(price);
        String clientBaseURL = "http://localhost:8080";

        ProductCreateParams.builder()
                .setName("Gold plan")
                .setUnitLabel()
                .setDefaultPriceData(
                ProductCreateParams.DefaultPriceData.builder()
                        .setCurrency("")
                        .setRecurring(
                                ProductCreateParams.DefaultPriceData.Recurring.builder()
                                                .setInterval(ProductCreateParams.DefaultPriceData.Recurring.Interval.MONTH).build()
                        )
                        .build()
        ).build()

        // Start by finding an existing customer record from Stripe or creating a new one if needed
        Customer customer = CustomerUtil.findOrCreateCustomer(requestDTO.getCustomerEmail(), requestDTO.getCustomerName());

        // Next, create a checkout session by adding the details of the checkout
        SessionCreateParams.Builder paramsBuilder =
                SessionCreateParams.builder()
                        .setMode(SessionCreateParams.Mode.PAYMENT)
                        .setCustomer(customer.getId())
                        .setSuccessUrl(clientBaseURL + "/success?session_id={CHECKOUT_SESSION_ID}")
                        .setCancelUrl(clientBaseURL + "/failure")
                        .addAllPaymentMethodType(List.of(
                                SessionCreateParams.PaymentMethodType.AMAZON_PAY,
                                SessionCreateParams.PaymentMethodType.CASHAPP,
                                SessionCreateParams.PaymentMethodType.CARD
                        ))
                        .addLineItem(
                                SessionCreateParams.LineItem.builder()
                                        .setQuantity(1L)
                                        .setPrice("")
                                        .setPriceData(
                                                SessionCreateParams.LineItem.PriceData.builder()
                                                        .setProductData(
                                                                SessionCreateParams.LineItem.PriceData.ProductData.builder()
                                                                        .putMetadata("app_id", product.getId())
                                                                        .setName(product.getName())
                                                                        .build()
                                                        )
                                                        .setCurrency(product.getDefaultPriceObject().getCurrency())
                                                        .setUnitAmountDecimal(product.getDefaultPriceObject().getUnitAmountDecimal())
                                                        .build()
                                        ).build()
                        );

        Session session = Session.create(paramsBuilder.build());

        return session.getUrl();

    }

    @PostMapping("/webhhook")
    public String webhook(@RequestBody Event event) {
        System.out.println(event);
        System.out.println(event.getType());
        return "yes";
    }

    @GetMapping("/success")
    public RedirectView handleSuccess(@RequestParam("session_id") String checkout_id) throws StripeException {
        Session session = Session.retrieve(checkout_id);
        System.out.println(session.getPaymentStatus());
        return new RedirectView("/success");
    }
}

class CustomerUtil {
    public static Customer findCustomerByEmail(String email) throws StripeException {
        CustomerSearchParams params =
                CustomerSearchParams
                        .builder()
                        .setQuery("email:'" + email + "'")
                        .build();

        CustomerSearchResult result = Customer.search(params);

        return result.getData().size() > 0 ? result.getData().get(0) : null;
    }

    public static Customer findOrCreateCustomer(String email, String name) throws StripeException {
        CustomerSearchParams params =
                CustomerSearchParams
                        .builder()
                        .setQuery("email:'" + email + "'")
                        .build();

        CustomerSearchResult result = Customer.search(params);

        Customer customer;

        // If no existing customer was found, create a new record
        if (result.getData().size() == 0) {

            CustomerCreateParams customerCreateParams = CustomerCreateParams.builder()
                    .setName(name)
                    .setEmail(email)
                    .build();
            RequestOptions options = new RequestOptions.RequestOptionsBuilder().setIdempotencyKey(UUID.randomUUID().toString()).build();
            customer = Customer.create(customerCreateParams, options);
        } else {
            customer = result.getData().get(0);
        }

        return customer;
    }
}