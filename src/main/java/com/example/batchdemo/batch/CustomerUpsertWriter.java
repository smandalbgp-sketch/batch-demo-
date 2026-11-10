package com.example.batchdemo.batch;

import com.example.batchdemo.entity.Customer;
import com.example.batchdemo.repo.CustomerRepository;
import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ItemWriter;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Upsert writer. Runs inside the chunk transaction, so:
 *  - entities loaded here are MANAGED -> changing them issues UPDATEs (batched on flush)
 *  - saveAll() on new entities issues INSERTs (batched on flush)
 * Hibernate flushes once when the chunk transaction commits.
 */
@Component
public class CustomerUpsertWriter implements ItemWriter<Customer> {

    private final CustomerRepository repository;

    public CustomerUpsertWriter(CustomerRepository repository) {
        this.repository = repository;
    }

    @Override
    public void write(Chunk<? extends Customer> chunk) {
        // De-duplicate inside the chunk (last row for an email wins)
        Map<String, Customer> incomingByEmail = new LinkedHashMap<>();
        for (Customer c : chunk) {
            incomingByEmail.put(c.getEmail(), c);
        }

        // 1 SELECT ... WHERE email IN (...) for the whole chunk
        Map<String, Customer> existing = repository.findByEmailIn(incomingByEmail.keySet())
                .stream()
                .collect(Collectors.toMap(Customer::getEmail, Function.identity()));

        List<Customer> toInsert = new ArrayList<>();
        for (Customer incoming : incomingByEmail.values()) {
            Customer db = existing.get(incoming.getEmail());
            if (db != null) {
                db.setName(incoming.getName());   // dirty checking -> UPDATE
                db.setCity(incoming.getCity());
            } else {
                toInsert.add(incoming);           // new row -> INSERT
            }
        }
        repository.saveAll(toInsert);
    }
}
