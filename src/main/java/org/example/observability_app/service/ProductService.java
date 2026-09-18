package org.example.observability_app.service;


import org.example.observability_app.entity.Product;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public interface ProductService {

    List<Product> getAllProduct();
}
