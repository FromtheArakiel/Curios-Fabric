/*
 * Copyright (c) 2018-2024 C4
 *
 * This file is part of Curios, a mod made for Minecraft.
 *
 * Curios is free software: you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as published
 * by the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */

package dev.arakiel.curios.common.event;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.mojang.datafixers.util.Pair;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Predicate;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.fabricmc.fabric.api.networking.v1.EntityTrackingEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerEntityWorldChangeEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.core.Holder;
import net.minecraft.core.NonNullList;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.util.Tuple;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeMap;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentEffectComponents;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import dev.arakiel.curios.api.CuriosApi;
import dev.arakiel.curios.CuriosConstants;
import dev.arakiel.curios.api.SlotAttribute;
import dev.arakiel.curios.api.SlotContext;
import dev.arakiel.curios.api.event.CurioChangeEvent;
import dev.arakiel.curios.api.event.CurioDropsEvent;
import dev.arakiel.curios.api.event.CuriosEventBus;
import dev.arakiel.curios.api.event.DropRulesEvent;
import dev.arakiel.curios.api.type.ICuriosMenu;
import dev.arakiel.curios.api.type.ISlotType;
import dev.arakiel.curios.api.type.capability.ICurio;
import dev.arakiel.curios.api.type.capability.ICurio.DropRule;
import dev.arakiel.curios.api.type.capability.ICuriosItemHandler;
import dev.arakiel.curios.api.type.inventory.ICurioStacksHandler;
import dev.arakiel.curios.api.type.inventory.IDynamicStackHandler;
import dev.arakiel.curios.common.CuriosConfig;
import dev.arakiel.curios.common.CuriosRegistry;
import dev.arakiel.curios.common.data.CuriosEntityManager;
import dev.arakiel.curios.common.data.CuriosSlotManager;
import dev.arakiel.curios.common.inventory.container.CuriosContainer;
import dev.arakiel.curios.common.network.NetworkHandler;
import dev.arakiel.curios.common.network.server.SPacketSetIcons;
import dev.arakiel.curios.common.network.server.sync.SPacketSyncCurios;
import dev.arakiel.curios.common.network.server.sync.SPacketSyncData;
import dev.arakiel.curios.common.network.server.sync.SPacketSyncModifiers;
import dev.arakiel.curios.common.network.server.sync.SPacketSyncStack;
import dev.arakiel.curios.common.network.server.sync.SPacketSyncStack.HandlerType;

/**
 * Server side game logic of Curios.
 *
 * <p>Every NeoForge event that the NeoForge edition subscribed to is replaced here by the matching
 * Fabric event, and where Fabric has no event the logic moved into a mixin (entity ticking,
 * experience orbs, ender masks, equipment changes and block experience).</p>
 */
public class CuriosEventHandler {

  public static boolean dirtyTags = false;

  private CuriosEventHandler() {
  }

  /** Registers every Fabric event this mod listens to. */
  public static void register() {
    ServerPlayConnectionEvents.JOIN.register(
        (handler, sender, server) -> playerLoggedIn(handler.player));
    ServerLifecycleEvents.END_DATA_PACK_RELOAD.register((server, resourceManager, success) -> {

      if (success && server != null) {

        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
          syncPlayer(player);
        }
      }
    });
    EntityTrackingEvents.START_TRACKING.register(
        (trackedEntity, player) -> playerStartTracking(player, trackedEntity));
    ServerPlayerEvents.COPY_FROM.register(
        (oldPlayer, newPlayer, alive) -> playerClone(newPlayer, oldPlayer));
    // Respawning (or changing dimension) replaces the player entity on both sides, and the client
    // builds a brand new LocalPlayer without any curios data. Without a fresh sync the panel is
    // empty and every later per slot update is dropped because the client has no slot handlers.
    //
    // The sync is deferred to the next server tick. Fabric fires the world change event from
    // ServerPlayer#setServerLevel, which vanilla calls at the very start of the join sequence -
    // before the login packet that puts the client into the play phase - so syncing inline would
    // send play phase payloads too early.
    ServerPlayerEvents.AFTER_RESPAWN.register(
        (oldPlayer, newPlayer, alive) -> deferSync(newPlayer));
    ServerEntityWorldChangeEvents.AFTER_PLAYER_CHANGE_WORLD.register(
        (player, origin, destination) -> deferSync(player));
    ServerLivingEntityEvents.AFTER_DEATH.register(CuriosEventHandler::playerDrops);
    UseItemCallback.EVENT.register(CuriosEventHandler::curioRightClick);
  }

  private static void handleDrops(
      String identifier,
      LivingEntity livingEntity,
      List<Tuple<Predicate<ItemStack>, DropRule>> dropRules,
      NonNullList<Boolean> renders,
      IDynamicStackHandler stacks,
      boolean cosmetic,
      Collection<ItemEntity> drops,
      boolean keepInventory,
      DamageSource source,
      boolean recentlyHit) {
    for (int i = 0; i < stacks.getSlots(); i++) {
      ItemStack stack = stacks.getStackInSlot(i);
      SlotContext slotContext =
          new SlotContext(
              identifier, livingEntity, i, cosmetic, renders.size() > i && renders.get(i));

      if (!stack.isEmpty()) {
        DropRule dropRuleOverride = null;

        for (Tuple<Predicate<ItemStack>, DropRule> override : dropRules) {

          if (override.getA().test(stack)) {
            dropRuleOverride = override.getB();
          }
        }
        DropRule dropRule = dropRuleOverride != null ? dropRuleOverride :
            CuriosApi.getCurio(stack).map(curio -> curio
                .getDropRule(slotContext, source, 0, recentlyHit)).orElse(DropRule.DEFAULT);

        if (dropRule == DropRule.DEFAULT) {
          dropRule =
              CuriosApi.getSlot(identifier, livingEntity.level())
                  .map(ISlotType::getDropRule)
                  .orElse(DropRule.DEFAULT);
        }

        if ((dropRule == DropRule.DEFAULT && keepInventory) || dropRule == DropRule.ALWAYS_KEEP) {
          continue;
        }

        if (!EnchantmentHelper.has(stack, EnchantmentEffectComponents.PREVENT_EQUIPMENT_DROP)
            && dropRule != DropRule.DESTROY) {
          drops.add(getDroppedItem(stack, livingEntity));
        }
        stacks.setStackInSlot(i, ItemStack.EMPTY);
      }
    }
  }

  private static ItemEntity getDroppedItem(ItemStack droppedItem, LivingEntity livingEntity) {
    double d0 = livingEntity.getY() - 0.30000001192092896D + livingEntity.getEyeHeight();
    ItemEntity entityitem =
        new ItemEntity(livingEntity.level(), livingEntity.getX(), d0, livingEntity.getZ(),
            droppedItem);
    entityitem.setPickUpDelay(40);
    float f = livingEntity.level().random.nextFloat() * 0.5F;
    float f1 = livingEntity.level().random.nextFloat() * ((float) Math.PI * 2F);
    entityitem.setDeltaMovement((-Mth.sin(f1) * f), 0.20000000298023224D, (Mth.cos(f1) * f));
    return entityitem;
  }

  private static void playerLoggedIn(ServerPlayer player) {
    syncPlayer(player);
  }

  /** Sends the slot/entity data, the equipped curios and the icons to a player. */
  private static void syncPlayer(ServerPlayer player) {

    try {
      syncPlayerUnchecked(player);
    } catch (Throwable throwable) {
      CuriosConstants.LOG.warn("Failed to sync the curios inventory to {}",
          player.getScoreboardName(), throwable);
    }
  }

  /** Runs {@link #syncPlayer(ServerPlayer)} on the next server tick. */
  private static void deferSync(ServerPlayer player) {
    net.minecraft.server.MinecraftServer server = player.getServer();

    if (server != null) {
      server.execute(() -> syncPlayer(player));
    }
  }

  private static void syncPlayerUnchecked(ServerPlayer player) {
    NetworkHandler.sendToPlayer(player,
        new SPacketSyncData(CuriosSlotManager.getSyncPacket(), CuriosEntityManager.getSyncPacket()));
    CuriosApi.getCuriosInventory(player)
        .ifPresent(
            handler -> {
              // A freshly created entity never had its slot structure built, because the capability
              // is only initialised while it deserialises something. Without this the very first
              // join of a new world sent an empty map to the client, which discarded every curio
              // slot and made the panel unusable until the world was left and entered again.
              if (handler.getCurios().isEmpty()) {
                handler.reset();
              }
              Tag tag = handler.writeTag();

              for (Map.Entry<String, ICurioStacksHandler> entry : handler.getCurios().entrySet()) {
                ICurioStacksHandler stacks = entry.getValue();

                for (int i = 0; i < stacks.getSlots(); i++) {
                  stacks.getStacks().setStackInSlot(i, ItemStack.EMPTY);
                  stacks.getCosmeticStacks().setStackInSlot(i, ItemStack.EMPTY);
                }
              }
              handler.readTag(tag);
              // The block above only staged the data instead of applying it; rebuilding right away
              // makes sure the packet below carries the real slot structure and the equipped
              // curios instead of the cleared placeholder map.
              handler.reset();
              NetworkHandler.sendToPlayer(player,
                  new SPacketSyncCurios(player.getId(), handler.getCurios()));

              if (player.containerMenu instanceof ICuriosMenu curiosContainer) {
                curiosContainer.resetSlots();
              }
            });
    sendIcons(player);
  }

  private static void sendIcons(ServerPlayer player) {
    Collection<ISlotType> slotTypes = CuriosApi.getPlayerSlots(player).values();
    Map<String, ResourceLocation> icons = new HashMap<>();
    slotTypes.forEach(type -> icons.put(type.getIdentifier(), type.getIcon()));
    NetworkHandler.sendToPlayer(player, new SPacketSetIcons(icons));
  }

  private static void playerStartTracking(ServerPlayer player, Entity target) {

    if (target instanceof LivingEntity livingBase) {
      CuriosApi.getCuriosInventory(livingBase)
          .ifPresent(handler -> NetworkHandler.sendToPlayer(player,
              new SPacketSyncCurios(target.getId(), handler.getCurios())));
    }
  }

  private static void playerClone(ServerPlayer player, ServerPlayer oldPlayer) {
    Optional<ICuriosItemHandler> oldHandler = CuriosApi.getCuriosInventory(oldPlayer);
    Optional<ICuriosItemHandler> newHandler = CuriosApi.getCuriosInventory(player);
    oldHandler.ifPresent(
        oldCurios -> newHandler.ifPresent(newCurios -> newCurios.readTag(oldCurios.writeTag())));
  }

  private static void playerDrops(LivingEntity livingEntity, DamageSource source) {

    if (!livingEntity.isSpectator()) {
      CuriosApi.getCuriosInventory(livingEntity)
          .ifPresent(
              handler -> {
                Collection<ItemEntity> curioDrops = new ArrayList<>();
                Map<String, ICurioStacksHandler> curios = handler.getCurios();
                boolean recentlyHit = livingEntity.getLastHurtByMob() != null
                    || livingEntity.getKillCredit() != null;
                DropRulesEvent dropRulesEvent =
                    new DropRulesEvent(livingEntity, handler, source, 0, recentlyHit);
                CuriosEventBus.post(dropRulesEvent);
                List<Tuple<Predicate<ItemStack>, DropRule>> dropRules =
                    dropRulesEvent.getOverrides();
                boolean keepInventory = false;

                if (livingEntity instanceof Player) {
                  keepInventory =
                      livingEntity.level().getGameRules().getBoolean(GameRules.RULE_KEEPINVENTORY);

                  if (CuriosConfig.SERVER.keepCurios.get() != CuriosConfig.KeepCurios.DEFAULT) {
                    keepInventory =
                        CuriosConfig.SERVER.keepCurios.get() == CuriosConfig.KeepCurios.ON;
                  }
                }
                boolean finalKeepInventory = keepInventory;
                curios.forEach(
                    (id, stacksHandler) -> {
                      handleDrops(
                          id,
                          livingEntity,
                          dropRules,
                          stacksHandler.getRenders(),
                          stacksHandler.getStacks(),
                          false,
                          curioDrops,
                          finalKeepInventory,
                          source,
                          recentlyHit);
                      handleDrops(
                          id,
                          livingEntity,
                          dropRules,
                          stacksHandler.getRenders(),
                          stacksHandler.getCosmeticStacks(),
                          true,
                          curioDrops,
                          finalKeepInventory,
                          source,
                          recentlyHit);
                    });
                CurioDropsEvent dropsEvent =
                    CuriosEventBus.post(
                        new CurioDropsEvent(
                            livingEntity,
                            handler,
                            source,
                            curioDrops,
                            0,
                            recentlyHit));

                if (!dropsEvent.isCanceled()) {
                  Level level = livingEntity.level();

                  for (ItemEntity drop : curioDrops) {
                    level.addFreshEntity(drop);
                  }
                }
              });
    }
  }

  private static InteractionResultHolder<ItemStack> curioRightClick(Player player, Level level,
                                                                    InteractionHand hand) {
    ItemStack stack = player.getItemInHand(hand);
    Optional<ICurio> possibleCurio = CuriosApi.getCurio(stack);

    if (possibleCurio.isEmpty()) {
      return InteractionResultHolder.pass(stack);
    }

    return CuriosApi.getCuriosInventory(player)
        .map(
            handler -> {
              ICurio curio = possibleCurio.get();
              Map<String, ICurioStacksHandler> curios = handler.getCurios();
              Tuple<IDynamicStackHandler, SlotContext> firstSlot = null;

              for (Map.Entry<String, ICurioStacksHandler> entry : curios.entrySet()) {
                IDynamicStackHandler stackHandler = entry.getValue().getStacks();
                NonNullList<Boolean> activeStates = entry.getValue().getActiveStates();

                for (int i = 0; i < stackHandler.getSlots(); i++) {
                  boolean active = activeStates.size() > i && activeStates.get(i);

                  if (!active) {
                    continue;
                  }
                  String id = entry.getKey();
                  NonNullList<Boolean> renderStates = entry.getValue().getRenders();
                  SlotContext slotContext =
                      new SlotContext(
                          id,
                          player,
                          i,
                          false,
                          renderStates.size() > i && renderStates.get(i));

                  if (stackHandler.isItemValid(i, stack)
                      && curio.canEquipFromUse(slotContext)) {
                    ItemStack present = stackHandler.getStackInSlot(i);

                    if (present.isEmpty()) {
                      stackHandler.setStackInSlot(i, stack.copy());
                      curio.onEquipFromUse(slotContext);

                      if (!player.isCreative()) {
                        int count = stack.getCount();
                        stack.shrink(count);
                      }
                      return InteractionResultHolder.success(stack);
                    } else if (firstSlot == null) {

                      if (stackHandler
                          .extractItem(i, stack.getMaxStackSize(), true)
                          .getCount()
                          == stack.getCount()) {
                        firstSlot = new Tuple<>(stackHandler, slotContext);
                      }
                    }
                  }
                }
              }

              if (firstSlot != null) {
                IDynamicStackHandler stackHandler = firstSlot.getA();
                SlotContext slotContext = firstSlot.getB();
                int i = slotContext.index();
                ItemStack present = stackHandler.getStackInSlot(i);
                stackHandler.setStackInSlot(i, stack.copy());
                curio.onEquipFromUse(slotContext);
                player.setItemInHand(hand, present.copy());
                return InteractionResultHolder.success(stack);
              }
              return InteractionResultHolder.pass(stack);
            })
        .orElse(InteractionResultHolder.pass(stack));
  }

  /** Computes the extra experience a broken block yields because of curio fortune bonuses. */
  public static int getFortuneExperience(LivingEntity entity, Level level, ItemStack tool) {

    if (!(level instanceof net.minecraft.server.level.ServerLevel serverLevel)) {
      return 0;
    }
    AtomicInteger experience = new AtomicInteger(0);
    CuriosApi.getCuriosInventory(entity)
        .ifPresent(
            handler -> {
              for (Map.Entry<String, ICurioStacksHandler> entry : handler.getCurios().entrySet()) {
                IDynamicStackHandler stacks = entry.getValue().getStacks();
                NonNullList<Boolean> renderStates = entry.getValue().getRenders();

                for (int i = 0; i < stacks.getSlots(); i++) {
                  SlotContext context =
                      new SlotContext(
                          entry.getKey(),
                          entity,
                          i,
                          false,
                          renderStates.size() > i && renderStates.get(i));
                  int fortune = CuriosApi.getCurio(stacks.getStackInSlot(i))
                      .map(curio -> curio.getFortuneLevel(context, null))
                      .orElse(0);

                  if (fortune != 0) {
                    experience.addAndGet(
                        EnchantmentHelper.processBlockExperience(serverLevel, tool, fortune));
                  }
                }
              }
            });
    return experience.get();
  }

  static Map<UUID, Pair<Long, Boolean>> enderManMaskCache = new HashMap<>();

  /** {@code true} when the player wears a curio that acts as an ender mask. */
  public static boolean hasEnderMask(Player player,
                                     net.minecraft.world.entity.monster.EnderMan enderMan) {
    // Check cached value first
    if (enderManMaskCache.size() > 500) {
      enderManMaskCache.clear();
    }
    long gameTime = player.level().getGameTime();

    if (enderManMaskCache.containsKey(player.getUUID())) {
      var pair = enderManMaskCache.get(player.getUUID());

      if (pair.getFirst() == gameTime) {
        return pair.getSecond();
      }
    }
    boolean[] result = {false};

    CuriosApi.getCuriosInventory(player)
        .ifPresent(
            handler -> {
              all:
              for (Map.Entry<String, ICurioStacksHandler> entry : handler.getCurios().entrySet()) {
                IDynamicStackHandler stacks = entry.getValue().getStacks();

                for (int i = 0; i < stacks.getSlots(); i++) {
                  final int index = i;
                  NonNullList<Boolean> renderStates = entry.getValue().getRenders();
                  boolean hasMask =
                      CuriosApi.getCurio(stacks.getStackInSlot(i))
                          .map(
                              curio ->
                                  curio.isEnderMask(
                                      new SlotContext(
                                          entry.getKey(),
                                          player,
                                          index,
                                          false,
                                          renderStates.size() > index && renderStates.get(index)),
                                      enderMan))
                          .orElse(false);

                  if (hasMask) {
                    result[0] = true;
                    break all;
                  }
                }
              }
            });
    enderManMaskCache.put(player.getUUID(), Pair.of(gameTime, result[0]));
    return result[0];
  }

  /** The entity tick logic, called from {@code MixinEntityTick} at the end of {@code Entity#tick}. */
  public static void tick(LivingEntity livingEntity) {

    if (livingEntity instanceof Player player
        && player.containerMenu instanceof CuriosContainer curiosContainer) {
      curiosContainer.checkQuickMove();
    }
    CuriosApi.getCuriosInventory(livingEntity)
        .ifPresent(
            handler -> {
              handler.clearCachedSlotModifiers();
              handler.handleInvalidStacks();
              Map<String, ICurioStacksHandler> curios = handler.getCurios();

              for (Map.Entry<String, ICurioStacksHandler> entry : curios.entrySet()) {
                ICurioStacksHandler stacksHandler = entry.getValue();
                String identifier = entry.getKey();
                IDynamicStackHandler stackHandler = stacksHandler.getStacks();
                IDynamicStackHandler cosmeticStackHandler = stacksHandler.getCosmeticStacks();
                NonNullList<Boolean> renderStates = stacksHandler.getRenders();

                for (int i = 0; i < stacksHandler.getSlots(); i++) {
                  stacksHandler.updateActiveState(i);
                  NonNullList<Boolean> activeStates = stacksHandler.getActiveStates();
                  boolean functional = activeStates.size() > i && activeStates.get(i);
                  SlotContext slotContext =
                      new SlotContext(
                          identifier,
                          livingEntity,
                          i,
                          false,
                          renderStates.size() > i && renderStates.get(i));
                  ItemStack stack = stackHandler.getStackInSlot(i);
                  Optional<ICurio> currentCurio = CuriosApi.getCurio(stack);

                  if (functional && !stack.isEmpty()) {
                    stack.inventoryTick(livingEntity.level(), livingEntity, -1, false);
                    currentCurio.ifPresent(curio -> curio.curioTick(slotContext));
                  }

                  if (!livingEntity.level().isClientSide) {
                    ItemStack prevStack = stackHandler.getPreviousStackInSlot(i);

                    if (!ItemStack.matches(stack, prevStack)) {
                      Optional<ICurio> prevCurio = CuriosApi.getCurio(prevStack);
                      syncCurios(
                          livingEntity,
                          stack,
                          currentCurio,
                          prevCurio,
                          identifier,
                          i,
                          false,
                          renderStates.size() > i && renderStates.get(i),
                          HandlerType.EQUIPMENT);

                      if (functional) {
                        CuriosEventBus.post(
                            new CurioChangeEvent(livingEntity, identifier, i, prevStack, stack));
                        ResourceLocation id = CuriosApi.getSlotId(slotContext);
                        AttributeMap attributeMap = livingEntity.getAttributes();

                        if (!prevStack.isEmpty()) {
                          Multimap<Holder<Attribute>, AttributeModifier> map =
                              CuriosApi.getAttributeModifiers(slotContext, id, prevStack);
                          Multimap<String, AttributeModifier> slots = HashMultimap.create();
                          Set<Holder<Attribute>> toRemove = new HashSet<>();

                          for (Holder<Attribute> attribute : map.keySet()) {

                            if (attribute.value() instanceof SlotAttribute wrapper) {
                              slots.putAll(wrapper.getIdentifier(), map.get(attribute));
                              toRemove.add(attribute);
                            }
                          }

                          for (Holder<Attribute> attribute : toRemove) {
                            map.removeAll(attribute);
                          }
                          map.forEach(
                              (key, value) -> {
                                AttributeInstance attInst = attributeMap.getInstance(key);

                                if (attInst != null) {
                                  attInst.removeModifier(value);
                                }
                              });
                          handler.removeSlotModifiers(slots);
                          prevCurio.ifPresent(curio -> curio.onUnequip(slotContext, stack));
                        }

                        if (!stack.isEmpty()) {
                          Multimap<Holder<Attribute>, AttributeModifier> map =
                              CuriosApi.getAttributeModifiers(slotContext, id, stack);
                          Multimap<String, AttributeModifier> slots = HashMultimap.create();
                          Set<Holder<Attribute>> toRemove = new HashSet<>();

                          for (Holder<Attribute> attribute : map.keySet()) {

                            if (attribute.value() instanceof SlotAttribute wrapper) {
                              slots.putAll(wrapper.getIdentifier(), map.get(attribute));
                              toRemove.add(attribute);
                            }
                          }

                          for (Holder<Attribute> attribute : toRemove) {
                            map.removeAll(attribute);
                          }
                          map.forEach(
                              (key, value) -> {
                                AttributeInstance attInst = attributeMap.getInstance(key);

                                if (attInst != null) {
                                  attInst.addOrUpdateTransientModifier(value);
                                }
                              });
                          handler.addTransientSlotModifiers(slots);
                          currentCurio.ifPresent(curio -> curio.onEquip(slotContext, prevStack));

                          if (livingEntity instanceof ServerPlayer serverPlayer) {
                            CuriosRegistry.EQUIP_TRIGGER
                                .get()
                                .trigger(slotContext, serverPlayer, stack);
                          }
                        }
                      }
                      stackHandler.setPreviousStackInSlot(i, stack.copy());
                    }
                    ItemStack cosmeticStack = cosmeticStackHandler.getStackInSlot(i);
                    ItemStack prevCosmeticStack = cosmeticStackHandler.getPreviousStackInSlot(i);

                    if (!ItemStack.matches(cosmeticStack, prevCosmeticStack)) {
                      syncCurios(
                          livingEntity,
                          cosmeticStack,
                          CuriosApi.getCurio(cosmeticStack),
                          CuriosApi.getCurio(prevCosmeticStack),
                          identifier,
                          i,
                          true,
                          true,
                          HandlerType.COSMETIC);
                      cosmeticStackHandler.setPreviousStackInSlot(i, cosmeticStack.copy());
                    }
                  }
                }
              }

              if (!livingEntity.level().isClientSide()) {
                Set<ICurioStacksHandler> updates = handler.getUpdatingInventories();

                if (!updates.isEmpty()) {
                  NetworkHandler.sendToTrackingAndSelf(livingEntity,
                      new SPacketSyncModifiers(livingEntity.getId(), updates));
                  updates.clear();
                }
              }
            });
  }

  /** Called from {@code MixinLivingEntityEquipment} after an equipment slot changed. */
  public static void livingEquipmentChange(LivingEntity entity, EquipmentSlot slot, ItemStack from,
                                           ItemStack to) {
    CuriosApi.getCuriosInventory(entity)
        .ifPresent(
            inv -> {

              if (!from.isEmpty()) {
                Multimap<String, AttributeModifier> slots = HashMultimap.create();
                from.forEachModifier(
                    slot,
                    (att, modifier) -> {

                      if (att.value() instanceof SlotAttribute wrapper) {
                        slots.putAll(wrapper.getIdentifier(), Collections.singleton(modifier));
                      }
                    });
                inv.removeSlotModifiers(slots);
              }

              if (!to.isEmpty()) {
                Multimap<String, AttributeModifier> slots = HashMultimap.create();
                to.forEachModifier(
                    slot,
                    (att, modifier) -> {

                      if (att.value() instanceof SlotAttribute wrapper) {
                        slots.putAll(wrapper.getIdentifier(), Collections.singleton(modifier));
                      }
                    });
                inv.addTransientSlotModifiers(slots);
              }
            });
  }

  private static void syncCurios(
      LivingEntity livingEntity,
      ItemStack stack,
      Optional<ICurio> currentCurio,
      Optional<ICurio> prevCurio,
      String identifier,
      int index,
      boolean cosmetic,
      boolean visible,
      HandlerType type) {
    SlotContext slotContext = new SlotContext(identifier, livingEntity, index, cosmetic, visible);
    boolean syncable =
        currentCurio.map(curio -> curio.canSync(slotContext)).orElse(false)
            || prevCurio.map(curio -> curio.canSync(slotContext)).orElse(false);
    CompoundTag syncTag =
        syncable
            ? currentCurio.map(curio -> curio.writeSyncData(slotContext)).orElse(new CompoundTag())
            : new CompoundTag();
    NetworkHandler.sendToTrackingAndSelf(
        livingEntity,
        new SPacketSyncStack(
            livingEntity.getId(), identifier, index, stack, type.ordinal(), syncTag));
  }
}
