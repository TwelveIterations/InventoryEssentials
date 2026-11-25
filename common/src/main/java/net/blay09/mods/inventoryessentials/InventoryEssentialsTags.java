package net.blay09.mods.inventoryessentials;

import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;

import static net.blay09.mods.inventoryessentials.InventoryEssentials.id;

public class InventoryEssentialsTags {

    public static final TagKey<Item> BUNDLES = TagKey.create(Registries.ITEM, id("bundles"));

}
