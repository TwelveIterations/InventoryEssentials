package net.blay09.mods.inventoryessentials;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.Iterator;

public class InventoryOperations {

    @FunctionalInterface
    public interface SlotPolicy {
        boolean isValidSlot(Slot slot);

        static SlotPolicy always() {
            return slot -> true;
        }
    }

    @FunctionalInterface
    public interface SlotClickHandler {
        void click(AbstractContainerMenu menu, Slot slot, int containerButton, ContainerInput containerInput);
    }

    private final SlotClickHandler slotClickHandler;
    private final SlotPolicy slotPolicy;

    public InventoryOperations(SlotClickHandler slotClickHandler, SlotPolicy slotPolicy) {
        this.slotClickHandler = slotClickHandler;
        this.slotPolicy = slotPolicy;
    }

    public boolean transferToContainer(AbstractContainerMenu menu, Player player, boolean fillEmptySlots) {
        if (!menu.getCarried().isEmpty() || menu instanceof InventoryMenu) {
            return false;
        }

        final var sourceSlots = new ArrayList<Slot>();
        final var nonEmptyTargetSlots = new ArrayList<Slot>();
        final var emptyTargetSlots = new ArrayList<Slot>();
        for (final var slot : menu.slots) {
            if (!slotPolicy.isValidSlot(slot)) {
                continue;
            }

            if (slot.container instanceof Inventory) {
                final var containerSlot = slot.getContainerSlot();
                if (containerSlot >= Inventory.SELECTION_SIZE && containerSlot < Inventory.INVENTORY_SIZE && slot.mayPickup(player) && slot.hasItem()) {
                    sourceSlots.add(slot);
                }
            } else if (slot.hasItem()) {
                nonEmptyTargetSlots.add(slot);
            } else if (fillEmptySlots) {
                emptyTargetSlots.add(slot);
            }
        }

        if (sourceSlots.isEmpty() || nonEmptyTargetSlots.isEmpty()) {
            return false;
        }

        return transferToSlots(menu, sourceSlots, nonEmptyTargetSlots, emptyTargetSlots);
    }

    public boolean transferToInventory(AbstractContainerMenu menu, Player player, boolean includeHotbar, boolean fillEmptySlots) {
        if (!menu.getCarried().isEmpty() || menu instanceof InventoryMenu) {
            return false;
        }

        final var sourceSlots = new ArrayList<Slot>();
        final var nonEmptyTargetSlots = new ArrayList<Slot>();
        final var emptyTargetSlots = new ArrayList<Slot>();
        for (final var slot : menu.slots) {
            if (!slotPolicy.isValidSlot(slot)) {
                continue;
            }

            if (slot.container instanceof Inventory) {
                final var containerSlot = slot.getContainerSlot();
                if (containerSlot >= 0 &&
                        containerSlot < Inventory.INVENTORY_SIZE &&
                        (includeHotbar || containerSlot >= Inventory.SELECTION_SIZE)) {
                    if (slot.hasItem()) {
                        nonEmptyTargetSlots.add(slot);
                    } else if (fillEmptySlots) {
                        emptyTargetSlots.add(slot);
                    }
                }
            } else if (slot.hasItem() && slot.mayPickup(player)) {
                sourceSlots.add(slot);
            }
        }

        if (sourceSlots.isEmpty() || nonEmptyTargetSlots.isEmpty()) {
            return false;
        }

        return transferToSlots(menu, sourceSlots, nonEmptyTargetSlots, emptyTargetSlots);
    }

    private boolean transferToSlots(AbstractContainerMenu menu, ArrayList<Slot> sourceSlots, ArrayList<Slot> nonEmptyTargetSlots, ArrayList<Slot> emptyTargetSlots) {
        boolean movedAny = false;
        for (final var sourceSlot : sourceSlots) {
            if (!sourceSlot.hasItem()) {
                continue;
            }

            slotClickHandler.click(menu, sourceSlot, 0, ContainerInput.PICKUP);
            var carried = menu.getCarried();
            if (carried.isEmpty()) {
                continue;
            }

            final var sourceStack = carried.copy();
            boolean hasMatchingItemInTargets = false;
            for (final var targetSlot : nonEmptyTargetSlots) {
                final var targetStack = targetSlot.getItem();
                if (targetStack.isEmpty() || !ItemStack.isSameItemSameComponents(sourceStack, targetStack)) {
                    continue;
                } else {
                    hasMatchingItemInTargets = true;
                }

                final int targetLimit = Math.min(targetSlot.getMaxStackSize(), targetSlot.getMaxStackSize(targetStack));
                if (targetStack.getCount() >= targetLimit) {
                    continue;
                }

                final int oldCarriedCount = menu.getCarried().getCount();
                slotClickHandler.click(menu, targetSlot, 0, ContainerInput.PICKUP);
                carried = menu.getCarried();
                if (carried.getCount() < oldCarriedCount) {
                    movedAny = true;
                }
                if (carried.isEmpty()) {
                    break;
                }
            }

            if (!emptyTargetSlots.isEmpty() && !carried.isEmpty() && hasMatchingItemInTargets) {
                for (final Iterator<Slot> iterator = emptyTargetSlots.iterator(); iterator.hasNext(); ) {
                    final var emptyTargetSlot = iterator.next();
                    if (emptyTargetSlot.hasItem()) {
                        nonEmptyTargetSlots.add(emptyTargetSlot);
                        iterator.remove();
                        continue;
                    }

                    if (!emptyTargetSlot.mayPlace(sourceStack)) {
                        continue;
                    }

                    final int oldCarriedCount = menu.getCarried().getCount();
                    slotClickHandler.click(menu, emptyTargetSlot, 0, ContainerInput.PICKUP);
                    carried = menu.getCarried();
                    if (carried.getCount() < oldCarriedCount) {
                        movedAny = true;
                        if (emptyTargetSlot.hasItem()) {
                            nonEmptyTargetSlots.add(emptyTargetSlot);
                            iterator.remove();
                        }
                    }

                    if (carried.isEmpty()) {
                        break;
                    }
                }
            }

            if (!menu.getCarried().isEmpty()) {
                slotClickHandler.click(menu, sourceSlot, 0, ContainerInput.PICKUP);
            }
        }

        return movedAny;
    }

    public boolean isValidSlot(Slot slot) {
        return slotPolicy.isValidSlot(slot);
    }

    public static InventoryOperations forServerPlayer(ServerPlayer player) {
        return new InventoryOperations((containerMenu, slot, mouseButton, ContainerInput) -> containerMenu.clicked(slot.index, mouseButton, ContainerInput, player), SlotPolicy.always());
    }

}
