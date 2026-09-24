package net.blay09.mods.inventoryessentials.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.blay09.mods.balm.client.BalmClientRegistrars;
import net.blay09.mods.balm.client.platform.event.callback.ClientLifecycleCallback;
import net.blay09.mods.balm.client.platform.event.callback.ScreenCallback;
import net.blay09.mods.inventoryessentials.InventoryEssentials;
import net.blay09.mods.inventoryessentials.InventoryEssentialsConfig;
import net.blay09.mods.inventoryessentials.InventoryEssentialsIgnores;
import net.blay09.mods.inventoryessentials.mixin.AbstractContainerScreenAccessor;
import net.blay09.mods.inventoryessentials.mixin.CreativeModeInventoryScreenAccessor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.protocol.game.ClientboundContainerSetSlotPacket;
import net.minecraft.network.protocol.game.ClientboundTakeItemEntityPacket;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.inventory.Slot;
import org.jspecify.annotations.Nullable;

public class InventoryEssentialsClient {

    private static final InventoryControls clientOnlyControls = new ClientOnlyInventoryControls();
    private static final InventoryControls creativeControls = new CreativeInventoryControls();
    private static final InventoryControls serverSupportedControls = new ServerSupportedInventoryControls();
    private static final BundleAutoFillHandler bundleAutoFillHandler = new BundleAutoFillHandler();
    private static final ToolRefillHandler toolRefillHandler = new ToolRefillHandler();
    private static final StackRefillHandler stackRefillHandler = new StackRefillHandler();

    private static @Nullable Slot lastDragHoverSlot;
    private static boolean hasDragClicked;

    public static void initialize(BalmClientRegistrars registrars) {
        ClientLifecycleCallback.DisconnectedFromServer.EVENT.register(client -> {
            InventoryEssentials.isServerSideInstalled = false;
            bundleAutoFillHandler.reset();
            toolRefillHandler.reset();
            stackRefillHandler.reset();
        });

        ModKeyMappings.initialize();

        ScreenCallback.MousePress.Before.EVENT.register(InventoryEssentialsClient::onMouseClick);
        ScreenCallback.MouseDrag.Before.EVENT.register(InventoryEssentialsClient::onMouseDrag);
        ScreenCallback.MouseRelease.Before.EVENT.register(InventoryEssentialsClient::onMouseRelease);
    }

    public static void onTakeItemEntityPacket(ClientboundTakeItemEntityPacket packet) {
        bundleAutoFillHandler.onTakeItemEntityPacket(Minecraft.getInstance(), packet);
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
        bundleAutoFillHandler.onContainerSetSlotPacket(client, packet);
    }

    public static InventoryControls getInventoryControls(Screen screen) {
        if (screen instanceof CreativeModeInventoryScreenAccessor) {
            return creativeControls;
        }

        return InventoryEssentials.isServerSideInstalled && !InventoryEssentialsConfig.getActive().forceClientImplementation ? serverSupportedControls : clientOnlyControls;
    }

    public static boolean onMouseRelease(Screen screen, double mouseX, double mouseY, int button) {
        if (button == InputConstants.MOUSE_BUTTON_LEFT || button == InputConstants.MOUSE_BUTTON_RIGHT) {
            lastDragHoverSlot = null;
        }

        if (screen instanceof AbstractContainerScreen<?> containerScreen) {
            Slot hoverSlot = ((AbstractContainerScreenAccessor) containerScreen).getHoveredSlot();
            if (hoverSlot == null || InventoryEssentialsIgnores.shouldIgnoreScreen(containerScreen) || InventoryEssentialsIgnores.shouldIgnoreSlot(containerScreen, hoverSlot)) {
                return false;
            }

            if (hasDragClicked) {
                hasDragClicked = false;
                return true;
            }
        }
        return false;
    }

    private static boolean onMouseClick(Screen screen, MouseButtonEvent event) {
        if (ModKeyMappings.keyDragTransfer.isActiveAndDown() && event.button() == InputConstants.MOUSE_BUTTON_LEFT || event.button() == InputConstants.MOUSE_BUTTON_RIGHT) {
            if (screen instanceof AbstractContainerScreen<?> containerScreen) {
                Slot hoverSlot = ((AbstractContainerScreenAccessor) containerScreen).getHoveredSlot();
                if (hoverSlot != null && !InventoryEssentialsIgnores.shouldIgnoreScreen(containerScreen) && !InventoryEssentialsIgnores.shouldIgnoreSlot(containerScreen, hoverSlot)) {
                    // Consider the clicked slot as lastDragHoverSlot to avoid doing double shift-clicks on the first slot
                    // Double shift-clicks are a problem with modded slots that may have more than 64 items in them (e.g. upgraded Sophisticated Backpacks)
                    lastDragHoverSlot = hoverSlot;
                }
            }
        }
        return false;
    }

    public static boolean onMouseDrag(Screen screen, double mouseX, double mouseY, int button, double horizontalAmount, double verticalAmount) {
        if (screen instanceof AbstractContainerScreen<?> containerScreen) {
            Slot hoverSlot = ((AbstractContainerScreenAccessor) containerScreen).getHoveredSlot();
            if (hoverSlot == null || InventoryEssentialsIgnores.shouldIgnoreScreen(containerScreen) || InventoryEssentialsIgnores.shouldIgnoreSlot(containerScreen, hoverSlot)) {
                return false;
            }

            // If shift is held, perform drag transfer
            if (ModKeyMappings.keyDragTransfer.isActiveAndDown() && (button == InputConstants.MOUSE_BUTTON_LEFT || button == InputConstants.MOUSE_BUTTON_RIGHT)) {
                if (hoverSlot.hasItem() && hoverSlot != lastDragHoverSlot) {
                    InventoryControls controls = getInventoryControls(containerScreen);
                    if (InventoryEssentialsConfig.getActive().enableShiftDrag) {
                        controls.dragTransfer(containerScreen, hoverSlot);
                    }
                    lastDragHoverSlot = hoverSlot;
                }
                return false;
            }

            // If dragging mouse button while holding a bundle, perform drag clicks
            if (InventoryEssentialsConfig.getActive().enableBundleDrag) {
                final var carriedStack = containerScreen.getMenu().getCarried();
                if (carriedStack.is(ItemTags.BUNDLES)) {
                    if (hoverSlot != lastDragHoverSlot) {
                        if ((button == InputConstants.MOUSE_BUTTON_LEFT && hoverSlot.hasItem()) || (button == InputConstants.MOUSE_BUTTON_RIGHT && !hoverSlot.hasItem())) {
                            final var controls = getInventoryControls(containerScreen);
                            controls.dragClick(containerScreen, hoverSlot, button);
                            hasDragClicked = true;
                            // Quick-craft causes subsequent clicks to not work right because it never gets reset due to our cancels
                            ((AbstractContainerScreenAccessor) containerScreen).setIsQuickCrafting(false);
                        }
                        lastDragHoverSlot = hoverSlot;
                    }
                    return true;
                }
            }

            lastDragHoverSlot = null;
        } else {
            lastDragHoverSlot = null;
        }

        return false;
    }

}
