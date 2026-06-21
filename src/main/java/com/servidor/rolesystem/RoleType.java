package com.servidor.rolesystem;

public enum RoleType {
    RAIDER,
    PACIFISTA,
    SIN_ROL;

    public String getDisplayName() {
        return switch (this) {
            case RAIDER    -> "§cRaider";
            case PACIFISTA -> "§aPacifista";
            case SIN_ROL   -> "§7Sin Rol";
        };
    }

    /** Devuelve null si el string no corresponde a ningún rol válido */
    public static RoleType fromString(String s) {
        return switch (s.toLowerCase()) {
            case "raider"    -> RAIDER;
            case "pacifista" -> PACIFISTA;
            default          -> null;
        };
    }
}
