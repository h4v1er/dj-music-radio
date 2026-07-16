package org.example.user.common;

import lombok.Data;

@Data
public class Result<T> {

    private Integer code;

    private String message;

    private T data;

    public static <T> Result<T> success(T data) {
        Result<T> r = new Result<>();
        r.code = 200;
        r.message = "success";
        r.data = data;
        return r;
    }

    public static Result<?> success() {
        Result<Object> r = new Result<>();
        r.code = 200;
        r.message = "success";
        return r;
    }

    public static Result<?> error(String msg) {
        Result<Object> r = new Result<>();
        r.code = 500;
        r.message = msg;
        return r;
    }
}