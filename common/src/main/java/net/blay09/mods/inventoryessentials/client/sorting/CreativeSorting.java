package net.blay09.mods.inventoryessentials.client.sorting;

import net.minecraft.client.Minecraft;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.ItemStack;

import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;

public final class CreativeSorting {

    public record ItemStackKey(ItemStack stack) {
        public ItemStackKey(ItemStack stack) {
            this.stack = stack.copyWithCount(1);
            if (this.stack.isDamageableItem()) {
                this.stack.setDamageValue(0);
            }
        }

        @Override
        public boolean equals(Object obj) {
            return obj instanceof ItemStackKey(ItemStack other) && ItemStack.isSameItemSameComponents(stack, other);
        }

        @Override
        public int hashCode() {
            return ItemStack.hashItemAndComponents(stack);
        }
    }

    private static volatile Map<ItemStackKey, Integer> creativeRanks = Map.of();

    private CreativeSorting() {
    }

    public static ItemStackKey keyOf(ItemStack stack) {
        return new ItemStackKey(stack);
    }

    public static Map<ItemStackKey, Integer> getCreativeRanks() {
        final var cachedRanks = creativeRanks;
        if (!cachedRanks.isEmpty()) {
            return cachedRanks;
        }

        synchronized (CreativeSorting.class) {
            if (!creativeRanks.isEmpty()) {
                return creativeRanks;
            }

            final var minecraft = Minecraft.getInstance();
            final var player = minecraft.player;
            final var level = minecraft.level;
            if (player != null && level != null) {
                final var showOperatorItems = player.canUseGameMasterBlocks()
                        && minecraft.options.operatorItemsTab().get();
                CreativeModeTabs.tryRebuildTabContents(level.enabledFeatures(), showOperatorItems, level.registryAccess());
            }

            final var computedRanks = new HashMap<ItemStackKey, Integer>();
            int rank = 0;
            for (final var tab : CreativeModeTabs.tabs()) {
                for (final var stack : tab.getDisplayItems()) {
                    final var key = keyOf(stack);
                    if (!computedRanks.containsKey(key)) {
                        computedRanks.put(key, rank++);
                    }
                }
            }

            creativeRanks = Map.copyOf(computedRanks);
            return creativeRanks;
        }
    }

    public static Comparator<ItemStack> getCreativeComparator() {
        final var creativeRanks = CreativeSorting.getCreativeRanks();
        return Comparator.comparingInt((ItemStack stack) -> creativeRanks.getOrDefault(CreativeSorting.keyOf(stack), Integer.MAX_VALUE));
    }
}
