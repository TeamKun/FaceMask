package net.kunmc.lab.facemask;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public class Face extends ItemStack {
    public final String facename;

    Face(String facename, Material material, int CustomModelData){
        super(material);
        ItemMeta meta = this.getItemMeta();
        meta.setDisplayName(facename);
        meta.setCustomModelData(CustomModelData);
        this.setItemMeta(meta);
        this.facename = facename;
    }
}
