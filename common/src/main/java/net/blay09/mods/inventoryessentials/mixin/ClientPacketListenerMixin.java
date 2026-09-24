package net.blay09.mods.inventoryessentials.mixin;

import net.blay09.mods.inventoryessentials.client.InventoryEssentialsClient;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.game.ClientboundContainerSetSlotPacket;
import net.minecraft.network.protocol.game.ClientboundTakeItemEntityPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPacketListener.class)
public class ClientPacketListenerMixin {

    @Inject(method = "handleTakeItemEntity", at = @At(value = "INVOKE", target = "Lnet/minecraft/network/protocol/PacketUtils;ensureRunningOnSameThread(Lnet/minecraft/network/protocol/Packet;Lnet/minecraft/network/PacketListener;Lnet/minecraft/network/PacketProcessor;)V", shift = At.Shift.AFTER))
    private void handleTakeItemEntity(ClientboundTakeItemEntityPacket packet, CallbackInfo callbackInfo) {
        InventoryEssentialsClient.onTakeItemEntityPacket(packet);
    }

    @Inject(method = "handleContainerSetSlot", at = @At(value = "INVOKE", target = "Lnet/minecraft/network/protocol/PacketUtils;ensureRunningOnSameThread(Lnet/minecraft/network/protocol/Packet;Lnet/minecraft/network/PacketListener;Lnet/minecraft/network/PacketProcessor;)V", shift = At.Shift.AFTER))
    private void beforeContainerSetSlot(ClientboundContainerSetSlotPacket packet, CallbackInfo callbackInfo) {
        InventoryEssentialsClient.beforeContainerSetSlotPacket(packet);
    }

    @Inject(method = "handleContainerSetSlot", at = @At("TAIL"))
    private void handleContainerSetSlot(ClientboundContainerSetSlotPacket packet, CallbackInfo callbackInfo) {
        InventoryEssentialsClient.afterContainerSetSlotPacket(packet);
    }
}
