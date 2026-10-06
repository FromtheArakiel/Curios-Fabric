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

import java.util.function.Supplier;

/**
 * A single, mutable configuration value.
 *
 * <p>Stands in for NeoForge's {@code ModConfigSpec.ConfigValue} so the call sites keep reading
 * naturally with {@code value.get()}.</p>
 *
 * @param <T> The value type
 */
public final class ConfigValue<T> implements Supplier<T> {

  private T value;

  public ConfigValue(T defaultValue) {
    this.value = defaultValue;
  }

  public void set(T value) {
    this.value = value;
  }

  @Override
  public T get() {
    return this.value;
  }
}
