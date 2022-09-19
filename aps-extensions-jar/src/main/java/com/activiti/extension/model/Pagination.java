package com.activiti.extension.model;

import lombok.Data;

@Data
public class Pagination {

    private int maxItems;
    private int skipCount;

    public Pagination(int maxItems, int skipCount) {
        this.maxItems = maxItems;
        this.skipCount = skipCount;
    }

    public static Pagination getDefaultPagination() {
        return new Pagination(25, 0);
    }

    public static Pagination getPagination(int maxItems, int skipCount) {
        return new Pagination(maxItems, skipCount);
    }
}
