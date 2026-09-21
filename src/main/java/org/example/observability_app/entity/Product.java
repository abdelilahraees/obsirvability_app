package org.example.observability_app.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.Data;

import java.util.UUID;

@Entity
@Data
public class Product {
    @GeneratedValue(strategy = GenerationType.UUID)
    @Id
    private UUID id;

    private String name;

    private double price;

    private String category;

    private int stock;
}
