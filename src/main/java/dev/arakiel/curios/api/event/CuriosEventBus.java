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

package dev.arakiel.curios.api.event;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

/**
 * A tiny, thread safe event bus for the Curios API events.
 *
 * <p>The NeoForge edition publishes its events on NeoForge's event bus. Fabric has no general
 * purpose event bus, so the port ships this small replacement: listeners are registered for an
 * event class (or one of its super types) and every {@link #post(ICuriosEvent)} call hands the
 * event instance to the matching listeners, in registration order.</p>
 */
public final class CuriosEventBus {

  private static final Map<Class<?>, List<Consumer<?>>> LISTENERS = new ConcurrentHashMap<>();

  private CuriosEventBus() {
  }

  /**
   * Registers a listener for the given event type. The type may be a super type of the events that
   * are actually posted, in which case the listener receives all of them.
   *
   * @param eventType The event type to listen for
   * @param listener  The listener
   * @param <T>       The event type
   */
  public static <T extends ICuriosEvent> void register(Class<T> eventType,
                                                       Consumer<T> listener) {
    LISTENERS.computeIfAbsent(eventType, key -> new CopyOnWriteArrayList<>()).add(listener);
  }

  /**
   * Dispatches the event to every matching listener.
   *
   * @param event The event instance
   * @param <T>   The event type
   * @return The same event instance, for convenience
   */
  @SuppressWarnings("unchecked")
  public static <T extends ICuriosEvent> T post(T event) {

    for (Map.Entry<Class<?>, List<Consumer<?>>> entry : LISTENERS.entrySet()) {

      if (entry.getKey().isInstance(event)) {

        for (Consumer<?> listener : entry.getValue()) {
          ((Consumer<T>) listener).accept(event);
        }
      }
    }
    return event;
  }
}
