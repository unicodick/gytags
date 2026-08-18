package lol.unic.gutags.neoforge;

import lol.unic.gutags.client.GutagsClientRuntime;
import net.minecraft.client.Minecraft;
import net.minecraft.commands.Commands;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterClientCommandsEvent;
import net.neoforged.neoforge.common.NeoForge;

@Mod(value = GutagsNeoForge.MOD_ID, dist = net.neoforged.api.distmarker.Dist.CLIENT)
public final class GutagsNeoForge {
    public static final String MOD_ID = "gutags";

    private final GutagsClientRuntime runtime;

    public GutagsNeoForge(IEventBus modBus, ModContainer modContainer) {
        runtime = new GutagsClientRuntime(FMLPaths.CONFIGDIR.get().resolve("gutags.json"));
        NeoForge.EVENT_BUS.addListener(this::onClientTick);
        NeoForge.EVENT_BUS.addListener(this::registerClientCommands);
    }

    private void onClientTick(ClientTickEvent.Post event) {
        runtime.tick(Minecraft.getInstance());
    }

    private void registerClientCommands(RegisterClientCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("gutags")
                .then(Commands.literal("on")
                        .executes(command -> runtime.setShowInNameTag(true)))
                .then(Commands.literal("off")
                        .executes(command -> runtime.setShowInNameTag(false))));
    }
}
