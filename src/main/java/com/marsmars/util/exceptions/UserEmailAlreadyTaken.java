package com.marsmars.util.exceptions;

public class UserEmailAlreadyTaken extends RuntimeException {
    public UserEmailAlreadyTaken(String message) {
        super(message);
    }
}
