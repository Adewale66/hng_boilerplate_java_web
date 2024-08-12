package hng_java_boilerplate.payment;

import com.stripe.model.Product;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RequestDTO {
    String[] items;
    String customerName;
    String customerEmail;


}
