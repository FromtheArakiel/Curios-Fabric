package dev.arakiel.curiosfabric.compat.modules.accessories.mixin;

import io.wispforest.accessories.networking.client.SyncEntireContainer;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Prevents delayed upstream packets from restoring a second client-side inventory snapshot. */
@Mixin(SyncEntireContainer.class)
public abstract class AccessoriesInventorySyncClientMixin {

  @Inject(method = "handlePacket", at = @At("HEAD"), cancellable = true)
  private static void curiosfabric$skipInventorySnapshot(
      SyncEntireContainer packet, Player player, CallbackInfo callback) {
    callback.cancel();
  }
}
