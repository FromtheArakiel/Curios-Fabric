package dev.arakiel.curiosfabric.api.type;

import java.util.Set;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.ApiStatus;
import dev.arakiel.curiosfabric.api.type.capability.ICurio;

public interface ISlotType extends Comparable<ISlotType> {

  /**
   * @return The identifier for this slot type
   */
  String getIdentifier();

  /**
   * @return The accessory implementation that provided this slot definition
   */
  default SlotSource getSource() {
    return SlotSource.CURIOSFABRIC;
  }

  /**
   * @return The {@link ResourceLocation} for the icon associated with this slot type
   */
  ResourceLocation getIcon();

  /**
   * @return The ordering priority of this slot type, lower numbers appear first
   */
  int getOrder();

  /**
   * @return The number of slots to give by default for this slot type
   */
  int getSize();

  /**
   * @return True if the slot type appears in the native Curios GUI, false otherwise
   */
  boolean useNativeGui();

  /**
   * @return True if the slot type has active cosmetic slots, false otherwise
   */
  boolean hasCosmetic();

  /**
   * @return True if the slot type can toggle rendering on entities, false otherwise
   */
  boolean canToggleRendering();

  /**
   * @return The {@link ICurio.DropRule} associated with this slot type
   */
  ICurio.DropRule getDropRule();

  /**
   * @return The set of {@link ResourceLocation} keyed to the validator predicates on this slot type
   */
  default Set<ResourceLocation> getValidators() {
    return Set.of();
  }

  default CompoundTag writeNbt() {
    return new CompoundTag();
  }

  /**
   * @deprecated Check if {@link ISlotType#getSize()} returns 0
   */
  @Deprecated(forRemoval = true)
  @ApiStatus.ScheduledForRemoval(inVersion = "1.21")
  default boolean isLocked() {
    return this.getSize() == 0;
  }

  /**
   * @deprecated Use {@link ISlotType#getOrder()}
   */
  @Deprecated(since = "1.20.1", forRemoval = true)
  @ApiStatus.ScheduledForRemoval(inVersion = "1.22")
  default int getPriority() {
    return this.getOrder();
  }

  /**
   * @deprecated Use {@link ISlotType#useNativeGui()}
   */
  @Deprecated(since = "1.20.1", forRemoval = true)
  @ApiStatus.ScheduledForRemoval(inVersion = "1.22")
  default boolean isVisible() {
    return this.useNativeGui();
  }
}
