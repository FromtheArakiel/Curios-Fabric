package dev.arakiel.curiosfabric.compat.modules.neoforgecurios.duck;

import net.minecraft.world.entity.LivingEntity;

/** Strongly typed view of NeoForge Curios' fixed SlotContext fields. */
public interface NeoforgeCuriosSlotContextDuck {

  String curiosfabric$identifier();

  LivingEntity curiosfabric$entity();

  int curiosfabric$index();

  boolean curiosfabric$cosmetic();

  boolean curiosfabric$visible();
}
