package dev.arakiel.curiosfabric.compat.modules.trinkets.mixin;

import java.util.Optional;
import java.util.Map;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import dev.arakiel.curiosfabric.compat.modules.trinkets.api.TrinketsApiFacade;

/**
 * Redirects Trinkets' component lookup to this mod's inventory.
 *
 * <p>Trinkets is handled data driven here: its item tags make an item a trinket for us, its GUI is
 * banned and its items are equipped through our right click handler. This mixin additionally makes
 * {@code TrinketsApi.getTrinketComponent} report the accessories that are actually worn, so mods
 * that only ask Trinkets whether something is equipped keep working.</p>
 */
@Mixin(targets = "dev.emi.trinkets.api.TrinketsApi")
public class TrinketsApiMixin {

  @Inject(method = "getTrinketComponent", at = @At("HEAD"), cancellable = true)
  private static void curiosfabric$getTrinketComponent(LivingEntity entity,
                                                       CallbackInfoReturnable<Optional<?>> cir) {
    Object bridged = TrinketsApiFacade.component(entity);

    if (bridged != null) {
      cir.setReturnValue(Optional.of(bridged));
    }
  }

  @Inject(method = "getEntitySlots(Lnet/minecraft/world/entity/Entity;)Ljava/util/Map;",
      at = @At("HEAD"), cancellable = true)
  private static void curiosfabric$getEntitySlots(Entity entity,
                                                  CallbackInfoReturnable<Map<String, ?>> cir) {
    Map<String, Object> bridged = TrinketsApiFacade.entitySlots(entity);
    if (!bridged.isEmpty()) {
      cir.setReturnValue(bridged);
    }
  }

  @Inject(method = "getEntitySlots(Lnet/minecraft/world/entity/EntityType;)Ljava/util/Map;",
      at = @At("HEAD"), cancellable = true)
  private static void curiosfabric$getEntitySlotsForType(
      EntityType<?> type, CallbackInfoReturnable<Map<String, ?>> cir) {
    Map<String, Object> bridged = TrinketsApiFacade.entitySlots(
        type, dev.arakiel.curiosfabric.CuriosFabricConstants.IS_CLIENT);
    if (!bridged.isEmpty()) {
      cir.setReturnValue(bridged);
    }
  }

  @Inject(method = "getEntitySlots(Lnet/minecraft/world/level/Level;"
      + "Lnet/minecraft/world/entity/EntityType;)Ljava/util/Map;",
      at = @At("HEAD"), cancellable = true)
  private static void curiosfabric$getEntitySlotsForLevel(
      Level level, EntityType<?> type, CallbackInfoReturnable<Map<String, ?>> cir) {
    Map<String, Object> bridged = TrinketsApiFacade.entitySlots(type, level.isClientSide());
    if (!bridged.isEmpty()) {
      cir.setReturnValue(bridged);
    }
  }

}
