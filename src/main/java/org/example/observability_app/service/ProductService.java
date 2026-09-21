package org.example.observability_app.service;

import org.example.observability_app.entity.Product;

import java.util.List;
import java.util.UUID;

public interface ProductService {
    List<Product> getAllProduct();

    Product getById(UUID id);

    Product create(Product product);
}
