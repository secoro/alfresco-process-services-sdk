package com.activiti.extension.model;

/**
 * @author Incentro.
 *
 * Enumeration container the three possible permissions that can be set on a dossier from the PUT addDossierPermissions call.
 */
public enum DossierPermission {
    READ("read"),
    WRITE("write"),
    MANAGE("manage");

    private final String dossierPermission;

    DossierPermission(String dossierPermission) {
        this.dossierPermission = dossierPermission;
    }

    public String getDossierPermission() {
        return dossierPermission;
    }

    public static DossierPermission fromText(String text) {
        for (DossierPermission p : DossierPermission.values()) {
            if (p.getDossierPermission().equals(text)) {
                return p;
            }
        }
        throw new IllegalArgumentException(String.format("DossierPermission text not recognized [%s]", text));
    }
}
