package org.example.observability_app.service.imp;

import org.example.observability_app.entity.Product;
import org.example.observability_app.repository.ProductRepo;
import org.example.observability_app.service.ProductService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ProductServiceImpl implements ProductService {
    private ProductRepo productRepo;


    public ProductServiceImpl(ProductRepo productRepo) {
        this.productRepo = productRepo;
    }

    @Override
    public List<Product> getAllProduct() {
        return productRepo.findAll();
    }
}
