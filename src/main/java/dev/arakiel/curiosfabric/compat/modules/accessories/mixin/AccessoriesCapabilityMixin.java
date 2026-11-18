package dev.arakiel.curiosfabric.compat.modules.accessories.mixin;

import dev.arakiel.curiosfabric.api.CuriosApi;
import dev.arakiel.curiosfabric.compat.modules.accessories.api.AccessoriesCapabilityFacade;
import io.wispforest.accessories.api.AccessoriesCapability;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Redirects the public Accessories capability lookup to the CuriosFabric inventory view. */
@Mixin(AccessoriesCapability.class)
public interface AccessoriesCapabilityMixin {

  @Inject(method = "get", at = @At("HEAD"), cancellable = true)
  private static void curiosfabric$useCuriosFabricInventory(
      LivingEntity entity, CallbackInfoReturnable<AccessoriesCapability> callback) {
    if (entity != null && CuriosApi.getCuriosInventory(entity).isPresent()) {
      callback.setReturnValue(AccessoriesCapabilityFacade.of(entity));
    }
  }
}
