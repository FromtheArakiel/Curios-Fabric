package dev.arakiel.curiosfabric.api.type.data;

import com.google.gson.JsonObject;
import net.minecraft.core.HolderLookup;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.fabricmc.fabric.api.resource.conditions.v1.ResourceCondition;
import dev.arakiel.curiosfabric.api.type.capability.ICurio;

/** Used in data generation to represent the slot data */
public interface ISlotData {

  ISlotData replace(boolean replace);

  ISlotData order(int order);

  ISlotData size(int size);

  default ISlotData operation(String operation) {
    return this.operation(AttributeModifier.Operation.ADD_VALUE);
  }

  /**
   * @see #operation(String)
   */
  @Deprecated(forRemoval = true)
  ISlotData operation(AttributeModifier.Operation operation);

  ISlotData useNativeGui(boolean useNativeGui);

  ISlotData addCosmetic(boolean addCosmetic);

  ISlotData renderToggle(boolean renderToggle);

  ISlotData icon(ResourceLocation icon);

  ISlotData dropRule(ICurio.DropRule dropRule);

  ISlotData addCondition(ResourceCondition condition);

  ISlotData addValidator(ResourceLocation resourceLocation);

  JsonObject serialize(HolderLookup.Provider provider);
}
