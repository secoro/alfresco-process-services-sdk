package com.activiti.extension.api.tasks.model;

import java.util.List;

public enum Assignment {
    assignee, candidate, involved, completed;

    public boolean isPresent(List<String> assignments) {
        return assignments.contains(this.name());
    }
}
