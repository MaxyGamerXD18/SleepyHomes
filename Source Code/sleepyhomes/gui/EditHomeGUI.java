package me.yourname.sleepyhomes.gui;

import me.yourname.sleepyhomes.SleepyHomes;
import me.yourname.sleepyhomes.data.HomeManager;
import me.yourname.sleepyhomes.util.ItemBuilder;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
public class EditHomeGUI implements Listener {
    private final SleepyHomes plugin;
    private final HomeManager homeManager;
    public EditHomeGUI(SleepyHomes plugin) {
        this.plugin = plugin;
        this.homeManager = plugin.getHomeManager();
    }
    public void openEdit(Player p, int homeNumber) {
        String editTitle = plugin.getConfig().getString("menu-titles.edit-home", "&6Edit Home #%number%");
        editTitle = editTitle.replace("%number%", String.valueOf(homeNumber));
        Inventory inv = Bukkit.createInventory(null, 3 * 9, plugin.format(editTitle));
        int backSlot = plugin.getConfig().getInt("items.edit-menu.back-slot", 10);      // default 10
        int deleteSlot = plugin.getConfig().getInt("items.edit-menu.delete-slot", 16);  // default 16
        inv.setItem(backSlot, ItemBuilder.fromConfig(plugin.getConfig(), "items.edit-menu.back"));
        inv.setItem(deleteSlot, ItemBuilder.fromConfig(plugin.getConfig(), "items.edit-menu.delete"));
        p.openInventory(inv);
        p.setMetadata("editingHome", new org.bukkit.metadata.FixedMetadataValue(plugin, homeNumber));
    }
    @EventHandler
    public void onClick(InventoryClickEvent e) {
        if (!(e.getWhoClicked() instanceof Player)) return;
        Player p = (Player) e.getWhoClicked();
        if (e.getView().getTitle() == null) return;
        String title = e.getView().getTitle();
        String strippedTitle = org.bukkit.ChatColor.stripColor(title);
        String baseTitleRaw = plugin.getConfig().getString("menu-titles.edit-home", "&6Edit Home #%number%");
        String baseTitle = baseTitleRaw.replace("%number%", "").trim();
        String baseTitleStripped = org.bukkit.ChatColor.stripColor(plugin.format(baseTitle));
        if (!strippedTitle.startsWith(baseTitleStripped)) return;
        e.setCancelled(true);
        if (!p.hasMetadata("editingHome")) {
            p.closeInventory();
            return;
        }
        int homeNumber = p.getMetadata("editingHome").get(0).asInt();
        int slot = e.getRawSlot();
        int backSlot = plugin.getConfig().getInt("items.edit-menu.back-slot", 10);
        int deleteSlot = plugin.getConfig().getInt("items.edit-menu.delete-slot", 16);
        if (slot == backSlot) {
            plugin.getMainGui().openMain(p);
            p.removeMetadata("editingHome", plugin);
        } else if (slot == deleteSlot) {
            if (!homeManager.hasHome(p.getUniqueId(), homeNumber)) {
                p.sendMessage(plugin.format(plugin.getConfig().getString("messages.no-home")));
            } else {
                homeManager.deleteHome(p.getUniqueId(), homeNumber);
                p.sendMessage(plugin.format(plugin.getConfig().getString("messages.home-deleted")
                        .replace("%number%", String.valueOf(homeNumber))));
            }
            plugin.getMainGui().openMain(p);
            p.removeMetadata("editingHome", plugin);
        }
    }
}
