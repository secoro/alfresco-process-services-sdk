package com.activiti.extension.model.messages;

import lombok.Builder;
import lombok.Data;

@Builder
@Data
public class EventMessageDTO<T> {

    private String id;
    private String event;
    private String source;
    private String sender;
    private T data;

    public EventErrorMessageDTO<T> toErrorEventMessage(String errorMessage) {
        return new EventErrorMessageDTO<>(this, errorMessage);
    }
}
