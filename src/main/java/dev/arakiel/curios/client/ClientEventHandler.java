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

package dev.arakiel.curios.client;

import com.google.common.collect.Multimap;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.item.v1.ItemTooltipCallback;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import dev.arakiel.curios.CuriosConstants;
import dev.arakiel.curios.api.CuriosApi;
import dev.arakiel.curios.api.SlotContext;
import dev.arakiel.curios.common.network.client.CPacketOpenCurios;

/**
 * Client side event handler.
 *
 * <p>NeoForge's tick, tooltip and attribute tooltip events are replaced by Fabric's
 * {@code ClientTickEvents} and {@code ItemTooltipCallback}. Because Fabric only offers one tooltip
 * callback, the slot list and the attribute list are both built there.</p>
 */
public class ClientEventHandler {

  private ClientEventHandler() {
  }

  /** Registers the client side callbacks; called from the client entry point. */
  public static void register() {
    ClientTickEvents.END_CLIENT_TICK.register(client -> {

      if (KeyRegistry.openCurios.consumeClick() && client.isWindowActive()) {
        ClientPlayNetworking.send(new CPacketOpenCurios(ItemStack.EMPTY));
      }
    });

    ItemTooltipCallback.EVENT.register(
        (stack, context, flag, lines) -> addTooltips(stack, flag, lines));
  }

  private static void addTooltips(ItemStack stack, TooltipFlag flag, List<Component> lines) {

    if (stack.isEmpty()) {
      return;
    }
    Player player = Minecraft.getInstance().player;
    List<String> slots = getItemStackSlots(stack, player);

    if (slots.isEmpty()) {
      return;
    }
    List<Component> curioLines = new ArrayList<>();
    MutableComponent slotsTooltip =
        Component.translatable("curios.tooltip.slot").append(" ").withStyle(ChatFormatting.GOLD);

    for (int j = 0; j < slots.size(); j++) {
      String id = slots.get(j);
      String key = "curios.identifier." + id;
      MutableComponent type =
          Component.translatableWithFallback(
              key, Character.toUpperCase(id.charAt(0)) + id.substring(1).toLowerCase());

      if (j < slots.size() - 1) {
        type = type.append(", ");
      }
      type = type.withStyle(ChatFormatting.YELLOW);
      slotsTooltip.append(type);
    }
    curioLines.add(slotsTooltip);
    curioLines.addAll(getAttributeTooltips(stack, player, slots));
    lines.addAll(1, curioLines);
  }

  private static List<Component> getAttributeTooltips(ItemStack stack, Player player,
                                                      List<String> slots) {
    List<Component> attributesTooltip = new ArrayList<>();

    for (String identifier : slots) {
      SlotContext slotContext = new SlotContext(identifier, player, 0, false, true);
      Multimap<Holder<Attribute>, AttributeModifier> attributes =
          CuriosApi.getAttributeModifiers(slotContext, CuriosApi.getSlotId(slotContext), stack);

      if (attributes.isEmpty()) {
        continue;
      }
      attributesTooltip.add(Component.empty());
      attributesTooltip.add(
          Component.translatable("curios.modifiers." + identifier).withStyle(ChatFormatting.GOLD));

      for (var entry : attributes.entries()) {
        attributesTooltip.add(CuriosTooltips.attributeModifier(entry.getKey(), entry.getValue()));
      }
    }
    return attributesTooltip;
  }

  private static List<String> getItemStackSlots(ItemStack stack, Player player) {
    Set<String> slots =
        Set.copyOf(
            (player != null
                    ? CuriosApi.getItemStackSlots(stack, player)
                    : CuriosApi.getItemStackSlots(stack, CuriosConstants.IS_CLIENT))
                .keySet());

    if (slots.contains("curio")) {
      slots = Set.of("curio");
    }
    return new ArrayList<>(slots);
  }
}
