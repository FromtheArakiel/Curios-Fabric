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

import com.google.gson.JsonObject;
import java.util.Locale;
import dev.arakiel.curios.CuriosConstants;
import dev.arakiel.curios.common.util.ConfigValue;
import dev.arakiel.curios.common.util.JsonConfigHelper;

/** Client only configuration, stored in {@code curios-client.json}. */
public class CuriosClientConfig {

  public static final Client CLIENT = new Client();

  private CuriosClientConfig() {
  }

  /** Reads the client configuration file, creating it with defaults when needed. */
  public static void load() {
    CLIENT.load();
  }

  public static class Client {

    public final ConfigValue<Boolean> renderCurios = new ConfigValue<>(true);
    public final ConfigValue<Boolean> enableButton = new ConfigValue<>(true);
    public final ConfigValue<Integer> buttonXOffset = new ConfigValue<>(0);
    public final ConfigValue<Integer> buttonYOffset = new ConfigValue<>(0);
    public final ConfigValue<Integer> creativeButtonXOffset = new ConfigValue<>(0);
    public final ConfigValue<Integer> creativeButtonYOffset = new ConfigValue<>(0);
    public final ConfigValue<ButtonCorner> buttonCorner = new ConfigValue<>(ButtonCorner.TOP_LEFT);

    private void load() {
      JsonObject defaults = new JsonObject();
      defaults.addProperty("renderCurios", true);
      defaults.addProperty("enableButton", true);
      defaults.addProperty("buttonXOffset", 0);
      defaults.addProperty("buttonYOffset", 0);
      defaults.addProperty("creativeButtonXOffset", 0);
      defaults.addProperty("creativeButtonYOffset", 0);
      defaults.addProperty("buttonCorner", ButtonCorner.TOP_LEFT.name());
      JsonObject object = JsonConfigHelper.readOrCreate("curios-client.json", defaults);
      this.renderCurios.set(JsonConfigHelper.getBoolean(object, "renderCurios", true));
      this.enableButton.set(JsonConfigHelper.getBoolean(object, "enableButton", true));
      this.buttonXOffset.set(clamp(JsonConfigHelper.getInt(object, "buttonXOffset", 0)));
      this.buttonYOffset.set(clamp(JsonConfigHelper.getInt(object, "buttonYOffset", 0)));
      this.creativeButtonXOffset.set(
          clamp(JsonConfigHelper.getInt(object, "creativeButtonXOffset", 0)));
      this.creativeButtonYOffset.set(
          clamp(JsonConfigHelper.getInt(object, "creativeButtonYOffset", 0)));
      String corner =
          JsonConfigHelper.getString(object, "buttonCorner", ButtonCorner.TOP_LEFT.name());

      try {
        this.buttonCorner.set(ButtonCorner.valueOf(corner.toUpperCase(Locale.ROOT)));
      } catch (IllegalArgumentException exception) {
        CuriosConstants.LOG.warn("Unknown buttonCorner value '{}', using TOP_LEFT", corner);
        this.buttonCorner.set(ButtonCorner.TOP_LEFT);
      }
    }

    private static int clamp(int value) {
      return Math.max(-100, Math.min(100, value));
    }

    public enum ButtonCorner {
      TOP_LEFT(26, -75, 73, -62), TOP_RIGHT(61, -75, 95, -62), BOTTOM_LEFT(26, -20, 73,
          -29), BOTTOM_RIGHT(61, -20, 95, -29);

      final int xoffset;
      final int yoffset;
      final int creativeXoffset;
      final int creativeYoffset;

      ButtonCorner(int x, int y, int creativeX, int creativeY) {
        xoffset = x;
        yoffset = y;
        creativeXoffset = creativeX;
        creativeYoffset = creativeY;
      }

      public int getXoffset() {
        return xoffset;
      }

      public int getYoffset() {
        return yoffset;
      }

      public int getCreativeXoffset() {
        return creativeXoffset;
      }

      public int getCreativeYoffset() {
        return creativeYoffset;
      }
    }
  }
}
