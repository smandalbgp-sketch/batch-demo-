package com.example.batchdemo.batch;

import com.example.batchdemo.entity.Customer;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.stereotype.Component;

@Component
public class CustomerProcessor implements ItemProcessor<CustomerCsv, Customer> {

    @Override
    public Customer process(CustomerCsv item) {
        String email = item.email();
        // Returning null filters the row out (counted as "filterCount")
        if (email == null || !email.contains("@")) {
            return null;
        }
        return new Customer(item.name(), email.toLowerCase(), item.city());
    }
}
