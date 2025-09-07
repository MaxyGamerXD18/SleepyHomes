package me.yourname.sleepyhomes.util;

import net.md_5.bungee.api.ChatColor;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import java.util.ArrayList;
import java.util.List;
public class ItemBuilder {
    public static String formatColors(String input) {
        if (input == null) return null;
        String legacy = input.replace('&', '§');
        return ChatColor.translateAlternateColorCodes('§', legacy);
    }
    public static ItemStack fromConfig(FileConfiguration cfg, String path) {
        if (!cfg.contains(path)) return new ItemStack(Material.AIR);
        String materialStr = cfg.getString(path + ".material", "BARRIER");
        Material mat = Material.matchMaterial(materialStr.toUpperCase());
        if (mat == null) mat = Material.BARRIER;
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return item;
        String name = cfg.getString(path + ".name", null);
        if (name != null) meta.setDisplayName(formatColors(name));
        List<String> lore = cfg.getStringList(path + ".lore");
        if (lore != null && !lore.isEmpty()) {
            List<String> formattedLore = new ArrayList<>();
            for (String line : lore) {
                formattedLore.add(formatColors(line));
            }
            meta.setLore(formattedLore);
        }
        item.setItemMeta(meta);
        return item;
    }
    public static void replaceNameAndLorePlaceholders(ItemStack item, String placeholder, String value) {
        if (item == null || item.getType().isAir()) return;
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return;
        if (meta.hasDisplayName()) {
            String replaced = meta.getDisplayName().replace(placeholder, value);
            meta.setDisplayName(formatColors(replaced));
        }
        if (meta.hasLore()) {
            List<String> lore = meta.getLore();
            List<String> replacedLore = new ArrayList<>();
            for (String line : lore) {
                replacedLore.add(formatColors(line.replace(placeholder, value)));
            }
            meta.setLore(replacedLore);
        }
        item.setItemMeta(meta);
    }
}
