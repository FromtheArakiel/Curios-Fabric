/*
 * Copyright (c) 2018-2024 C4
 *
 * This file is part of Curios, a mod made for Minecraft.
 *
 * Curios is free software: you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as published
 * by the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Curios is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Curios.  If not, see <https://www.gnu.org/licenses/>.
 *
 */

package dev.arakiel.curios.common.network.client;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import dev.arakiel.curios.api.CuriosApi;
import dev.arakiel.curios.api.SlotContext;
import dev.arakiel.curios.api.event.SlotModifiersUpdatedEvent;
import dev.arakiel.curios.api.event.CuriosEventBus;
import dev.arakiel.curios.api.type.ICuriosMenu;
import dev.arakiel.curios.api.type.capability.ICurio;
import dev.arakiel.curios.api.type.inventory.ICurioStacksHandler;
import dev.arakiel.curios.client.gui.CuriosScreen;
import dev.arakiel.curios.common.data.CuriosEntityManager;
import dev.arakiel.curios.common.data.CuriosSlotManager;
import dev.arakiel.curios.common.inventory.CurioStacksHandler;
import dev.arakiel.curios.common.inventory.container.CuriosContainer;
import dev.arakiel.curios.common.network.server.SPacketBreak;
import dev.arakiel.curios.common.network.server.SPacketGrabbedItem;
import dev.arakiel.curios.common.network.server.SPacketPage;
import dev.arakiel.curios.common.network.server.SPacketQuickMove;
import dev.arakiel.curios.common.network.server.SPacketSetIcons;
import dev.arakiel.curios.common.network.server.sync.SPacketSyncActiveState;
import dev.arakiel.curios.common.network.server.sync.SPacketSyncCurios;
import dev.arakiel.curios.common.network.server.sync.SPacketSyncData;
import dev.arakiel.curios.common.network.server.sync.SPacketSyncModifiers;
import dev.arakiel.curios.common.network.server.sync.SPacketSyncRender;
import dev.arakiel.curios.common.network.server.sync.SPacketSyncStack;
import dev.arakiel.curios.server.command.CurioArgumentType;
import dev.arakiel.curios.compat.Diag;

public class CuriosClientPackets {

  /** Registers the client side receivers; only called from the client entry point. */
  public static void registerReceivers() {
    CuriosClientPayloadHandler handler = CuriosClientPayloadHandler.getInstance();
    ClientPlayNetworking.registerGlobalReceiver(SPacketSetIcons.TYPE,
        (payload, context) -> handler.handle(payload));
    ClientPlayNetworking.registerGlobalReceiver(SPacketQuickMove.TYPE,
        (payload, context) -> handler.handle(payload));
    ClientPlayNetworking.registerGlobalReceiver(SPacketPage.TYPE,
        (payload, context) -> handler.handle(payload));
    ClientPlayNetworking.registerGlobalReceiver(SPacketBreak.TYPE,
        (payload, context) -> handler.handle(payload));
    ClientPlayNetworking.registerGlobalReceiver(SPacketSyncRender.TYPE,
        (payload, context) -> handler.handle(payload));
    ClientPlayNetworking.registerGlobalReceiver(SPacketSyncModifiers.TYPE,
        (payload, context) -> handler.handle(payload));
    ClientPlayNetworking.registerGlobalReceiver(SPacketSyncData.TYPE,
        (payload, context) -> handler.handle(payload));
    ClientPlayNetworking.registerGlobalReceiver(SPacketSyncCurios.TYPE,
        (payload, context) -> handler.handle(payload));
    ClientPlayNetworking.registerGlobalReceiver(SPacketGrabbedItem.TYPE,
        (payload, context) -> handler.handle(payload));
    ClientPlayNetworking.registerGlobalReceiver(SPacketSyncStack.TYPE,
        (payload, context) -> handler.handle(payload));
    ClientPlayNetworking.registerGlobalReceiver(SPacketSyncActiveState.TYPE,
        (payload, context) -> handler.handle(payload));
  }

  public static void handle(final SPacketSetIcons data) {
    ClientLevel world = Minecraft.getInstance().level;
    Set<String> slotIds = new HashSet<>();

    if (world != null) {
      CuriosApi.getIconHelper().clearIcons();
      Map<String, ResourceLocation> icons = new HashMap<>();

      for (Map.Entry<String, ResourceLocation> entry : data.map.entrySet()) {
        CuriosApi.getIconHelper().addIcon(entry.getKey(), entry.getValue());
        icons.put(entry.getKey(), entry.getValue());
        slotIds.add(entry.getKey());
      }
      CuriosSlotManager.CLIENT.setIcons(icons);
    }
    CurioArgumentType.slotIds = slotIds;
  }

  public static void handle(final SPacketQuickMove data) {
    Minecraft mc = Minecraft.getInstance();
    LocalPlayer clientPlayer = mc.player;

    if (clientPlayer != null && clientPlayer.containerMenu instanceof CuriosContainer container) {
      container.quickMoveStack(clientPlayer, data.moveIndex());
    }
  }

  public static void handle(final SPacketPage data) {
    Minecraft mc = Minecraft.getInstance();
    LocalPlayer clientPlayer = mc.player;
    Screen screen = mc.screen;

    if (clientPlayer != null) {
      AbstractContainerMenu container = clientPlayer.containerMenu;

      if (container instanceof CuriosContainer && container.containerId == data.windowId()) {
        ((CuriosContainer) container).setPage(data.page());
      }
    }

    if (screen instanceof CuriosScreen) {
      ((CuriosScreen) screen).updateRenderButtons();
    }
  }

  public static void handle(final SPacketBreak data) {
    ClientLevel world = Minecraft.getInstance().level;

    if (world != null) {
      Entity entity = world.getEntity(data.entityId());

      if (entity instanceof LivingEntity livingEntity) {
        CuriosApi.getCuriosInventory(livingEntity)
            .flatMap(handler -> handler.getStacksHandler(data.curioId()))
            .ifPresent(
                stacks -> {
                  ItemStack stack = stacks.getStacks().getStackInSlot(data.slotId());
                  Optional<ICurio> possibleCurio = CuriosApi.getCurio(stack);
                  NonNullList<Boolean> renderStates = stacks.getRenders();
                  possibleCurio.ifPresent(
                      curio ->
                          curio.curioBreak(
                              new SlotContext(
                                  data.curioId(),
                                  livingEntity,
                                  data.slotId(),
                                  false,
                                  renderStates.size() > data.slotId()
                                      && renderStates.get(data.slotId()))));

                  if (possibleCurio.isEmpty()) {
                    ICurio.playBreakAnimation(stack, livingEntity);
                  }
                });
      }
    }
  }

  public static void handle(final SPacketSyncRender data) {
    ClientLevel world = Minecraft.getInstance().level;

    if (world != null) {
      Entity entity = world.getEntity(data.entityId());

      if (entity instanceof LivingEntity) {
        CuriosApi.getCuriosInventory((LivingEntity) entity)
            .flatMap(handler -> handler.getStacksHandler(data.curioId()))
            .ifPresent(
                stacksHandler -> {
                  int index = data.slotId();
                  NonNullList<Boolean> renderStatuses = stacksHandler.getRenders();

                  if (renderStatuses.size() > index) {
                    renderStatuses.set(index, data.value());
                  }
                });
      }
    }
  }

  public static void handle(final SPacketSyncModifiers data) {
    Minecraft mc = Minecraft.getInstance();
    ClientLevel world = mc.level;

    if (world != null) {
      Entity entity = world.getEntity(data.entityId);

      if (entity instanceof LivingEntity livingEntity) {
        CuriosApi.getCuriosInventory(livingEntity)
            .ifPresent(
                handler -> {
                  Map<String, ICurioStacksHandler> curios = handler.getCurios();

                  for (Map.Entry<String, CompoundTag> entry : data.updates.entrySet()) {
                    String id = entry.getKey();
                    ICurioStacksHandler stacksHandler = curios.get(id);

                    if (stacksHandler != null) {
                      stacksHandler.applySyncTag(entry.getValue());
                    }
                  }

                  if (!data.updates.isEmpty()) {
                    CuriosEventBus.post(
                        new SlotModifiersUpdatedEvent(livingEntity, data.updates.keySet()));
                  }

                  if (entity instanceof LocalPlayer localPlayer) {

                    if (localPlayer.containerMenu instanceof ICuriosMenu curiosMenu) {
                      curiosMenu.resetSlots();
                    }

                    if (mc.screen instanceof CuriosScreen screen) {
                      screen.updateRenderButtons();
                    }
                  }
                });
      }
    }
  }

  public static void handle(final SPacketSyncData data) {
    CuriosSlotManager.applySyncPacket(data.slotData);
    CuriosEntityManager.applySyncPacket(data.entityData);
  }

  public static void handle(final SPacketSyncCurios data) {
    ClientLevel world = Minecraft.getInstance().level;

    if (world != null) {
      Entity entity = world.getEntity(data.entityId);

      if (!(entity instanceof LivingEntity)) {
        Diag.once("sync-miss:" + data.entityId,
            "SPacketSyncCurios for entity {} was dropped: the entity is not on the client yet "
                + "(entries={})", data.entityId, data.map.size());
      }
      if (entity instanceof LivingEntity) {
        Diag.once("sync:" + data.entityId,
            "SPacketSyncCurios applied for entity {} (entries={})", data.entityId, data.map.size());
        CuriosApi.getCuriosInventory((LivingEntity) entity)
            .ifPresent(
                handler -> {
                  Map<String, ICurioStacksHandler> stacks = new LinkedHashMap<>();

                  for (Map.Entry<String, CompoundTag> entry : data.map.entrySet()) {
                    ICurioStacksHandler stacksHandler =
                        new CurioStacksHandler(handler, entry.getKey());
                    stacksHandler.applySyncTag(entry.getValue());
                    stacks.put(entry.getKey(), stacksHandler);
                  }
                  handler.setCurios(stacks);

                  if (entity instanceof LocalPlayer localPlayer
                      && localPlayer.containerMenu instanceof ICuriosMenu curiosContainer) {
                    curiosContainer.resetSlots();
                  }
                });
      }
    }
  }

  public static void handle(final SPacketGrabbedItem data) {
    LocalPlayer clientPlayer = Minecraft.getInstance().player;

    if (clientPlayer != null) {
      clientPlayer.containerMenu.setCarried(data.stack().copy());
    }
  }

  public static void handle(final SPacketSyncStack data) {
    ClientLevel world = Minecraft.getInstance().level;

    if (world != null) {
      Entity entity = world.getEntity(data.entityId());

      if (entity instanceof LivingEntity livingEntity) {
        CuriosApi.getCuriosInventory(livingEntity)
            .flatMap(handler -> handler.getStacksHandler(data.curioId()))
            .ifPresent(
                stacksHandler -> {
                  ItemStack stack = data.stack().copy();
                  CompoundTag compoundNBT = data.compoundTag();
                  int slot = data.slotId();
                  boolean cosmetic =
                      SPacketSyncStack.HandlerType.fromValue(data.handlerType())
                          == SPacketSyncStack.HandlerType.COSMETIC;

                  if (!compoundNBT.isEmpty()) {
                    NonNullList<Boolean> renderStates = stacksHandler.getRenders();
                    CuriosApi.getCurio(stack)
                        .ifPresent(
                            curio ->
                                curio.readSyncData(
                                    new SlotContext(
                                        data.curioId(),
                                        livingEntity,
                                        slot,
                                        cosmetic,
                                        renderStates.size() > slot && renderStates.get(slot)),
                                    compoundNBT));
                  }

                  if (cosmetic) {
                    stacksHandler.getCosmeticStacks().setStackInSlot(slot, stack);
                  } else {
                    stacksHandler.getStacks().setStackInSlot(slot, stack);
                  }
                });
      }
    }
  }

  public static void handle(SPacketSyncActiveState data) {
    ClientLevel world = Minecraft.getInstance().level;

    if (world != null) {
      Entity entity = world.getEntity(data.entityId());

      if (entity instanceof LivingEntity) {
        CuriosApi.getCuriosInventory((LivingEntity) entity)
            .flatMap(handler -> handler.getStacksHandler(data.curioId()))
            .ifPresent(
                stacksHandler -> {
                  int index = data.slotId();
                  NonNullList<Boolean> functionStatuses = stacksHandler.getActiveStates();

                  if (functionStatuses.size() > index) {
                    functionStatuses.set(index, data.value());
                  }
                });
      }
    }
  }
}
