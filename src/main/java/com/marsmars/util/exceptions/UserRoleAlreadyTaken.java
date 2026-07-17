package com.marsmars.util.exceptions;

public class UserRoleAlreadyTaken extends RuntimeException {
    public UserRoleAlreadyTaken(String message) {
        super(message);
    }
}
