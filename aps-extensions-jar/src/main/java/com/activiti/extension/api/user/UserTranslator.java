package com.activiti.extension.api.user;

import com.activiti.extension.api.user.model.CustomLightUserRepresentation;
import com.activiti.model.idm.LightUserRepresentation;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Translator service for translating different kinds of user representations to each other.
 */

@Service
public class UserTranslator {

    /**
     * Method for translating a LightUserRepresentation to a CustomLightUserRepresentation.
     * @param lightUserRepresentation the LightUserRepresentation to translate.
     * @return a translated CustomLightUserRepresentation
     */
    public CustomLightUserRepresentation translate (LightUserRepresentation lightUserRepresentation) {
        CustomLightUserRepresentation clur = new CustomLightUserRepresentation();

        clur.setId(lightUserRepresentation.getId());
        clur.setFirstName(lightUserRepresentation.getFirstName());
        clur.setLastName(lightUserRepresentation.getLastName());
        clur.setEmail(lightUserRepresentation.getEmail());
        clur.setExternalId(lightUserRepresentation.getExternalId());
        clur.setCompany(lightUserRepresentation.getCompany());
        clur.setPictureId(lightUserRepresentation.getPictureId());
        clur.setFullName(StringUtils.join(new String[] {
                lightUserRepresentation.getFirstName(),
                lightUserRepresentation.getLastName()
        }, ' '));

        return clur;
    }

    /**
     * Method for translating a List of LightUserRepresentations to a List of CustomLightUserRepresentations.
     * @param lightUserRepresentationsList the List of LightUserRepresentations to translate.
     * @return a List of translated CustomLightUserRepresentations
     */
    public List<CustomLightUserRepresentation> translate(List<LightUserRepresentation> lightUserRepresentationsList) {
        return lightUserRepresentationsList.stream().map(this::translate).collect(Collectors.toList());
    }

}
