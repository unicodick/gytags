package lol.unic.gutags.mixin.client;

import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import lol.unic.gutags.client.BadgeRenderer;

@Mixin(EntityRenderer.class)
public abstract class EntityRendererMixin {
    @Inject(method = "getNameTag", at = @At("RETURN"), cancellable = true)
    private void gutags$decorateNameTag(Entity entity, CallbackInfoReturnable<Component> callback) {
        if (entity instanceof Player) {
            Component original = callback.getReturnValue();
            if (original != null) {
                callback.setReturnValue(BadgeRenderer.decorateName(original, entity.getName().getString()));
            }
        }
    }
}
