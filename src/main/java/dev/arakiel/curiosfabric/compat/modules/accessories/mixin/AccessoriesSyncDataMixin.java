package dev.arakiel.curiosfabric.compat.modules.accessories.mixin;

import dev.arakiel.curiosfabric.api.type.SlotSource;
import dev.arakiel.curiosfabric.compat.CompatibilityRuntime;
import dev.arakiel.curiosfabric.compat.core.contract.BridgeRefreshReason;
import dev.arakiel.curiosfabric.compat.core.contract.BridgeSide;
import io.wispforest.accessories.networking.client.SyncData;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Refreshes Accessories metadata after its clientbound slot snapshot is installed. */
@Mixin(SyncData.class)
public abstract class AccessoriesSyncDataMixin {

  @Inject(method = "handlePacket", at = @At("TAIL"))
  private static void curiosfabric$refreshAccessoriesBridge(
      SyncData packet, Player player, CallbackInfo callback) {
    CompatibilityRuntime.refreshSource(
        SlotSource.ACCESSORIES,
        BridgeSide.CLIENT,
        BridgeRefreshReason.CLIENT_SYNC,
        true);
  }
}
