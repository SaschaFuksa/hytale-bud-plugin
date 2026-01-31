package com.bud.result;

import java.util.List;
import java.util.Set;

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
        return this.dataList != null;
    }

    @Override
    public List<T> getDataList() {
        return this.dataList;
    }

    @Override
    public void printResult() {
        System.out.println("[BUD] Result: " + this.message);
    }
}
