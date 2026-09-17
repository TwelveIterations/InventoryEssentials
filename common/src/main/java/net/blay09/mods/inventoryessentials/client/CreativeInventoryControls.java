package net.blay09.mods.inventoryessentials.client;

import net.blay09.mods.inventoryessentials.InventoryOperations;
import net.blay09.mods.inventoryessentials.mixin.SlotWrapperAccessor;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.Slot;

public class CreativeInventoryControls extends ClientOnlyInventoryControls {

    @Override
    protected InventoryOperations createOperations() {
        return new InventoryOperations(this::slotClick, slot -> slot.container instanceof Inventory);
    }

    @Override
    protected void slotClick(AbstractContainerMenu menu, Slot slot, int containerButton, ContainerInput containerInput) {
        if (slot instanceof SlotWrapperAccessor accessor) {
            final var player = Minecraft.getInstance().player;
            if (player != null) {
                slotClick(player.inventoryMenu, accessor.getTarget().index, containerButton, containerInput);
            }
        } else {
            slotClick(menu, slot.index, containerButton, containerInput);
        }
    }

    @Override
    protected void slotClick(AbstractContainerMenu menu, int slotIndex, int containerButton, ContainerInput containerInput) {
        final var player = Minecraft.getInstance().player;
        if (player != null) {
            menu.clicked(slotIndex, containerButton, containerInput, player);
            player.inventoryMenu.broadcastChanges();
        }
    }
}
