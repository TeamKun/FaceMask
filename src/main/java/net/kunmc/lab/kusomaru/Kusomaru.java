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
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public final class Kusomaru extends JavaPlugin implements TabCompleter, CommandExecutor, Listener {
    private final Map<String, Face> Faces = new HashMap<>();
    private int CustomModelData = 256;
    private final HashMap<UUID, Face> wearers = new HashMap<>();

    @Override
    public void onEnable() {
        getDataFolder().mkdirs();
        saveDefaultConfig();
        FileConfiguration config = getConfig();
        Map<String,String> faceRelations = ((Map<String, String>) config.getMapList("Faces").get(0));
        for (String key : faceRelations.keySet()) {
            Faces.put(key, new Face(key, Material.valueOf(faceRelations.get(key))));
        }
        CustomModelData = config.getInt("CustomModelData");

        getServer().getPluginManager().registerEvents(this, this);
        getServer().getPluginCommand("kusomaru").setExecutor(this);
    }

    @Override
    public void onDisable() {
        for (UUID uuid : wearers.keySet()) {
            Player p = Bukkit.getPlayer(uuid);
            if (p == null) continue;
            ItemStack[] armors = p.getInventory().getArmorContents();
            armors[3] = new ItemStack(Material.AIR);
            p.getInventory().setArmorContents(armors);
            wearers.remove(p.getUniqueId());
        }
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

                List<Player> players = Arrays.stream(Objects.requireNonNull(CommandUtils.getTargets(sender, args[1]))).filter(x -> x instanceof Player).map((x -> (Player)x)).collect(Collectors.toList());
                if (players.isEmpty()) {
                    sender.sendMessage(ChatColor.RED + "変更対象は存在しません.");
                    break;
                }

                String facename = args[2].toLowerCase();
                if (Faces.keySet().stream().noneMatch(x -> x.equalsIgnoreCase(facename))) {
                    sender.sendMessage(ChatColor.RED + facename + "は存在しません");
                    break;
                }

                Face face = Faces.get(facename);
                ItemStack item = new ItemStack(face.material);
                ItemMeta meta = item.getItemMeta();
                meta.setCustomModelData(CustomModelData);
                item.setItemMeta(meta);

                for (Player p : players) {
                    ItemStack[] armors = p.getInventory().getArmorContents();
                    p.getInventory().addItem(armors[3]);
                    armors[3] = item;
                    p.getInventory().setArmorContents(armors);
                    wearers.put(p.getUniqueId(), face);
                }
                break;
            }
            case "unset": {
                if (args.length < 2) {
                    sender.sendMessage("Usage: /kusomaru unset <player>");
                    break;
                }

                List<Player> players = Arrays.stream(Objects.requireNonNull(CommandUtils.getTargets(sender, args[1]))).filter(x -> x instanceof Player).map((x -> (Player)x)).collect(Collectors.toList());
                if (players.isEmpty()) {
                    sender.sendMessage(ChatColor.RED + "変更対象は存在しません.");
                    break;
                }
                for (Player p : players) {
                    ItemStack[] armors = p.getInventory().getArmorContents();
                    armors[3] = new ItemStack(Material.AIR);
                    p.getInventory().setArmorContents(armors);
                    wearers.remove(p.getUniqueId());
                }
                break;
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
                return Stream.concat(Bukkit.getOnlinePlayers().stream().map(Player::getName),Stream.of("@a", "@a[distance=..")).filter(x -> x.startsWith(args[1])).collect(Collectors.toList());
            case 3:
                if (args[0].equalsIgnoreCase("set")) {
                    return Faces.keySet().stream().filter(x -> x.startsWith(args[2])).collect(Collectors.toList());
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
}
