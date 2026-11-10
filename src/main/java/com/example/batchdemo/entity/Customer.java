package com.example.batchdemo.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "customers", uniqueConstraints = @UniqueConstraint(columnNames = "email"))
public class Customer {

    // SEQUENCE (not IDENTITY!) is required for Hibernate to batch inserts.
    // allocationSize should match hibernate.jdbc.batch_size.
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "customer_seq")
    @SequenceGenerator(name = "customer_seq", sequenceName = "customer_seq", allocationSize = 50)
    private Long id;

    private String name;

    @Column(nullable = false)
    private String email;

    private String city;

    protected Customer() {
    }

    public Customer(String name, String email, String city) {
        this.name = name;
        this.email = email;
        this.city = city;
    }

    public Long getId() { return id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getCity() { return city; }
    public void setCity(String city) { this.city = city; }
}
