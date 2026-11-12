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

package dev.arakiel.curiosfabric.common.network.client;

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

/**
 * Handles the server to client payloads.
 *
 * <p>Fabric invokes these handlers on the client thread, so the {@code enqueueWork} plumbing of the
 * Neoforge edition is unnecessary and has been removed.</p>
 */
public class CuriosFabricClientPayloadHandler {

  private static final CuriosFabricClientPayloadHandler INSTANCE = new CuriosFabricClientPayloadHandler();

  public static CuriosFabricClientPayloadHandler getInstance() {
    return INSTANCE;
  }

  public void handle(final SPacketSetIcons data) {
    CuriosFabricClientPackets.handle(data);
  }

  public void handle(final SPacketQuickMove data) {
    CuriosFabricClientPackets.handle(data);
  }

  public void handle(final SPacketPage data) {
    CuriosFabricClientPackets.handle(data);
  }

  public void handle(final SPacketBreak data) {
    CuriosFabricClientPackets.handle(data);
  }

  public void handle(final SPacketSyncRender data) {
    CuriosFabricClientPackets.handle(data);
  }

  public void handle(final SPacketSyncModifiers data) {
    CuriosFabricClientPackets.handle(data);
  }

  public void handle(final SPacketSyncData data) {
    CuriosFabricClientPackets.handle(data);
  }

  public void handle(final SPacketSyncCurios data) {
    CuriosFabricClientPackets.handle(data);
  }

  public void handle(final SPacketGrabbedItem data) {
    CuriosFabricClientPackets.handle(data);
  }

  public void handle(final SPacketSyncStack data) {
    CuriosFabricClientPackets.handle(data);
  }

  public void handle(final SPacketSyncActiveState data) {
    CuriosFabricClientPackets.handle(data);
  }
}
