package net.blay09.mods.inventoryessentials;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Prediction;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.Slot;

public class ServerInventoryTransfers {
    public static void singleTransfer(ServerPlayer player, AbstractContainerMenu menu, Slot slot) {
        if (!slot.mayPickup(player)) {
            return;
        }

        final var sourceStack = slot.getItem();
        if (sourceStack.getCount() == 1) {
            menu.clicked(slot.index, 0, ContainerInput.QUICK_MOVE, player);
        } else if (!sourceStack.isEmpty()) {
            final var restStack = sourceStack.copy();
            sourceStack.setCount(1);

            // We specifically set the slot stack as some mods return transient copies in getItem that will not be reflected back to the inventory
            slot.set(sourceStack);

            restStack.shrink(1);
            menu.clicked(slot.index, 0, ContainerInput.QUICK_MOVE, player);
            if (!slot.hasItem()) {
                slot.set(restStack);
            } else {
                if (!player.addItem(restStack)) {
                    player.drop(restStack, false, Prediction.SERVER_ONLY);
                }
            }
        }
    }
}
