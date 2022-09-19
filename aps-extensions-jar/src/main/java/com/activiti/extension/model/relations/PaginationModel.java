package com.activiti.extension.model.relations;

import javax.validation.constraints.Min;

/**
 * @author Incentro.
 * Class representing a pagination request and response body received and responded in a call that supports pagination.
 */

public class PaginationModel {

    @Min(value=0, message = "skipCount parameter can not be smaller than 0")
    private int skipCount;
    @Min(value=-1, message = "maxItems parameter can not be smaller than -1")
    private int maxItems;
    private String sortField;
    private Boolean ascending;
    private Boolean hasMoreItems;
    private int totalItems;
    private int count;

    public PaginationModel(Integer skipCount, Integer maxItems, String sortField, Boolean ascending) {
        this.skipCount = skipCount;
        this.maxItems = maxItems;
        this.sortField = sortField;
        this.ascending = ascending;
    }

    public PaginationModel() {
    }

    public int getSkipCount() {
        return skipCount;
    }

    public void setSkipCount(int skipCount) {
        this.skipCount = skipCount;
    }

    public String getSortField() {
        return sortField;
    }

    public void setSortField(String sortField) {
        this.sortField = sortField;
    }

    public Boolean getAscending() {
        return ascending;
    }

    public void setAscending(Boolean ascending) {
        this.ascending = ascending;
    }

    public int getMaxItems() {
        return maxItems;
    }

    public void setMaxItems(int maxItems) {
        this.maxItems = maxItems;
    }

    public Boolean getHasMoreItems() {
        return hasMoreItems;
    }

    public void setHasMoreItems(Boolean hasMoreItems) {
        this.hasMoreItems = hasMoreItems;
    }

    public int getTotalItems() {
        return totalItems;
    }

    public void setTotalItems(int totalItems) {
        this.totalItems = totalItems;
    }

    public int getCount() {
        return count;
    }

    public void setCount(int count) {
        this.count = count;
    }
}
