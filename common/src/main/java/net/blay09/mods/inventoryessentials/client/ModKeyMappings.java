package net.blay09.mods.inventoryessentials.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.blay09.mods.inventoryessentials.InventoryEssentials;
import net.blay09.mods.inventoryessentials.InventoryEssentialsIgnores;
import net.blay09.mods.inventoryessentials.InventoryEssentialsConfig;
import net.blay09.mods.inventoryessentials.mixin.AbstractContainerScreenAccessor;
import net.blay09.mods.kuma.api.*;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.resources.Identifier;
import net.minecraft.world.inventory.Slot;

import java.util.function.BiFunction;
import java.util.function.Supplier;

public class ModKeyMappings {

    public static ManagedKeyMapping keySingleTransfer;
    public static ManagedKeyMapping keyBulkTransfer;
    public static ManagedKeyMapping keyBulkTransferSingle;
    public static ManagedKeyMapping keyBulkTransferAll;
    public static ManagedKeyMapping keyBulkDrop;
    public static ManagedKeyMapping keyScreenBulkDrop;
    public static ManagedKeyMapping keyDragTransfer;
    public static ManagedKeyMapping keySortInventory;
    public static ManagedKeyMapping keyRestockContainer;
    public static ManagedKeyMapping keyRestockInventory;
    public static ManagedKeyMapping keyDumpToContainer;

    public static void initialize() {
        keySingleTransfer = Kuma.createKeyMapping(Identifier.fromNamespaceAndPath(InventoryEssentials.MOD_ID, "single_transfer"))
                .withDefault(InputBinding.mouse(InputConstants.MOUSE_BUTTON_LEFT, KeyModifiers.of(KeyModifier.CONTROL)))
                .handleScreenInput(event -> handleSlotInput(event, () -> InventoryEssentialsConfig.getActive().enableSingleTransfer,
                        (screen, slot) -> InventoryEssentialsClient.getInventoryControls(screen).singleTransfer(screen, slot)))
                .build();

        keyBulkTransfer = Kuma.createKeyMapping(Identifier.fromNamespaceAndPath(InventoryEssentials.MOD_ID, "bulk_transfer"))
                .withDefault(InputBinding.mouse(InputConstants.MOUSE_BUTTON_LEFT, KeyModifiers.of(KeyModifier.SHIFT, KeyModifier.CONTROL)))
                .handleScreenInput(event -> handleSlotInput(event, () -> InventoryEssentialsConfig.getActive().enableBulkTransfer,
                        (screen, slot) -> InventoryEssentialsClient.getInventoryControls(screen).bulkTransferByType(screen, slot)))
                .build();

        keyBulkTransferSingle = Kuma.createKeyMapping(Identifier.fromNamespaceAndPath(InventoryEssentials.MOD_ID, "bulk_transfer_single"))
                .withDefault(InputBinding.mouse(InputConstants.MOUSE_BUTTON_RIGHT, KeyModifiers.ofCustom(InputConstants.Type.KEYBOARD.getOrCreate(InputConstants.KEY_SPACE))))
                .handleScreenInput(event -> handleSlotInput(event, () -> InventoryEssentialsConfig.getActive().enableBulkTransferSingle,
                        (screen, slot) -> InventoryEssentialsClient.getInventoryControls(screen).bulkTransferSingle(screen, slot)))
                .build();

        keyBulkTransferAll = Kuma.createKeyMapping(Identifier.fromNamespaceAndPath(InventoryEssentials.MOD_ID, "bulk_transfer_all"))
                .withDefault(InputBinding.mouse(InputConstants.MOUSE_BUTTON_LEFT, KeyModifiers.ofCustom(InputConstants.Type.KEYBOARD.getOrCreate(InputConstants.KEY_SPACE))))
                .handleScreenInput(event -> handleSlotInput(event, () -> InventoryEssentialsConfig.getActive().enableBulkTransferAll,
                        (screen, slot) -> InventoryEssentialsClient.getInventoryControls(screen).bulkTransferAll(screen, slot)))
                .build();

        keyBulkDrop = Kuma.createKeyMapping(Identifier.fromNamespaceAndPath(InventoryEssentials.MOD_ID, "bulk_drop"))
                .withDefault(InputBinding.key(InputConstants.KEY_Q, KeyModifiers.of(KeyModifier.SHIFT, KeyModifier.CONTROL)))
                .handleScreenInput(event -> handleSlotInput(event, () -> InventoryEssentialsConfig.getActive().enableBulkDrop,
                        (screen, slot) -> InventoryEssentialsClient.getInventoryControls(screen).dropByType(screen, slot)))
                .build();

        keyScreenBulkDrop = Kuma.createKeyMapping(Identifier.fromNamespaceAndPath(InventoryEssentials.MOD_ID, "screen_bulk_drop"))
                .withDefault(InputBinding.mouse(InputConstants.MOUSE_BUTTON_LEFT, KeyModifiers.of(KeyModifier.SHIFT)))
                .handleScreenInput(event -> {
                    if (!InventoryEssentialsConfig.getActive().enableBulkDrop) {
                        return false;
                    }

                    if (InventoryEssentialsIgnores.shouldIgnoreScreen(event.screen())) {
                        return false;
                    }

                    if (!(event.screen() instanceof AbstractContainerScreen<?> containerScreen)) {
                        return false;
                    }

                    final var accessor = (AbstractContainerScreenAccessor) containerScreen;
                    final var clickedOutside = accessor.callHasClickedOutside(event.mouseX(),
                            event.mouseY(),
                            accessor.getLeftPos(),
                            accessor.getTopPos());
                    return clickedOutside && InventoryEssentialsClient.getInventoryControls(containerScreen)
                            .dropByType(containerScreen, containerScreen.getMenu().getCarried());
                })
                .build();

        keyDragTransfer = Kuma.createKeyMapping(Identifier.fromNamespaceAndPath(InventoryEssentials.MOD_ID, "drag_transfer"))
                .withDefault(InputBinding.key(InputConstants.KEY_LSHIFT))
                .withContext(KeyConflictContext.SCREEN)
                .build();

        keySortInventory = Kuma.createKeyMapping(Identifier.fromNamespaceAndPath(InventoryEssentials.MOD_ID, "sort_inventory"))
                .withDefault(InputBinding.mouse(InputConstants.MOUSE_BUTTON_MIDDLE))
                .handleScreenInput(event -> handleSlotInput(event, () -> true,
                        (screen, slot) -> InventoryEssentialsClient.getInventoryControls(screen).sort(screen, slot)))
                .build();

        keyRestockContainer = Kuma.createKeyMapping(Identifier.fromNamespaceAndPath(InventoryEssentials.MOD_ID, "restock_container"))
                .withContext(KeyConflictContext.SCREEN)
                .handleScreenInput(event -> {
                    if (InventoryEssentialsIgnores.shouldIgnoreScreen(event.screen())) {
                        return false;
                    }

                    if (!(event.screen() instanceof AbstractContainerScreen<?> containerScreen)) {
                        return false;
                    }

                    return InventoryEssentialsClient.getInventoryControls(containerScreen).restockContainer(containerScreen);
                })
                .build();

        keyRestockInventory = Kuma.createKeyMapping(Identifier.fromNamespaceAndPath(InventoryEssentials.MOD_ID, "restock_inventory"))
                .withContext(KeyConflictContext.SCREEN)
                .handleScreenInput(event -> {
                    if (InventoryEssentialsIgnores.shouldIgnoreScreen(event.screen())) {
                        return false;
                    }

                    if (!(event.screen() instanceof AbstractContainerScreen<?> containerScreen)) {
                        return false;
                    }

                    return InventoryEssentialsClient.getInventoryControls(containerScreen).restockInventory(containerScreen);
                })
                .build();

        keyDumpToContainer = Kuma.createKeyMapping(Identifier.fromNamespaceAndPath(InventoryEssentials.MOD_ID, "dump_to_container"))
                .withContext(KeyConflictContext.SCREEN)
                .handleScreenInput(event -> {
                    if (InventoryEssentialsIgnores.shouldIgnoreScreen(event.screen())) {
                        return false;
                    }

                    if (!(event.screen() instanceof AbstractContainerScreen<?> containerScreen)) {
                        return false;
                    }

                    return InventoryEssentialsClient.getInventoryControls(containerScreen).dumpToContainer(containerScreen);
                })
                .build();
    }

    private static boolean handleSlotInput(ScreenInputEvent event, Supplier<Boolean> predicate, BiFunction<AbstractContainerScreen<?>, Slot, Boolean> handler) {
        if (!predicate.get()) {
            return false;
        }

        if (InventoryEssentialsIgnores.shouldIgnoreScreen(event.screen())) {
            return false;
        }

        if (!(event.screen() instanceof AbstractContainerScreen<?> containerScreen)) {
            return false;
        }

        final var hoverSlot = ((AbstractContainerScreenAccessor) containerScreen).getHoveredSlot();
        if (InventoryEssentialsIgnores.shouldIgnoreSlot(containerScreen, hoverSlot)) {
            return false;
        }

        return handler.apply(containerScreen, hoverSlot);
    }
}
