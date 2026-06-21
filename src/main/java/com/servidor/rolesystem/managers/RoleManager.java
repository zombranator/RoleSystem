package com.servidor.rolesystem.managers;

import com.servidor.rolesystem.RoleSystemPlugin;
import com.servidor.rolesystem.RoleType;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.util.UUID;

public class RoleManager {

    private final RoleSystemPlugin plugin;
    private final File dataFile;
    private YamlConfiguration data;

    public RoleManager(RoleSystemPlugin plugin) {
        this.plugin = plugin;
        this.dataFile = new File(plugin.getDataFolder(), "roles.yml");
        load();
    }

    private void load() {
        if (!dataFile.exists()) {
            try { dataFile.createNewFile(); } catch (IOException e) { e.printStackTrace(); }
        }
        data = YamlConfiguration.loadConfiguration(dataFile);
    }

    public void saveData() {
        try { data.save(dataFile); } catch (IOException e) {
            plugin.getLogger().severe("No se pudo guardar roles.yml: " + e.getMessage());
        }
    }

    // ── Consulta de roles ──────────────────────────────────────────────────────

    public RoleType getRole(UUID uuid) {
        String raw = data.getString("roles." + uuid);
        if (raw == null) return RoleType.SIN_ROL;
        try { return RoleType.valueOf(raw); } catch (Exception e) { return RoleType.SIN_ROL; }
    }

    public RoleType getRole(OfflinePlayer p) { return getRole(p.getUniqueId()); }
    public RoleType getRole(Player p)         { return getRole(p.getUniqueId()); }

    public boolean isRaider(UUID uuid)    { return getRole(uuid) == RoleType.RAIDER; }
    public boolean isPacifista(UUID uuid) { return getRole(uuid) == RoleType.PACIFISTA; }
    public boolean isRaider(Player p)     { return isRaider(p.getUniqueId()); }
    public boolean isPacifista(Player p)  { return isPacifista(p.getUniqueId()); }
    public boolean hasRole(Player p)      { return getRole(p) != RoleType.SIN_ROL; }

    // ── Asignación de roles ────────────────────────────────────────────────────

    public void setRole(Player player, RoleType role) {
        UUID uuid = player.getUniqueId();
        data.set("roles." + uuid, role.name());
        data.set("cooldowns." + uuid, System.currentTimeMillis());
        saveData();

        // Sincronizar con LuckPerms vía comandos de consola
        var console = Bukkit.getConsoleSender();
        String name = player.getName();
        // Quitar ambos grupos primero
        Bukkit.dispatchCommand(console, "lp user " + name + " parent remove raider");
        Bukkit.dispatchCommand(console, "lp user " + name + " parent remove pacifista");
        // Asignar el nuevo
        if (role != RoleType.SIN_ROL) {
            Bukkit.dispatchCommand(console, "lp user " + name + " parent add " + role.name().toLowerCase());
        }
    }

    // Versión para admin que cambia el rol de un jugador offline (no actualiza LP hasta que se conecte)
    public void setRoleOffline(UUID uuid, RoleType role) {
        data.set("roles." + uuid, role.name());
        data.set("cooldowns." + uuid, System.currentTimeMillis());
        saveData();
    }

    // ── Cooldown ───────────────────────────────────────────────────────────────

    public boolean isOnCooldown(UUID uuid) {
        if (!data.contains("cooldowns." + uuid)) return false;
        long cooldownMs = plugin.getConfig().getLong("cooldown-horas", 24) * 3_600_000L;
        long lastChange = data.getLong("cooldowns." + uuid, 0);
        return (System.currentTimeMillis() - lastChange) < cooldownMs;
    }

    /** Devuelve las horas restantes (redondeado hacia arriba) */
    public long getHorasRestantes(UUID uuid) {
        long cooldownMs = plugin.getConfig().getLong("cooldown-horas", 24) * 3_600_000L;
        long lastChange = data.getLong("cooldowns." + uuid, 0);
        long remainingMs = cooldownMs - (System.currentTimeMillis() - lastChange);
        return (long) Math.ceil(remainingMs / 3_600_000.0);
    }
}
