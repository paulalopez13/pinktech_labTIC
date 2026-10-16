package uy.edu.um.proyTic2026.services;

import org.springframework.http.HttpStatus;
import lombok.Getter;

@Getter
public class LoginException extends RuntimeException {
    private final HttpStatus status;

    public LoginException(HttpStatus status, String message) {
        super(message);
        this.status = status;
    }
}
