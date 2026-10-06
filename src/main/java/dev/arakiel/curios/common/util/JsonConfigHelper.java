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

package dev.arakiel.curios.common.util;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import net.fabricmc.loader.api.FabricLoader;
import dev.arakiel.curios.CuriosConstants;

/**
 * Helpers for the small JSON configuration files that replace NeoForge's config system.
 */
public final class JsonConfigHelper {

  private static final Gson GSON =
      new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();

  private JsonConfigHelper() {
  }

  /** The path of a config file inside the loader's config directory. */
  public static Path configPath(String fileName) {
    return FabricLoader.getInstance().getConfigDir().resolve(fileName);
  }

  /**
   * Reads a config file, creating it with the given defaults when it does not exist yet.
   *
   * @param fileName The file name inside the config directory
   * @param defaults The default values
   * @return The parsed contents, or the defaults when reading failed
   */
  public static JsonObject readOrCreate(String fileName, JsonObject defaults) {
    Path path = configPath(fileName);

    if (!Files.exists(path)) {
      write(path, defaults);
      return defaults;
    }

    try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
      JsonElement element = JsonParser.parseReader(reader);

      if (element != null && element.isJsonObject()) {
        return element.getAsJsonObject();
      }
      CuriosConstants.LOG.warn("Config file {} is not a JSON object, using defaults", fileName);
    } catch (IOException | RuntimeException exception) {
      CuriosConstants.LOG.error("Failed to read the config file {}", fileName, exception);
    }
    return defaults;
  }

  private static void write(Path path, JsonObject object) {

    try {
      Files.createDirectories(path.getParent());

      try (Writer writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8)) {
        GSON.toJson(object, writer);
      }
    } catch (IOException exception) {
      CuriosConstants.LOG.error("Failed to write the config file {}", path, exception);
    }
  }

  public static boolean getBoolean(JsonObject object, String key, boolean defaultValue) {
    JsonElement element = object.get(key);
    return element != null && element.isJsonPrimitive() ? element.getAsBoolean() : defaultValue;
  }

  public static int getInt(JsonObject object, String key, int defaultValue) {
    JsonElement element = object.get(key);
    return element != null && element.isJsonPrimitive() ? element.getAsInt() : defaultValue;
  }

  public static String getString(JsonObject object, String key, String defaultValue) {
    JsonElement element = object.get(key);
    return element != null && element.isJsonPrimitive() ? element.getAsString() : defaultValue;
  }

  public static List<String> getStringList(JsonObject object, String key) {
    List<String> result = new ArrayList<>();
    JsonElement element = object.get(key);

    if (element != null && element.isJsonArray()) {
      JsonArray array = element.getAsJsonArray();

      for (JsonElement child : array) {

        if (child.isJsonPrimitive()) {
          result.add(child.getAsString());
        }
      }
    }
    return result;
  }
}
