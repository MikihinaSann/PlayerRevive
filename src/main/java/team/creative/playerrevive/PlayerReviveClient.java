package team.creative.playerrevive;

import net.fabricmc.api.ClientModInitializer;
import team.creative.creativecore.client.CreativeCoreClient;
import team.creative.playerrevive.client.ReviveEventClient;

public class PlayerReviveClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        CreativeCoreClient.registerClientConfig(PlayerReviveFabric.MODID);
        ReviveEventClient.register();
    }
}
