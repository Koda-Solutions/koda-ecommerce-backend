package com.koda.ecommerce.user.utils;

import lombok.Getter;

@Getter
public class ReturnObject<T> {

    private String message;
    private boolean status;
    private T data;
    private String errorCode;

    private ReturnObject() {
    }

    public static <T> ReturnObject<T> ok(String message, T data) {
        ReturnObject<T> body = new ReturnObject<>();
        body.message = message;
        body.status = true;
        body.data = data;
        return body;
    }

    public static <T> ReturnObject<T> fail(String message, String errorCode) {
        ReturnObject<T> body = new ReturnObject<>();
        body.message = message;
        body.status = false;
        body.errorCode = errorCode;
        return body;
    }
}