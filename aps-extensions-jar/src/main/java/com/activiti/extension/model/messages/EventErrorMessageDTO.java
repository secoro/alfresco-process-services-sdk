package com.activiti.extension.model.messages;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class EventErrorMessageDTO<T> extends EventMessageDTO<T> {

    private String errorMessage;

    public EventErrorMessageDTO(EventMessageDTO eventMessageDTO, String errorMessage) {
        super(eventMessageDTO.getId(),
                eventMessageDTO.getEvent(),
                eventMessageDTO.getSource(),
                eventMessageDTO.getSender(),
                (T) eventMessageDTO.getData());
        this.errorMessage = errorMessage;
    }
}
