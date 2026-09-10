package com.koda.ecommerce.user.utils;

public final class ErrorCodes {

    public static final String VALIDATION_FAILED = "VALIDATION_FAILED";
    public static final String INTERNAL_SERVER_ERROR = "INTERNAL_SERVER_ERROR";
    public static final String UNAUTHORIZED = "UNAUTHORIZED";
    public static final String BAD_CREDENTIALS = "BAD_CREDENTIALS";
    public static final String ACCOUNT_BLOCKED = "ACCOUNT_BLOCKED";
    public static final String RATE_LIMITED = "RATE_LIMITED";
    public static final String EMAIL_ALREADY_EXISTS = "EMAIL_ALREADY_EXISTS";
    public static final String MOBILE_ALREADY_EXISTS = "MOBILE_ALREADY_EXISTS";
    public static final String NOT_FOUND = "NOT_FOUND";
    public static final String METHOD_NOT_ALLOWED = "METHOD_NOT_ALLOWED";

    private ErrorCodes() {
    }
}