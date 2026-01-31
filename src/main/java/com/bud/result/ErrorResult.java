package com.bud.result;

public class ErrorResult implements IResult {

    private final String message;

    public ErrorResult(String errorMessage) {
        this.message = errorMessage;
    }

    @Override
    public boolean isSuccess() {
        return false;
    }

    @Override
    public String getMessage() {
        return this.message;
    }

    @Override
    public void printResult() {
        System.err.println("[BUD] Error: " + this.message);
    }

}
