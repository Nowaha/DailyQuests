package me.nowaha.dailyquests;

import org.bukkit.ChatColor;
import org.bukkit.plugin.java.*;
import net.milkbowl.vault.economy.*;
import org.bukkit.plugin.*;
import me.tyler.ts.event.*;
import net.md_5.bungee.api.*;
import net.md_5.bungee.api.chat.*;
import org.bukkit.entity.*;
import org.bukkit.event.*;
import org.bukkit.event.entity.*;
import org.bukkit.event.block.*;
import org.bukkit.event.inventory.*;
import me.nowaha.points.*;
import org.bukkit.*;
import org.bukkit.configuration.*;
import org.bukkit.command.*;
import org.bukkit.enchantments.*;
import org.bukkit.inventory.*;
import java.util.*;
import org.bukkit.scheduler.*;

public final class DailyQuests extends JavaPlugin implements Listener
{
    Economy econ;
    Map<Material, Map<UUID, Map<Integer, Integer>>> playersCollecting;
    Map<Material, Map<UUID, Map<Integer, Integer>>> playersSmelting;
    Map<Material, Map<UUID, Map<Integer, Integer>>> playersSelling;
    Map<Material, Map<UUID, Map<Double, Double>>> playersSellingMoney;
    Map<EntityType, Map<UUID, Map<Integer, Integer>>> playersKilling;
    Map<UUID, Integer> activeQuests;
    
    public DailyQuests() {
        this.playersCollecting = new HashMap<Material, Map<UUID, Map<Integer, Integer>>>();
        this.playersSmelting = new HashMap<Material, Map<UUID, Map<Integer, Integer>>>();
        this.playersSelling = new HashMap<Material, Map<UUID, Map<Integer, Integer>>>();
        this.playersSellingMoney = new HashMap<Material, Map<UUID, Map<Double, Double>>>();
        this.playersKilling = new HashMap<EntityType, Map<UUID, Map<Integer, Integer>>>();
        this.activeQuests = new HashMap<UUID, Integer>();
    }
    
    public void onEnable() {
        this.saveResource("config.yml", false);
        this.getServer().getPluginManager().registerEvents((Listener)this, (Plugin)this);
        if (!this.setupEconomy()) {
            this.getLogger().severe(String.format("[%s] - Disabled due to no Vault dependency found!", this.getDescription().getName()));
            this.getServer().getPluginManager().disablePlugin((Plugin)this);
        }
    }
    
    private boolean setupEconomy() {
        if (this.getServer().getPluginManager().getPlugin("Vault") == null) {
            return false;
        }
        final RegisteredServiceProvider<Economy> rsp = (RegisteredServiceProvider<Economy>)this.getServer().getServicesManager().getRegistration((Class)Economy.class);
        if (rsp == null) {
            return false;
        }
        this.econ = (Economy)rsp.getProvider();
        return this.econ != null;
    }
    
    public void onDisable() {
    }
    
    @EventHandler(ignoreCancelled = true)
    public void onSell(final SellEvent e) {
        for (final ItemStack item : e.getItems()) {
            if (item == null) {
                continue;
            }
            if (this.playersSelling.containsKey(item.getType())) {
                final Map<UUID, Map<Integer, Integer>> currentlyCollecting = this.playersSelling.get(item.getType());
                if (currentlyCollecting.containsKey(e.getPlayer().getUniqueId())) {
                    final Map<Integer, Integer> playerData = currentlyCollecting.get(e.getPlayer().getUniqueId());
                    Integer currentAmount = (Integer)playerData.keySet().toArray()[0];
                    final Integer requiredamount = (Integer)playerData.values().toArray()[0];
                    currentAmount += item.getAmount();
                    playerData.clear();
                    playerData.put(currentAmount, requiredamount);
                    if (currentAmount.equals(requiredamount) || (currentAmount >= requiredamount && currentAmount - item.getAmount() < requiredamount)) {
                        e.getPlayer().sendMessage(" ");
                        e.getPlayer().sendMessage(new ComponentBuilder("§a§lQUEST COMPLETE! §aClick here to view progress!").event(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new ComponentBuilder("§3Click to view your progress!").create())).event(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/dq")).create());
                        e.getPlayer().sendMessage(" ");
                        final Player player = e.getPlayer();
                        this.playCompletionSound(player);
                    }
                    currentlyCollecting.put(e.getPlayer().getUniqueId(), playerData);
                    e.getPlayer().sendMessage(ChatMessageType.ACTION_BAR, new BaseComponent[] { new TextComponent("§a" + currentAmount + " / " + requiredamount + " sold!") });
                    this.playersSelling.put(item.getType(), currentlyCollecting);
                }
            }
            if (!this.playersSellingMoney.containsKey(item.getType())) {
                continue;
            }
            final Map<UUID, Map<Double, Double>> currentlyCollecting2 = this.playersSellingMoney.get(item.getType());
            if (!currentlyCollecting2.containsKey(e.getPlayer().getUniqueId())) {
                continue;
            }
            final Map<Double, Double> playerData2 = currentlyCollecting2.get(e.getPlayer().getUniqueId());
            Double currentAmount2 = (Double)playerData2.keySet().toArray()[0];
            final Double requiredamount2 = (Double)playerData2.values().toArray()[0];
            currentAmount2 += e.getItemCosts().get(item);
            playerData2.clear();
            playerData2.put(currentAmount2, requiredamount2);
            if (currentAmount2 == requiredamount2 || (currentAmount2 >= requiredamount2 && currentAmount2 - e.getItemCosts().get(item) < requiredamount2)) {
                e.getPlayer().sendMessage(" ");
                e.getPlayer().sendMessage(new ComponentBuilder("§a§lQUEST COMPLETE! §aClick here to view progress!").event(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new ComponentBuilder("§3Click to view your progress!").create())).event(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/dq")).create());
                e.getPlayer().sendMessage(" ");
                this.playCompletionSound(e.getPlayer());
            }
            currentlyCollecting2.put(e.getPlayer().getUniqueId(), playerData2);
            e.getPlayer().sendMessage(ChatMessageType.ACTION_BAR, new BaseComponent[] { new TextComponent("§a" + this.econ.format((double)currentAmount2) + " / " + this.econ.format((double)requiredamount2) + " sold!") });
            this.playersSellingMoney.put(item.getType(), currentlyCollecting2);
        }
    }
    
    @EventHandler(ignoreCancelled = true)
    public void onEntityDeath(final EntityDeathEvent e) {
        try {
            final Player player = e.getEntity().getKiller();
            if (player == null) {
                return;
            }
            if (this.playersKilling.containsKey(e.getEntity().getType())) {
                final Map<UUID, Map<Integer, Integer>> currentlyCollecting = this.playersKilling.get(e.getEntity().getType());
                if (currentlyCollecting.containsKey(player.getUniqueId())) {
                    final Map<Integer, Integer> playerData = currentlyCollecting.get(player.getUniqueId());
                    Integer currentAmount = (Integer)playerData.keySet().toArray()[0];
                    final Integer requiredamount = (Integer)playerData.values().toArray()[0];
                    ++currentAmount;
                    playerData.clear();
                    playerData.put(currentAmount, requiredamount);
                    if (currentAmount.equals(requiredamount)) {
                        player.sendMessage(" ");
                        player.spigot().sendMessage(new ComponentBuilder("§a§lQUEST COMPLETE! §aClick here to view progress!").event(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new ComponentBuilder("§3Click to view progress!").create())).event(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/dq")).create());
                        player.sendMessage(" ");
                        this.playCompletionSound(player);
                    }
                    currentlyCollecting.put(player.getUniqueId(), playerData);
                    player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new BaseComponent[] { new TextComponent("§a" + currentAmount + " / " + requiredamount + " killed!") });
                    this.playersKilling.put(e.getEntity().getType(), currentlyCollecting);
                }
            }
        }
        catch (Exception ex) {
            this.getLogger().severe("ERROR! THIS ENTITY CAUSED AN ERROR: " + e.getEntity().getType().name());
            ex.printStackTrace();
        }
    }
    
    @EventHandler(ignoreCancelled = true)
    public void onBlockBreak(final BlockBreakEvent e) {
        if (this.playersCollecting.containsKey(e.getBlock().getType())) {
            final Map<UUID, Map<Integer, Integer>> currentlyCollecting = this.playersCollecting.get(e.getBlock().getType());
            if (currentlyCollecting.containsKey(e.getPlayer().getUniqueId())) {
                final Map<Integer, Integer> playerData = currentlyCollecting.get(e.getPlayer().getUniqueId());
                Integer currentAmount = (Integer)playerData.keySet().toArray()[0];
                final Integer requiredamount = (Integer)playerData.values().toArray()[0];
                ++currentAmount;
                playerData.clear();
                playerData.put(currentAmount, requiredamount);
                if (currentAmount.equals(requiredamount)) {
                    e.getPlayer().sendMessage(" ");
                    e.getPlayer().spigot().sendMessage(new ComponentBuilder("§a§lQUEST COMPLETE! §aClick here to view progress!").event(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new ComponentBuilder("§3Click to view progress!").create())).event(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/dq")).create());
                    e.getPlayer().sendMessage(" ");
                    this.playCompletionSound(e.getPlayer());
                }
                currentlyCollecting.put(e.getPlayer().getUniqueId(), playerData);
                e.getPlayer().spigot().sendMessage(ChatMessageType.ACTION_BAR, new BaseComponent[] { new TextComponent("§a" + currentAmount + " / " + requiredamount + " collected!") });
                this.playersCollecting.put(e.getBlock().getType(), currentlyCollecting);
            }
        }
    }
    
    @EventHandler(ignoreCancelled = true)
    public void onFurnaceExtract(final FurnaceExtractEvent e) {
        if (this.playersSmelting.containsKey(e.getItemType())) {
            final Map<UUID, Map<Integer, Integer>> currentlyCollecting = this.playersSmelting.get(e.getItemType());
            if (currentlyCollecting.containsKey(e.getPlayer().getUniqueId())) {
                final Map<Integer, Integer> playerData = currentlyCollecting.get(e.getPlayer().getUniqueId());
                Integer currentAmount = (Integer)playerData.keySet().toArray()[0];
                final Integer requiredamount = (Integer)playerData.values().toArray()[0];
                currentAmount += e.getItemAmount();
                playerData.clear();
                playerData.put(currentAmount, requiredamount);
                if (currentAmount.equals(requiredamount) || (currentAmount >= requiredamount && currentAmount - e.getItemAmount() < requiredamount)) {
                    e.getPlayer().sendMessage(" ");
                    e.getPlayer().spigot().sendMessage(new ComponentBuilder("§a§lQUEST COMPLETE! §aClick here to view progress!").event(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new ComponentBuilder("§3Click to view progress!").create())).event(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/dq")).create());
                    e.getPlayer().sendMessage(" ");
                    final Player player = e.getPlayer();
                    this.playCompletionSound(player);
                }
                currentlyCollecting.put(e.getPlayer().getUniqueId(), playerData);
                e.getPlayer().spigot().sendMessage(ChatMessageType.ACTION_BAR, new BaseComponent[] { new TextComponent("§a" + currentAmount + " / " + requiredamount + " smelted!") });
                this.playersSmelting.put(e.getItemType(), currentlyCollecting);
            }
        }
    }
    
    @EventHandler(ignoreCancelled = true)
    public void onInventoryClick(final InventoryClickEvent e) {
        if (e.getCurrentItem() == null) {
            return;
        }
        if (e.getClickedInventory() == null) {
            return;
        }
        if (e.getView().getTitle().contains("Rolling")) {
            e.setCancelled(true);
            return;
        }
        if (e.getView().getTitle().contains("Daily Quests")) {
            e.setCancelled(true);
            if (e.getCurrentItem().hasItemMeta() && e.getCurrentItem().getItemMeta().hasDisplayName()) {
                if (e.getCurrentItem().getItemMeta().getDisplayName().contains("Close")) {
                    e.getWhoClicked().closeInventory();
                }
                else if (e.getCurrentItem().getItemMeta().getDisplayName().contains("Reroll")) {
                    if (!e.getWhoClicked().hasPermission("rerolls.unlimited")) {
                        if (this.getConfig().getInt("rerolls." + e.getWhoClicked().getUniqueId(), 1) < 1) {
                            ((Player)e.getWhoClicked()).playSound(e.getWhoClicked().getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 10.0f, 0.0f);
                            e.getWhoClicked().sendMessage("§cYou are out of rerolls.");
                            return;
                        }
                        this.getConfig().set("rerolls." + e.getWhoClicked().getUniqueId(), (Object)(this.getConfig().getInt("rerolls." + e.getWhoClicked().getUniqueId(), 1) - 1));
                        this.saveConfig();
                    }
                    final Inventory inventory = Bukkit.createInventory((InventoryHolder)null, 54, "Rolling Daily Quests...");
                    this.reloadConfig();
                    this.getConfig().set("playerData." + e.getWhoClicked().getUniqueId(), (Object)null);
                    this.saveConfig();
                    this.activeQuests.remove(e.getWhoClicked().getUniqueId());
                    for (final Material key : this.playersSellingMoney.keySet()) {
                        if (this.playersSellingMoney.get(key).containsKey(e.getWhoClicked().getUniqueId())) {
                            final Map<UUID, Map<Double, Double>> playerData = this.playersSellingMoney.get(key);
                            playerData.remove(e.getWhoClicked().getUniqueId());
                            this.playersSellingMoney.put(key, playerData);
                        }
                    }
                    for (final Material key : this.playersSelling.keySet()) {
                        if (this.playersSelling.get(key).containsKey(e.getWhoClicked().getUniqueId())) {
                            final Map<UUID, Map<Integer, Integer>> playerData2 = this.playersSelling.get(key);
                            playerData2.remove(e.getWhoClicked().getUniqueId());
                            this.playersSelling.put(key, playerData2);
                        }
                    }
                    for (final Material key : this.playersSmelting.keySet()) {
                        if (this.playersSmelting.get(key).containsKey(e.getWhoClicked().getUniqueId())) {
                            final Map<UUID, Map<Integer, Integer>> playerData2 = this.playersSmelting.get(key);
                            playerData2.remove(e.getWhoClicked().getUniqueId());
                            this.playersSmelting.put(key, playerData2);
                        }
                    }
                    for (final EntityType key2 : this.playersKilling.keySet()) {
                        if (this.playersKilling.get(key2).containsKey(e.getWhoClicked().getUniqueId())) {
                            final Map<UUID, Map<Integer, Integer>> playerData2 = this.playersKilling.get(key2);
                            playerData2.remove(e.getWhoClicked().getUniqueId());
                            this.playersKilling.put(key2, playerData2);
                        }
                    }
                    for (final Material key : this.playersCollecting.keySet()) {
                        if (this.playersCollecting.get(key).containsKey(e.getWhoClicked().getUniqueId())) {
                            final Map<UUID, Map<Integer, Integer>> playerData2 = this.playersCollecting.get(key);
                            playerData2.remove(e.getWhoClicked().getUniqueId());
                            this.playersCollecting.put(key, playerData2);
                        }
                    }
                    ((Player)e.getWhoClicked()).playSound(((Player)e.getWhoClicked()).getLocation(), Sound.BLOCK_NOTE_BLOCK_BELL, 10.0f, 0.1f);
                    for (int i = 0; i < 4; ++i) {
                        final int finalI = i;
                        inventory.setItem(19 + finalI * 2, e.getClickedInventory().getItem(19 + finalI * 2));
                        inventory.setItem(19 + finalI * 2 + 9, e.getClickedInventory().getItem(19 + finalI * 2 + 9));
                        final Inventory inventory2;
                        final int n;
                        Bukkit.getScheduler().scheduleSyncDelayedTask((Plugin)this, () -> {
                            this.animateItem3((Player)e.getWhoClicked(), inventory2, e.getClickedInventory().getItem(19 + n * 2), 19 + n * 2, 12, 4);
                            this.animateItem3((Player)e.getWhoClicked(), inventory2, e.getClickedInventory().getItem(19 + n * 2 + 9), 19 + n * 2 + 9, 12, 4);
                            return;
                        }, (long)(8 * finalI));
                    }
                    final Integer ID = Bukkit.getScheduler().scheduleSyncRepeatingTask((Plugin)this, () -> ((Player)e.getWhoClicked()).playSound(e.getWhoClicked().getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 10.0f, 0.5f), 0L, 4L);
                    Bukkit.getScheduler().scheduleSyncDelayedTask((Plugin)this, () -> {
                        Bukkit.getScheduler().cancelTask((int)ID);
                        ((Player)e.getWhoClicked()).performCommand("quests SKIP_INTRO");
                        return;
                    }, 76L);
                    e.getWhoClicked().openInventory(inventory);
                }
                else {
                    if (!e.getCurrentItem().getItemMeta().hasLore()) {
                        return;
                    }
                    for (final String str : e.getCurrentItem().getLore()) {
                        if (str.contains("Click to complete")) {
                            Integer questID = this.activeQuests.get(e.getWhoClicked().getUniqueId());
                            this.activeQuests.remove(e.getWhoClicked().getUniqueId());
                            for (final Material key3 : this.playersSellingMoney.keySet()) {
                                if (this.playersSellingMoney.get(key3).containsKey(e.getWhoClicked().getUniqueId())) {
                                    final Map<UUID, Map<Double, Double>> playerData3 = this.playersSellingMoney.get(key3);
                                    playerData3.remove(e.getWhoClicked().getUniqueId());
                                    this.playersSellingMoney.put(key3, playerData3);
                                }
                            }
                            for (final Material key3 : this.playersSelling.keySet()) {
                                if (this.playersSelling.get(key3).containsKey(e.getWhoClicked().getUniqueId())) {
                                    final Map<UUID, Map<Integer, Integer>> playerData4 = this.playersSelling.get(key3);
                                    playerData4.remove(e.getWhoClicked().getUniqueId());
                                    this.playersSelling.put(key3, playerData4);
                                }
                            }
                            for (final Material key3 : this.playersSmelting.keySet()) {
                                if (this.playersSmelting.get(key3).containsKey(e.getWhoClicked().getUniqueId())) {
                                    final Map<UUID, Map<Integer, Integer>> playerData4 = this.playersSmelting.get(key3);
                                    playerData4.remove(e.getWhoClicked().getUniqueId());
                                    this.playersSmelting.put(key3, playerData4);
                                }
                            }
                            for (final EntityType key4 : this.playersKilling.keySet()) {
                                if (this.playersKilling.get(key4).containsKey(e.getWhoClicked().getUniqueId())) {
                                    final Map<UUID, Map<Integer, Integer>> playerData4 = this.playersKilling.get(key4);
                                    playerData4.remove(e.getWhoClicked().getUniqueId());
                                    this.playersKilling.put(key4, playerData4);
                                }
                            }
                            for (final Material key3 : this.playersCollecting.keySet()) {
                                if (this.playersCollecting.get(key3).containsKey(e.getWhoClicked().getUniqueId())) {
                                    final Map<UUID, Map<Integer, Integer>> playerData4 = this.playersCollecting.get(key3);
                                    playerData4.remove(e.getWhoClicked().getUniqueId());
                                    this.playersCollecting.put(key3, playerData4);
                                }
                            }
                            questID = ((questID == 1) ? 4 : ((questID == 2) ? 3 : ((questID == 3) ? 2 : 1)));
                            final String questKey = this.getConfig().getString("playerData." + e.getWhoClicked().getUniqueId() + ".quest" + questID + ".questKey");
                            this.getConfig().set("playerData." + e.getWhoClicked().getUniqueId() + ".quest" + questID + ".questKey", (Object)"COMPLETED");
                            this.saveConfig();
                            final ConfigurationSection rewardData = this.getConfig().getConfigurationSection("quests." + questKey + ".rewardPool." + this.getConfig().get("playerData." + e.getWhoClicked().getUniqueId() + ".quest" + questID + ".rewardChoice"));
                            final String string = rewardData.getString("rewardType");
                            switch (string) {
                                case "CASH": {
                                    this.econ.depositPlayer((OfflinePlayer)e.getWhoClicked(), rewardData.getDouble("rewardData"));
                                    e.getWhoClicked().sendMessage("§a§lQUEST REWARD! §f+" + this.econ.format(rewardData.getDouble("rewardData")));
                                    break;
                                }
                                case "ITEM": {
                                    final String data = rewardData.getString("rewardData");
                                    String type = data.split("x")[1];
                                    final Integer amount = Integer.parseInt(data.split("x")[0]);
                                    e.getWhoClicked().getInventory().addItem(new ItemStack[] { new ItemStack(Material.getMaterial(type.toUpperCase()), (int)amount) });
                                    type = type.replaceAll("_", " ");
                                    type = type.toLowerCase();
                                    type = type.substring(0, 1).toUpperCase() + type.substring(1);
                                    e.getWhoClicked().sendMessage("§a§lQUEST REWARD! §f+" + amount + "x " + type);
                                    break;
                                }
                                case "POINTS": {
                                    Points.pm.addPlayerPoints((Player)e.getWhoClicked(), rewardData.getDouble("rewardData"));
                                    e.getWhoClicked().sendMessage("§a§lQUEST REWARD! §f+" + this.econ.format(rewardData.getDouble("rewardData")).split("\\$")[1] + " Points");
                                    break;
                                }
                                case "COMMAND": {
                                    final ConfigurationSection commandData = rewardData.getConfigurationSection("rewardData");
                                    e.getWhoClicked().sendMessage("§a§lQUEST REWARD! §f" + ChatColor.stripColor(ChatColor.translateAlternateColorCodes('&', commandData.getString("text"))));
                                    this.getServer().dispatchCommand((CommandSender)this.getServer().getConsoleSender(), commandData.getString("command").replace("%player%", e.getWhoClicked().getName()));
                                    break;
                                }
                            }
                            final Player player = (Player)e.getWhoClicked();
                            this.playCompletionSound(player);
                            ((Player)e.getWhoClicked()).performCommand("daily");
                        }
                        else {
                            if (!str.contains("Click to activate")) {
                                continue;
                            }
                            if (!this.activeQuests.containsKey(e.getWhoClicked().getUniqueId())) {
                                this.activeQuests.put(e.getWhoClicked().getUniqueId(), (e.getSlot() - 17) / 2);
                                ((Player)e.getWhoClicked()).playSound(e.getWhoClicked().getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1.0f, 2.0f);
                                ((Player)e.getWhoClicked()).performCommand("daily");
                            }
                            else {
                                ((Player)e.getWhoClicked()).playSound(e.getWhoClicked().getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 0.0f);
                            }
                        }
                    }
                }
            }
        }
    }
    
    private void playCompletionSound(final Player player) {
        if (this.getServer().getTPS()[0] < 16.0) {
            return;
        }
        player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_GUITAR, 10.0f, 1.0f);
        player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BELL, 10.0f, 0.0f);
        player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 10.0f, 0.0f);
        Bukkit.getScheduler().scheduleSyncDelayedTask((Plugin)this, () -> {
            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BELL, 10.0f, 1.0f);
            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 10.0f, 1.0f);
            Bukkit.getScheduler().scheduleSyncDelayedTask((Plugin)this, () -> {
                player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_GUITAR, 10.0f, 1.0f);
                player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BELL, 10.0f, 1.5f);
                player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 10.0f, 1.5f);
                Bukkit.getScheduler().scheduleSyncDelayedTask((Plugin)this, () -> {
                    player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_GUITAR, 10.0f, 1.5f);
                    player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BELL, 10.0f, 1.0f);
                    player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 10.0f, 1.0f);
                    Bukkit.getScheduler().scheduleSyncDelayedTask((Plugin)this, () -> {
                        player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BELL, 10.0f, 1.5f);
                        player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 10.0f, 1.5f);
                        Bukkit.getScheduler().scheduleSyncDelayedTask((Plugin)this, () -> {
                            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_GUITAR, 10.0f, 2.0f);
                            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BELL, 10.0f, 2.0f);
                            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 10.0f, 2.0f);
                            player.playSound(player.getLocation(), Sound.ENTITY_FIREWORK_ROCKET_TWINKLE_FAR, 10.0f, 0.0f);
                        }, 4L);
                    }, 4L);
                }, 8L);
            }, 4L);
        }, 4L);
    }
    
    public boolean onCommand(final CommandSender sender, final Command command, final String label, final String[] args) {
        if (command.getLabel().equalsIgnoreCase("rerolls") && sender.hasPermission("rerolls.give")) {
            if (args.length >= 2) {
                final Player player = Bukkit.getPlayer(args[0]);
                if (player != null) {
                    try {
                        final Integer amount = Integer.valueOf(args[1]);
                        this.reloadConfig();
                        this.getConfig().set("rerolls." + player.getUniqueId(), (Object)(this.getConfig().getInt("rerolls." + player.getUniqueId(), 1) + amount));
                        this.saveConfig();
                        if (args.length >= 3 && args[3].equalsIgnoreCase("-s")) {
                            return true;
                        }
                        sender.sendMessage("§aGave " + player.getName() + " " + amount + " rerolls.");
                    }
                    catch (NumberFormatException e) {
                        sender.sendMessage("§cPlease provide a rounded number for the amount.");
                    }
                }
                else {
                    sender.sendMessage("§cThat player could not be found.");
                }
            }
            return true;
        }
        final Player player = (Player)sender;
        final Integer delay1 = 4;
        final BukkitScheduler sc = Bukkit.getScheduler();
        if (this.getConfig().isSet("playerData." + player.getUniqueId())) {
            final Date now = new Date();
            final Long dateExpire = this.getConfig().getLong("playerData." + player.getUniqueId().toString() + ".dateExpire", now.getTime() - 1L);
            final Long timeDiff = dateExpire - now.getTime();
            Long seconds = timeDiff / 1000L % 60L;
            Long minutes = timeDiff / 60000L % 60L;
            Long hours = timeDiff / 3600000L % 24L;
            if (seconds <= 0L) {
                seconds = 0L;
            }
            if (minutes <= 0L) {
                minutes = 0L;
            }
            if (hours <= 0L) {
                hours = 0L;
            }
            if (timeDiff >= 0L) {
                String minutesString = minutes + "";
                if (minutesString.length() == 1) {
                    minutesString = 0 + minutesString;
                }
                String secondsString = seconds + "";
                if (secondsString.length() == 1) {
                    secondsString = 0 + secondsString;
                }
                final Inventory inventory = Bukkit.createInventory((InventoryHolder)null, 54, "Daily Quests               " + hours + ":" + minutesString + ":" + secondsString + "");
                for (int i = 0; i < 9; ++i) {
                    inventory.setItem(i, new ItemStackBuilder(Material.BLACK_STAINED_GLASS_PANE).displayName("§awww.tribewars.net").build());
                }
                inventory.setItem(4, new ItemStackBuilder(Material.END_CRYSTAL).displayName("§eReroll Quests").lore(new String[] { "§7Click to reroll your quests.", "§7This will give you a new set", "§7of quests to complete!", " ", player.hasPermission("rerolls.unlimited") ? "§e§lUnlimited rerolls!" : ("§bRerolls left: " + this.getConfig().getInt("rerolls." + player.getUniqueId(), 1)) }).build());
                for (int i = 45; i < 54; ++i) {
                    inventory.setItem(i, new ItemStackBuilder(Material.BLACK_STAINED_GLASS_PANE).displayName("§awww.tribewars.net").build());
                }
                inventory.setItem(49, new ItemStackBuilder(Material.RED_STAINED_GLASS_PANE).displayName("§cClose").build());
                final String playerPath = "playerData." + player.getUniqueId().toString();
                for (int j = 0; j < 4; ++j) {
                    final String questName = "quest" + (j + 1);
                    final String questPlayerPath = playerPath + "." + questName;
                    final String questKey = this.getConfig().getString(questPlayerPath + ".questKey");
                    if (questKey.equalsIgnoreCase("COMPLETED")) {
                        final Integer upperSlot = 25 - j * 2;
                        inventory.setItem((int)upperSlot, new ItemStackBuilder(Material.OBSIDIAN).displayName("§a§lCompleted Quest!").lore(new String[] { "§eThis quest has been completed." }).build());
                        inventory.setItem(upperSlot + 9, new ItemStackBuilder(Material.GRAY_STAINED_GLASS_PANE).displayName("§a§lCompleted Quest!").lore(new String[] { "§eThis quest has been completed." }).build());
                    }
                    else {
                        Integer completed = 0;
                        List<Integer> questPoolChoices = new ArrayList<Integer>();
                        questPoolChoices = (List<Integer>)this.getConfig().getIntegerList(questPlayerPath + ".questPoolChoices");
                        final String itemName = ChatColor.translateAlternateColorCodes('&', this.getConfig().getString("quests." + questKey + ".name"));
                        final String itemColor = this.getConfig().getString("quests." + questKey + ".color");
                        final List<String> itemLore = new ArrayList<String>();
                        final Integer upperSlot2 = 25 - j * 2;
                        final Boolean isActivated = this.activeQuests.getOrDefault(player.getUniqueId(), 0) * 2 + 17 == upperSlot2;
                        int index = 1;
                        for (final Integer choice : questPoolChoices) {
                            final ConfigurationSection questInfo = this.getConfig().getConfigurationSection("quests." + questKey + ".questPool." + choice);
                            final String questType = questInfo.getString("questType");
                            final String questData = questInfo.getString("questData");
                            final String description = questInfo.getString("description");
                            String complete = "§c\u2718";
                            String progress;
                            if (questType.equalsIgnoreCase("SELL_ITEMS_MONEY")) {
                                progress = this.econ.format(0.0) + "/" + this.econ.format(Double.parseDouble(questData.split("x")[0]));
                            }
                            else {
                                progress = "0/" + questData.split("x")[0];
                            }
                            if (isActivated) {
                                if (questType.equalsIgnoreCase("BREAK_BLOCKS")) {
                                    final Integer amount2 = Integer.valueOf(questData.split("x")[0]);
                                    final Material mat = Material.getMaterial(questData.split("x")[1]);
                                    final Map<UUID, Map<Integer, Integer>> collectingList = this.playersCollecting.getOrDefault(mat, new HashMap<UUID, Map<Integer, Integer>>());
                                    if (collectingList.containsKey(player.getUniqueId())) {
                                        final Map<Integer, Integer> playerProgress = collectingList.get(player.getUniqueId());
                                        progress = playerProgress.keySet().toArray()[0] + "/" + playerProgress.values().toArray()[0];
                                        if (playerProgress.keySet().toArray(new Integer[0])[0] >= amount2) {
                                            complete = "§a§l\u2713";
                                            ++completed;
                                        }
                                    }
                                    else {
                                        final Map<Integer, Integer> playerProgress = new HashMap<Integer, Integer>();
                                        playerProgress.put(0, amount2);
                                        progress = playerProgress.keySet().toArray()[0] + "/" + playerProgress.values().toArray()[0];
                                        collectingList.put(player.getUniqueId(), playerProgress);
                                        this.playersCollecting.put(mat, collectingList);
                                    }
                                }
                                else if (questType.equalsIgnoreCase("SMELT_ITEMS")) {
                                    final Integer amount2 = Integer.valueOf(questData.split("x")[0]);
                                    final Material mat = Material.getMaterial(questData.split("x")[1]);
                                    final Map<UUID, Map<Integer, Integer>> collectingList = this.playersSmelting.getOrDefault(mat, new HashMap<UUID, Map<Integer, Integer>>());
                                    if (collectingList.containsKey(player.getUniqueId())) {
                                        final Map<Integer, Integer> playerProgress = collectingList.get(player.getUniqueId());
                                        progress = playerProgress.keySet().toArray()[0] + "/" + playerProgress.values().toArray()[0];
                                        if (playerProgress.keySet().toArray(new Integer[0])[0] >= amount2) {
                                            complete = "§a§l\u2713";
                                            ++completed;
                                        }
                                    }
                                    else {
                                        final Map<Integer, Integer> playerProgress = new HashMap<Integer, Integer>();
                                        playerProgress.put(0, amount2);
                                        progress = playerProgress.keySet().toArray()[0] + "/" + playerProgress.values().toArray()[0];
                                        collectingList.put(player.getUniqueId(), playerProgress);
                                        this.playersSmelting.put(mat, collectingList);
                                    }
                                }
                                else if (questType.equalsIgnoreCase("SELL_ITEMS")) {
                                    final Integer amount2 = Integer.valueOf(questData.split("x")[0]);
                                    final Material mat = Material.getMaterial(questData.split("x")[1]);
                                    final Map<UUID, Map<Integer, Integer>> collectingList = this.playersSelling.getOrDefault(mat, new HashMap<UUID, Map<Integer, Integer>>());
                                    if (collectingList.containsKey(player.getUniqueId())) {
                                        final Map<Integer, Integer> playerProgress = collectingList.get(player.getUniqueId());
                                        progress = playerProgress.keySet().toArray()[0] + "/" + playerProgress.values().toArray()[0];
                                        if (playerProgress.keySet().toArray(new Integer[0])[0] >= amount2) {
                                            complete = "§a§l\u2713";
                                            ++completed;
                                        }
                                    }
                                    else {
                                        final Map<Integer, Integer> playerProgress = new HashMap<Integer, Integer>();
                                        playerProgress.put(0, amount2);
                                        progress = playerProgress.keySet().toArray()[0] + "/" + playerProgress.values().toArray()[0];
                                        collectingList.put(player.getUniqueId(), playerProgress);
                                        this.playersSelling.put(mat, collectingList);
                                    }
                                }
                                else if (questType.equalsIgnoreCase("KILL_ENTITY")) {
                                    final Integer amount2 = Integer.valueOf(questData.split("x")[0]);
                                    final EntityType mat2 = EntityType.valueOf(questData.split("x")[1]);
                                    final Map<UUID, Map<Integer, Integer>> collectingList = this.playersKilling.getOrDefault(mat2, new HashMap<UUID, Map<Integer, Integer>>());
                                    if (collectingList.containsKey(player.getUniqueId())) {
                                        final Map<Integer, Integer> playerProgress = collectingList.get(player.getUniqueId());
                                        progress = playerProgress.keySet().toArray()[0] + "/" + playerProgress.values().toArray()[0];
                                        if (playerProgress.keySet().toArray(new Integer[0])[0] >= amount2) {
                                            complete = "§a§l\u2713";
                                            ++completed;
                                        }
                                    }
                                    else {
                                        final Map<Integer, Integer> playerProgress = new HashMap<Integer, Integer>();
                                        playerProgress.put(0, amount2);
                                        progress = playerProgress.keySet().toArray()[0] + "/" + playerProgress.values().toArray()[0];
                                        collectingList.put(player.getUniqueId(), playerProgress);
                                        this.playersKilling.put(mat2, collectingList);
                                    }
                                }
                                else if (questType.equalsIgnoreCase("SELL_ITEMS_MONEY")) {
                                    final Double amount3 = Double.valueOf(questData.split("x")[0]);
                                    final Material mat = Material.valueOf(questData.split("x")[1]);
                                    final Map<UUID, Map<Double, Double>> collectingList2 = this.playersSellingMoney.getOrDefault(mat, new HashMap<UUID, Map<Double, Double>>());
                                    if (collectingList2.containsKey(player.getUniqueId())) {
                                        final Map<Double, Double> playerProgress2 = collectingList2.get(player.getUniqueId());
                                        progress = this.econ.format((double)playerProgress2.keySet().toArray()[0]) + "/" + this.econ.format((double)playerProgress2.values().toArray()[0]) + "";
                                        if (playerProgress2.keySet().toArray(new Double[0])[0] >= amount3) {
                                            complete = "§a§l\u2713";
                                            ++completed;
                                        }
                                    }
                                    else {
                                        final Map<Double, Double> playerProgress2 = new HashMap<Double, Double>();
                                        playerProgress2.put(0.0, amount3);
                                        progress = this.econ.format((double)playerProgress2.keySet().toArray()[0]) + "/" + this.econ.format((double)playerProgress2.values().toArray()[0]) + "";
                                        collectingList2.put(player.getUniqueId(), playerProgress2);
                                        this.playersSellingMoney.put(mat, collectingList2);
                                    }
                                }
                            }
                            itemLore.add(complete + " §7" + description + " §9" + progress);
                            ++index;
                        }
                        itemLore.add(" ");
                        itemLore.add("§fReward:");
                        final Integer rewardPoolChoice = this.getConfig().getInt("playerData." + player.getUniqueId() + ".quest" + (j + 1) + ".rewardChoice");
                        final ConfigurationSection reward = this.getConfig().getConfigurationSection("quests." + questKey + ".rewardPool." + rewardPoolChoice);
                        final String data = reward.getString("rewardData");
                        final String string = reward.getString("rewardType");
                        switch (string) {
                            case "CASH": {
                                final Double amountDouble = reward.getDouble("rewardData");
                                itemLore.add("§a+" + this.econ.format((double)amountDouble).split("\\$")[1] + "$");
                                break;
                            }
                            case "ITEM": {
                                final String type = data.split("x")[1];
                                final Integer amount4 = Integer.parseInt(data.split("x")[0]);
                                itemLore.add("§e+" + amount4 + "x " + type);
                                break;
                            }
                            case "POINTS": {
                                final Double amountDouble = reward.getDouble("rewardData");
                                itemLore.add("§b+" + this.econ.format((double)amountDouble).split("\\$")[1] + " Points");
                                break;
                            }
                            case "COMMAND": {
                                itemLore.add(ChatColor.translateAlternateColorCodes('&', reward.getString("rewardData.text")));
                                break;
                            }
                        }
                        final ItemStack item = new ItemStackBuilder(Material.valueOf(itemColor + "_WOOL")).lore(new String[] { "§eClick to activate!" }).displayName(itemName).build();
                        final ItemStack itemDesc = new ItemStackBuilder(Material.valueOf(itemColor + "_STAINED_GLASS_PANE")).displayName(itemName).lore(itemLore.toArray(new String[0])).build();
                        if (isActivated) {
                            item.addUnsafeEnchantment(Enchantment.ARROW_INFINITE, 1);
                            item.addItemFlags(new ItemFlag[] { ItemFlag.HIDE_ENCHANTS });
                            item.setLore((List)Arrays.asList("§e§lActivated!"));
                            itemDesc.addUnsafeEnchantment(Enchantment.ARROW_INFINITE, 1);
                            itemDesc.addItemFlags(new ItemFlag[] { ItemFlag.HIDE_ENCHANTS });
                        }
                        if (completed >= questPoolChoices.size()) {
                            final List<String> lore = (List<String>)itemDesc.getLore();
                            lore.add(" ");
                            lore.add("§aClick to complete!");
                            itemDesc.setLore((List)lore);
                        }
                        inventory.setItem((int)upperSlot2, item);
                        inventory.setItem(upperSlot2 + 9, itemDesc);
                    }
                }
                player.openInventory(inventory);
                return true;
            }
        }
        this.getConfig().set("playerData." + player.getUniqueId(), (Object)null);
        this.saveConfig();
        final Random rnd = new Random();
        final ConfigurationSection quests = this.getConfig().getConfigurationSection("quests");
        final List<ItemStack> questList = new ArrayList<ItemStack>();
        final List<ItemStack> questDescList = new ArrayList<ItemStack>();
        Double maxChance = 0.0;
        final Map<Double, String> chances = new HashMap<Double, String>();
        for (final String key : quests.getKeys(false)) {
            chances.put(quests.getDouble(key + ".chance"), key);
            maxChance += quests.getDouble(key + ".chance");
        }
        for (int k = 0; k < 4; ++k) {
            final Double currentChance = rnd.nextDouble() * maxChance;
            String keyResult = "noob";
            Double prev = 0.0;
            for (final Double key2 : chances.keySet()) {
                final Double next = prev + key2;
                if (currentChance > prev && currentChance <= next) {
                    keyResult = chances.get(key2);
                    break;
                }
                prev = next;
            }
            this.getConfig().set("playerData." + player.getUniqueId() + ".dateExpire", (Object)(new Date().getTime() + 86400000L));
            this.getConfig().set("playerData." + player.getUniqueId() + ".quest" + (k + 1) + ".questKey", (Object)keyResult);
            final String color = quests.getString(keyResult + ".color");
            final String name = quests.getString(keyResult + ".name");
            final Integer questsAmount = quests.getInt(keyResult + ".quests");
            final Integer rewardPoolChoice2 = rnd.nextInt(quests.getConfigurationSection(keyResult + ".rewardPool").getKeys(false).size()) + 1;
            final List<Integer> receivedQuests = new ArrayList<Integer>();
            final List<String> lore2 = new ArrayList<String>();
            for (int l = 0; l < questsAmount; ++l) {
                Integer randomIndexQuest;
                do {
                    randomIndexQuest = rnd.nextInt(quests.getConfigurationSection(keyResult + ".questPool").getKeys(false).size()) + 1;
                } while (receivedQuests.contains(randomIndexQuest));
                receivedQuests.add(randomIndexQuest);
                lore2.add("§c\u2718 §7" + quests.getString(keyResult + ".questPool." + randomIndexQuest + ".description"));
            }
            lore2.add(" ");
            lore2.add("§fReward:");
            this.getConfig().set("playerData." + player.getUniqueId() + ".quest" + (k + 1) + ".rewardChoice", (Object)rewardPoolChoice2);
            final ConfigurationSection reward2 = this.getConfig().getConfigurationSection("quests." + keyResult + ".rewardPool." + rewardPoolChoice2);
            final String data2 = reward2.getString("rewardData");
            final String string2 = reward2.getString("rewardType");
            switch (string2) {
                case "CASH": {
                    final Double amountDouble2 = reward2.getDouble("rewardData");
                    lore2.add("§a+" + this.econ.format((double)amountDouble2).split("\\$")[1] + "$");
                    break;
                }
                case "ITEM": {
                    final String type2 = data2.split("x")[1];
                    final Integer amount5 = Integer.parseInt(data2.split("x")[0]);
                    lore2.add("§e+" + amount5 + "x " + type2);
                    break;
                }
                case "POINTS": {
                    final Double amountDouble2 = reward2.getDouble("rewardData");
                    lore2.add("§b+" + this.econ.format((double)amountDouble2).split("\\$")[1] + " Points");
                    break;
                }
                case "COMMAND": {
                    final ConfigurationSection commandData = reward2.getConfigurationSection("rewardData");
                    lore2.add(ChatColor.translateAlternateColorCodes('&', commandData.getString("text")));
                    break;
                }
            }
            final ItemStack questStack = new ItemStackBuilder(Material.valueOf(color + "_WOOL")).displayName(ChatColor.translateAlternateColorCodes('&', name)).lore(new String[] { "§eClick to activate!" }).build();
            final ItemStack questDesc = new ItemStackBuilder(Material.valueOf(color + "_STAINED_GLASS_PANE")).displayName(ChatColor.translateAlternateColorCodes('&', name)).lore(lore2.toArray(new String[0])).build();
            questList.add(questStack);
            questDescList.add(questDesc);
            this.getConfig().set("playerData." + player.getUniqueId() + ".quest" + (k + 1) + ".questPoolChoices", (Object)receivedQuests);
            this.saveConfig();
        }
        final Inventory inventory2 = Bukkit.createInventory((InventoryHolder)null, 54, "Rolling Daily Quests...");
        boolean skip = false;
        if (args.length >= 1 && args[0].equals("SKIP_INTRO")) {
            skip = true;
        }
        if (this.getServer().getTPS()[0] < 16.0) {
            player.sendMessage("§9§lINFORMATION §fAnimation skipped because of low tps.");
            player.performCommand("dq");
            return true;
        }
        if (!skip) {
            inventory2.setItem(19, new ItemStackBuilder(Material.WHITE_WOOL).displayName("§7???").build());
            inventory2.setItem(20, new ItemStackBuilder(Material.YELLOW_WOOL).displayName("§7???").build());
            inventory2.setItem(21, new ItemStackBuilder(Material.LIME_WOOL).displayName("§7???").build());
            inventory2.setItem(22, new ItemStackBuilder(Material.CYAN_WOOL).displayName("§7???").build());
            inventory2.setItem(23, new ItemStackBuilder(Material.PINK_WOOL).displayName("§7???").build());
            inventory2.setItem(24, new ItemStackBuilder(Material.RED_WOOL).displayName("§7???").build());
            inventory2.setItem(25, new ItemStackBuilder(Material.ORANGE_WOOL).displayName("§7???").build());
            inventory2.setItem(28, new ItemStackBuilder(Material.WHITE_STAINED_GLASS_PANE).displayName("§7???").build());
            inventory2.setItem(29, new ItemStackBuilder(Material.YELLOW_STAINED_GLASS_PANE).displayName("§7???").build());
            inventory2.setItem(30, new ItemStackBuilder(Material.LIME_STAINED_GLASS_PANE).displayName("§7???").build());
            inventory2.setItem(31, new ItemStackBuilder(Material.CYAN_STAINED_GLASS_PANE).displayName("§7???").build());
            inventory2.setItem(32, new ItemStackBuilder(Material.PINK_STAINED_GLASS_PANE).displayName("§7???").build());
            inventory2.setItem(33, new ItemStackBuilder(Material.RED_STAINED_GLASS_PANE).displayName("§7???").build());
            inventory2.setItem(34, new ItemStackBuilder(Material.ORANGE_STAINED_GLASS_PANE).displayName("§7???").build());
        }
        player.openInventory(inventory2);
        final Integer offset = 9;
        final boolean finalSkip = skip;
        int m;
        final Inventory inventory3;
        final int n3;
        final Integer n4;
        final BukkitScheduler bukkitScheduler;
        int i2;
        final Inventory inventory4;
        final Integer offset2;
        final int n5;
        final BukkitScheduler bukkitScheduler2;
        int i3;
        final Inventory inventory5;
        final BukkitScheduler bukkitScheduler3;
        final Integer animDelay;
        final BukkitScheduler bukkitScheduler4;
        final Player player2;
        final Integer task;
        final BukkitScheduler bukkitScheduler5;
        final Integer n6;
        final Player player3;
        final Integer delay2;
        final Integer task2;
        int i4;
        int finalI;
        final Player player4;
        final Inventory inventory6;
        final List<ItemStack> list;
        final int n7;
        final List<ItemStack> list2;
        final BukkitScheduler bukkitScheduler6;
        final Integer n8;
        final Player player5;
        int i5;
        int finalI2;
        final ItemStackBuilder itemStackBuilder;
        final int n9;
        final Player player6;
        final Inventory inventory7;
        final Integer delay3;
        final ItemStackBuilder itemStackBuilder2;
        final int n10;
        final Player player7;
        final Inventory inventory8;
        final Integer delay4;
        final boolean b;
        final Integer n11;
        final Integer n12;
        final Integer n13;
        sc.scheduleSyncDelayedTask((Plugin)this, () -> {
            for (int m = 0; m < 54; ++m) {
                inventory3.setItem(m, new ItemStack(Material.AIR));
            }
            if (n3 == 0) {
                inventory3.setItem(19 - n4, new ItemStackBuilder(Material.WHITE_WOOL).displayName("§7???").build());
                inventory3.setItem(20 - n4, new ItemStackBuilder(Material.YELLOW_WOOL).displayName("§7???").build());
                inventory3.setItem(21 - n4, new ItemStackBuilder(Material.LIME_WOOL).displayName("§7???").build());
                inventory3.setItem(22 - n4, new ItemStackBuilder(Material.CYAN_WOOL).displayName("§7???").build());
                inventory3.setItem(23 - n4, new ItemStackBuilder(Material.PINK_WOOL).displayName("§7???").build());
                inventory3.setItem(24 - n4, new ItemStackBuilder(Material.RED_WOOL).displayName("§7???").build());
                inventory3.setItem(25 - n4, new ItemStackBuilder(Material.ORANGE_WOOL).displayName("§7???").build());
                inventory3.setItem(28 + n4, new ItemStackBuilder(Material.WHITE_STAINED_GLASS_PANE).displayName("§7???").build());
                inventory3.setItem(29 + n4, new ItemStackBuilder(Material.YELLOW_STAINED_GLASS_PANE).displayName("§7???").build());
                inventory3.setItem(30 + n4, new ItemStackBuilder(Material.LIME_STAINED_GLASS_PANE).displayName("§7???").build());
                inventory3.setItem(31 + n4, new ItemStackBuilder(Material.CYAN_STAINED_GLASS_PANE).displayName("§7???").build());
                inventory3.setItem(32 + n4, new ItemStackBuilder(Material.PINK_STAINED_GLASS_PANE).displayName("§7???").build());
                inventory3.setItem(33 + n4, new ItemStackBuilder(Material.RED_STAINED_GLASS_PANE).displayName("§7???").build());
                inventory3.setItem(34 + n4, new ItemStackBuilder(Material.ORANGE_STAINED_GLASS_PANE).displayName("§7???").build());
            }
            bukkitScheduler.scheduleSyncDelayedTask((Plugin)this, () -> {
                for (int i2 = 0; i2 < 54; ++i2) {
                    inventory4.setItem(i2, new ItemStack(Material.AIR));
                }
                offset2 = 18;
                if (n5 == 0) {
                    inventory4.setItem(19 - offset2, new ItemStackBuilder(Material.WHITE_WOOL).displayName("§7???").build());
                    inventory4.setItem(20 - offset2, new ItemStackBuilder(Material.YELLOW_WOOL).displayName("§7???").build());
                    inventory4.setItem(21 - offset2, new ItemStackBuilder(Material.LIME_WOOL).displayName("§7???").build());
                    inventory4.setItem(22 - offset2, new ItemStackBuilder(Material.CYAN_WOOL).displayName("§7???").build());
                    inventory4.setItem(23 - offset2, new ItemStackBuilder(Material.PINK_WOOL).displayName("§7???").build());
                    inventory4.setItem(24 - offset2, new ItemStackBuilder(Material.RED_WOOL).displayName("§7???").build());
                    inventory4.setItem(25 - offset2, new ItemStackBuilder(Material.ORANGE_WOOL).displayName("§7???").build());
                    inventory4.setItem(28 + offset2, new ItemStackBuilder(Material.WHITE_STAINED_GLASS_PANE).displayName("§7???").build());
                    inventory4.setItem(29 + offset2, new ItemStackBuilder(Material.YELLOW_STAINED_GLASS_PANE).displayName("§7???").build());
                    inventory4.setItem(30 + offset2, new ItemStackBuilder(Material.LIME_STAINED_GLASS_PANE).displayName("§7???").build());
                    inventory4.setItem(31 + offset2, new ItemStackBuilder(Material.CYAN_STAINED_GLASS_PANE).displayName("§7???").build());
                    inventory4.setItem(32 + offset2, new ItemStackBuilder(Material.PINK_STAINED_GLASS_PANE).displayName("§7???").build());
                    inventory4.setItem(33 + offset2, new ItemStackBuilder(Material.RED_STAINED_GLASS_PANE).displayName("§7???").build());
                    inventory4.setItem(34 + offset2, new ItemStackBuilder(Material.ORANGE_STAINED_GLASS_PANE).displayName("§7???").build());
                }
                bukkitScheduler2.scheduleSyncDelayedTask((Plugin)this, () -> {
                    for (i3 = 0; i3 < 54; ++i3) {
                        inventory5.setItem(i3, new ItemStack(Material.AIR));
                    }
                    bukkitScheduler3.scheduleSyncDelayedTask((Plugin)this, () -> {
                        animDelay = 4;
                        task = bukkitScheduler4.scheduleSyncRepeatingTask((Plugin)this, () -> player2.playSound(player2.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 1.0f, 0.0f), 0L, (long)animDelay);
                        bukkitScheduler4.scheduleSyncDelayedTask((Plugin)this, () -> {
                            bukkitScheduler5.cancelTask((int)n6);
                            task2 = bukkitScheduler5.scheduleSyncRepeatingTask((Plugin)this, () -> player3.playSound(player3.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 1.0f, 1.0f), 0L, (long)delay2);
                            for (i4 = 0; i4 < 4; ++i4) {
                                finalI = i4;
                                bukkitScheduler5.scheduleSyncDelayedTask((Plugin)this, () -> this.animateItem2(player4, inventory6, list.get(n7), list2.get(n7), 12 + n7 * 2, delay2), (long)(delay2 + i4 * 8));
                            }
                            bukkitScheduler5.scheduleSyncDelayedTask((Plugin)this, () -> {
                                bukkitScheduler6.cancelTask((int)n8);
                                player5.playSound(player5.getLocation(), Sound.ENTITY_FIREWORK_ROCKET_LARGE_BLAST, 10.0f, 1.0f);
                                player5.performCommand("dq");
                            }, (long)(14 * (delay2 * 2) + 2));
                            return;
                        }, (long)(28 * animDelay));
                        for (i5 = 0; i5 < 4; ++i5) {
                            finalI2 = i5;
                            bukkitScheduler4.scheduleSyncDelayedTask((Plugin)this, () -> {
                                new ItemStackBuilder((n9 % 2 == 0) ? Material.GRAY_WOOL : Material.LIGHT_GRAY_WOOL);
                                this.animateItem(player6, inventory7, itemStackBuilder.displayName("§7???").build(), 27 - n9 * 8, delay3);
                                Bukkit.getScheduler().scheduleSyncDelayedTask((Plugin)this, () -> {
                                    new ItemStackBuilder((n10 % 2 == 0) ? Material.BLACK_STAINED_GLASS_PANE : Material.GRAY_STAINED_GLASS_PANE);
                                    this.animateItem(player7, inventory8, itemStackBuilder2.displayName("§7???").build(), 26 - n10 * 8, delay4);
                                }, (long)delay3);
                                return;
                            }, (long)(animDelay + i5 * 8));
                        }
                    }, b ? 0L : ((long)(n11 * 2)));
                }, (n5 != 0) ? 0L : ((long)n12));
            }, (n3 != 0) ? 0L : ((long)n13));
            return;
        }, skip ? 0L : ((long)delay1));
        return super.onCommand(sender, command, label, args);
    }
    
    void animateItem(final Player player, final Inventory inventory, final ItemStack itemStack, final Integer end, final Integer delay) {
        final Integer ID = Bukkit.getScheduler().scheduleSyncRepeatingTask((Plugin)this, (Runnable)new Runnable() {
            Integer slot = 36;
            Integer slotRem = 0;
            
            @Override
            public void run() {
                inventory.setItem((int)this.slot, itemStack);
                inventory.setItem((int)this.slotRem, new ItemStack(Material.AIR));
                this.slotRem = this.slot;
                switch (this.slot) {
                    case 19:
                    case 21:
                    case 23:
                    case 28:
                    case 30:
                    case 32:
                    case 34:
                    case 37:
                    case 39:
                    case 41:
                    case 43: {
                        this.slot -= 9;
                        break;
                    }
                    case 11:
                    case 13:
                    case 15:
                    case 20:
                    case 22:
                    case 24:
                    case 29:
                    case 31:
                    case 33: {
                        this.slot += 9;
                        break;
                    }
                    default: {
                        final Integer slot = this.slot;
                        ++this.slot;
                        break;
                    }
                }
            }
        }, 0L, (long)delay);
        Bukkit.getScheduler().scheduleSyncDelayedTask((Plugin)this, () -> {
            Bukkit.getScheduler().cancelTask((int)ID);
            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 1.0f, 2.0f);
        }, (long)(end * delay));
    }
    
    void animateItem2(final Player player, final Inventory inventory, final ItemStack itemStack, final ItemStack descriptionItem, final Integer end, final Integer delay) {
        final Integer ID = Bukkit.getScheduler().scheduleSyncRepeatingTask((Plugin)this, (Runnable)new Runnable() {
            Integer slot = 36;
            Integer slotRem = 0;
            
            @Override
            public void run() {
                inventory.setItem((int)this.slot, itemStack);
                inventory.setItem((int)this.slotRem, new ItemStack(Material.AIR));
                this.slotRem = this.slot;
                switch (this.slot) {
                    case 26:
                    case 35:
                    case 44: {
                        this.slot -= 9;
                        break;
                    }
                    case 10:
                    case 11:
                    case 12:
                    case 13:
                    case 14:
                    case 15:
                    case 16:
                    case 17: {
                        final Integer slot = this.slot;
                        --this.slot;
                        break;
                    }
                    default: {
                        final Integer slot2 = this.slot;
                        ++this.slot;
                        break;
                    }
                }
            }
        }, 0L, (long)delay);
        Integer slot;
        final Integer finalSlot;
        final Integer n;
        Bukkit.getScheduler().scheduleSyncDelayedTask((Plugin)this, () -> {
            Bukkit.getScheduler().cancelTask((int)ID);
            Bukkit.getScheduler().scheduleSyncDelayedTask((Plugin)this, () -> {
                slot = 0;
                switch (end) {
                    case 18: {
                        slot = 10;
                        break;
                    }
                    case 16: {
                        slot = 12;
                        break;
                    }
                    case 14: {
                        slot = 14;
                        break;
                    }
                    case 12: {
                        slot = 16;
                        break;
                    }
                }
                inventory.setItem(slot + 9, itemStack);
                inventory.setItem((int)slot, (ItemStack)null);
                finalSlot = slot;
                Bukkit.getScheduler().scheduleSyncDelayedTask((Plugin)this, () -> {
                    player.playSound(player.getLocation(), Sound.ENTITY_FIREWORK_ROCKET_LAUNCH, 1.0f, 1.0f);
                    inventory.setItem(n + 18, descriptionItem);
                }, (long)delay);
            }, (long)delay);
        }, (long)(end * delay));
    }
    
    void animateItem3(final Player player, final Inventory inventory, final ItemStack itemStack, final Integer start, final Integer end, final Integer delay) {
        final Integer ID = Bukkit.getScheduler().scheduleSyncRepeatingTask((Plugin)this, (Runnable)new Runnable() {
            Integer slot = start;
            Integer slotRem = 0;
            
            @Override
            public void run() {
                if (this.slot == 8) {
                    inventory.setItem((int)this.slot, (ItemStack)null);
                    inventory.setItem(9, (ItemStack)null);
                    return;
                }
                inventory.setItem((int)this.slot, itemStack);
                inventory.setItem((int)this.slotRem, new ItemStack(Material.AIR));
                this.slotRem = this.slot;
                switch (this.slot) {
                    case 19:
                    case 21:
                    case 23:
                    case 25:
                    case 28:
                    case 30:
                    case 32:
                    case 34: {
                        this.slot -= 9;
                        break;
                    }
                    default: {
                        final Integer slot = this.slot;
                        --this.slot;
                        break;
                    }
                }
            }
        }, 0L, (long)delay);
        Bukkit.getScheduler().scheduleSyncDelayedTask((Plugin)this, () -> Bukkit.getScheduler().cancelTask((int)ID), (long)(end * delay));
    }
}
