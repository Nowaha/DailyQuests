package me.nowaha.dailyquests;

import org.bukkit.*;
import java.util.*;
import org.bukkit.enchantments.*;
import org.bukkit.inventory.*;
import org.bukkit.entity.*;
import org.bukkit.inventory.meta.*;

public class ItemStackBuilder
{
    private ItemStack itemStack;
    private ItemMeta itemMeta;
    
    public ItemStackBuilder(final Material material) {
        this.itemStack = new ItemStack(material);
        this.itemMeta = this.itemStack.getItemMeta();
    }
    
    public ItemStackBuilder amount(final int amount) {
        this.itemStack.setAmount(amount);
        return this;
    }
    
    public ItemStackBuilder durability(final short durability) {
        this.itemStack.setDurability(durability);
        return this;
    }
    
    public ItemStackBuilder displayName(final String displayName) {
        this.itemMeta.setDisplayName(displayName);
        return this;
    }
    
    public ItemStackBuilder lore(final String[] lore) {
        final List<String> loreArray = new ArrayList<String>();
        for (final String loreBit : lore) {
            loreArray.add(ChatColor.WHITE + loreBit);
        }
        this.itemMeta.setLore((List)loreArray);
        return this;
    }
    
    public ItemStackBuilder enchant(final Enchantment enchanement, final int level, final boolean ignoreLevelRestriction) {
        this.itemMeta.addEnchant(enchanement, level, ignoreLevelRestriction);
        return this;
    }
    
    public ItemStackBuilder itemFlags(final ItemFlag... flags) {
        this.itemMeta.addItemFlags(flags);
        return this;
    }
    
    public ItemStackBuilder unbreakable(final boolean unbreakable) {
        this.itemMeta.spigot().setUnbreakable(unbreakable);
        return this;
    }
    
    public ItemStackBuilder skullOwner(final Player player) {
        final SkullMeta playerheadmeta = (SkullMeta)this.itemMeta;
        playerheadmeta.setOwner(player.getName());
        return this;
    }
    
    public ItemStack build() {
        final ItemStack clonedStack = this.itemStack.clone();
        clonedStack.setItemMeta(this.itemMeta.clone());
        return clonedStack;
    }
}
