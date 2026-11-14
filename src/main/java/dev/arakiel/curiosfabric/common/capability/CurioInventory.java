package dev.arakiel.curiosfabric.common.capability;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.mojang.datafixers.util.Pair;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import javax.annotation.Nonnull;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import dev.arakiel.curiosfabric.api.CuriosApi;
import dev.arakiel.curiosfabric.api.SlotResult;
import dev.arakiel.curiosfabric.api.type.ISlotType;
import dev.arakiel.curiosfabric.api.type.capability.ICuriosItemHandler;
import dev.arakiel.curiosfabric.api.type.inventory.ICurioStacksHandler;
import dev.arakiel.curiosfabric.api.type.inventory.IDynamicStackHandler;
import dev.arakiel.curiosfabric.common.inventory.CurioStacksHandler;
import dev.arakiel.curiosfabric.common.slottype.SlotOrdering;
import dev.arakiel.gemini.component.ComponentKey;
import dev.arakiel.gemini.component.GeminiComponents;
import dev.arakiel.gemini.component.RespawnableComponent;

public class CurioInventory implements RespawnableComponent<CurioInventory> {

  public static final ComponentKey<CurioInventory> KEY =
      GeminiComponents.key("curiosfabric", "inventory", CurioInventory.class);

  final Map<String, ICurioStacksHandler> curios = new LinkedHashMap<>();
  ICuriosItemHandler curiosItemHandler;
  NonNullList<ItemStack> invalidStacks = NonNullList.create();
  Set<ICurioStacksHandler> updates = new HashSet<>();
  CompoundTag deserialized = new CompoundTag();
  boolean markDeserialized = false;

  final Cache<String, Pair<Long, Optional<SlotResult>>> firstCurioCache =
      CacheBuilder.newBuilder().maximumSize(100).expireAfterWrite(1, TimeUnit.SECONDS).build();
  final Cache<String, Pair<Long, List<SlotResult>>> findCuriosCache =
      CacheBuilder.newBuilder().maximumSize(100).expireAfterWrite(1, TimeUnit.SECONDS).build();

  public void init(final ICuriosItemHandler curiosItemHandler) {
    this.curiosItemHandler = curiosItemHandler;
    this.curios.clear();
    LivingEntity livingEntity = curiosItemHandler.getWearer();

    if (!this.markDeserialized) {
      List<ISlotType> sorted =
          SlotOrdering.order(CuriosApi.getEntitySlots(livingEntity).values());

      for (ISlotType slotType : sorted) {
        this.curios.put(
            slotType.getIdentifier(),
            new CurioStacksHandler(
                curiosItemHandler,
                slotType.getIdentifier(),
                slotType.getSize(),
                slotType.useNativeGui(),
                slotType.hasCosmetic(),
                slotType.canToggleRendering(),
                slotType.getDropRule()));
      }
    } else {
      this.markDeserialized = false;

      ListTag tagList = this.deserialized.getList("Curios", Tag.TAG_COMPOUND);
      Map<String, ICurioStacksHandler> curios = new LinkedHashMap<>();
      Map<ISlotType, ICurioStacksHandler> sortedCurios = new LinkedHashMap<>();
      List<ISlotType> sorted =
          SlotOrdering.order(CuriosApi.getEntitySlots(livingEntity).values());

      for (ISlotType slotType : sorted) {
        sortedCurios.put(
            slotType,
            new CurioStacksHandler(
                curiosItemHandler,
                slotType.getIdentifier(),
                slotType.getSize(),
                slotType.useNativeGui(),
                slotType.hasCosmetic(),
                slotType.canToggleRendering(),
                slotType.getDropRule()));
      }

      for (int i = 0; i < tagList.size(); i++) {
        CompoundTag tag = tagList.getCompound(i);
        String identifier = tag.getString("Identifier");
        CurioStacksHandler prevStacksHandler =
            new CurioStacksHandler(curiosItemHandler, identifier);
        prevStacksHandler.deserializeNBT(tag.getCompound("StacksHandler"));

        Optional<ISlotType> optionalType =
            Optional.ofNullable(CuriosApi.getEntitySlots(livingEntity).get(identifier));
        optionalType.ifPresent(
            slotType -> {
              CurioStacksHandler newStacksHandler =
                  new CurioStacksHandler(
                      curiosItemHandler,
                      slotType.getIdentifier(),
                      slotType.getSize(),
                      slotType.useNativeGui(),
                      slotType.hasCosmetic(),
                      slotType.canToggleRendering(),
                      slotType.getDropRule());
              newStacksHandler.copyModifiers(prevStacksHandler);
              int index = 0;

              while (index < newStacksHandler.getSlots() && index < prevStacksHandler.getSlots()) {
                ItemStack prevStack = prevStacksHandler.getStacks().getStackInSlot(index);

                if (!prevStack.isEmpty()) {

                  if (newStacksHandler.getStacks().isItemValid(index, prevStack)) {
                    newStacksHandler.getStacks().setStackInSlot(index, prevStack);
                  } else {
                    this.curiosItemHandler.loseInvalidStack(prevStack);
                  }
                }
                ItemStack prevCosmetic =
                    prevStacksHandler.getCosmeticStacks().getStackInSlot(index);

                if (!prevCosmetic.isEmpty()) {

                  if (newStacksHandler.getStacks().isItemValid(index, prevCosmetic)) {
                    newStacksHandler
                        .getCosmeticStacks()
                        .setStackInSlot(
                            index, prevStacksHandler.getCosmeticStacks().getStackInSlot(index));
                  } else {
                    this.curiosItemHandler.loseInvalidStack(prevCosmetic);
                  }
                }
                index++;
              }

              while (index < prevStacksHandler.getSlots()) {
                this.curiosItemHandler.loseInvalidStack(
                    prevStacksHandler.getStacks().getStackInSlot(index));
                this.curiosItemHandler.loseInvalidStack(
                    prevStacksHandler.getCosmeticStacks().getStackInSlot(index));
                index++;
              }
              sortedCurios.put(slotType, newStacksHandler);

              for (int j = 0;
                   j < newStacksHandler.getRenders().size()
                       && j < prevStacksHandler.getRenders().size();
                   j++) {
                newStacksHandler.getRenders().set(j, prevStacksHandler.getRenders().get(j));
              }

              for (int j = 0;
                   j < newStacksHandler.getActiveStates().size()
                       && j < prevStacksHandler.getActiveStates().size();
                   j++) {
                newStacksHandler.getActiveStates()
                    .set(j, prevStacksHandler.getActiveStates().get(j));
              }
            });

        if (optionalType.isEmpty()) {
          IDynamicStackHandler stackHandler = prevStacksHandler.getStacks();
          IDynamicStackHandler cosmeticStackHandler = prevStacksHandler.getCosmeticStacks();

          for (int j = 0; j < stackHandler.getSlots(); j++) {
            ItemStack stack = stackHandler.getStackInSlot(j);

            if (!stack.isEmpty()) {
              this.curiosItemHandler.loseInvalidStack(stack);
            }

            ItemStack cosmeticStack = cosmeticStackHandler.getStackInSlot(j);

            if (!cosmeticStack.isEmpty()) {
              this.curiosItemHandler.loseInvalidStack(cosmeticStack);
            }
          }
        }
      }
      sortedCurios.forEach(
          (slotType, stacksHandler) -> curios.put(slotType.getIdentifier(), stacksHandler));
      this.curios.putAll(curios);
      this.deserialized = new CompoundTag();
    }
  }

  public Map<String, ICurioStacksHandler> asMap() {
    return this.curios;
  }

  public void replace(Map<String, ICurioStacksHandler> curios) {
    this.curios.clear();
    this.curios.putAll(curios);
  }

  public CompoundTag serializeNBT(@Nonnull HolderLookup.Provider provider) {

    if (!this.deserialized.isEmpty()) {
      return this.deserialized;
    }
    CompoundTag compound = new CompoundTag();

    ListTag taglist = new ListTag();
    this.curios.forEach(
        (key, stacksHandler) -> {
          CompoundTag tag = new CompoundTag();
          tag.put("StacksHandler", stacksHandler.serializeNBT());
          tag.putString("Identifier", key);
          taglist.add(tag);
        });
    compound.put("Curios", taglist);
    return compound;
  }

  public void deserializeNBT(@Nonnull HolderLookup.Provider provider, @Nonnull CompoundTag nbt) {
    this.deserialized = nbt;
    this.markDeserialized = true;
  }

  @Override
  public void readFromNbt(CompoundTag tag, HolderLookup.Provider provider) {
    this.deserializeNBT(provider, tag);
  }

  @Override
  public void writeToNbt(CompoundTag tag, HolderLookup.Provider provider) {
    tag.merge(this.serializeNBT(provider));
  }
}
