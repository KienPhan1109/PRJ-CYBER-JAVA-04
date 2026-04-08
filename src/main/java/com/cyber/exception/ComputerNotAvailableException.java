package com.cyber.exception;

class ComputerNotAvailableException extends BusinessException {
    public ComputerNotAvailableException(String message) { super("ERR_COMPUTER_NOT_AVAILABLE", message); }
}