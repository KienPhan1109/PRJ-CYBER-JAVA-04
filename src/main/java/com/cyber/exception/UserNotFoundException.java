package com.cyber.exception;

class UserNotFoundException extends BusinessException {
    public UserNotFoundException(String message) { super("ERR_USER_NOT_FOUND", message); }
}