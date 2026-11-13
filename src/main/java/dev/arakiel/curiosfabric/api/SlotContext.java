package dev.arakiel.curiosfabric.api;

import net.minecraft.world.entity.LivingEntity;

/**
 * A record representing the accessible slot information related to its context
 *
 * @param identifier The identifier of the slot type
 * @param entity     The wearer or intended wearer of the slot type
 * @param index      The index of the slot
 * @param cosmetic   True if the slot is cosmetic, false if the slot is functional
 * @param visible    True if the slot can render its item on the wearer, false if not
 */
public record SlotContext(String identifier, LivingEntity entity, int index, boolean cosmetic,
                          boolean visible) {

}
