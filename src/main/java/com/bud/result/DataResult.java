package com.bud.result;

import java.util.logging.Level;

import com.hypixel.hytale.builtin.hytalegenerator.LoggerUtil;

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
        if (isSuccess()) {
            LoggerUtil.getLogger().log(Level.FINER, "[BUD] Success: ", this.message);
        } else {
            LoggerUtil.getLogger().log(Level.WARNING, "[BUD] Error: ", this.message);
        }
    }
}
