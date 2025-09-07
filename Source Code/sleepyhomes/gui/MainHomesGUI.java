package me.yourname.sleepyhomes.gui;
import me.yourname.sleepyhomes.SleepyHomes;
import me.yourname.sleepyhomes.data.HomeManager;
import me.yourname.sleepyhomes.util.ItemBuilder;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.ChatMessageType;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
public class MainHomesGUI implements Listener {
    private final SleepyHomes plugin;
    private final HomeManager homeManager;
    public MainHomesGUI(SleepyHomes plugin) {
        this.plugin = plugin;
        this.homeManager = plugin.getHomeManager();
    }
    public void openMain(Player p) {
        int rows = plugin.getConfig().getInt("settings.rows", 4);
        if (rows < 1) rows = 1;
        if (rows > 6) rows = 6;
        String title = plugin.getConfig().getString("menu-titles.main-homes", "&6Your Homes");
        Inventory inv = Bukkit.createInventory(null, rows * 9, plugin.format(title));
        List<Integer> homeSlots = plugin.getConfig().getIntegerList("settings.homes-slots");
        List<Integer> dyeSlots = plugin.getConfig().getIntegerList("settings.dyes-slots");
        if (homeSlots.size() < 7) {
            homeSlots = List.of(10, 11, 12, 13, 14, 15, 16);
        }
        if (dyeSlots.size() < 7) {
            dyeSlots = List.of(19, 20, 21, 22, 23, 24, 25);
        }
        ConfigurationSection items = plugin.getConfig().getConfigurationSection("items");
        for (int i = 0; i < 7; i++) {
            int homeSlot = homeSlots.get(i);
            int dyeSlot = dyeSlots.get(i);
            int homeNumber = i + 1;
            boolean has = homeManager.hasHome(p.getUniqueId(), homeNumber);
            String bedKey = has ? "items.bed-set" : "items.bed-empty";
            ItemStack bedItem = ItemBuilder.fromConfig(plugin.getConfig(), bedKey);
            ItemBuilder.replaceNameAndLorePlaceholders(bedItem, "%number%", String.valueOf(homeNumber));
            inv.setItem(homeSlot, bedItem);
            String dyeKey = has ? "items.dye-set" : "items.dye-empty";
            ItemStack dyeItem = ItemBuilder.fromConfig(plugin.getConfig(), dyeKey);
            ItemBuilder.replaceNameAndLorePlaceholders(dyeItem, "%number%", String.valueOf(homeNumber));
            inv.setItem(dyeSlot, dyeItem);
        }
        p.openInventory(inv);
    }
    @EventHandler
    public void onClick(InventoryClickEvent e) {
        if (!(e.getWhoClicked() instanceof Player)) return;
        Player p = (Player) e.getWhoClicked();
        if (e.getView().getTitle() == null) return;
        String title = e.getView().getTitle();
        String strippedTitle = org.bukkit.ChatColor.stripColor(title);
        String configuredTitle = plugin.getConfig().getString("menu-titles.main-homes", "&6Your Homes");
        String formattedTitle = org.bukkit.ChatColor.stripColor(plugin.format(configuredTitle));
        if (!strippedTitle.startsWith(formattedTitle)) return;
        e.setCancelled(true);
        int raw = e.getRawSlot();
        List<Integer> homeSlots = plugin.getConfig().getIntegerList("settings.homes-slots");
        List<Integer> dyeSlots = plugin.getConfig().getIntegerList("settings.dyes-slots");
        if (homeSlots.size() < 7) homeSlots = List.of(10, 11, 12, 13, 14, 15, 16);
        if (dyeSlots.size() < 7) dyeSlots = List.of(19, 20, 21, 22, 23, 24, 25);
        for (int i = 0; i < 7; i++) {
            int homeNumber = i + 1;
            if (raw == homeSlots.get(i)) {
                if (!p.hasPermission("home.teleport." + homeNumber)) {
                    p.sendMessage(plugin.format(plugin.getConfig().getString("messages.no-permission")));
                    p.closeInventory();
                    return;
                }
                if (!homeManager.hasHome(p.getUniqueId(), homeNumber)) {
                    p.sendMessage(plugin.format(plugin.getConfig().getString("messages.no-home")));
                    return;
                }
                startTeleportCountdown(p, homeManager.getHome(p.getUniqueId(), homeNumber));
                p.closeInventory();
                return;
            } else if (raw == dyeSlots.get(i)) {
                if (!p.hasPermission("home.teleport." + homeNumber)) {
                    p.sendMessage(plugin.format(plugin.getConfig().getString("messages.no-permission")));
                    p.closeInventory();
                    return;
                }
                if (!homeManager.hasHome(p.getUniqueId(), homeNumber)) {
                    homeManager.setHome(p.getUniqueId(), homeNumber, p.getLocation());
                    p.sendMessage(plugin.format(plugin.getConfig().getString("messages.home-set")
                            .replace("%number%", String.valueOf(homeNumber))));
                    openMain(p);
                } else {
                    plugin.getEditGui().openEdit(p, homeNumber);
                }
                return;
            }
        }
    }
    public void startTeleportCountdown(Player p, org.bukkit.Location loc) {
        int delay = plugin.getConfig().getInt("settings.teleport-delay", 5);
        for (int i = delay; i >= 1; i--) {
            final int secondsLeft = i;
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                String msg = plugin.getConfig().getString("messages.teleporting", "&7Teleporting in &a%seconds%");
                msg = msg.replace("%seconds%", String.valueOf(secondsLeft));
                BaseComponent[] components = parseColors(plugin.format(msg));
                p.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(components));
                if (secondsLeft == 1) {
                    p.teleport(loc);
                }
            }, (delay - i) * 20L);
        }
    }
    private BaseComponent[] parseColors(String message) {
        if (message == null) return new BaseComponent[0];

        Pattern hexPattern = Pattern.compile("&#([A-Fa-f0-9]{6})");
        Matcher matcher = hexPattern.matcher(message);
        StringBuffer sb = new StringBuffer();

        while (matcher.find()) {
            String hexCode = matcher.group(1);
            String replacement = ChatColor.of("#" + hexCode).toString();
            matcher.appendReplacement(sb, replacement);
        }
        matcher.appendTail(sb);

        return TextComponent.fromLegacyText(sb.toString());
    }
}
