package net.blay09.mods.inventoryessentials.client;

import net.blay09.mods.inventoryessentials.InventoryEssentialsConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.network.protocol.game.ClientboundContainerSetSlotPacket;
import net.minecraft.network.protocol.game.ClientboundTakeItemEntityPacket;
import net.minecraft.core.component.DataComponents;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

final class BundleAutoFillHandler {

    private ItemStack pendingPickedUpStack = ItemStack.EMPTY;

    public void onTakeItemEntityPacket(Minecraft client, ClientboundTakeItemEntityPacket packet) {
        final var player = client.player;
        final var level = client.level;
        if (player == null || level == null || packet.getPlayerId() != player.getId()) {
            return;
        }

        if (level.getEntity(packet.getItemId()) instanceof ItemEntity itemEntity) {
            pendingPickedUpStack = itemEntity.getItem().copyWithCount(packet.getAmount());
        } else {
            pendingPickedUpStack = ItemStack.EMPTY;
        }
    }

    public void onContainerSetSlotPacket(Minecraft client, ClientboundContainerSetSlotPacket packet) {
        if (pendingPickedUpStack.isEmpty()) {
            return;
        }

        final var player = client.player;
        final var gameMode = client.gameMode;
        if (player == null || gameMode == null) {
            pendingPickedUpStack = ItemStack.EMPTY;
            return;
        }

        final var menu = player.containerMenu;
        if (!InventoryEssentialsConfig.getActive().enableBundleAutoFill
                || !menu.getCarried().isEmpty()
                || packet.getContainerId() != menu.containerId) {
            return;
        }

        if (!menu.isValidSlotIndex(packet.getSlot())) {
            return;
        }

        final var sourceSlot = menu.getSlot(packet.getSlot());
        if (!isValidSlot(sourceSlot)) {
            return;
        }

        final var currentStack = sourceSlot.getItem();
        if (currentStack.isEmpty()
                || currentStack.is(ItemTags.BUNDLES)
                || !ItemStack.isSameItemSameComponents(currentStack, pendingPickedUpStack)) {
            return;
        }

        if (currentStack.getCount() == pendingPickedUpStack.getCount()) {
            tryAutoFillBundle(menu, sourceSlot);
        }
        pendingPickedUpStack = ItemStack.EMPTY;
    }

    public void reset() {
        pendingPickedUpStack = ItemStack.EMPTY;
    }

    private boolean tryAutoFillBundle(AbstractContainerMenu menu, Slot sourceSlot) {
        final var sourceStack = sourceSlot.getItem().copy();
        final var bundleSlot = findMatchingBundleSlot(menu, sourceSlot, sourceStack);
        if (bundleSlot == null) {
            return false;
        }

        clickSlot(menu, sourceSlot, 0);
        if (!ItemStack.isSameItemSameComponents(menu.getCarried(), sourceStack)) {
            if (!menu.getCarried().isEmpty()) {
                clickSlot(menu, sourceSlot, 0);
            }
            return false;
        }

        clickSlot(menu, bundleSlot, 0);
        if (!menu.getCarried().isEmpty()) {
            clickSlot(menu, sourceSlot, 0);
        }

        return !ItemStack.matches(sourceSlot.getItem(), sourceStack) || !menu.getCarried().isEmpty();
    }

    private @Nullable Slot findMatchingBundleSlot(AbstractContainerMenu menu, Slot sourceSlot, ItemStack sourceStack) {
        for (final var slot : menu.slots) {
            if (slot == sourceSlot || !isValidSlot(slot) || !slot.hasItem()) {
                continue;
            }

            final var bundleStack = slot.getItem();
            if (!bundleStack.is(ItemTags.BUNDLES)) {
                continue;
            }

            final var bundleContents = bundleStack.get(DataComponents.BUNDLE_CONTENTS);
            if (bundleContents == null || bundleContents.isEmpty()) {
                continue;
            }

            final boolean containsMatchingItem = bundleContents.itemCopies().anyMatch(bundleItem -> ItemStack.isSameItemSameComponents(bundleItem, sourceStack));
            if (containsMatchingItem) {
                return slot;
            }
        }

        return null;
    }

    private boolean isValidSlot(Slot slot) {
        final var player = Minecraft.getInstance().player;
        return slot.container instanceof Inventory
                && slot.isActive()
                && !slot.isFake()
                && player != null
                && slot.mayPickup(player);
    }

    private void clickSlot(AbstractContainerMenu menu, Slot slot, int mouseButton) {
        final var player = Minecraft.getInstance().player;
        final var gameMode = Minecraft.getInstance().gameMode;
        if (player != null && gameMode != null) {
            gameMode.handleContainerInput(menu.containerId, slot.index, mouseButton, ContainerInput.PICKUP, player);
        }
    }
}
