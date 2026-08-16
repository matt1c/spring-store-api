package com.marsmars.controllers;

import com.marsmars.util.exceptions.InsufficientStockException;
import com.marsmars.util.exceptions.OrderNotFound;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/test")
public class TestExceptionClass {
    @GetMapping("/not-found")
    public void throwNotFound() {
        throw new OrderNotFound("not found");
    }

    @GetMapping("/conflict")
    public void throwConflict() {
        throw new InsufficientStockException("conflict test");
    }

    @GetMapping("/server-error")
    public void throwServerError() {
        throw new NullPointerException("server error test");
    }
}
