package net.kunmc.lab.kusomaru;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public final class Kusomaru extends JavaPlugin implements TabCompleter, CommandExecutor, Listener {
    private static Material KUSOMARU1;
    private static Material KUSOMARU2;
    private static Material KUN;
    private final HashMap<UUID, Face> wearers = new HashMap<>();

    @Override
    public void onEnable() {
        getDataFolder().mkdirs();
        saveDefaultConfig();
        FileConfiguration config = getConfig();
        KUSOMARU1 = Material.valueOf(config.getString("kusomaru1"));
        KUSOMARU2 = Material.valueOf(config.getString("kusomaru2"));
        KUN = Material.valueOf(config.getString("kun"));
        getServer().getPluginManager().registerEvents(this, this);
        getServer().getPluginCommand("kusomaru").setExecutor(this);
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (args.length < 1) return false;
        switch (args[0].toLowerCase()) {
            case "set": {
                if (args.length < 3) {
                    sender.sendMessage("Usage: /kusomaru set <player> <facename>");
                    break;
                }

                Player p = Bukkit.getPlayer(args[1]);
                if (p == null) {
                    sender.sendMessage(ChatColor.RED + args[1] + "は存在しません.");
                    break;
                }

                String facename = args[2].toLowerCase();
                if (Arrays.stream(Face.values()).noneMatch(x -> x.label.equalsIgnoreCase(facename))) {
                    sender.sendMessage(ChatColor.RED + facename + "は存在しません");
                    break;
                }
                Face item = Face.valueOf(facename);
                ItemStack[] armors = p.getInventory().getArmorContents();
                armors[3] = new ItemStack(item.material);
                ;
                p.getInventory().setArmorContents(armors);
                wearers.put(p.getUniqueId(), item);
                break;
            }
            case "unset": {
                if (args.length < 2) {
                    sender.sendMessage("Usage: /kusomaru unset <player>");
                    break;
                }

                Player p = Bukkit.getPlayer(args[1]);
                if (p == null) {
                    sender.sendMessage(ChatColor.RED + args[1] + "は存在しません.");
                    break;
                }
                ItemStack[] armors = p.getInventory().getArmorContents();
                armors[3] = new ItemStack(Material.AIR);
                p.getInventory().setArmorContents(armors);
                wearers.remove(p.getUniqueId());
            }
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        switch (args.length) {
            case 1:
                return Stream.of("set", "unset").filter(x -> x.startsWith(args[0])).collect(Collectors.toList());
            case 2:
                return Bukkit.getOnlinePlayers().stream().map(Player::getName).filter(x -> x.startsWith(args[1])).collect(Collectors.toList());
            case 3:
                if (args[2].equalsIgnoreCase("set")) {
                    return Arrays.stream(Face.values()).map(x -> x.label).filter(x -> x.startsWith(args[2])).collect(Collectors.toList());
                }
            default:
                return new ArrayList<>();
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onDeath(PlayerDeathEvent e) {
        Player p = e.getEntity();
        if (wearers.containsKey(p.getUniqueId())) {
            ItemStack[] armors = p.getInventory().getArmorContents();
            armors[3] = new ItemStack(Material.AIR);
            p.getInventory().setArmorContents(armors);
            e.getDrops().removeIf(x -> x.getType() == wearers.get(p.getUniqueId()).material);
        }
    }

    @EventHandler
    public void onRespawn(PlayerRespawnEvent e) {
        Player p = e.getPlayer();
        if (wearers.containsKey(p.getUniqueId())) {
            ItemStack[] armors = p.getInventory().getArmorContents();
            armors[3] = new ItemStack(wearers.get(p.getUniqueId()).material);
            p.getInventory().setArmorContents(armors);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInventoryClick(InventoryClickEvent e) {
        Player p = (Player) (e.getInventory().getHolder());
        if (wearers.containsKey(p.getUniqueId()) && e.getSlot() == 39) {
            e.setCancelled(true);
            p.closeInventory();
        }
    }

    private enum Face {
        kusomaru1("kusomaru1", KUSOMARU1),
        kusomaru2("kusomaru2", KUSOMARU2),
        kun("kun", KUN);

        private final String label;
        private final Material material;

        Face(String label, Material material) {
            this.label = label;
            this.material = material;
        }
    }
}
