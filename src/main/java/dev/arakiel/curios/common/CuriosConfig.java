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

package dev.arakiel.curios.common;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import java.util.List;
import java.util.Locale;
import dev.arakiel.curios.CuriosConstants;
import dev.arakiel.curios.common.util.ConfigValue;
import dev.arakiel.curios.common.util.JsonConfigHelper;

/**
 * Server and common configuration.
 *
 * <p>NeoForge's config spec files are replaced by two plain JSON files in the loader's config
 * directory: {@code curios-common.json} and {@code curios-server.json}.</p>
 */
public class CuriosConfig {

  public static final Common COMMON = new Common();
  public static final Server SERVER = new Server();

  private CuriosConfig() {
  }

  /** Reads both configuration files, creating them with defaults when needed. */
  public static void load() {
    COMMON.load();
    SERVER.load();
  }

  public static class Common {

    /**
     * List of slots to create or modify, using the
     * {@code id=...;size=...;operation=...} syntax of the Curios documentation.
     */
    public final ConfigValue<List<String>> slots = new ConfigValue<>(List.of());

    private void load() {
      JsonObject defaults = new JsonObject();
      defaults.add("slots", new JsonArray());
      JsonObject object = JsonConfigHelper.readOrCreate("curios-common.json", defaults);
      this.slots.set(List.copyOf(JsonConfigHelper.getStringList(object, "slots")));
    }
  }

  public static class Server {

    public final ConfigValue<KeepCurios> keepCurios = new ConfigValue<>(KeepCurios.DEFAULT);
    public final ConfigValue<Integer> minimumColumns = new ConfigValue<>(1);
    public final ConfigValue<Integer> maxSlotsPerPage = new ConfigValue<>(48);

    private void load() {
      JsonObject defaults = new JsonObject();
      defaults.addProperty("keepCurios", KeepCurios.DEFAULT.name());
      defaults.addProperty("minimumColumns", 1);
      defaults.addProperty("maxSlotsPerPage", 48);
      JsonObject object = JsonConfigHelper.readOrCreate("curios-server.json", defaults);
      String keep = JsonConfigHelper.getString(object, "keepCurios", KeepCurios.DEFAULT.name());
      KeepCurios keepCurios;

      try {
        keepCurios = KeepCurios.valueOf(keep.toUpperCase(Locale.ROOT));
      } catch (IllegalArgumentException exception) {
        CuriosConstants.LOG.warn("Unknown keepCurios value '{}', using DEFAULT", keep);
        keepCurios = KeepCurios.DEFAULT;
      }
      this.keepCurios.set(keepCurios);
      this.minimumColumns.set(
          Math.max(1, Math.min(8, JsonConfigHelper.getInt(object, "minimumColumns", 1))));
      this.maxSlotsPerPage.set(
          Math.max(1, Math.min(48, JsonConfigHelper.getInt(object, "maxSlotsPerPage", 48))));
    }
  }

  public enum KeepCurios {
    ON,
    DEFAULT,
    OFF
  }
}
