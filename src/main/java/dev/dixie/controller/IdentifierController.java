package dev.dixie.controller;

import dev.dixie.service.IdService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/id")
@RequiredArgsConstructor
public class IdentifierController {

    private final IdService idService;

    @GetMapping("/generate")
    public ResponseEntity<String> generateID() {
        var id = idService.generateId();
        return new ResponseEntity<>(id, HttpStatus.OK);
    }
}