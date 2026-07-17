package org.example.user.common;

public class Result<T> {

    private Integer code;
    private String message;
    private T data;

    public Integer getCode() {
        return code;
    }

    public void setCode(Integer code) {
        this.code = code;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public T getData() {
        return data;
    }

    public void setData(T data) {
        this.data = data;
    }

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
        return error(500, msg);
    }

    public static Result<?> error(Integer code, String msg) {
        Result<Object> r = new Result<>();
        r.code = code;
        r.message = msg;
        return r;
    }
}
