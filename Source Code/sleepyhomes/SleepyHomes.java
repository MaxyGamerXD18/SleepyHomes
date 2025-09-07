package me.yourname.sleepyhomes;

import me.yourname.sleepyhomes.data.HomeManager;
import me.yourname.sleepyhomes.gui.EditHomeGUI;
import me.yourname.sleepyhomes.gui.MainHomesGUI;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.StringUtil;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SleepyHomes extends JavaPlugin implements TabExecutor {
    private static SleepyHomes instance;
    private HomeManager homeManager;
    private MainHomesGUI mainGui;
    private EditHomeGUI editGui;
    public static SleepyHomes getInstance() {
        return instance;
    }
    public HomeManager getHomeManager() {
        return homeManager;
    }
    public MainHomesGUI getMainGui() {
        return mainGui;
    }
    public EditHomeGUI getEditGui() {
        return editGui;
    }

    public static String format(String message) {
        if (message == null) return "";
        message = ChatColor.translateAlternateColorCodes('&', message);
        Pattern hexPattern = Pattern.compile("#[a-fA-F0-9]{6}");
        Matcher matcher = hexPattern.matcher(message);
        StringBuffer buffer = new StringBuffer();
        while (matcher.find()) {
            String color = matcher.group();
            matcher.appendReplacement(buffer, ChatColor.of(color) + "");
        }
        matcher.appendTail(buffer);
        return buffer.toString();
    }
    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();
        homeManager = new HomeManager(this);
        mainGui = new MainHomesGUI(this);
        editGui = new EditHomeGUI(this);
        getServer().getPluginManager().registerEvents(mainGui, this);
        getServer().getPluginManager().registerEvents(editGui, this);
        getCommand("homes").setExecutor(this);
        getCommand("home").setExecutor(this);
        getCommand("sethome").setExecutor(this);
        getCommand("delhome").setExecutor(this);
        getCommand("sleepyhomes").setExecutor(this);
        getCommand("home").setTabCompleter(this);
        getCommand("homes").setTabCompleter(this);
        getCommand("sleepyhomes").setTabCompleter(this);
        getCommand("delhome").setTabCompleter(this);
        getLogger().info("SleepyHomes enabled.");
    }
    @Override
    public void onDisable() {
        if (homeManager != null) homeManager.saveAll();
        getLogger().info("SleepyHomes disabled.");
    }
    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Only players.");
            return true;
        }
        Player p = (Player) sender;
        switch (cmd.getName().toLowerCase()) {
            case "homes":
            case "home":
                handleHomeOrHomesCommand(p, args);
                break;
            case "sethome":
                handleSetHome(p);
                break;
            case "delhome":
                handleDelHome(p, args);
                break;
            case "sleepyhomes":
                handleInfoOrReloadCommand(p, args);
                break;
            default:
                return false;
        }
        return true;
    }
    private void handleHomeOrHomesCommand(Player p, String[] args) {
        if (args.length == 0) {
            mainGui.openMain(p);
            return;
        }
        String invalidMsg = format(getConfig().getString("messages.invalid-home", "&cYou must select a valid home."));
        int num;
        try {
            num = Integer.parseInt(args[0]);
        } catch (NumberFormatException e) {
            p.sendMessage(invalidMsg);
            return;
        }
        if (num < 1 || num > 7) {
            p.sendMessage(invalidMsg);
            return;
        }
        String permission = getConfig().getString("home-permissions." + num);
        if (permission != null && !p.hasPermission(permission)) {
            String noPermMsg = format(getConfig().getString("messages.no-permission", "&cYou don't have permission for this home #%number%."))
                    .replace("%number%", String.valueOf(num));
            p.sendMessage(noPermMsg);
            return;
        }
        if (!homeManager.hasHome(p.getUniqueId(), num)) {
            String noHomeMsg = format(getConfig().getString("messages.no-home", "&cNo home #%number% has been set!"))
                    .replace("%number%", String.valueOf(num));
            p.sendMessage(noHomeMsg);
            return;
        }
        Location homeLoc = homeManager.getHome(p.getUniqueId(), num);
        if (homeLoc != null) {
            mainGui.startTeleportCountdown(p, homeLoc);
        }
    }
    private void handleSetHome(Player p) {
        boolean setHome = false;
        for (int i = 1; i <= 7; i++) {
            String permission = getConfig().getString("home-permissions." + i);
            if (permission != null && !p.hasPermission(permission)) {
                continue;
            }
            if (!homeManager.hasHome(p.getUniqueId(), i)) {
                homeManager.setHome(p.getUniqueId(), i, p.getLocation());
                String msg = format(getConfig().getString("messages.home-set", "&aHome #%number% has been set."));
                p.sendMessage(msg.replace("%number%", String.valueOf(i)));
                setHome = true;
                break;
            }
        }
        if (!setHome) {
            p.sendMessage(format("&cAll home slots (1–7) you have permission for are already set or unavailable."));
        }
    }
    private void handleDelHome(Player p, String[] args) {
        String invalidMsg = format(getConfig().getString("messages.invalid-home", "&cYou must select a valid home."));
        if (args.length != 1) {
            p.sendMessage(invalidMsg);
            return;
        }
        int num;
        try {
            num = Integer.parseInt(args[0]);
        } catch (NumberFormatException e) {
            p.sendMessage(invalidMsg);
            return;
        }
        if (num < 1 || num > 7) {
            p.sendMessage(invalidMsg);
            return;
        }
        if (!homeManager.hasHome(p.getUniqueId(), num)) {
            String noHomeMsg = format(getConfig().getString("messages.no-home", "&cNo home #%number% has been set!"))
                    .replace("%number%", String.valueOf(num));
            p.sendMessage(noHomeMsg);
            return;
        }
        homeManager.deleteHome(p.getUniqueId(), num);
        String delMsg = format(getConfig().getString("messages.home-deleted", "&cHome #%number% has been deleted."))
                .replace("%number%", String.valueOf(num));
        p.sendMessage(delMsg);
    }
    private void handleInfoOrReloadCommand(Player p, String[] args) {
        if (args.length == 1 && args[0].equalsIgnoreCase("reload")) {
            if (!p.hasPermission("sleepyhomes.reload")) {
                p.sendMessage(format(getConfig().getString("messages.no-permission", "&cYou don't have permission.")));
                return;
            }
            reloadConfig();
            saveDefaultConfig();
            getConfig().options().copyDefaults(true);
            saveConfig();

            String msg = format(getConfig().getString("messages.config-reloaded", "&aConfiguration reloaded successfully!"));
            p.sendMessage(msg);
            return;
        }
        if (args.length == 1 && args[0].equalsIgnoreCase("about")) {
            sendAboutMessage(p);
            return;
        }
        if (args.length == 1 && args[0].equalsIgnoreCase("version")) {
            p.sendMessage(format("&fSleepyHomes version: &a" + getDescription().getVersion()));
            return;
        }
        sendAboutMessage(p);
    }

    private void sendAboutMessage(Player p) {
        p.sendMessage(format("&8&l&m----------&r &b&lSleepyHomes Info &8&l&m----------"));
        p.sendMessage(format("&fVersion: &a" + getDescription().getVersion()));
        p.sendMessage(format("&fAuthor: &a" + String.join(", ", getDescription().getAuthors())));
        String website = getDescription().getWebsite() != null
                ? getDescription().getWebsite().toString()
                : "https://discord.gg/AQ4YgRYzXT";
        TextComponent supportText = new TextComponent(format("&fSupport: &a" + website));
        supportText.setClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, website));
        p.spigot().sendMessage(supportText);
        p.sendMessage(format("&8&l&m--------------------------------"));
    }
    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String alias, String[] args) {
        if (cmd.getName().equalsIgnoreCase("sleepyhomes")) {
            if (args.length == 1) {
                List<String> subCommands = new ArrayList<>();
                if (sender.hasPermission("sleepyhomes.reload")) subCommands.add("reload");
                subCommands.add("about");
                subCommands.add("version");
                return StringUtil.copyPartialMatches(args[0], subCommands, new ArrayList<>());
            }
            return Collections.emptyList();
        }
        if (cmd.getName().equalsIgnoreCase("home") || cmd.getName().equalsIgnoreCase("homes")) {
            if (args.length == 1) {
                List<String> homeNumbers = new ArrayList<>();
                for (int i = 1; i <= 7; i++) {
                    homeNumbers.add(String.valueOf(i));
                }
                return StringUtil.copyPartialMatches(args[0], homeNumbers, new ArrayList<>());
            }
        }
        if (cmd.getName().equalsIgnoreCase("delhome")) {
            if (args.length == 1) {
                List<String> homeNumbers = new ArrayList<>();
                for (int i = 1; i <= 7; i++) {
                    homeNumbers.add(String.valueOf(i));
                }
                return StringUtil.copyPartialMatches(args[0], homeNumbers, new ArrayList<>());
            }
        }
        return Collections.emptyList();
    }
}
