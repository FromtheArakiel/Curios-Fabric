package dev.arakiel.curiosfabric.compat.modules.neoforgecurios.mixin;

import dev.arakiel.curiosfabric.compat.modules.neoforgecurios.duck.NeoforgeCuriosSlotContextDuck;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

/** Injects a namespaced, strongly typed view into NeoForge Curios SlotContext. */
@Mixin(targets = "top.theillusivec4.curios.api.SlotContext", remap = false)
public abstract class NeoforgeCuriosSlotContextMixin implements NeoforgeCuriosSlotContextDuck {

  @Shadow
  @Final
  private String identifier;

  @Shadow
  @Final
  private LivingEntity entity;

  @Shadow
  @Final
  private int index;

  @Shadow
  @Final
  private boolean cosmetic;

  @Shadow
  @Final
  private boolean visible;

  @Override
  public String curiosfabric$identifier() {
    return identifier;
  }

  @Override
  public LivingEntity curiosfabric$entity() {
    return entity;
  }

  @Override
  public int curiosfabric$index() {
    return index;
  }

  @Override
  public boolean curiosfabric$cosmetic() {
    return cosmetic;
  }

  @Override
  public boolean curiosfabric$visible() {
    return visible;
  }
}
