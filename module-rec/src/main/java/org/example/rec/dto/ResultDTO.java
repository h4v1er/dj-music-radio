package org.example.rec.dto;

/**
 * 统一响应包装 — 对应 { "code": 200, "msg": "success", "data": {} }
 * 用于解析其他微服务返回的 JSON
 */
public class ResultDTO<T> {

    private Integer code;
    private String msg;
    private T data;

    public Integer getCode() {
        return code;
    }

    public void setCode(Integer code) {
        this.code = code;
    }

    public String getMsg() {
        return msg;
    }

    public void setMsg(String msg) {
        this.msg = msg;
    }

    public T getData() {
        return data;
    }

    public void setData(T data) {
        this.data = data;
    }

    /** 判断是否成功返回 */
    public boolean isSuccess() {
        return code != null && code == 200;
    }
}
