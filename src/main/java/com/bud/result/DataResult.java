package com.bud.result;

public class DataResult<T> implements IDataResult<T> {

    private final T data;
    private final String message;

    public DataResult(T data, String message) {
        this.data = data;
        this.message = message;
    }

    @Override
    public boolean isSuccess() {
        return this.data != null;
    }

    @Override
    public String getMessage() {
        return this.message;
    }

    @Override
    public T getData() {
        return this.data;
    }

    @Override
    public void printResult() {
        System.out.println("[BUD] Result: " + this.message);
    }
}
