package com.liv.cryptomodule.controllers;

import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

// This is a test controller to verify that the container was deployed correctly
@RestController
@CrossOrigin(origins = "*")
public class HelloWorldController {
    @GetMapping("/hello")
    public String sayHello() {
        System.out.println("Hello from console!");
        return "Hello World!";
    }
}
