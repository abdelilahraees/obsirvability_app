package org.example.observability_app.service;

import org.example.observability_app.entity.Cart;

import java.util.UUID;

public interface CartService {
    Cart getOrCreate(String email);

    Cart addItem(String email, UUID productId, int quantity);

    Cart removeItem(String email, UUID productId);

    Cart checkout(String email);
}
