package com.bud.result;

import java.util.List;

public interface IDataListResult<T> extends IResult {
    List<T> getDataList();
}