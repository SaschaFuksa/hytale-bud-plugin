package com.bud.result;

import java.util.logging.Level;

import com.hypixel.hytale.builtin.hytalegenerator.LoggerUtil;

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
        LoggerUtil.getLogger().log(Level.WARNING, "[BUD] Error: ", this.message);
    }

}
