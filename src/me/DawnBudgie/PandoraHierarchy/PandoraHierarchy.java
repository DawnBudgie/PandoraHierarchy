package me.DawnBudgie.PandoraHierarchy;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.logging.Level;

import net.milkbowl.vault.permission.Permission;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

public class PandoraHierarchy
  extends JavaPlugin
  implements CommandExecutor, Listener
{
  private static PandoraHierarchy plugin = null;
  public void onEnable()
  {
    plugin = this;
    if ((getConfig().getBoolean("settings.notify-update")) && (isUpdateAvailable()))
    {
      getLogger().info("There is an update available for pandorahierarchy!");
      getLogger().info("Download at: http://github.com/DawnBudgie/pandorahierarchy/");
    }
    if (!Bukkit.getPluginManager().isPluginEnabled("Vault"))
    {
      disable(Level.WARNING, 
        "There was an error enabling PandoraHierarchy! Is Vault installed?");
      return;
    }
    RegisteredServiceProvider<Permission> permissionProvider = Bukkit.getServicesManager().getRegistration(Permission.class);
    if (permissionProvider != null)
    {
      getProvider();
    }
    else
    {
      disable(Level.WARNING, 
        "There was an error enabling PandoraHierarchy! Is Vault installed?");
      return;
    }
    reload();
  }
  
  private void getProvider() {
	// TODO Auto-generated method stub
	
}

private boolean isUpdateAvailable() {
	// TODO Auto-generated method stub
	return false;
}

public void onDisable()
  {
    plugin = null;
  }
  
  public void disable(Level level, String reason)
  {
    getLogger().log(level, reason);
    setEnabled(false);
  }
  
  public void reload()
  {
    if (!new File(getDataFolder(), "config.yml").exists()) {
      saveDefaultConfig();
    }
    reloadConfig();
  }
  
  public boolean onCommand(CommandSender sender, Command cmd, String commandLabel, String[] args)
  {
    if (commandLabel.equalsIgnoreCase("rank")) {
      switch (args.length)
      {
      case 0: 
      default: 
        sender.sendMessage(ChatColor.GOLD + "<>---------------[ " + ChatColor.GOLD + getDescription().getName() + ChatColor.GOLD + " ]---------------<>");
        sender.sendMessage(ChatColor.GOLD + "Description: " + ChatColor.DARK_RED + getDescription().getDescription());
        sender.sendMessage(ChatColor.GOLD + "Author: " + ChatColor.DARK_RED + (String)getDescription().getAuthors().get(0));
        sender.sendMessage(ChatColor.GOLD + "Version: " + ChatColor.DARK_RED + getDescription().getVersion());
        sender.sendMessage(ChatColor.GOLD + "Website: " + ChatColor.DARK_RED + getDescription().getWebsite());
        sender.sendMessage(ChatColor.DARK_RED + "For help type: " + ChatColor.GOLD + "/" + commandLabel + " help");
        break;
      case 1: 
        if (args[0].equalsIgnoreCase("help"))
        {
          sender.sendMessage(ChatColor.GOLD + "<>-------------[ " + ChatColor.DARK_RED + getDescription().getName() + ChatColor.GOLD + " ]-------------<>");
          sender.sendMessage(ChatColor.GRAY + "Required: < > Optional: [ ]");
          sender.sendMessage(ChatColor.DARK_RED + "-" + ChatColor.GOLD + " /rank <Name> <Group> [World]");
          sender.sendMessage(ChatColor.DARK_RED + "-" + ChatColor.GOLD + " /rank reload");
        }
        else if (args[0].equalsIgnoreCase("reload"))
        {
          if (!sender.hasPermission("pandorahierarchy.reload"))
          {
            sender.sendMessage(ChatColor.RED + "You do not have permission for this command!");
            return true;
          }
          reload();
          sender.sendMessage(ChatColor.GREEN + "Configuration reloaded!");
        }
        else
        {
          sender.sendMessage("The specified command was not found! Type /" + commandLabel + " help to see available commands.");
        }
        break;
      case 2: 
        rank(sender, args[0], args[1], null);
        
        break;
      case 3: 
        rank(sender, args[0], args[1], args[2]);
      }
    }
    return true;
  }
  
  public void rank(CommandSender sender, String name, String group, String world)
  {
    String messageToRankedPlayer = getConfig().getString("messages.to-ranked-player");
    String messageToOnlinePlayers = getConfig().getString("messages.to-online-players");
    String messageToRankingPlayer = getConfig().getString("messages.to-ranking-player");
    
    boolean logChanges = getConfig().getBoolean("settings.log-changes");
    boolean autoComplete = getConfig().getBoolean("settings.auto-complete");
    if (!sender.hasPermission("pandorahierarchy.rank." + group.toLowerCase()))
    {
      sender.sendMessage(ChatColor.RED + "You cannot rank players to the specified group!");
      return;
    }
    if (autoComplete) {
      for (Player p : Bukkit.getServer().getOnlinePlayers()) {
        if (p.getName().startsWith(name))
        {
          name = p.getName();
          break;
        }
      }
    }
    for (String g : getPermissions().getPlayerGroups(world, name)) {
      getPermissions().playerRemoveGroup(world, name, g);
    }
    getPermissions().playerAddGroup(world, name, group);
    if (logChanges) {
      logChange(sender, name, group, world);
    }
    for (Player p : Bukkit.getServer().getOnlinePlayers()) {
      if (!sender.equals(p)) {
        p.sendMessage(parseMessage(messageToOnlinePlayers, sender.getName(), name, group));
      }
    }
    sender.sendMessage(parseMessage(messageToRankingPlayer, sender.getName(), name, group));
    if (Bukkit.getServer().getPlayerExact(name) != null) {
      Bukkit.getServer().getPlayerExact(name).sendMessage(parseMessage(messageToRankedPlayer, sender.getName(), name, group));
    }
  }
  
  private Permission getPermissions() {
	// TODO Auto-generated method stub
	return null;
}

public String parseMessage(String base, String ranker, String ranked, String rank)
  {
    String msg = base;
    
    msg = msg.replace("{RANKER}", ranker);
    msg = msg.replace("{RANKED}", ranked);
    msg = msg.replace("{RANK}", rank);
    msg = ChatColor.translateAlternateColorCodes("&".charAt(0), msg);
    
    return msg;
  }
  
  public void logChange(CommandSender sender, String name, String group, String world)
  {
    try
    {
      FileWriter fstream = new FileWriter(plugin.getDataFolder() + File.separator + "log.txt", true);
      BufferedWriter out = new BufferedWriter(fstream);
      
      DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
      Date date = new Date();
      out.append("At: " + dateFormat.format(date) + " " + sender.getName() + " made " + name + " a(n) " + group + (world != null ? " in " + world : "") + "!\n");
      
      out.close();
    }
    catch (Exception e)
    {
      System.err.println(e.getMessage());
    }
  }
  
}
