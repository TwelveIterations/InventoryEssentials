package net.blay09.mods.inventoryessentials;

import net.blay09.mods.balm.Balm;
import net.blay09.mods.balm.core.BalmRegistrars;
import net.blay09.mods.balm.platform.event.callback.ServerPlayerCallback;
import net.blay09.mods.inventoryessentials.data.ConfigJsonCompatLoader;
import net.blay09.mods.inventoryessentials.data.ConfigJsonExtensionLoader;
import net.blay09.mods.inventoryessentials.data.ModFileJsonCompatLoader;
import net.blay09.mods.inventoryessentials.data.ModFileJsonExtensionLoader;
import net.blay09.mods.inventoryessentials.network.HelloMessage;
import net.blay09.mods.inventoryessentials.network.ModNetworking;
import net.minecraft.resources.Identifier;

public class InventoryEssentials {

    public static final String MOD_ID = "inventoryessentials";
    public static boolean isServerSideInstalled;

    public static void initialize(BalmRegistrars registrars) {
        InventoryEssentialsConfig.initialize();
        ModNetworking.initialize(Balm.networking());

        ServerPlayerCallback.Join.EVENT.register(player -> Balm.networking().sendTo(player, HelloMessage.INSTANCE));

        Balm.config().onConfigAvailable(InventoryEssentialsConfig.class, config -> {
            ModFileJsonCompatLoader.load();
            ConfigJsonCompatLoader.load();
            ModFileJsonExtensionLoader.load();
            ConfigJsonExtensionLoader.load();
        });
    }

    public static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(MOD_ID, path);
    }
}
