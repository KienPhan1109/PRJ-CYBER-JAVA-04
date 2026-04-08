package com.cyber.exception;

class InsufficientBalanceException extends BusinessException {
    public InsufficientBalanceException(String message) { super("ERR_INSUFFICIENT_BALANCE", message); }
}