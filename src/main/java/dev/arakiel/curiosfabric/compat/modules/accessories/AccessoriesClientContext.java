package dev.arakiel.curiosfabric.compat.modules.accessories;

import net.minecraft.client.Minecraft;
import net.minecraft.world.level.Level;

/** Client-only level lookup kept out of the dedicated-server class path. */
final class AccessoriesClientContext {

  private AccessoriesClientContext() {
  }

  static Level level() {
    return Minecraft.getInstance().level;
  }
}
