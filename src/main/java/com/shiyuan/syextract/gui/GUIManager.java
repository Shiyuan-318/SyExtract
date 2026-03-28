package com.shiyuan.syextract.gui;

import com.shiyuan.syextract.SyExtractPlugin;
import com.shiyuan.syextract.model.RedEnvelope;
import com.shiyuan.syextract.util.MessageUtil;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.text.SimpleDateFormat;
import java.util.*;

public class GUIManager implements Listener {

    private final SyExtractPlugin plugin;
    private final Map<UUID, Integer> playerPages;
    private final Map<UUID, Inventory> openInventories;
    private final Map<Integer, UUID> inventoryEnvelopeMap;
    private static final int ITEMS_PER_PAGE = 45;
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm");

    public GUIManager(SyExtractPlugin plugin) {
        this.plugin = plugin;
        this.playerPages = new HashMap<>();
        this.openInventories = new HashMap<>();
        this.inventoryEnvelopeMap = new HashMap<>();
    }

    public void openGUI(Player player) {
        openGUI(player, 0);
    }

    public void openGUI(Player player, int page) {
        if (!plugin.getBanManager().canClaimEnvelope(player.getUniqueId())) {
            player.sendMessage(MessageUtil.getMessage("error.banned-claim", 
                Map.of("expire", formatTime(plugin.getBanManager().getBan(player.getUniqueId()).getExpireTime()))));
            return;
        }

        List<RedEnvelope> availableEnvelopes = plugin.getRedEnvelopeManager().getAvailableEnvelopesForPlayer(player.getUniqueId());
        
        int totalPages = Math.max(1, (int) Math.ceil((double) availableEnvelopes.size() / ITEMS_PER_PAGE));
        if (page < 0) page = 0;
        if (page >= totalPages) page = totalPages - 1;

        String title = MessageUtil.colorize(plugin.getConfig().getString("gui.title", "&c&l红包大厅"));
        if (totalPages > 1 || availableEnvelopes.isEmpty()) {
            title += " &7(" + (page + 1) + "/" + totalPages + ")";
        }

        Inventory inventory = Bukkit.createInventory(null, 54, title);
        inventoryEnvelopeMap.clear();

        if (!availableEnvelopes.isEmpty()) {
            int startIndex = page * ITEMS_PER_PAGE;
            int endIndex = Math.min(startIndex + ITEMS_PER_PAGE, availableEnvelopes.size());

            for (int i = startIndex; i < endIndex; i++) {
                RedEnvelope envelope = availableEnvelopes.get(i);
                ItemStack item = createEnvelopeItem(envelope);
                int slot = i - startIndex;
                inventory.setItem(slot, item);
                inventoryEnvelopeMap.put(slot, envelope.getId());
            }
        }

        if (totalPages > 1) {
            if (page > 0) {
                inventory.setItem(45, createNavigationItem(Material.ARROW, "&e上一页"));
            }
            if (page < totalPages - 1) {
                inventory.setItem(53, createNavigationItem(Material.ARROW, "&e下一页"));
            }
        }

        inventory.setItem(49, createInfoItem(availableEnvelopes.size()));

        if (availableEnvelopes.isEmpty()) {
            inventory.setItem(22, createEmptyItem());
        }

        for (int i = 45; i < 54; i++) {
            if (inventory.getItem(i) == null) {
                inventory.setItem(i, createGlassPane());
            }
        }

        playerPages.put(player.getUniqueId(), page);
        openInventories.put(player.getUniqueId(), inventory);
        player.openInventory(inventory);
    }

    private ItemStack createEnvelopeItem(RedEnvelope envelope) {
        String materialName = plugin.getConfig().getString("gui.item-material", "SUNFLOWER");
        Material material = Material.matchMaterial(materialName);
        if (material == null) {
            material = Material.SUNFLOWER;
        }

        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();

        String name = plugin.getConfig().getString("gui.item-name", "&6&l{sender} &e的红包")
                .replace("{sender}", envelope.getSenderName());
        meta.setDisplayName(MessageUtil.colorize(name));

        List<String> lore = new ArrayList<>();
        List<String> loreTemplate = plugin.getConfig().getStringList("gui.item-lore");
        
        for (String line : loreTemplate) {
            lore.add(MessageUtil.colorize(line
                    .replace("{name}", envelope.getName())
                    .replace("{id}", envelope.getShortId())
                    .replace("{amount}", String.format("%.2f", envelope.getTotalAmount()))
                    .replace("{count}", String.valueOf(envelope.getTotalCount()))
                    .replace("{remaining}", String.valueOf(envelope.getRemainingCount()))
                    .replace("{expire}", DATE_FORMAT.format(new Date(envelope.getExpireTime())))));
        }

        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createNavigationItem(Material material, String name) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(MessageUtil.colorize(name));
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createInfoItem(int total) {
        ItemStack item = new ItemStack(Material.BOOK);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(MessageUtil.colorize("&6&l红包信息"));
        List<String> lore = new ArrayList<>();
        lore.add(MessageUtil.colorize("&7可领取红包数: &e" + total));
        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createEmptyItem() {
        ItemStack item = new ItemStack(Material.BARRIER);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(MessageUtil.colorize("&c&l暂无红包"));
        List<String> lore = new ArrayList<>();
        lore.add(MessageUtil.colorize("&7当前没有可领取的红包"));
        lore.add(MessageUtil.colorize("&7请稍后再来查看!"));
        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createGlassPane() {
        ItemStack item = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(" ");
        item.setItemMeta(meta);
        return item;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }

        Inventory openInv = openInventories.get(player.getUniqueId());
        if (openInv == null || !event.getInventory().equals(openInv)) {
            return;
        }

        event.setCancelled(true);

        int slot = event.getRawSlot();
        if (slot < 0 || slot >= 54) {
            return;
        }

        int currentPage = playerPages.getOrDefault(player.getUniqueId(), 0);

        if (slot == 45 && currentPage > 0) {
            openGUI(player, currentPage - 1);
            return;
        }

        if (slot == 53) {
            openGUI(player, currentPage + 1);
            return;
        }

        if (slot >= 45) {
            return;
        }

        UUID envelopeId = inventoryEnvelopeMap.get(slot);
        if (envelopeId == null) {
            return;
        }

        double amount = plugin.getRedEnvelopeManager().claimEnvelope(envelopeId, player.getUniqueId());
        
        if (amount > 0) {
            RedEnvelope envelope = plugin.getRedEnvelopeManager().getEnvelope(envelopeId);
            String senderName = envelope != null ? envelope.getSenderName() : "Unknown";
            
            plugin.getEconomyManager().deposit(player.getUniqueId(), amount);
            
            player.sendMessage(MessageUtil.getMessage("success.claim", 
                Map.of("sender", senderName, "amount", String.format("%.2f", amount))));
            
            player.closeInventory();
            
            Bukkit.getScheduler().runTaskLater(plugin, () -> openGUI(player, currentPage), 2L);
        } else {
            player.sendMessage(MessageUtil.getMessage("error.envelope-not-found"));
            player.closeInventory();
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player player)) {
            return;
        }

        Inventory openInv = openInventories.get(player.getUniqueId());
        if (openInv != null && event.getInventory().equals(openInv)) {
            openInventories.remove(player.getUniqueId());
            playerPages.remove(player.getUniqueId());
        }
    }

    private String formatTime(long timestamp) {
        return DATE_FORMAT.format(new Date(timestamp));
    }
}
