package net.blay09.mods.inventoryessentials;

import net.minecraft.core.component.DataComponents;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class InventoryUtils {

    public static boolean isBundle(ItemStack stack) {
        return stack.is(ItemTags.BUNDLES) || stack.is(InventoryEssentialsTags.BUNDLES);
    }

    public static boolean isSameInventory(Slot targetSlot, Slot slot) {
        return isSameInventory(targetSlot, slot, false);
    }

    public static boolean isSameInventory(Slot targetSlot, Slot slot, boolean treatHotBarAsSeparate) {
        boolean isTargetPlayerInventory = targetSlot.container instanceof Inventory;
        boolean isTargetHotBar = isTargetPlayerInventory && Inventory.isHotbarSlot(targetSlot.getContainerSlot());
        boolean isPlayerInventory = slot.container instanceof Inventory;
        boolean isHotBar = isPlayerInventory && Inventory.isHotbarSlot(slot.getContainerSlot());

        if (isTargetPlayerInventory && isPlayerInventory && treatHotBarAsSeparate) {
            return isHotBar == isTargetHotBar;
        }

        return PlatformBindings.INSTANCE.isSameInventory(targetSlot, slot);
    }

    public static boolean containerContainsPlayerInventory(AbstractContainerMenu menu) {
        for (Slot slot : menu.slots) {
            if (slot.container instanceof Inventory && slot.getContainerSlot() >= 9 && slot.getContainerSlot() < 37) {
                return true;
            }
        }

        return false;
    }

    public static Map<EquipmentSlot, Slot> findMatchingArmorSetSlots(AbstractContainerMenu menu, Slot baseSlot) {
        final var result = new HashMap<EquipmentSlot, Slot>();
        final var equipmentSlots = Arrays.stream(EquipmentSlot.values()).filter(EquipmentSlot::isArmor).toList();
        final var baseItem = baseSlot.getItem();
        final var baseEquippable = baseItem.get(DataComponents.EQUIPPABLE);
        if (baseEquippable != null && baseEquippable.slot().isArmor()) {
            result.put(baseEquippable.slot(), baseSlot);
        }

        for (final var slot : menu.slots) {
            if (menu instanceof InventoryMenu && slot.index >= InventoryMenu.ARMOR_SLOT_START && slot.index < InventoryMenu.ARMOR_SLOT_END) {
                continue;
            }

            final var slotStack = slot.getItem();
            final var slotEquippable = slotStack.get(DataComponents.EQUIPPABLE);
            if (slotEquippable != null && slotEquippable.slot().isArmor()) {
                if (isMatchingArmorSet(baseItem, slotStack) && !result.containsKey(slotEquippable.slot())) {
                    result.put(slotEquippable.slot(), slot);
                }
            }
            if (result.size() >= equipmentSlots.size()) {
                break;
            }
        }

        return result;
    }

    private static boolean isMatchingArmorSet(ItemStack baseItem, ItemStack otherItem) {
        final var baseEquippable = baseItem.get(DataComponents.EQUIPPABLE);
        final var otherEquippable = otherItem.get(DataComponents.EQUIPPABLE);
        if (baseEquippable != null && otherEquippable != null) {
            return Objects.equals(baseEquippable.assetId().orElse(null), otherEquippable.assetId().orElse(null));
        }

        return false;
    }
}
