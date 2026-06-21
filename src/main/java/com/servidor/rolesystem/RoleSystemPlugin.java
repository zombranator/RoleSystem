package com.servidor.rolesystem;

import com.servidor.rolesystem.commands.RoleCommand;
import com.servidor.rolesystem.hooks.GriefPreventionHook;
import com.servidor.rolesystem.listeners.PvPListener;
import com.servidor.rolesystem.listeners.RaidListener;
import com.servidor.rolesystem.managers.ColiseumManager;
import com.servidor.rolesystem.managers.RoleManager;
import org.bukkit.plugin.java.JavaPlugin;

public class RoleSystemPlugin extends JavaPlugin {

    private RoleManager roleManager;
    private ColiseumManager coliseumManager;
    private GriefPreventionHook gpHook;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        this.roleManager    = new RoleManager(this);
        this.coliseumManager = new ColiseumManager(this);

        // Hook GriefPrevention (softdepend)
        if (getServer().getPluginManager().getPlugin("GriefPrevention") != null) {
            this.gpHook = new GriefPreventionHook();
            getLogger().info("GriefPrevention detectado. Bypass Raider/Raider activado.");
        } else {
            getLogger().warning("GriefPrevention no encontrado. El bypass de claims no estará disponible.");
        }

        // Hook WorldGuard (softdepend)
        if (getServer().getPluginManager().getPlugin("WorldGuard") != null) {
            coliseumManager.initWorldGuard();
            getLogger().info("WorldGuard detectado. El coliseo está listo.");
        } else {
            getLogger().warning("WorldGuard no encontrado. El coliseo no funcionará hasta que se instale.");
        }

        // Registrar eventos
        var pm = getServer().getPluginManager();
        pm.registerEvents(new PvPListener(this), this);
        pm.registerEvents(new RaidListener(this), this);

        // Registrar comandos
        var rolCmd = getCommand("rol");
        if (rolCmd != null) {
            var roleCommand = new RoleCommand(this);
            rolCmd.setExecutor(roleCommand);
            rolCmd.setTabCompleter(roleCommand);
        }

        getLogger().info("§aRoleSystem §fcargado correctamente. §7(Pacifista/Raider)");
    }

    @Override
    public void onDisable() {
        if (roleManager != null) roleManager.saveData();
        getLogger().info("RoleSystem desactivado. Datos guardados.");
    }

    public RoleManager getRoleManager()       { return roleManager; }
    public ColiseumManager getColiseumManager() { return coliseumManager; }
    public GriefPreventionHook getGpHook()    { return gpHook; }
}
