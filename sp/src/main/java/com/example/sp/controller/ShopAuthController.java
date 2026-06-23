package com.example.sp.controller;

import com.example.sp.dto.shop.ShopCustomerDTO;
import com.example.sp.dto.shop.ShopLoginRequest;
import com.example.sp.dto.shop.ShopRegisterRequest;
import com.example.sp.service.shop.CustomerAuthService;
import com.example.sp.service.shop.ShopSessionKeys;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/shop/auth")
@RequiredArgsConstructor
public class ShopAuthController {

    private final CustomerAuthService customerAuthService;

    @PostMapping("/login")
    public ShopCustomerDTO login(@Valid @RequestBody ShopLoginRequest request, HttpSession session) {
        ShopCustomerDTO customer = customerAuthService.login(request);
        session.setAttribute(ShopSessionKeys.CUSTOMER_ID, customer.getId());
        return customer;
    }

    @PostMapping("/register")
    public ShopCustomerDTO register(@Valid @RequestBody ShopRegisterRequest request, HttpSession session) {
        ShopCustomerDTO customer = customerAuthService.register(request);
        session.setAttribute(ShopSessionKeys.CUSTOMER_ID, customer.getId());
        return customer;
    }

    @GetMapping("/me")
    public ResponseEntity<ShopCustomerDTO> me(HttpSession session) {
        Integer customerId = (Integer) session.getAttribute(ShopSessionKeys.CUSTOMER_ID);
        ShopCustomerDTO customer = customerAuthService.getCurrentCustomer(customerId);
        if (customer == null) {
            session.removeAttribute(ShopSessionKeys.CUSTOMER_ID);
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.ok(customer);
    }

    @PostMapping("/logout")
    public void logout(HttpSession session) {
        session.invalidate();
    }
}
