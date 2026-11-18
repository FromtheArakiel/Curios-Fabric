package dev.arakiel.curiosfabric.compat.modules.neoforgecurios.mixin;

import dev.arakiel.curiosfabric.api.type.capability.ICurio;
import dev.arakiel.curiosfabric.compat.modules.neoforgecurios.duck.NeoforgeCuriosLegacySlotDuck;
import java.util.Set;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

/** Adds a namespaced typed view to NeoForge Curios' legacy slot type. */
@Mixin(targets = "top.theillusivec4.curios.common.slottype.SlotType", remap = false)
public abstract class NeoforgeCuriosLegacySlotMixin
    implements NeoforgeCuriosLegacySlotDuck {

  @Shadow
  public abstract int getOrder();

  @Shadow
  public abstract int getSize();

  @Shadow
  public abstract boolean useNativeGui();

  @Shadow
  public abstract boolean hasCosmetic();

  @Shadow
  public abstract boolean canToggleRendering();

  @Shadow
  public abstract ResourceLocation getIcon();

  @Shadow
  public abstract Set<ResourceLocation> getValidators();

  @Shadow
  public abstract CompoundTag writeNbt();

  @Override
  public int curiosfabric$order() {
    return getOrder();
  }

  @Override
  public int curiosfabric$size() {
    return getSize();
  }

  @Override
  public boolean curiosfabric$useNativeGui() {
    return useNativeGui();
  }

  @Override
  public boolean curiosfabric$hasCosmetic() {
    return hasCosmetic();
  }

  @Override
  public boolean curiosfabric$renderToggle() {
    return canToggleRendering();
  }

  @Override
  public ResourceLocation curiosfabric$icon() {
    return getIcon();
  }

  @Override
  public Set<ResourceLocation> curiosfabric$validators() {
    return getValidators();
  }

  @Override
  public ICurio.DropRule curiosfabric$dropRule() {
    int ordinal = writeNbt().getInt("DropRule");
    ICurio.DropRule[] values = ICurio.DropRule.values();
    return ordinal >= 0 && ordinal < values.length ? values[ordinal] : ICurio.DropRule.DEFAULT;
  }
}
