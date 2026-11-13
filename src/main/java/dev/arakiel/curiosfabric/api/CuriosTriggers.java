package dev.arakiel.curiosfabric.api;

import javax.annotation.Nonnull;
import net.minecraft.advancements.Criterion;
import net.minecraft.advancements.CriterionTriggerInstance;
import net.minecraft.advancements.critereon.ItemPredicate;
import net.minecraft.advancements.critereon.LocationPredicate;

public class CuriosTriggers {

  /**
   * Builds a new {@link EquipBuilder} for data generation using the trigger that fires when a
   * curio item is equipped
   *
   * @return A new builder instance
   */
  @Nonnull
  public static EquipBuilder equip() {
    return new EquipBuilder();
  }

  /**
   * @deprecated Use {@link CuriosTriggers#equip()} to build the needed parameters
   */
  @Deprecated
  @Nonnull
  public static Criterion<? extends CriterionTriggerInstance> equip(
      ItemPredicate.Builder itemPredicate) {
    return new Criterion<>(null, null);
  }

  /**
   * @deprecated Use {@link CuriosTriggers#equip()} to build the needed parameters
   */
  @Deprecated
  @Nonnull
  public static Criterion<? extends CriterionTriggerInstance> equipAtLocation(
      ItemPredicate.Builder itemPredicate, LocationPredicate.Builder locationPredicate) {
    return new Criterion<>(null, null);
  }

  public static final class EquipBuilder {

    private ItemPredicate.Builder itemPredicate;
    private LocationPredicate.Builder locationPredicate;
    private SlotPredicate.Builder slotPredicate;

    private EquipBuilder() {
    }

    public EquipBuilder withItem(ItemPredicate.Builder builder) {
      this.itemPredicate = builder;
      return this;
    }

    public EquipBuilder withLocation(LocationPredicate.Builder builder) {
      this.locationPredicate = builder;
      return this;
    }

    public EquipBuilder withSlot(SlotPredicate.Builder builder) {
      this.slotPredicate = builder;
      return this;
    }

    public Criterion<? extends CriterionTriggerInstance> build() {
      return new Criterion<>(null, null);
    }
  }
}
