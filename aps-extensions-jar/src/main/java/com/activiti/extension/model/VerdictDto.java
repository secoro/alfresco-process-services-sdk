package com.activiti.extension.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.activiti.engine.impl.util.json.JSONObject;

import java.time.ZonedDateTime;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class VerdictDto {

    private String approvalType;
    private String verdict;
    private String userName;
    private String fullName;
    private String comment;
    private ZonedDateTime timestamp;

    public JSONObject toVerdictJSONObject() {
        return new JSONObject(this);
    }
}
