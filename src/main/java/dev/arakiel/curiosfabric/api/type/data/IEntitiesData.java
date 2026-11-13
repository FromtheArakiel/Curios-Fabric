package dev.arakiel.curiosfabric.api.type.data;

import com.google.gson.JsonObject;
import net.minecraft.core.HolderLookup;
import net.minecraft.world.entity.EntityType;
import net.fabricmc.fabric.api.resource.conditions.v1.ResourceCondition;

/**
 * Used in data generation to represent the entities data
 */
public interface IEntitiesData {

  IEntitiesData replace(boolean replace);

  IEntitiesData addPlayer();

  IEntitiesData addEntities(EntityType<?>... entityTypes);

  IEntitiesData addSlots(String... slots);

  IEntitiesData addCondition(ResourceCondition condition);

  JsonObject serialize(HolderLookup.Provider provider);
}
