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

package dev.arakiel.curios.common.network.client;

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
 * Handles the server to client payloads.
 *
 * <p>Fabric invokes these handlers on the client thread, so the {@code enqueueWork} plumbing of the
 * NeoForge edition is unnecessary and has been removed.</p>
 */
public class CuriosClientPayloadHandler {

  private static final CuriosClientPayloadHandler INSTANCE = new CuriosClientPayloadHandler();

  public static CuriosClientPayloadHandler getInstance() {
    return INSTANCE;
  }

  public void handle(final SPacketSetIcons data) {
    CuriosClientPackets.handle(data);
  }

  public void handle(final SPacketQuickMove data) {
    CuriosClientPackets.handle(data);
  }

  public void handle(final SPacketPage data) {
    CuriosClientPackets.handle(data);
  }

  public void handle(final SPacketBreak data) {
    CuriosClientPackets.handle(data);
  }

  public void handle(final SPacketSyncRender data) {
    CuriosClientPackets.handle(data);
  }

  public void handle(final SPacketSyncModifiers data) {
    CuriosClientPackets.handle(data);
  }

  public void handle(final SPacketSyncData data) {
    CuriosClientPackets.handle(data);
  }

  public void handle(final SPacketSyncCurios data) {
    CuriosClientPackets.handle(data);
  }

  public void handle(final SPacketGrabbedItem data) {
    CuriosClientPackets.handle(data);
  }

  public void handle(final SPacketSyncStack data) {
    CuriosClientPackets.handle(data);
  }

  public void handle(final SPacketSyncActiveState data) {
    CuriosClientPackets.handle(data);
  }
}
