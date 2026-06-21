package com.servidor.rolesystem.listeners;

import com.servidor.rolesystem.RoleSystemPlugin;
import com.servidor.rolesystem.RoleType;
import com.servidor.rolesystem.managers.ColiseumManager;
import com.servidor.rolesystem.managers.RoleManager;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

public class PvPListener implements Listener {

    private final RoleSystemPlugin plugin;
    private final RoleManager roleManager;
    private final ColiseumManager coliseumManager;

    public PvPListener(RoleSystemPlugin plugin) {
        this.plugin = plugin;
        this.roleManager = plugin.getRoleManager();
        this.coliseumManager = plugin.getColiseumManager();
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onPvP(EntityDamageByEntityEvent event) {
        // Solo nos interesan daños entre jugadores
        if (!(event.getEntity() instanceof Player defender)) return;

        Player attacker = null;
        if (event.getDamager() instanceof Player p) {
            attacker = p;
        } else if (event.getDamager() instanceof Projectile proj
                   && proj.getShooter() instanceof Player p) {
            attacker = p;
        }
        if (attacker == null) return;
        if (attacker.equals(defender)) return;

        // El staff con bypass ignora todas las reglas
        if (attacker.hasPermission("rolesystem.bypass.pvp")) return;

        // ── Coliseo: se permite todo ────────────────────────────────────────
        if (coliseumManager.isInColiseum(defender.getLocation())
                && coliseumManager.isInColiseum(attacker.getLocation())) {
            event.setCancelled(false);
            return;
        }

        // ── Fuera del coliseo: aplicar reglas de roles ──────────────────────
        RoleType roleAtk = roleManager.getRole(attacker);
        RoleType roleDef = roleManager.getRole(defender);

        // Sin rol asignado
        if (roleAtk == RoleType.SIN_ROL) {
            event.setCancelled(true);
            msg(attacker, "pvp-sin-rol-atacante");
            return;
        }
        if (roleDef == RoleType.SIN_ROL) {
            event.setCancelled(true);
            msg(attacker, "pvp-sin-rol-defensor",
                "{jugador}", defender.getName());
            return;
        }

        // Raider vs Raider → permitido (cancelado o no, lo forzamos a permitido)
        if (roleAtk == RoleType.RAIDER && roleDef == RoleType.RAIDER) {
            event.setCancelled(false);
            return;
        }

        // Cualquier otra combinación → prohibido
        event.setCancelled(true);

        if (roleAtk == RoleType.RAIDER && roleDef == RoleType.PACIFISTA) {
            msg(attacker, "pvp-raider-a-pacifista");
        } else if (roleAtk == RoleType.PACIFISTA) {
            msg(attacker, "pvp-pacifista-cualquiera");
        }
    }

    // ── Utilidad ─────────────────────────────────────────────────────────────

    private void msg(Player player, String key, String... replacements) {
        String raw = plugin.getConfig().getString("mensajes." + key, "&cMensaje no configurado: " + key);
        for (int i = 0; i < replacements.length - 1; i += 2) {
            raw = raw.replace(replacements[i], replacements[i + 1]);
        }
        player.sendMessage(raw.replace("&", "§"));
    }
}
