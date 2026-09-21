package org.example.observability_app.service.imp;

import io.micrometer.observation.annotation.Observed;
import org.example.observability_app.entity.Product;
import org.example.observability_app.repository.ProductRepo;
import org.example.observability_app.service.ProductService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

@Service
@Observed(name = "product.service")
public class ProductServiceImpl implements ProductService {

    private final ProductRepo productRepo;

    public ProductServiceImpl(ProductRepo productRepo) {
        this.productRepo = productRepo;
    }

    @Override
    public List<Product> getAllProduct() {
        return productRepo.findAll();
    }

    @Override
    public Product getById(UUID id) {
        return productRepo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "product not found"));
    }

    @Override
    public Product create(Product product) {
        product.setId(null);
        return productRepo.save(product);
    }
}
