package dev.arakiel.curiosfabric.common.util;

import java.util.function.Supplier;

/**
 * A minimal stand-in for Neoforge's {@code DeferredHolder}/{@code RegistryObject}.
 *
 * <p>It keeps the familiar {@code registry.CURIO_MENU.get()} call sites working while the value is
 * assigned during mod initialization.</p>
 *
 * @param <T> The held type
 */
public final class Deferred<T> implements Supplier<T> {

  private T value;

  public void set(T value) {
    this.value = value;
  }

  @Override
  public T get() {
    return this.value;
  }
}
