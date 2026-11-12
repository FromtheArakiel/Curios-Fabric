package dev.arakiel.curiosfabric.common.capability;

import dev.arakiel.gemini.component.ComponentFactoryRegistries;
import dev.arakiel.gemini.component.ComponentInitializers;
import dev.arakiel.gemini.component.RespawnCopyStrategy;
import net.minecraft.world.entity.LivingEntity;

/** Registers Curios inventory storage with Gemini Components. */
public final class CuriosFabricEntityComponents implements ComponentInitializers.Entity {

  @Override
  public void registerEntityComponentFactories(ComponentFactoryRegistries.Entity registry) {
    registry.beginRegistration(LivingEntity.class, CurioInventory.KEY)
        .respawnStrategy(RespawnCopyStrategy.ALWAYS_COPY)
        .end(entity -> new CurioInventory());
  }
}
