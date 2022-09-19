package com.activiti.extension.model;

import lombok.*;
import org.activiti.engine.impl.util.json.JSONObject;

@Getter
@Setter
public class Comment {

    private String user;
    private String date;
    private String comment;
    private String type;

    public Comment() {
    }

    public Comment(String user, String date, String comment) {
        this.user = user;
        this.date = date;
        this.comment = comment;
    }

    public Comment(String user, String date, String comment, String type) {
        this.user = user;
        this.date = date;
        this.comment = comment;
        this.type = type;
    }

    public JSONObject toCommentJSONObject() {
        return new JSONObject(this);
    }
}
