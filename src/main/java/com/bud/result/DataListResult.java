package com.bud.result;

import java.util.List;
import java.util.Set;

import java.util.logging.Level;

import com.hypixel.hytale.builtin.hytalegenerator.LoggerUtil;

public class DataListResult<T> implements IDataListResult<T> {

    private final List<T> dataList;
    private final String message;

    public DataListResult(List<T> dataList, String message) {
        this.dataList = dataList;
        this.message = message;
    }

    public DataListResult(Set<T> dataList, String message) {
        this.dataList = List.copyOf(dataList);
        this.message = message;
    }

    @Override
    public String getMessage() {
        return this.message;
    }

    @Override
    public boolean isSuccess() {
        return this.dataList != null && !this.dataList.isEmpty();
    }

    @Override
    public List<T> getDataList() {
        return this.dataList;
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
