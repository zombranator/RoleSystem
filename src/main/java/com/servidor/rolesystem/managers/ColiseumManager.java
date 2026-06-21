package com.servidor.rolesystem.managers;

import com.servidor.rolesystem.RoleSystemPlugin;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.RegionContainer;
import org.bukkit.Location;

public class ColiseumManager {

    private final RoleSystemPlugin plugin;
    private boolean worldGuardAvailable = false;

    public ColiseumManager(RoleSystemPlugin plugin) {
        this.plugin = plugin;
    }

    public void initWorldGuard() {
        try {
            Class.forName("com.sk89q.worldguard.WorldGuard");
            worldGuardAvailable = true;
        } catch (ClassNotFoundException e) {
            worldGuardAvailable = false;
        }
    }

    /**
     * Devuelve true si la ubicación está dentro de la región del coliseo.
     * Si WorldGuard no está disponible, siempre devuelve false.
     */
    public boolean isInColiseum(Location location) {
        if (!worldGuardAvailable || location.getWorld() == null) return false;
        try {
            String regionName = plugin.getConfig().getString("coliseo-region", "coliseo");
            RegionContainer container = WorldGuard.getInstance().getPlatform().getRegionContainer();
            RegionManager rm = container.get(BukkitAdapter.adapt(location.getWorld()));
            if (rm == null) return false;

            var blockVector = BukkitAdapter.asBlockVector(location);
            return rm.getApplicableRegions(blockVector)
                     .getRegions()
                     .stream()
                     .anyMatch(r -> r.getId().equalsIgnoreCase(regionName));
        } catch (Exception e) {
            return false;
        }
    }

    public boolean isWorldGuardAvailable() { return worldGuardAvailable; }
}
