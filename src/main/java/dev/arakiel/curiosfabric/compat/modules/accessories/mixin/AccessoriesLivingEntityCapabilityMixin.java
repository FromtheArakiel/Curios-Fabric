package dev.arakiel.curiosfabric.compat.modules.accessories.mixin;

import dev.arakiel.curiosfabric.api.CuriosApi;
import dev.arakiel.curiosfabric.compat.modules.accessories.api.AccessoriesCapabilityFacade;
import io.wispforest.accessories.api.AccessoriesCapability;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Covers direct calls to the injected AccessoriesAPIAccess method. Accessories applies its own
 * LivingEntity mixin first; this mixin runs later and redirects that method to the same facade.
 */
@Mixin(value = LivingEntity.class, priority = 2000)
public abstract class AccessoriesLivingEntityCapabilityMixin {

  @Inject(method = "accessoriesCapability", at = @At("HEAD"), cancellable = true)
  private void curiosfabric$useCuriosFabricInventory(
      CallbackInfoReturnable<AccessoriesCapability> callback) {
    LivingEntity entity = (LivingEntity) (Object) this;
    if (CuriosApi.getCuriosInventory(entity).isPresent()) {
      callback.setReturnValue(AccessoriesCapabilityFacade.of(entity));
    }
  }
}
