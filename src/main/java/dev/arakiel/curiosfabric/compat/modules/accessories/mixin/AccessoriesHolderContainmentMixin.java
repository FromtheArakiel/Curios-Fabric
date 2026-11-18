package dev.arakiel.curiosfabric.compat.modules.accessories.mixin;

import dev.arakiel.curiosfabric.compat.modules.accessories.api.AccessoriesCapabilityFacade;
import io.wispforest.accessories.api.AccessoriesCapability;
import io.wispforest.accessories.api.AccessoriesContainer;
import io.wispforest.accessories.impl.AccessoriesHolderImpl;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.world.entity.LivingEntity;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Keeps the concrete upstream holder from exposing its own inventory.
 *
 * <p>Accessories' public holder interface has no container accessor, but third-party code can cast
 * to this implementation and call {@code getSlotContainers()}. Returning CuriosFabric-backed views
 * here closes that bypass without duplicating the holder's UI preference state.</p>
 */
@Mixin(AccessoriesHolderImpl.class)
public abstract class AccessoriesHolderContainmentMixin {

  @Unique
  @Nullable
  private LivingEntity curiosfabric$entity;

  @Inject(method = "init", at = @At("HEAD"))
  private void curiosfabric$captureEntity(
      AccessoriesCapability capability, CallbackInfo callback) {
    curiosfabric$entity = capability.entity();
  }

  @Inject(method = "getSlotContainers", at = @At("RETURN"), cancellable = true)
  private void curiosfabric$useCurioBackedContainers(
      CallbackInfoReturnable<Map<String, AccessoriesContainer>> callback) {
    LivingEntity entity = curiosfabric$resolveEntity(callback.getReturnValue());
    if (entity == null) {
      return;
    }
    Map<String, AccessoriesContainer> containers = new LinkedHashMap<>(
        AccessoriesCapabilityFacade.of(entity).getContainers());
    callback.setReturnValue(Collections.unmodifiableMap(containers));
  }

  @Unique
  @Nullable
  private LivingEntity curiosfabric$resolveEntity(
      Map<String, AccessoriesContainer> containers) {
    if (curiosfabric$entity != null) {
      return curiosfabric$entity;
    }
    for (AccessoriesContainer container : containers.values()) {
      LivingEntity entity = container.capability().entity();
      if (entity != null) {
        curiosfabric$entity = entity;
        return entity;
      }
    }
    return null;
  }
}
