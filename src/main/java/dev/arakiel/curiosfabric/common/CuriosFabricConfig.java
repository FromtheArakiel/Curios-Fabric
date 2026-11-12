/*
 * Copyright (c) 2018-2024 C4
 *
 * This file is part of Curios, a mod made for Minecraft.
 */

package dev.arakiel.curiosfabric.common;

import dev.arakiel.gemini.config.Config;
import dev.arakiel.gemini.config.ConfigEntry;
import dev.arakiel.gemini.config.ConfigHandle;
import dev.arakiel.gemini.config.GeminiConfig;
import dev.arakiel.gemini.config.Range;
import java.util.List;

/** Gemini-backed common and server configuration. */
public final class CuriosFabricConfig {

  public static ConfigHandle<Common> COMMON;
  public static ConfigHandle<Server> SERVER;

  private CuriosFabricConfig() {
  }

  public static void load() {
    GeminiConfig.init();
    if (COMMON == null) {
      COMMON = GeminiConfig.register("curiosfabric", "common", true, Common.class);
    }
    if (SERVER == null) {
      SERVER = GeminiConfig.register("curiosfabric", "server", true, Server.class);
    }
  }

  @Config(namespace = "curiosfabric", value = "common")
  public static final class Common {

    @ConfigEntry(
        value = "slots",
        comment = "Slot declarations using id=...;size=...;operation=...;order=... entries.")
    public List<String> slots = List.of();
  }

  @Config(namespace = "curiosfabric", value = "server")
  public static final class Server {

    @ConfigEntry(
        value = "keep_curios",
        comment = "ON keeps curios on death, OFF drops them, DEFAULT follows keepInventory.")
    public KeepCurios keepCurios = KeepCurios.DEFAULT;

    @ConfigEntry(value = "minimum_columns")
    @Range(min = 1, max = 8)
    public int minimumColumns = 1;

    @ConfigEntry(value = "max_slots_per_page")
    @Range(min = 1, max = 48)
    public int maxSlotsPerPage = 48;
  }

  public enum KeepCurios {
    ON,
    DEFAULT,
    OFF
  }
}
