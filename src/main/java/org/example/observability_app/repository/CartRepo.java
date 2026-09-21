package org.example.observability_app.repository;

import org.example.observability_app.entity.Cart;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface CartRepo extends JpaRepository<Cart, UUID> {
    Optional<Cart> findByUserEmail(String email);
}
