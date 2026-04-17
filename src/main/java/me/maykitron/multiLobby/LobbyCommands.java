package me.maykitron.multiLobby;

import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.UUID;

public class LobbyCommands implements CommandExecutor {

    private final MultiLobby plugin;
    private final HashMap<UUID, Long> cooldown = new HashMap<>();

    public LobbyCommands(MultiLobby plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player)) {
            if (cmd.getName().equalsIgnoreCase("multilobby") && args.length > 0 &&
                    (args[0].equalsIgnoreCase("yenile") || args[0].equalsIgnoreCase("reload"))) {
                plugin.loadConfigValues();
                sender.sendMessage("[MultiLobby] Config, lang ve menuler basariyla yenilendi.");
            } else {
                sender.sendMessage("[MultiLobby] Sadece oyuncular bu komutlari kullanabilir. Yenilemek icin: /multilobby reload");
            }
            return true;
        }

        Player p = (Player) sender;

        if (cooldown.containsKey(p.getUniqueId()) && System.currentTimeMillis() - cooldown.get(p.getUniqueId()) < 1000) {
            p.sendMessage(plugin.getLang().s("messages.spam-warning", "§cÇok hızlı komut kullanıyorsun!"));
            return true;
        }
        cooldown.put(p.getUniqueId(), System.currentTimeMillis());

        String name = cmd.getName().toLowerCase();

        switch (name) {
            case "spawn":
            case "lobby":
                if (plugin.getSpawnLocation() == null) {
                    p.sendMessage(plugin.getLang().s("spawn.not-set", "§cSpawn noktası ayarlı değil!"));
                    return true;
                }
                plugin.getTeleportUtil().startWarmupTeleport(p, plugin.getSpawnLocation(), plugin.getLang().s("spawn.name", "Spawn"), null, null);
                return true;

            case "setspawn":
            case "setlobby":
                if (!p.hasPermission("multilobby.admin")) {
                    p.sendMessage(plugin.getLang().s("messages.no-permission", "§cYetkin yok."));
                    return true;
                }
                Location l = p.getLocation();
                plugin.getConfig().set("spawn.world", l.getWorld().getName());
                plugin.getConfig().set("spawn.x", l.getX());
                plugin.getConfig().set("spawn.y", l.getY());
                plugin.getConfig().set("spawn.z", l.getZ());
                plugin.getConfig().set("spawn.yaw", l.getYaw());
                plugin.getConfig().set("spawn.pitch", l.getPitch());
                plugin.saveConfig();
                plugin.loadConfigValues();
                p.sendMessage(plugin.getLang().s("spawn.set-success", "§aSpawn noktası başarıyla ayarlandı!"));
                return true;

            case "setwarp":
                if (!p.hasPermission("multilobby.admin") || args.length == 0) return true;
                plugin.getWarpManager().setWarp(args[0], p.getLocation());
                p.sendMessage(plugin.getLang().s("warp.set-success").replace("%name%", args[0]));
                return true;

            case "delwarp":
                if (!p.hasPermission("multilobby.admin") || args.length == 0) return true;
                plugin.getWarpManager().delWarp(args[0]);
                p.sendMessage(plugin.getLang().s("warp.del-success").replace("%name%", args[0]));
                return true;

            case "home":
                Location bed = plugin.getHomeManager().getHome(p.getUniqueId());
                if (bed == null) bed = p.getBedSpawnLocation();
                if (bed == null) { p.sendMessage(plugin.getLang().s("home.no-bed", "§cBir yatağın yok!")); return true; }
                plugin.getTeleportUtil().startWarmupTeleport(p, bed, plugin.getLang().s("home.name", "Ev"), Particle.TOTEM_OF_UNDYING, "PULSE");
                return true;

            case "warp":
                if (args.length > 0) {
                    Location loc = plugin.getWarpManager().getWarp(args[0]);
                    if (loc != null) plugin.getTeleportUtil().startWarmupTeleport(p, loc, args[0], null, null);
                    else p.sendMessage(plugin.getLang().s("warp.not-found"));
                } else {
                    // Bedrock kontrolü
                    if (plugin.getServer().getPluginManager().getPlugin("floodgate") != null &&
                            org.geysermc.floodgate.api.FloodgateApi.getInstance().isFloodgatePlayer(p.getUniqueId())) {
                        plugin.getMenuManager().openBedrockWarpForm(p);
                    } else {
                        plugin.getMenuManager().openWarpMenu(p);
                    }
                }
                return true;

            case "rtp":
            case "kaptan":
                plugin.getMenuManager().openRtpMenu(p);
                return true;

            case "discord":
                sendClickableLink(p, "discord"); return true;
            case "tiktok":
                sendClickableLink(p, "tiktok"); return true;
            case "website":
            case "site":
                sendClickableLink(p, "website"); return true;
            case "map":
                sendClickableLink(p, "map"); return true;

            case "lobimatik":
            case "multilobby":
                if (args.length == 1 && (args[0].equalsIgnoreCase("yenile") || args[0].equalsIgnoreCase("reload"))) {
                    if (p.hasPermission("multilobby.admin")) {
                        plugin.loadConfigValues();
                        p.sendMessage(plugin.getLang().s("messages.reload-success", "§aMultiLobby ayarları yenilendi."));
                    } else {
                        p.sendMessage(plugin.getLang().s("messages.no-permission", "§cYetkin yok."));
                    }
                } else {
                    p.sendMessage("§7MultiLobby Komutları: §f/spawn, /warp, /kaptan, /home");
                    if (p.hasPermission("multilobby.admin")) {
                        p.sendMessage("§cAdmin: §f/multilobby reload");
                    }
                }
                return true;

            case "back":
                plugin.getBackManager().teleportBack(p);
                return true;
        }
        return false;
    }

    private void sendClickableLink(Player p, String path) {
        // Bedrock kontrolü
        if (plugin.getServer().getPluginManager().getPlugin("floodgate") != null &&
                org.geysermc.floodgate.api.FloodgateApi.getInstance().isFloodgatePlayer(p.getUniqueId())) {
            plugin.getMenuManager().openBedrockLinksForm(p);
            return;
        }

        // Java Oyuncuları için normal tıklanabilir mesaj
        String text = plugin.getLang().s("links." + path + ".text", "§bTıkla!");
        String url = plugin.getLang().s("links." + path + ".url", "https://google.com");
        String hover = plugin.getLang().s("links." + path + ".hover", "§7Gitmek için tıkla.");

        TextComponent message = new TextComponent(text);
        message.setClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, url));
        message.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new ComponentBuilder(hover).create()));
        p.spigot().sendMessage(message);
    }
}