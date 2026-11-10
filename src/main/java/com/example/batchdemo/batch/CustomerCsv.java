package com.example.batchdemo.batch;

/** One raw line from the uploaded CSV: name,email,city */
public record CustomerCsv(String name, String email, String city) {
}
