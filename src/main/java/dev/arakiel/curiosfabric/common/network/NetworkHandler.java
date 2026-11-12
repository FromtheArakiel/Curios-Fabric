/*
 * Copyright (c) 2018-2024 C4
 *
 * This file is part of Curios, a mod made for Minecraft.
 */

package dev.arakiel.curiosfabric.common.network;

import dev.arakiel.curiosfabric.common.network.client.CPacketDestroy;
import dev.arakiel.curiosfabric.common.network.client.CPacketOpenCurios;
import dev.arakiel.curiosfabric.common.network.client.CPacketOpenVanilla;
import dev.arakiel.curiosfabric.common.network.client.CPacketPage;
import dev.arakiel.curiosfabric.common.network.client.CPacketToggleCosmetics;
import dev.arakiel.curiosfabric.common.network.client.CPacketToggleRender;
import dev.arakiel.curiosfabric.common.network.client.CuriosFabricClientPayloadHandler;
import dev.arakiel.curiosfabric.common.network.server.CuriosFabricServerPayloadHandler;
import dev.arakiel.curiosfabric.common.network.server.SPacketBreak;
import dev.arakiel.curiosfabric.common.network.server.SPacketGrabbedItem;
import dev.arakiel.curiosfabric.common.network.server.SPacketPage;
import dev.arakiel.curiosfabric.common.network.server.SPacketQuickMove;
import dev.arakiel.curiosfabric.common.network.server.SPacketSetIcons;
import dev.arakiel.curiosfabric.common.network.server.sync.SPacketSyncActiveState;
import dev.arakiel.curiosfabric.common.network.server.sync.SPacketSyncCurios;
import dev.arakiel.curiosfabric.common.network.server.sync.SPacketSyncData;
import dev.arakiel.curiosfabric.common.network.server.sync.SPacketSyncModifiers;
import dev.arakiel.curiosfabric.common.network.server.sync.SPacketSyncRender;
import dev.arakiel.curiosfabric.common.network.server.sync.SPacketSyncStack;
import dev.arakiel.gemini.network.GeminiNetwork;
import dev.arakiel.gemini.network.NetworkChannel;
import java.util.List;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;

/** Gemini-backed networking for Curios Fabric. */
public final class NetworkHandler {

  private static NetworkChannel channel;

  private NetworkHandler() {
  }

  public static void register() {
    GeminiNetwork.init();
    channel = GeminiNetwork.channel("main").required();

    CuriosFabricServerPayloadHandler server = CuriosFabricServerPayloadHandler.getInstance();
    channel.registerServerbound(CPacketDestroy.class, CPacketDestroy.STREAM_CODEC,
        (payload, player) -> server.handleDestroyPacket(payload, player));
    channel.registerServerbound(CPacketOpenCurios.class, CPacketOpenCurios.STREAM_CODEC,
        (payload, player) -> server.handleOpenCurios(payload, player));
    channel.registerServerbound(CPacketOpenVanilla.class, CPacketOpenVanilla.STREAM_CODEC,
        (payload, player) -> server.handleOpenVanilla(payload, player));
    channel.registerServerbound(CPacketPage.class, CPacketPage.STREAM_CODEC,
        (payload, player) -> server.handlePage(payload, player));
    channel.registerServerbound(CPacketToggleRender.class, CPacketToggleRender.STREAM_CODEC,
        (payload, player) -> server.handlerToggleRender(payload, player));
    channel.registerServerbound(CPacketToggleCosmetics.class,
        CPacketToggleCosmetics.STREAM_CODEC,
        (payload, player) -> server.handlerToggleCosmetics(payload, player));

    CuriosFabricClientPayloadHandler client = CuriosFabricClientPayloadHandler.getInstance();
    channel.registerClientbound(SPacketSyncData.class, SPacketSyncData.STREAM_CODEC,
        client::handle);
    channel.registerClientbound(SPacketSyncCurios.class, SPacketSyncCurios.STREAM_CODEC,
        client::handle);
    channel.registerClientbound(SPacketSyncStack.class, SPacketSyncStack.STREAM_CODEC,
        client::handle);
    channel.registerClientbound(SPacketSyncModifiers.class, SPacketSyncModifiers.STREAM_CODEC,
        client::handle);
    channel.registerClientbound(SPacketSyncRender.class, SPacketSyncRender.STREAM_CODEC,
        client::handle);
    channel.registerClientbound(SPacketSyncActiveState.class,
        SPacketSyncActiveState.STREAM_CODEC, client::handle);
    channel.registerClientbound(SPacketBreak.class, SPacketBreak.STREAM_CODEC, client::handle);
    channel.registerClientbound(SPacketPage.class, SPacketPage.STREAM_CODEC, client::handle);
    channel.registerClientbound(SPacketQuickMove.class, SPacketQuickMove.STREAM_CODEC,
        client::handle);
    channel.registerClientbound(SPacketSetIcons.class, SPacketSetIcons.STREAM_CODEC,
        client::handle);
    channel.registerClientbound(SPacketGrabbedItem.class, SPacketGrabbedItem.STREAM_CODEC,
        client::handle);
  }

  public static NetworkChannel channel() {
    return channel;
  }

  public static <T> boolean sendToServer(T payload) {
    if (channel == null || !GeminiNetwork.canSendToServer(channel)) {
      return false;
    }
    channel.sendToServer(payload);
    return true;
  }

  public static boolean sendToPlayer(ServerPlayer player, Object payload) {
    if (channel == null || player.connection == null
        || !channel.canSendTo(player, payload.getClass())) {
      return false;
    }
    sendUnchecked(player, payload);
    return true;
  }

  public static void sendToTrackingAndSelf(Entity entity, Object payload) {
    if (channel != null) {
      sendTrackingUnchecked(entity, payload);
    }
  }

  public static boolean sendBundle(ServerPlayer player, List<?> payloads) {
    if (channel == null || payloads.isEmpty()
        || !channel.canSendTo(player, payloads.getFirst().getClass())) {
      return false;
    }
    channel.sendBundle(player, payloads);
    return true;
  }

  @SuppressWarnings("unchecked")
  private static <T> void sendUnchecked(ServerPlayer player, Object payload) {
    channel.sendTo(player, (T) payload);
  }

  @SuppressWarnings("unchecked")
  private static <T> void sendTrackingUnchecked(Entity entity, Object payload) {
    channel.sendToTrackingAndSelf(entity, (T) payload);
  }
}
