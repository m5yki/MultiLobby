package me.maykitron.multiLobby;

import org.bukkit.*;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.HashMap;
import java.util.Map;

public class MenuManager implements Listener {

    private final MultiLobby plugin;
    private final NamespacedKey rtpKey;

    private String warpTitle;
    private int warpRows;
    private final Map<String, ItemStack> warpIcons = new HashMap<>();

    private String rtpTitle;
    private int rtpRows;
    private final Map<Integer, ItemStack> rtpItems = new HashMap<>();

    public MenuManager(MultiLobby plugin) {
        this.plugin = plugin;
        this.rtpKey = new NamespacedKey(plugin, "rtp_target_world");
    }

    public void loadMenus() {
        warpIcons.clear(); rtpItems.clear();

        warpTitle = plugin.getConfig().getString("warp-menu.title", "§aWarplar");
        warpRows = Math.max(1, Math.min(6, plugin.getConfig().getInt("warp-menu.rows", 3)));
        ConfigurationSection ws = plugin.getConfig().getConfigurationSection("warps");
        if (ws != null) {
            for (String key : ws.getKeys(false)) {
                ConfigurationSection s = ws.getConfigurationSection(key);
                if (s == null) continue;

                Material m = Material.matchMaterial(s.getString("icon.material", "COMPASS"));
                ItemStack icon = new ItemStack(m != null ? m : Material.COMPASS);
                ItemMeta meta = icon.getItemMeta();
                meta.setDisplayName(s.getString("icon.name", "§e" + key));
                meta.setLore(s.getStringList("icon.lore"));
                icon.setItemMeta(meta);

                warpIcons.put(key.toLowerCase(), icon);
            }
        }

        rtpTitle = plugin.getConfig().getString("rtp.menu.title", "§dRTP Dünya Seç");
        rtpRows = Math.max(1, Math.min(6, plugin.getConfig().getInt("rtp.menu.rows", 3)));
        ConfigurationSection rs = plugin.getConfig().getConfigurationSection("rtp.worlds");
        if (rs != null) {
            for (String id : rs.getKeys(false)) {
                ConfigurationSection s = rs.getConfigurationSection(id);
                if (s == null) continue;

                Material m = Material.matchMaterial(s.getString("icon.material", "ENDER_PEARL"));
                ItemStack icon = new ItemStack(m != null ? m : Material.ENDER_PEARL);
                ItemMeta meta = icon.getItemMeta();
                meta.setDisplayName(s.getString("icon.name", "§d" + id));
                meta.setLore(s.getStringList("icon.lore"));

                String targetWorld = s.getString("target-world", id);
                meta.getPersistentDataContainer().set(rtpKey, PersistentDataType.STRING, targetWorld);
                icon.setItemMeta(meta);

                int slot = s.getInt("icon.slot", -1);
                if (slot >= 0) rtpItems.put(slot, icon);
            }
        }
    }

    public void openWarpMenu(Player p) {
        if (warpIcons.isEmpty()) { p.sendMessage(plugin.getLang().s("warp.not-found", "§cKayıtlı warp yok.")); return; }
        Inventory inv = Bukkit.createInventory(null, warpRows * 9, warpTitle);
        int i = 0;
        for (ItemStack icon : warpIcons.values()) {
            if (i >= inv.getSize()) break;
            inv.setItem(i++, icon);
        }
        p.openInventory(inv);
    }

    public void openRtpMenu(Player p) {
        if (rtpItems.isEmpty()) { p.sendMessage("§cRTP dünyası ayarlanmamış."); return; }
        Inventory inv = Bukkit.createInventory(null, rtpRows * 9, rtpTitle);
        for (Map.Entry<Integer, ItemStack> entry : rtpItems.entrySet()) {
            if (entry.getKey() < inv.getSize()) inv.setItem(entry.getKey(), entry.getValue());
        }
        p.openInventory(inv);
    }

    @EventHandler
    public void onClick(InventoryClickEvent e) {
        if (!(e.getWhoClicked() instanceof Player)) return;
        if (e.getView().getTitle() == null) return;

        Player p = (Player) e.getWhoClicked();
        String title = e.getView().getTitle();

        if (title.equals(warpTitle)) {
            e.setCancelled(true);
            if (e.getCurrentItem() == null || !e.getCurrentItem().hasItemMeta()) return;

            String clickedName = ChatColor.stripColor(e.getCurrentItem().getItemMeta().getDisplayName());
            for (Map.Entry<String, ItemStack> entry : warpIcons.entrySet()) {
                if (ChatColor.stripColor(entry.getValue().getItemMeta().getDisplayName()).equals(clickedName)) {
                    p.closeInventory();
                    plugin.getTeleportUtil().startWarmupTeleport(p, plugin.getWarpManager().getWarp(entry.getKey()), "warp " + entry.getKey(), Particle.END_ROD, "SPIRAL");
                    break;
                }
            }
        }
        else if (title.equals(rtpTitle)) {
            e.setCancelled(true);
            if (e.getCurrentItem() == null || !e.getCurrentItem().hasItemMeta()) return;

            String targetWorld = e.getCurrentItem().getItemMeta().getPersistentDataContainer().get(rtpKey, PersistentDataType.STRING);
            if (targetWorld != null) {
                p.closeInventory();
                plugin.getRtpQueueManager().addToQueue(p, targetWorld);
            }
        }
    }

    @EventHandler
    public void onDrag(InventoryDragEvent e) {
        String title = e.getView().getTitle();
        if (title != null && (title.equals(warpTitle) || title.equals(rtpTitle))) {
            e.setCancelled(true);
        }
    }
}