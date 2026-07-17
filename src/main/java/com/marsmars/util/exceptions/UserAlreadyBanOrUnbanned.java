package com.marsmars.util.exceptions;

public class UserAlreadyBanOrUnbanned extends RuntimeException {
    public UserAlreadyBanOrUnbanned(String message) {
        super(message);
    }
}
