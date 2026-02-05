package com.bud.result;

import java.util.List;
import com.hypixel.hytale.builtin.hytalegenerator.LoggerUtil;
import java.util.logging.Level;

/**
 * A result representing an asynchronous operation that has successfully
 * started.
 * isSuccess() always returns true.
 */
public class AsyncDataListResult<T> implements IDataListResult<T> {

    private final List<T> dataList;
    private final String message;

    public AsyncDataListResult(List<T> dataList, String message) {
        this.dataList = dataList;
        this.message = message;
    }

    @Override
    public List<T> getDataList() {
        return this.dataList;
    }

    @Override
    public String getMessage() {
        return this.message;
    }

    @Override
    public boolean isSuccess() {
        return true; // Always true because the async process started successfully
    }

    @Override
    public void printResult() {
        LoggerUtil.getLogger().log(Level.INFO, "[BUD] Async Process Started: " + this.message);
    }
}
