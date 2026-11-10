package com.example.batchdemo.repo;

import com.example.batchdemo.entity.Customer;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface CustomerRepository extends JpaRepository<Customer, Long> {

    // One query per chunk to find which incoming rows already exist (upsert)
    List<Customer> findByEmailIn(Collection<String> emails);
}
