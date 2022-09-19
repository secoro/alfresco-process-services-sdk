package com.activiti.extension.api.user.model;

import com.activiti.domain.idm.User;
import com.activiti.model.idm.AbstractUserRepresentation;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

/**
 * Custom implementation of LightUserRepresentation that adds more properties.
 */
public class CustomLightUserRepresentation extends AbstractUserRepresentation {

    protected String fullName;

    public CustomLightUserRepresentation(User user) {
        super(user);
    }

    public CustomLightUserRepresentation() {
    }

    @JsonInclude(Include.NON_NULL)
    public String getExternalId() {
        return externalId;
    }

    @JsonInclude(Include.NON_NULL)
    public Long getPictureId() {
        return pictureId;
    }

    @JsonInclude(Include.NON_NULL)
    public String getCompany() {
        return company;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }
}
