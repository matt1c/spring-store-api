package com.marsmars.util.exceptions;

public class UserPasswordIsAlreadyValid extends RuntimeException {
    public UserPasswordIsAlreadyValid(String message) {
        super(message);
    }
}
