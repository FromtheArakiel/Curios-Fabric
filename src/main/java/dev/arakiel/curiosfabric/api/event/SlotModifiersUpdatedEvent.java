package dev.arakiel.curiosfabric.api.event;

import com.google.common.collect.ImmutableSet;
import java.util.Set;
import net.minecraft.world.entity.LivingEntity;

/**
 * {@link SlotModifiersUpdatedEvent} is fired when the slot size is dynamically changed during
 * gameplay through slot modifiers.
 * <br> This event is fired on both the client and the server.
 * <br>
 * {@link #types} contains the affected {@link dev.arakiel.curiosfabric.api.type.ISlotType}. <br>
 * <br>
 * This event is fired on the {@link CuriosEventBus}.
 **/
public class SlotModifiersUpdatedEvent implements ICuriosEvent {

  private final LivingEntity livingEntity;
  private final Set<String> types;

  public SlotModifiersUpdatedEvent(LivingEntity livingEntity, Set<String> types) {
    this.livingEntity = livingEntity;
    this.types = types;
  }

  /** The entity whose slot modifiers changed. */
  public LivingEntity getEntity() {
    return this.livingEntity;
  }

  public Set<String> getTypes() {
    return ImmutableSet.copyOf(this.types);
  }
}
