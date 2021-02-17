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

    /*@GetMapping("/notify/{email:.+}")
    public void sendTestEmail(@PathVariable String email) throws IOException, IllegalStateException {
        try {
            EmailService.sendEmail(email, "kostyanich7@gmail.com",
                    "TestSubject", MailContentBuilder.generateMailContent(
                            new EmailPayload("Test Mail",
                                    "kostiantyn.nechvolod@gmail.com",
                                    "Konstantin",
                                    "Nechvolod",
                                    "http://18.192.22.193:8080/ipfs/QmaNxbQNrJdLzzd8CKRutBjMZ6GXRjvuPepLuNSsfdeJRJ")
                    ));
        } catch (NullPointerException e) {
            throw new IllegalStateException("JSON payload structure is incorrect");
        }
    }*/
}
