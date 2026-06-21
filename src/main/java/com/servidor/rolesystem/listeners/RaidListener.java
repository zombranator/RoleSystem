package com.servidor.rolesystem.listeners;

import com.servidor.rolesystem.RoleSystemPlugin;
import com.servidor.rolesystem.managers.RoleManager;
import com.servidor.rolesystem.hooks.GriefPreventionHook;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;

import java.util.UUID;

/**
 * Hace bypass de GriefPrevention (escucha a HIGHEST con ignoreCancelled=false)
 * para permitir que Raiders raiden bases de otros Raiders.
 *
 * Reglas:
 *   Raider  → base de Raider    : PERMITIDO  (uncancela lo que GP canceló)
 *   Raider  → base de Pacifista : PROHIBIDO  (deja el cancel de GP)
 *   Pacifista → cualquier base  : PROHIBIDO  (deja el cancel de GP)
 *   Sin rol → cualquier base    : PROHIBIDO  (deja el cancel de GP)
 *   Claim de Admin              : NUNCA se bypasea (protección de admin)
 */
public class RaidListener implements Listener {

    private final RoleSystemPlugin plugin;
    private final RoleManager roleManager;
    private final GriefPreventionHook gpHook;

    public RaidListener(RoleSystemPlugin plugin) {
        this.plugin = plugin;
        this.roleManager = plugin.getRoleManager();
        this.gpHook = plugin.getGpHook();
    }

    // ── Romper bloques ────────────────────────────────────────────────────────

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onBlockBreak(BlockBreakEvent event) {
        if (!event.isCancelled()) return; // GP no canceló nada, no hace falta actuar
        if (gpHook == null) return;

        Player player = event.getPlayer();
        var loc = event.getBlock().getLocation();

        if (tryBypass(player, loc)) {
            event.setCancelled(false);
        } else if (roleManager.isRaider(player) && !isOwnerRaider(loc)) {
            // Es Raider pero intenta raidear base de Pacifista → aviso
            msg(player, "raid-base-pacifista");
        } else if (!roleManager.isRaider(player) && gpHook.isInClaim(loc)) {
            msg(player, "raid-no-raider");
        }
    }

    // ── Colocar bloques ───────────────────────────────────────────────────────

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onBlockPlace(BlockPlaceEvent event) {
        if (!event.isCancelled()) return;
        if (gpHook == null) return;

        Player player = event.getPlayer();
        if (tryBypass(player, event.getBlock().getLocation())) {
            event.setCancelled(false);
        }
    }

    // ── Interacción con contenedores / palancas / botones ─────────────────────

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onInteract(PlayerInteractEvent event) {
        if (!event.isCancelled()) return;
        if (gpHook == null) return;
        // Solo mano principal para evitar doble disparo
        if (event.getHand() != EquipmentSlot.HAND) return;
        if (event.getClickedBlock() == null) return;

        Player player = event.getPlayer();
        if (tryBypass(player, event.getClickedBlock().getLocation())) {
            event.setCancelled(false);
        }
    }

    // ── Lógica compartida ─────────────────────────────────────────────────────

    /**
     * Devuelve true si se debe hacer bypass:
     *   - El jugador es Raider
     *   - La ubicación está en un claim
     *   - El dueño del claim es Raider
     *   - No es un claim de admin
     */
    private boolean tryBypass(Player player, org.bukkit.Location loc) {
        if (!roleManager.isRaider(player)) return false;
        if (!gpHook.isInClaim(loc)) return false;
        if (gpHook.isAdminClaim(loc)) return false;
        return isOwnerRaider(loc);
    }

    /**
     * Comprueba si el dueño del claim en esa ubicación tiene rol Raider.
     * Funciona aunque el dueño esté offline (roles.yml almacena todos los UUIDs).
     */
    private boolean isOwnerRaider(org.bukkit.Location loc) {
        UUID ownerUUID = gpHook.getClaimOwner(loc);
        if (ownerUUID == null) return false; // admin claim o sin claim
        return roleManager.isRaider(ownerUUID);
    }

    // ── Utilidad ─────────────────────────────────────────────────────────────

    private void msg(Player player, String key) {
        String raw = plugin.getConfig().getString("mensajes." + key, "&cError: " + key);
        player.sendMessage(raw.replace("&", "§"));
    }
}
