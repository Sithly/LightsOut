package sith.me.lightsout;

import java.util.*;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Effect;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.plugin.java.JavaPlugin;

public class Main extends JavaPlugin implements Listener {
	private String modName = ChatColor.WHITE+"["+ChatColor.YELLOW+"Lights"+ChatColor.DARK_GRAY+"Out"+ChatColor.WHITE+"] "+ChatColor.RESET;
	
	private Map<Player, List<Block>> blockCache = new HashMap<Player, List<Block>>();
	private Map<Block, Integer> torches = new HashMap<Block, Integer>();
	
	private List<String> allowedWorlds;
	private Boolean isEnabled = true;
	
	private int radius = 5;
	private int life = 100;
	private int degrade = 1;
	//private int cooldown = 0;
	
	@Override
	public void onEnable() {
		getServer().getPluginManager().registerEvents(this, this);
		loadConfig();
	}
	@Override
	public void onDisable() {
	}
	
	@EventHandler
    public void onBlockBreak(BlockBreakEvent e) {
		if (!isEnabled) {return;}
		Player p = e.getPlayer();
		Boolean isListed = false;
	    for (String w : allowedWorlds) {
	    	if (Bukkit.getWorld(w) == p.getWorld()) {
	    		isListed = true;
	    	}
	    }
	    if (!isListed) { return; }
	    Block b = e.getBlock();
	    if (b.getType() == Material.WALL_TORCH || b.getType() == Material.TORCH) {
	    	if (torches.containsKey(b)) {
	    		torches.remove(b);
	    	}
	    	b.setType(Material.AIR);
	    }
	}
	
	@EventHandler
	public void onMove(PlayerMoveEvent e) {
		if (!isEnabled) {return;}
		if (torches.size() >= 200) { torches.clear(); blockCache.clear(); }
		Player p = e.getPlayer();
		Boolean isListed = false;
	    for (String w : allowedWorlds) {
	    	if (Bukkit.getWorld(w) == p.getWorld()) {
	    		isListed = true;
	    	}
	    }
	    if (!isListed) { return; }
		
		if (!blockCache.containsKey(p)) {
			blockCache.put(p, getNearbyBlocks(p.getLocation(), radius));
		} else {
			List<Block> reCache = getNearbyBlocks(p.getLocation(), radius);
			if (getNearbyBlocks(p.getLocation(), radius) != blockCache.get(p)) {
				blockCache.replace(p, blockCache.get(p), reCache);
			}
		}
		
		for (Block t : torches.keySet()) {
			if (torches.get(t) <= 0) {
				if (t != null) {
					t.getWorld().playSound(t.getLocation(), Sound.ENTITY_GENERIC_BURN, 5, 1);
					t.getWorld().playEffect(t.getLocation(), Effect.MOBSPAWNER_FLAMES, 1);
					t.setType(Material.AIR);
					t.breakNaturally();
					torches.remove(t);
					p.sendMessage(modName + "A torch near you has burned out");
				}
				break;
			} else {
				/*if (p.isOp() && !p.getName().contains("Agent")) {
					p.sendMessage(p.getWorld().getName() + " <> " + t.getWorld().getName());
					p.sendMessage(modName + blockCache.size() +":"+ blockCache.values().size() + ChatColor.RED + "|" + ChatColor.RESET + torches.size() +":" + torches.get(t));
				}*/
				if (p.getWorld().getName() == t.getWorld().getName()) {
					if (torches.get(t) == life/3 && p.getLocation().distance(t.getLocation()) <= radius) {
						p.sendMessage(modName + "A torch near you is starting to fade out...");
					}
					if (p.getLocation().distance(t.getLocation()) <= radius) {
						torches.replace(t, torches.get(t), torches.get(t) - degrade);
					}
				}
			}
		}
		
		for (Block b : blockCache.get(p)) {
			if (b.getType() == Material.WALL_TORCH || b.getType() == Material.TORCH) {
				Block torch = b;
				if (!torches.containsKey(torch)) {
					torches.put(torch, life);
				}
			}
		}
	}
	
	public boolean onCommand(CommandSender s, Command cmd, String cmdtype, String[] args) 
	{
		Player p;
		if (s instanceof Player) { p = (Player) s; } else { return true; }
		if (!s.isOp()) { return true; }
		switch(cmdtype) {
			case "lotoggle":
				isEnabled = !isEnabled;
				Bukkit.broadcastMessage(modName + "Is enabled has been set to "+isEnabled);
				updateConfig();
				break;
			case "loadd":
				if (!allowedWorlds.contains(p.getWorld().getName())) {
					allowedWorlds.add(p.getWorld().getName());
					Bukkit.broadcastMessage(modName + "An operator has added " + ChatColor.GREEN + p.getWorld().getName() + ChatColor.RESET + " to the active world list.");
					updateConfig();
				} else {
					p.sendMessage(modName + "That world is already on the active list...");
				}
				break;
			case "loremove":
				if (allowedWorlds.contains(p.getWorld().getName())) {
					allowedWorlds.remove(p.getWorld().getName());
					Bukkit.broadcastMessage(modName + "An operator has removed " + ChatColor.RED + p.getWorld().getName() + ChatColor.RESET + " from the active world list.");
					updateConfig();
				} else {
					p.sendMessage(modName + "That world is not on the active world list...");
				}
				break;
			case "lolife":
				if (args.length >= 1) {
					try { 
						life = Integer.parseInt(args[0]);
						Bukkit.broadcastMessage(modName + "An operator has set the torch life to " + ChatColor.YELLOW + args[0] + ChatColor.RESET);
						updateConfig();
					} catch(NumberFormatException error) {
						p.sendMessage(modName + "Failed to parse that integer");
					}
				} else {
					p.sendMessage(modName + "You need to provide a positive number, default(400)");
				}
				break;
			case "lodegrade":
				if (args.length >= 1) {
					try { 
						degrade = Integer.parseInt(args[0]);
						Bukkit.broadcastMessage(modName + "An operator has set the torch degrade to " + ChatColor.YELLOW + args[0] + ChatColor.RESET);
						updateConfig();
					} catch(NumberFormatException error) {
						p.sendMessage(modName + "Failed to parse that integer");
					}
				} else {
					p.sendMessage(modName + "You need to provide a positive number, default(1)");
				}
				break;
			case "loradius":
				if (args.length >= 1) {
					try { 
						radius = Integer.parseInt(args[0]);
						Bukkit.broadcastMessage(modName + "An operator has set the scan radius to " + ChatColor.YELLOW + args[0] + ChatColor.RESET);
						updateConfig();
					} catch(NumberFormatException error) {
						p.sendMessage(modName + "Failed to parse that integer");
					}
				} else {
					p.sendMessage(modName + "You need to provide a positive number, default(6)");
				}
				break;
			default:
				p.sendMessage(modName + "Something went wrong...");
				break;
		}
		return true;
	}
	
	public List<Block> getNearbyBlocks(Location l, Integer r) {
        List<Block> blocks = new ArrayList<Block>();
        for(int x = l.getBlockX() - r; x <= l.getBlockX() + r; x++) {
            for(int y = l.getBlockY() - r; y <= l.getBlockY() + r; y++) {
                for(int z = l.getBlockZ() - r; z <= l.getBlockZ() + r; z++) {
                   blocks.add(l.getWorld().getBlockAt(x, y, z));
                }
            }
        }
        return blocks;
    }

	public void loadConfig() {
		this.getConfig().options().copyDefaults(true);
		blockCache.clear();
		torches.clear();
		degrade = this.getConfig().getInt("global-degrade");
		radius = this.getConfig().getInt("global-radius");
		life = this.getConfig().getInt("global-life");
		isEnabled = this.getConfig().getBoolean("is-enabled");
		if (this.getConfig().getStringList("allowed-worlds") != null) {
			allowedWorlds = this.getConfig().getStringList("allowed-worlds");
		}
		this.saveConfig();
	}
	
	public void updateConfig() {
		this.getConfig().set("is-enabled", isEnabled);
		this.getConfig().set("global-degrade", degrade);
		this.getConfig().set("global-life", life);
		this.getConfig().set("global-radius", radius);
		this.getConfig().set("allowed-worlds", allowedWorlds);
		this.saveConfig();
		Bukkit.broadcastMessage(modName + "The config was updated.");
	}
}
