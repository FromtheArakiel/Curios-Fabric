package dev.arakiel.curiosfabric.compat.modules.accessories.mixin;

import io.wispforest.accessories.networking.client.SyncEntireContainer;
import java.util.function.Consumer;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Disables Accessories' second inventory synchronization channel. Slot metadata remains enabled. */
@Mixin(SyncEntireContainer.class)
public abstract class AccessoriesInventorySyncMixin {

  @Inject(method = "syncTo", at = @At("HEAD"), cancellable = true)
  private static void curiosfabric$skipInventorySync(
      LivingEntity entity, Consumer<Record> packetConsumer, CallbackInfo callback) {
    callback.cancel();
  }
}
