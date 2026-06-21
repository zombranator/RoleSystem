package com.servidor.rolesystem.hooks;

import me.ryanhamshire.GriefPrevention.Claim;
import me.ryanhamshire.GriefPrevention.GriefPrevention;
import org.bukkit.Location;

import java.util.UUID;

public class GriefPreventionHook {

    /**
     * Devuelve el UUID del dueño del claim en esa ubicación,
     * o null si no hay claim o es un claim de admin.
     */
    public UUID getClaimOwner(Location location) {
        try {
            Claim claim = GriefPrevention.instance.dataStore.getClaimAt(location, false, null);
            if (claim == null) return null;
            return claim.ownerID; // null en claims de administrador
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Devuelve true si la ubicación está dentro de algún claim.
     */
    public boolean isInClaim(Location location) {
        try {
            return GriefPrevention.instance.dataStore.getClaimAt(location, false, null) != null;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Devuelve true si la ubicación está en un claim de administrador
     * (ownerID == null en GriefPrevention).
     */
    public boolean isAdminClaim(Location location) {
        try {
            Claim claim = GriefPrevention.instance.dataStore.getClaimAt(location, false, null);
            return claim != null && claim.ownerID == null;
        } catch (Exception e) {
            return false;
        }
    }
}
