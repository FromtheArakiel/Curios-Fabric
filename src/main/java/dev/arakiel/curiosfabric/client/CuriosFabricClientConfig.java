/*
 * Copyright (c) 2018-2024 C4
 *
 * This file is part of Curios, a mod made for Minecraft.
 */

package dev.arakiel.curiosfabric.client;

import dev.arakiel.gemini.config.Config;
import dev.arakiel.gemini.config.ConfigEntry;
import dev.arakiel.gemini.config.ConfigHandle;
import dev.arakiel.gemini.config.GeminiConfig;
import dev.arakiel.gemini.config.Range;

/** Gemini-backed client configuration. */
public final class CuriosFabricClientConfig {

  public static ConfigHandle<Client> CLIENT;

  private CuriosFabricClientConfig() {
  }

  public static void load() {
    GeminiConfig.init();
    if (CLIENT == null) {
      CLIENT = GeminiConfig.register("curiosfabric", "client", true, Client.class);
    }
  }

  @Config(namespace = "curiosfabric", value = "client")
  public static final class Client {

    @ConfigEntry(value = "render_curios")
    public boolean renderCurios = true;

    @ConfigEntry(value = "enable_button")
    public boolean enableButton = true;

    @ConfigEntry(value = "button_x_offset")
    @Range(min = -100, max = 100)
    public int buttonXOffset = 0;

    @ConfigEntry(value = "button_y_offset")
    @Range(min = -100, max = 100)
    public int buttonYOffset = 0;

    @ConfigEntry(value = "creative_button_x_offset")
    @Range(min = -100, max = 100)
    public int creativeButtonXOffset = 0;

    @ConfigEntry(value = "creative_button_y_offset")
    @Range(min = -100, max = 100)
    public int creativeButtonYOffset = 0;

    @ConfigEntry(value = "button_corner")
    public ButtonCorner buttonCorner = ButtonCorner.TOP_LEFT;
  }

  public enum ButtonCorner {
    TOP_LEFT(26, -75, 73, -62),
    TOP_RIGHT(61, -75, 95, -62),
    BOTTOM_LEFT(26, -20, 73, -29),
    BOTTOM_RIGHT(61, -20, 95, -29);

    private final int xoffset;
    private final int yoffset;
    private final int creativeXoffset;
    private final int creativeYoffset;

    ButtonCorner(int x, int y, int creativeX, int creativeY) {
      this.xoffset = x;
      this.yoffset = y;
      this.creativeXoffset = creativeX;
      this.creativeYoffset = creativeY;
    }

    public int getXoffset() {
      return this.xoffset;
    }

    public int getYoffset() {
      return this.yoffset;
    }

    public int getCreativeXoffset() {
      return this.creativeXoffset;
    }

    public int getCreativeYoffset() {
      return this.creativeYoffset;
    }
  }
}
