package com.bud.result;

public class SuccessResult implements IResult {

    private final String message;

    public SuccessResult(String message) {
        this.message = message;
    }

    @Override
    public boolean isSuccess() {
        return true;
    }

    @Override
    public void printResult() {
        System.out.println("[BUD] Success: " + this.message);
    }

}
