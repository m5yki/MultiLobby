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
import org.geysermc.cumulus.form.SimpleForm;
import org.geysermc.floodgate.api.FloodgateApi;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MenuManager implements Listener {

    private final MultiLobby plugin;
    private final NamespacedKey actionKey;
    private final NamespacedKey valueKey;

    private String warpTitle;
    private int warpRows;
    private final Map<String, ItemStack> warpIcons = new HashMap<>();

    private String defaultRtpMenu;
    private final Map<String, RtpMenuDef> rtpMenus = new HashMap<>();

    public MenuManager(MultiLobby plugin) {
        this.plugin = plugin;
        this.actionKey = new NamespacedKey(plugin, "menu_action");
        this.valueKey = new NamespacedKey(plugin, "menu_value");
    }

    private static class RtpMenuDef {
        String title;
        int size;
        Map<Integer, RtpItemDef> items = new HashMap<>();
    }

    private static class RtpItemDef {
        String material;
        String name;
        List<String> lore;
        String action;
        String actionValue;
    }

    public void loadMenus() {
        warpIcons.clear();
        rtpMenus.clear();

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

        defaultRtpMenu = plugin.getConfig().getString("rtp.default-menu", "overworld");
        ConfigurationSection ms = plugin.getConfig().getConfigurationSection("rtp.menus");
        if (ms != null) {
            for (String menuId : ms.getKeys(false)) {
                ConfigurationSection s = ms.getConfigurationSection(menuId);
                if (s == null) continue;

                RtpMenuDef def = new RtpMenuDef();
                def.title = ChatColor.translateAlternateColorCodes('&', s.getString("title", menuId));
                def.size = Math.max(1, Math.min(6, s.getInt("rows", 6))) * 9;

                ConfigurationSection is = s.getConfigurationSection("items");
                if (is != null) {
                    for (String itemKey : is.getKeys(false)) {
                        ConfigurationSection idx = is.getConfigurationSection(itemKey);
                        if (idx == null) continue;

                        RtpItemDef it = new RtpItemDef();
                        it.material = idx.getString("material", "STONE");
                        it.name = idx.getString("name");
                        if (it.name != null) it.name = ChatColor.translateAlternateColorCodes('&', it.name);

                        List<String> lore = idx.getStringList("lore");
                        for (int i = 0; i < lore.size(); i++) lore.set(i, ChatColor.translateAlternateColorCodes('&', lore.get(i)));
                        it.lore = lore;

                        it.action = idx.getString("action");
                        it.actionValue = idx.getString("action-value");

                        int slot = idx.getInt("slot", -1);
                        if (slot >= 0 && slot < def.size) {
                            def.items.put(slot, it);
                        }
                    }
                }
                rtpMenus.put(menuId, def);
            }
        }
    }

    // BEDROCK KONTROLÜ İÇİN YARDIMCI METOT
    private boolean isBedrock(Player p) {
        return Bukkit.getPluginManager().getPlugin("floodgate") != null &&
                FloodgateApi.getInstance().isFloodgatePlayer(p.getUniqueId());
    }

    public void openWarpMenu(Player p) {
        if (isBedrock(p)) {
            openBedrockWarpForm(p);
            return;
        }

        if (warpIcons.isEmpty()) { p.sendMessage(plugin.getLang().s("warp.not-found", "§cKayıtlı warp yok.")); return; }
        Inventory inv = Bukkit.createInventory(null, warpRows * 9, warpTitle);
        int i = 0;
        for (ItemStack icon : warpIcons.values()) {
            if (i >= inv.getSize()) break;
            inv.setItem(i++, icon);
        }
        p.openInventory(inv);
    }

    public void openRtpMenu(Player p, String menuId) {
        if (isBedrock(p)) {
            openBedrockRtpForm(p);
            return;
        }

        if (menuId == null || !rtpMenus.containsKey(menuId)) menuId = defaultRtpMenu;
        RtpMenuDef def = rtpMenus.get(menuId);

        if (def == null) {
            p.sendMessage("§cRTP menüsü henüz ayarlanmamış.");
            return;
        }

        Inventory inv = Bukkit.createInventory(null, def.size, def.title);

        for (Map.Entry<Integer, RtpItemDef> entry : def.items.entrySet()) {
            RtpItemDef it = entry.getValue();
            ItemStack stack = buildItemStack(it);
            inv.setItem(entry.getKey(), stack);
        }
        p.openInventory(inv);
    }

    public void openRtpMenu(Player p) {
        openRtpMenu(p, defaultRtpMenu);
    }

    // BEDROCK İÇİN RTP FORMU
    private void openBedrockRtpForm(Player p) {
        SimpleForm form = SimpleForm.builder()
                .title("Işınlanma Menüsü")
                .content("Lütfen ışınlanmak istediğiniz diyarı seçin:")
                .button("§aSurvival Dünyası")
                .button("§cCehennem Dünyası")
                .button("§dSonsuzluk Dünyası")
                .button("§4Kapat")
                .validResultHandler(response -> {
                    int clickedId = response.clickedButtonId();
                    switch (clickedId) {
                        case 0:
                            plugin.getRtpQueueManager().addToQueue(p, "survival");
                            break;
                        case 1:
                            plugin.getRtpQueueManager().addToQueue(p, "survival_nether");
                            break;
                        case 2:
                            plugin.getRtpQueueManager().addToQueue(p, "survival_the_end");
                            break;
                    }
                })
                .build();

        FloodgateApi.getInstance().getPlayer(p.getUniqueId()).sendForm(form);
    }

    // BEDROCK İÇİN WARP FORMU
    public void openBedrockWarpForm(Player p) {
        Map<String, Location> allWarps = plugin.getWarpManager().getAllWarps();
        if (allWarps.isEmpty()) {
            p.sendMessage(plugin.getLang().s("warp.not-found", "§cKayıtlı warp yok."));
            return;
        }

        SimpleForm.Builder form = SimpleForm.builder()
                .title("Işınlanma Noktaları (Warp)")
                .content("Gitmek istediğiniz noktayı seçin:");

        for (String warpName : allWarps.keySet()) {
            form.button("§e" + warpName.toUpperCase());
        }
        form.button("§4Kapat");

        form.validResultHandler(response -> {
            int clickedId = response.clickedButtonId();
            if (clickedId < allWarps.size()) {
                String selectedWarp = (String) allWarps.keySet().toArray()[clickedId];
                Location target = allWarps.get(selectedWarp);
                plugin.getTeleportUtil().startWarmupTeleport(p, target, selectedWarp, Particle.END_ROD, "SPIRAL");
            }
        });

        FloodgateApi.getInstance().getPlayer(p.getUniqueId()).sendForm(form.build());
    }

    // BEDROCK İÇİN SOSYAL LİNKLER FORMU
    public void openBedrockLinksForm(Player p) {
        SimpleForm form = SimpleForm.builder()
                .title("Sosyal Medya & Linkler")
                .content("Bağlantılara gitmek için butonları kullanın:")
                .button("§9Discord")
                .button("§dTikTok")
                .button("§aWeb Sitesi")
                .button("§eHarita (Map)")
                .button("§4Kapat")
                .validResultHandler(response -> {
                    String path = "";
                    switch (response.clickedButtonId()) {
                        case 0: path = "discord"; break;
                        case 1: path = "tiktok"; break;
                        case 2: path = "website"; break;
                        case 3: path = "map"; break;
                        default: return;
                    }
                    String url = plugin.getLang().s("links." + path + ".url", "Bulunamadı");
                    p.sendMessage("§b[Link] §f" + url);
                })
                .build();

        FloodgateApi.getInstance().getPlayer(p.getUniqueId()).sendForm(form);
    }

    private ItemStack buildItemStack(RtpItemDef def) {
        ItemStack item = null;
        String matStr = def.material;

        if (matStr.toLowerCase().startsWith("itemsadder-")) {
            matStr = matStr.substring(11);
        }

        if (matStr.contains(":") && Bukkit.getPluginManager().getPlugin("ItemsAdder") != null) {
            try {
                dev.lone.itemsadder.api.CustomStack customStack = dev.lone.itemsadder.api.CustomStack.getInstance(matStr);
                if (customStack != null) {
                    item = customStack.getItemStack().clone();
                }
            } catch (Exception ignored) {}
        }

        if (item == null) {
            Material m = Material.matchMaterial(matStr);
            item = new ItemStack(m != null ? m : Material.STONE);
        }

        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            if (def.name != null) {
                meta.setDisplayName(def.name);
            } else {
                meta.setDisplayName(" ");
            }

            if (def.lore != null && !def.lore.isEmpty()) meta.setLore(def.lore);

            if (def.action != null && def.actionValue != null) {
                meta.getPersistentDataContainer().set(actionKey, PersistentDataType.STRING, def.action);
                meta.getPersistentDataContainer().set(valueKey, PersistentDataType.STRING, def.actionValue);
            }
            item.setItemMeta(meta);
        }
        return item;
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
            return;
        }

        boolean isRtpMenu = rtpMenus.values().stream().anyMatch(m -> m.title.equals(title));
        if (isRtpMenu) {
            e.setCancelled(true);
            if (e.getCurrentItem() == null || !e.getCurrentItem().hasItemMeta()) return;

            ItemMeta meta = e.getCurrentItem().getItemMeta();
            if (!meta.getPersistentDataContainer().has(actionKey, PersistentDataType.STRING)) return;

            String action = meta.getPersistentDataContainer().get(actionKey, PersistentDataType.STRING);
            String value = meta.getPersistentDataContainer().get(valueKey, PersistentDataType.STRING);

            if ("menu".equalsIgnoreCase(action)) {
                openRtpMenu(p, value);
            } else if ("rtp".equalsIgnoreCase(action)) {
                p.closeInventory();
                plugin.getRtpQueueManager().addToQueue(p, value);
            }
        }
    }

    @EventHandler
    public void onDrag(InventoryDragEvent e) {
        String title = e.getView().getTitle();
        if (title != null && (title.equals(warpTitle) || rtpMenus.values().stream().anyMatch(m -> m.title.equals(title)))) {
            e.setCancelled(true);
        }
    }
}