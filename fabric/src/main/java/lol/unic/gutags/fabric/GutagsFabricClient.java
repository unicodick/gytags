package lol.unic.gutags.fabric;

import lol.unic.gutags.client.GutagsClientRuntime;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.loader.api.FabricLoader;

public final class GutagsFabricClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        GutagsClientRuntime runtime = new GutagsClientRuntime(
                FabricLoader.getInstance().getConfigDir().resolve("gutags.json")
        );

        ClientTickEvents.END_CLIENT_TICK.register(runtime::tick);
        ClientLifecycleEvents.CLIENT_STOPPING.register(client -> runtime.stop());
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> dispatcher.register(
                ClientCommandManager.literal("gutags")
                        .then(ClientCommandManager.literal("on")
                                .executes(context -> runtime.setShowInNameTag(true)))
                        .then(ClientCommandManager.literal("off")
                                .executes(context -> runtime.setShowInNameTag(false)))
        ));
    }
}
