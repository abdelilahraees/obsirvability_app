package org.example.observability_app.config;

import org.example.observability_app.entity.Product;
import org.example.observability_app.repository.ProductRepo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class DataSeed {

    private static final Logger log = LoggerFactory.getLogger(DataSeed.class);

    @Bean
    CommandLineRunner seedProducts(ProductRepo repo) {
        return args -> {
            if (repo.count() > 0) {
                return;
            }
            List<Product> products = List.of(
                    build("Keyboard", "hardware", 79.90, 25),
                    build("Mouse", "hardware", 29.50, 50),
                    build("Monitor 27\"", "hardware", 249.00, 10),
                    build("USB-C Cable", "accessory", 12.90, 200),
                    build("Coffee Mug", "goodies", 9.00, 100));
            repo.saveAll(products);
            log.info("seeded {} products", products.size());
        };
    }

    private static Product build(String name, String category, double price, int stock) {
        Product p = new Product();
        p.setName(name);
        p.setCategory(category);
        p.setPrice(price);
        p.setStock(stock);
        return p;
    }
}
