package dev.arakiel.curiosfabric.mixin.core;

import net.minecraft.world.entity.monster.EnderMan;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import dev.arakiel.curiosfabric.common.event.CuriosFabricEventHandler;

/**
 * Mirrors Neoforge's {@code EnderManAngerEvent}: a worn curio can act as an ender mask and stop the
 * enderman from getting angry.
 */
@Mixin(EnderMan.class)
public class MixinEnderMan {

  @Inject(method = "isLookingAtMe", at = @At("HEAD"), cancellable = true)
  private void curios$isEnderMask(Player player, CallbackInfoReturnable<Boolean> cir) {

    if (CuriosFabricEventHandler.hasEnderMask(player, (EnderMan) (Object) this)) {
      cir.setReturnValue(false);
    }
  }
}
