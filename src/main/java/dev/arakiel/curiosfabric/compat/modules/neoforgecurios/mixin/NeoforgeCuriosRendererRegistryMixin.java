package dev.arakiel.curiosfabric.compat.modules.neoforgecurios.mixin;

import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;
import net.minecraft.world.item.Item;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Makes NeoForge Curios resolve a registered renderer supplier even though its normal client setup
 * is disabled by CuriosFabric.
 */
@Mixin(targets = "top.theillusivec4.curios.api.client.CuriosRendererRegistry", remap = false)
public abstract class NeoforgeCuriosRendererRegistryMixin {

  @Shadow
  @Final
  private static Map<Item, Supplier<?>> RENDERER_REGISTRY;

  @Inject(method = "getRenderer", at = @At("RETURN"), cancellable = true, remap = false)
  private static void curiosfabric$resolveRegisteredSupplier(
      Item item, CallbackInfoReturnable<Optional<?>> cir) {
    Optional<?> resolved = cir.getReturnValue();
    if (resolved != null && resolved.isPresent()) {
      return;
    }
    Supplier<?> supplier = RENDERER_REGISTRY.get(item);
    if (supplier == null) {
      return;
    }
    Object renderer = supplier.get();
    if (renderer != null) {
      cir.setReturnValue(Optional.of(renderer));
    }
  }
}
