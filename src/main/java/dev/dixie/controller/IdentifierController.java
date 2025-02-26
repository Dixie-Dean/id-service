package dev.dixie.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class IdentifierController {

    @GetMapping("/generate")
    public ResponseEntity<Void> generateID() {
        return null;
    }

}