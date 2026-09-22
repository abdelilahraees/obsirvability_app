package org.example.observability_app.service.imp;

import io.micrometer.observation.annotation.Observed;
import jakarta.transaction.Transactional;
import org.example.observability_app.config.SpanTagger;
import org.example.observability_app.entity.Cart;
import org.example.observability_app.entity.CartItem;
import org.example.observability_app.entity.Product;
import org.example.observability_app.entity.User;
import org.example.observability_app.repository.CartRepo;
import org.example.observability_app.repository.ProductRepo;
import org.example.observability_app.repository.UserRepo;
import org.example.observability_app.service.CartService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

@Service
@Observed(name = "cart.service")
@Transactional
public class CartServiceImpl implements CartService {

    private static final Logger log = LoggerFactory.getLogger(CartServiceImpl.class);

    private final CartRepo cartRepo;
    private final UserRepo userRepo;
    private final ProductRepo productRepo;
    private final SpanTagger span;

    public CartServiceImpl(CartRepo cartRepo, UserRepo userRepo, ProductRepo productRepo, SpanTagger span) {
        this.cartRepo = cartRepo;
        this.userRepo = userRepo;
        this.productRepo = productRepo;
        this.span = span;
    }

    @Override
    public Cart getOrCreate(String email) {
        return cartRepo.findByUserEmail(email).orElseGet(() -> {
            User user = userRepo.findByEmail(email)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "user not found"));
            Cart cart = new Cart();
            cart.setUser(user);
            Cart saved = cartRepo.save(cart);
            log.info("cart created id={} user={}", saved.getId(), email);
            return saved;
        });
    }

    @Override
    public Cart addItem(String email, UUID productId, int quantity) {
        span.tag("user.email", email);
        span.tag("product.id", productId.toString());
        span.tag("qty", quantity);
        if (quantity <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "quantity must be > 0");
        }
        Cart cart = getOrCreate(email);
        Product product = productRepo.findById(productId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "product not found"));
        span.tag("product.name", product.getName());

        CartItem existing = cart.getItems().stream()
                .filter(i -> i.getProduct().getId().equals(productId))
                .findFirst()
                .orElse(null);

        if (existing != null) {
            existing.setQuantity(existing.getQuantity() + quantity);
        } else {
            CartItem item = new CartItem();
            item.setCart(cart);
            item.setProduct(product);
            item.setQuantity(quantity);
            cart.getItems().add(item);
        }
        log.info("cart add user={} product={} qty={}", email, product.getName(), quantity);
        return cartRepo.save(cart);
    }

    @Override
    public Cart removeItem(String email, UUID productId) {
        Cart cart = getOrCreate(email);
        boolean removed = cart.getItems().removeIf(i -> i.getProduct().getId().equals(productId));
        if (!removed) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "item not in cart");
        }
        log.info("cart remove user={} product={}", email, productId);
        return cartRepo.save(cart);
    }

    @Override
    public Cart checkout(String email) {
        span.tag("user.email", email);
        Cart cart = getOrCreate(email);
        if (cart.getItems().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "cart is empty");
        }
        double total = 0;
        for (CartItem item : cart.getItems()) {
            Product p = item.getProduct();
            if (p.getStock() < item.getQuantity()) {
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "not enough stock for " + p.getName());
            }
            p.setStock(p.getStock() - item.getQuantity());
            productRepo.save(p);
            total += p.getPrice() * item.getQuantity();
        }
        int count = cart.getItems().size();
        span.tag("cart.items", count);
        span.tag("cart.total", String.valueOf(total));
        cart.getItems().clear();
        cartRepo.save(cart);
        log.info("checkout ok user={} items={} total={}", email, count, total);
        return cart;
    }
}
