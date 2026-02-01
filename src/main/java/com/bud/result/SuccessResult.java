package com.bud.result;

import java.util.logging.Level;

import com.hypixel.hytale.builtin.hytalegenerator.LoggerUtil;

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
    public String getMessage() {
        return this.message;
    }

    @Override
    public void printResult() {
        LoggerUtil.getLogger().log(Level.FINER, "[BUD] Success: ", this.message);
    }
}
