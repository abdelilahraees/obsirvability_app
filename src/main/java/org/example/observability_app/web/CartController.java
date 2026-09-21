package org.example.observability_app.web;

import io.micrometer.observation.annotation.Observed;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import org.example.observability_app.entity.Cart;
import org.example.observability_app.service.CartService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("cart")
@PreAuthorize("hasRole('USER')")
public class CartController {

    private final CartService cartService;

    public CartController(CartService cartService) {
        this.cartService = cartService;
    }

    public record AddItemRequest(@NotNull UUID productId, @Positive int quantity) {}

    @GetMapping
    @Observed(name = "cart.get")
    public Cart getCart(Authentication auth) {
        return cartService.getOrCreate(auth.getName());
    }

    @PostMapping("/items")
    @Observed(name = "cart.addItem")
    public Cart addItem(@Valid @RequestBody AddItemRequest req, Authentication auth) {
        return cartService.addItem(auth.getName(), req.productId(), req.quantity());
    }

    @DeleteMapping("/items/{productId}")
    @Observed(name = "cart.removeItem")
    public Cart removeItem(@PathVariable UUID productId, Authentication auth) {
        return cartService.removeItem(auth.getName(), productId);
    }

    @PostMapping("/checkout")
    @Observed(name = "cart.checkout")
    public Cart checkout(Authentication auth) {
        return cartService.checkout(auth.getName());
    }
}
