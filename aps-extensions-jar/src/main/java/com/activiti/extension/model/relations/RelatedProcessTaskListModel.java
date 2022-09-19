package com.activiti.extension.model.relations;

import java.util.List;

/**
 * @author Incentro.
 * Class representing a GET response model of a list of tasks that are related to processes that are related to a dossier
 */

public class RelatedProcessTaskListModel {

    private List<RelatedProcessTaskModel> relations;
    private PaginationModel pagination;

    public List<RelatedProcessTaskModel> getRelations() {
        return relations;
    }

    public void setRelations(List<RelatedProcessTaskModel> relations) {
        this.relations = relations;
    }

    public PaginationModel getPagination() {
        return pagination;
    }

    public void setPagination(PaginationModel pagination) {
        this.pagination = pagination;
    }
}
