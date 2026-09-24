package net.blay09.mods.inventoryessentials.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.blay09.mods.balm.api.Balm;
import net.blay09.mods.balm.api.event.client.DisconnectedFromServerEvent;
import net.blay09.mods.balm.api.event.client.screen.ScreenMouseEvent;
import net.blay09.mods.inventoryessentials.InventoryEssentials;
import net.blay09.mods.inventoryessentials.InventoryEssentialsIgnores;
import net.blay09.mods.inventoryessentials.InventoryEssentialsConfig;
import net.blay09.mods.inventoryessentials.mixin.AbstractContainerScreenAccessor;
import net.blay09.mods.inventoryessentials.mixin.CreativeModeInventoryScreenAccessor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.protocol.game.ClientboundContainerSetSlotPacket;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.inventory.Slot;

public class InventoryEssentialsClient {

    private static final InventoryControls clientOnlyControls = new ClientOnlyInventoryControls();
    private static final InventoryControls creativeControls = new CreativeInventoryControls();
    private static final InventoryControls serverSupportedControls = new ServerSupportedInventoryControls();
    private static final ToolRefillHandler toolRefillHandler = new ToolRefillHandler();
    private static final StackRefillHandler stackRefillHandler = new StackRefillHandler();

    private static Slot lastDragHoverSlot;

    public static void initialize() {
        Balm.getEvents().onEvent(DisconnectedFromServerEvent.class, event -> {
            InventoryEssentials.isServerSideInstalled = false;
            toolRefillHandler.reset();
            stackRefillHandler.reset();
        });

        ModKeyMappings.initialize();

        Balm.getEvents().onEvent(ScreenMouseEvent.Click.Pre.class, InventoryEssentialsClient::onMouseClick);
        Balm.getEvents().onEvent(ScreenMouseEvent.Drag.Pre.class, InventoryEssentialsClient::onMouseDrag);
        Balm.getEvents().onEvent(ScreenMouseEvent.Release.Pre.class, InventoryEssentialsClient::onMouseRelease);
    }

    public static void beforeContainerSetSlotPacket(ClientboundContainerSetSlotPacket packet) {
        final var client = Minecraft.getInstance();
        toolRefillHandler.beforeContainerSetSlot(client, packet);
        stackRefillHandler.beforeContainerSetSlot(client, packet);
    }

    public static void beforeUseItemOn(LocalPlayer player, InteractionHand hand) {
        stackRefillHandler.beforeUseItemOn(Minecraft.getInstance(), player, hand);
    }

    public static void afterContainerSetSlotPacket(ClientboundContainerSetSlotPacket packet) {
        final var client = Minecraft.getInstance();
        toolRefillHandler.afterContainerSetSlot(client, packet);
        stackRefillHandler.afterContainerSetSlot(client, packet);
    }

    public static InventoryControls getInventoryControls(Screen screen) {
        if (screen instanceof CreativeModeInventoryScreenAccessor) {
            return creativeControls;
        }

        return InventoryEssentials.isServerSideInstalled && !InventoryEssentialsConfig.getActive().forceClientImplementation ? serverSupportedControls : clientOnlyControls;
    }

    public static void onMouseClick(ScreenMouseEvent.Click.Pre event) {
        if (ModKeyMappings.keyDragTransfer.isActiveAndDown() && event.getButton() == InputConstants.MOUSE_BUTTON_LEFT || event.getButton() == InputConstants.MOUSE_BUTTON_RIGHT) {
            if (event.getScreen() instanceof AbstractContainerScreen<?> screen) {
                Slot hoverSlot = ((AbstractContainerScreenAccessor) screen).getHoveredSlot();
                if (hoverSlot != null && !InventoryEssentialsIgnores.shouldIgnoreScreen(screen) && !InventoryEssentialsIgnores.shouldIgnoreSlot(screen, hoverSlot)) {
                    // Consider the clicked slot as lastDragHoverSlot to avoid doing double shift-clicks on the first slot
                    // Double shift-clicks are a problem with modded slots that may have more than 64 items in them (e.g. upgraded Sophisticated Backpacks)
                    lastDragHoverSlot = hoverSlot;
                }
            }
        }
    }

    public static void onMouseDrag(ScreenMouseEvent.Drag.Pre event) {
        if (ModKeyMappings.keyDragTransfer.isActiveAndDown() && (event.getButton() == 0 || event.getButton() == 1)) {
            if (event.getScreen() instanceof AbstractContainerScreen<?> screen) {
                Slot hoverSlot = ((AbstractContainerScreenAccessor) screen).getHoveredSlot();
                if (hoverSlot == null || InventoryEssentialsIgnores.shouldIgnoreScreen(screen) || InventoryEssentialsIgnores.shouldIgnoreSlot(screen, hoverSlot)) {
                    return;
                }

                if (hoverSlot.hasItem() && hoverSlot != lastDragHoverSlot) {
                    InventoryControls controls = getInventoryControls(screen);
                    if (InventoryEssentialsConfig.getActive().enableShiftDrag) {
                        controls.dragTransfer(screen, hoverSlot);
                    }
                    lastDragHoverSlot = hoverSlot;
                }
            }
        } else {
            lastDragHoverSlot = null;
        }
    }

    public static void onMouseRelease(ScreenMouseEvent.Release.Pre event) {
        if (event.getButton() == InputConstants.MOUSE_BUTTON_LEFT || event.getButton() == InputConstants.MOUSE_BUTTON_RIGHT) {
            lastDragHoverSlot = null;
        }
    }

}
