package com.servidor.rolesystem.commands;

import com.servidor.rolesystem.RoleSystemPlugin;
import com.servidor.rolesystem.RoleType;
import com.servidor.rolesystem.managers.RoleManager;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.Arrays;
import java.util.List;

public class RoleCommand implements CommandExecutor, TabCompleter {

    private final RoleSystemPlugin plugin;
    private final RoleManager roleManager;

    public RoleCommand(RoleSystemPlugin plugin) {
        this.plugin = plugin;
        this.roleManager = plugin.getRoleManager();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {

        // /rol  (sin argumentos) → menú de selección
        if (args.length == 0) {
            if (!(sender instanceof Player player)) {
                sender.sendMessage("§cEste comando solo puede usarlo un jugador.");
                return true;
            }
            showRoleMenu(player);
            return true;
        }

        String sub = args[0].toLowerCase();

        // /rol ver [jugador]
        if (sub.equals("ver")) {
            return handleVer(sender, args);
        }

        // /rol raider | /rol pacifista
        if (sub.equals("raider") || sub.equals("pacifista")) {
            if (!(sender instanceof Player player)) {
                sender.sendMessage("§cEste comando solo puede usarlo un jugador.");
                return true;
            }
            return handleSetSelf(player, sub);
        }

        // /rol setrol <jugador> <rol>  — solo admin
        if (sub.equals("setrol")) {
            return handleSetAdmin(sender, args);
        }

        // /rol reload — solo admin
        if (sub.equals("reload")) {
            if (!sender.hasPermission("rolesystem.admin")) {
                msg(sender, "no-permiso"); return true;
            }
            plugin.reloadConfig();
            sender.sendMessage("§a[RoleSystem] Configuración recargada.");
            return true;
        }

        msg(sender, "uso-comando");
        if (sender.hasPermission("rolesystem.admin")) msg(sender, "uso-admin");
        return true;
    }

    // ── Menú de selección ─────────────────────────────────────────────────────

    private void showRoleMenu(Player player) {
        RoleType current = roleManager.getRole(player);
        player.sendMessage("§8§m──────────────────────────────");
        player.sendMessage("§6§l    ⚔  ELIGE TU ROL  ⚔");
        player.sendMessage("§8§m──────────────────────────────");
        player.sendMessage("§7Tu rol actual: " + current.getDisplayName());
        player.sendMessage("");
        player.sendMessage("§c§lRAIDER §r§7→ §f/rol raider");
        player.sendMessage("  §7• Puedes atacar a otros Raiders");
        player.sendMessage("  §7• Puedes raidear bases de Raiders");
        player.sendMessage("  §7• Tu base puede ser raideada");
        player.sendMessage("");
        player.sendMessage("§a§lPACIFISTA §r§7→ §f/rol pacifista");
        player.sendMessage("  §7• No puedes atacar ni ser atacado");
        player.sendMessage("  §7• Tu base está protegida de Raiders");
        player.sendMessage("  §7• No puedes raidear a nadie");
        player.sendMessage("");
        if (roleManager.isOnCooldown(player.getUniqueId()) && !player.hasPermission("rolesystem.bypass.cooldown")) {
            long horas = roleManager.getHorasRestantes(player.getUniqueId());
            player.sendMessage("§c⏳ Cooldown: debes esperar §e" + horas + "h §cmás para cambiar.");
        }
        player.sendMessage("§8§m──────────────────────────────");
    }

    // ── /rol raider | /rol pacifista ──────────────────────────────────────────

    private boolean handleSetSelf(Player player, String rolStr) {
        if (!player.hasPermission("rolesystem.rol")) { msg(player, "no-permiso"); return true; }

        RoleType nuevoRol = RoleType.fromString(rolStr);
        if (nuevoRol == null) { msg(player, "uso-comando"); return true; }

        RoleType actual = roleManager.getRole(player);
        if (actual == nuevoRol) {
            msg(player, "rol-ya-elegido", "{rol}", nuevoRol.getDisplayName());
            return true;
        }

        if (roleManager.isOnCooldown(player.getUniqueId())
                && !player.hasPermission("rolesystem.bypass.cooldown")) {
            long horas = roleManager.getHorasRestantes(player.getUniqueId());
            msg(player, "cooldown", "{horas}", String.valueOf(horas));
            return true;
        }

        roleManager.setRole(player, nuevoRol);
        msg(player, "rol-elegido", "{rol}", nuevoRol.getDisplayName());
        return true;
    }

    // ── /rol ver [jugador] ────────────────────────────────────────────────────

    private boolean handleVer(CommandSender sender, String[] args) {
        if (!sender.hasPermission("rolesystem.ver")) { msg(sender, "no-permiso"); return true; }

        OfflinePlayer target;
        if (args.length < 2) {
            if (!(sender instanceof Player player)) {
                sender.sendMessage("§cIndica un jugador: /rol ver <jugador>");
                return true;
            }
            target = player;
        } else {
            target = Bukkit.getOfflinePlayer(args[1]);
            if (!target.hasPlayedBefore() && !target.isOnline()) {
                msg(sender, "jugador-no-encontrado"); return true;
            }
        }

        RoleType rol = roleManager.getRole(target);
        if (rol == RoleType.SIN_ROL) {
            msg(sender, "sin-rol-ver", "{jugador}", target.getName() != null ? target.getName() : args[1]);
        } else {
            msg(sender, "rol-ver",
                "{jugador}", target.getName() != null ? target.getName() : args[1],
                "{rol}", rol.getDisplayName());
        }
        return true;
    }

    // ── /rol setrol <jugador> <rol> ───────────────────────────────────────────

    private boolean handleSetAdmin(CommandSender sender, String[] args) {
        if (!sender.hasPermission("rolesystem.admin")) { msg(sender, "no-permiso"); return true; }
        if (args.length < 3) { msg(sender, "uso-admin"); return true; }

        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) { msg(sender, "jugador-no-encontrado"); return true; }

        RoleType nuevoRol = RoleType.fromString(args[2]);
        if (nuevoRol == null || nuevoRol == RoleType.SIN_ROL) {
            sender.sendMessage("§cRol inválido. Usa: raider o pacifista");
            return true;
        }

        roleManager.setRole(target, nuevoRol);
        msg(sender, "rol-cambiado-admin",
            "{jugador}", target.getName(),
            "{rol}", nuevoRol.getDisplayName());
        msg(target, "rol-elegido", "{rol}", nuevoRol.getDisplayName());
        return true;
    }

    // ── TabCompleter ──────────────────────────────────────────────────────────

    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String alias, String[] args) {
        if (args.length == 1) {
            List<String> opts = new java.util.ArrayList<>(Arrays.asList("raider", "pacifista", "ver"));
            if (sender.hasPermission("rolesystem.admin")) {
                opts.add("setrol");
                opts.add("reload");
            }
            return opts.stream()
                    .filter(s -> s.startsWith(args[0].toLowerCase()))
                    .toList();
        }
        if (args.length == 2 && (args[0].equalsIgnoreCase("ver") || args[0].equalsIgnoreCase("setrol"))) {
            return Bukkit.getOnlinePlayers().stream()
                    .map(Player::getName)
                    .filter(n -> n.toLowerCase().startsWith(args[1].toLowerCase()))
                    .toList();
        }
        if (args.length == 3 && args[0].equalsIgnoreCase("setrol")) {
            return Arrays.asList("raider", "pacifista");
        }
        return List.of();
    }

    // ── Utilidad ──────────────────────────────────────────────────────────────

    private void msg(CommandSender sender, String key, String... replacements) {
        String raw = plugin.getConfig().getString("mensajes." + key, "&cError: " + key);
        for (int i = 0; i < replacements.length - 1; i += 2) {
            raw = raw.replace(replacements[i], replacements[i + 1]);
        }
        sender.sendMessage(raw.replace("&", "§"));
    }
}
