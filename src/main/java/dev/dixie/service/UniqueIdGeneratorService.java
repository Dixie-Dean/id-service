package dev.dixie.service;

import org.springframework.stereotype.Service;

import java.util.Base64;
import java.util.Random;

@Service
public class UniqueIdGeneratorService implements IdService {

    private static final int BYTE_LENGTH = 6;

    @Override
    public String generateId() {
        byte[] randomBytes = new byte[BYTE_LENGTH];
        new Random().nextBytes(randomBytes);
        return Base64.getUrlEncoder().encodeToString(randomBytes);
    }
}
