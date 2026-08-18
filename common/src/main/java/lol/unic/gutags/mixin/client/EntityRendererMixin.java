package lol.unic.gutags.mixin.client;

import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;

import lol.unic.gutags.client.BadgeRenderer;

@Mixin(EntityRenderer.class)
public abstract class EntityRendererMixin {
    @ModifyArgs(
            method = "render",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/renderer/entity/EntityRenderer;"
                            + "renderNameTag(Lnet/minecraft/world/entity/Entity;"
                            + "Lnet/minecraft/network/chat/Component;"
                            + "Lcom/mojang/blaze3d/vertex/PoseStack;"
                            + "Lnet/minecraft/client/renderer/MultiBufferSource;IF)V"
            )
    )
    private void gutags$decorateNameTag(Args args) {
        Entity entity = args.get(0);
        if (entity instanceof Player) {
            Component original = args.get(1);
            args.set(1, BadgeRenderer.decorateName(original, entity.getName().getString()));
        }
    }
}
