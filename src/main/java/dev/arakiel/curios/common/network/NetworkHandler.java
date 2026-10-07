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
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR PARTICULAR PURPOSE.  See the GNU Lesser General Public
 * License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Curios.  If not, see <https://www.gnu.org/licenses/>.
 *
 */

package dev.arakiel.curios.common.network;

import java.util.HashSet;
import java.util.Set;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.PlayerLookup;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import dev.arakiel.curios.common.network.client.CPacketDestroy;
import dev.arakiel.curios.common.network.client.CPacketOpenCurios;
import dev.arakiel.curios.common.network.client.CPacketOpenVanilla;
import dev.arakiel.curios.common.network.client.CPacketPage;
import dev.arakiel.curios.common.network.client.CPacketToggleCosmetics;
import dev.arakiel.curios.common.network.client.CPacketToggleRender;
import dev.arakiel.curios.common.network.server.CuriosServerPayloadHandler;
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

/**
 * Fabric networking glue.
 *
 * <p>The payload records are plain vanilla {@link CustomPacketPayload}s, so nothing about them had
 * to change. Only the registration (NeoForge's {@code PayloadRegistrar} versus Fabric's
 * {@code PayloadTypeRegistry}) and the dispatch differ.</p>
 */
public class NetworkHandler {

  private NetworkHandler() {
  }

  /** Registers all payload types and the server side receivers. Runs on both sides. */
  public static void register() {
    // Client -> Server
    PayloadTypeRegistry.playC2S().register(CPacketDestroy.TYPE, CPacketDestroy.STREAM_CODEC);
    PayloadTypeRegistry.playC2S().register(CPacketOpenCurios.TYPE, CPacketOpenCurios.STREAM_CODEC);
    PayloadTypeRegistry.playC2S()
        .register(CPacketOpenVanilla.TYPE, CPacketOpenVanilla.STREAM_CODEC);
    PayloadTypeRegistry.playC2S().register(CPacketPage.TYPE, CPacketPage.STREAM_CODEC);
    PayloadTypeRegistry.playC2S()
        .register(CPacketToggleRender.TYPE, CPacketToggleRender.STREAM_CODEC);
    PayloadTypeRegistry.playC2S()
        .register(CPacketToggleCosmetics.TYPE, CPacketToggleCosmetics.STREAM_CODEC);

    // Server -> Client
    PayloadTypeRegistry.playS2C().register(SPacketSyncStack.TYPE, SPacketSyncStack.STREAM_CODEC);
    PayloadTypeRegistry.playS2C()
        .register(SPacketGrabbedItem.TYPE, SPacketGrabbedItem.STREAM_CODEC);
    PayloadTypeRegistry.playS2C().register(SPacketSyncCurios.TYPE, SPacketSyncCurios.STREAM_CODEC);
    PayloadTypeRegistry.playS2C().register(SPacketSyncData.TYPE, SPacketSyncData.STREAM_CODEC);
    PayloadTypeRegistry.playS2C()
        .register(SPacketSyncModifiers.TYPE, SPacketSyncModifiers.STREAM_CODEC);
    PayloadTypeRegistry.playS2C().register(SPacketSyncRender.TYPE, SPacketSyncRender.STREAM_CODEC);
    PayloadTypeRegistry.playS2C()
        .register(SPacketSyncActiveState.TYPE, SPacketSyncActiveState.STREAM_CODEC);
    PayloadTypeRegistry.playS2C().register(SPacketBreak.TYPE, SPacketBreak.STREAM_CODEC);
    PayloadTypeRegistry.playS2C().register(SPacketPage.TYPE, SPacketPage.STREAM_CODEC);
    PayloadTypeRegistry.playS2C().register(SPacketSetIcons.TYPE, SPacketSetIcons.STREAM_CODEC);
    PayloadTypeRegistry.playS2C().register(SPacketQuickMove.TYPE, SPacketQuickMove.STREAM_CODEC);

    CuriosServerPayloadHandler handler = CuriosServerPayloadHandler.getInstance();
    ServerPlayNetworking.registerGlobalReceiver(CPacketDestroy.TYPE,
        (payload, context) -> handler.handleDestroyPacket(payload, context.player()));
    ServerPlayNetworking.registerGlobalReceiver(CPacketOpenCurios.TYPE,
        (payload, context) -> handler.handleOpenCurios(payload, context.player()));
    ServerPlayNetworking.registerGlobalReceiver(CPacketOpenVanilla.TYPE,
        (payload, context) -> handler.handleOpenVanilla(payload, context.player()));
    ServerPlayNetworking.registerGlobalReceiver(CPacketPage.TYPE,
        (payload, context) -> handler.handlePage(payload, context.player()));
    ServerPlayNetworking.registerGlobalReceiver(CPacketToggleRender.TYPE,
        (payload, context) -> handler.handlerToggleRender(payload, context.player()));
    ServerPlayNetworking.registerGlobalReceiver(CPacketToggleCosmetics.TYPE,
        (payload, context) -> handler.handlerToggleCosmetics(payload, context.player()));
  }

  /** Sends a payload to a single player. */
  public static void sendToPlayer(ServerPlayer player, CustomPacketPayload payload) {

    if (canReceive(player, payload)) {
      ServerPlayNetworking.send(player, payload);
    }
  }

  /**
   * Sends a payload to every player that tracks the entity, including the entity itself when it is
   * a player. This mirrors NeoForge's {@code sendToPlayersTrackingEntityAndSelf}.
   */
  public static void sendToTrackingAndSelf(Entity entity, CustomPacketPayload payload) {

    if (!(entity.level() instanceof ServerLevel)) {
      return;
    }

    // PlayerLookup.tracking is explicitly documented as not guaranteeing the entity's own player,
    // and in practice the server never tracks a player with their own entity. Without the extra
    // entry the player's own client never received the render toggles, the equipped stacks or the
    // break animations that this project broadcasts, so the client had to be re-synced by relogging.
    Set<ServerPlayer> targets = new HashSet<>(PlayerLookup.tracking(entity));

    if (entity instanceof ServerPlayer self) {
      targets.add(self);
    }

    for (ServerPlayer player : targets) {

      if (canReceive(player, payload)) {
        ServerPlayNetworking.send(player, payload);
      }
    }
  }

  /**
   * {@code true} when the player's client is able to receive this payload.
   *
   * <p>Sending a payload the client has not declared throws. Because some of the syncs are triggered
   * from the player join sequence - where the client only finishes the channel handshake once it
   * receives the login packet - an unguarded send aborts the join and shows up as a connection
   * failure. Checking first turns such a send into a harmless no-op; the regular sync sends it
   * again.</p>
   */
  private static boolean canReceive(ServerPlayer player, CustomPacketPayload payload) {

    try {
      return player.connection != null && ServerPlayNetworking.canSend(player, payload.type());
    } catch (Throwable throwable) {
      return false;
    }
  }
}
