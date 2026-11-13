package dev.arakiel.curiosfabric.common.data;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.util.*;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.EntityType;
import net.fabricmc.fabric.api.resource.conditions.v1.ResourceCondition;
import dev.arakiel.curiosfabric.common.util.CuriosConditions;
import dev.arakiel.curiosfabric.api.type.data.IEntitiesData;

public class EntitiesData implements IEntitiesData {

  private final Set<EntityType<?>> entities = new LinkedHashSet<>();
  private final Set<String> slots = new LinkedHashSet<>();
  private Boolean replace;
  private List<ResourceCondition> conditions;

  @Override
  public EntitiesData replace(boolean replace) {
    this.replace = replace;
    return this;
  }

  @Override
  public EntitiesData addPlayer() {
    return addEntities(EntityType.PLAYER);
  }

  @Override
  public EntitiesData addEntities(EntityType<?>... entityTypes) {
    this.entities.addAll(Arrays.stream(entityTypes).toList());
    return this;
  }

  @Override
  public EntitiesData addSlots(String... slots) {
    this.slots.addAll(Arrays.stream(slots).toList());
    return this;
  }

  @Override
  public EntitiesData addCondition(ResourceCondition condition) {

    if (this.conditions == null) {
      this.conditions = new ArrayList<>();
    }
    this.conditions.add(condition);
    return this;
  }

  @Override
  public JsonObject serialize(HolderLookup.Provider provider) {
    JsonObject jsonObject = new JsonObject();

    if (this.replace != null) {
      jsonObject.addProperty("replace", this.replace);
    }

    if (!this.entities.isEmpty()) {
      JsonArray arr = new JsonArray();
      this.entities.forEach(entityType -> arr.add(
          Objects.requireNonNull(BuiltInRegistries.ENTITY_TYPE.getKey(entityType)).toString()));
      jsonObject.add("entities", arr);
    }

    if (!this.slots.isEmpty()) {
      JsonArray arr = new JsonArray();
      this.slots.forEach(arr::add);
      jsonObject.add("slots", arr);
    }

    if (this.conditions != null) {
      CuriosConditions.write(jsonObject, this.conditions);
    }
    return jsonObject;
  }
}
