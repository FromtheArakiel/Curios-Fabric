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

package dev.arakiel.curios.compat.bridge;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import dev.arakiel.curios.CuriosConstants;
import dev.arakiel.curios.compat.CompatTargets;

/**
 * Helpers for presenting this mod's objects as another accessory implementation's interfaces.
 *
 * <p>The adapters are {@link Proxy dynamic proxies} built from interface names, so this mod never
 * has to link against (or even compile against) NeoForge or Trinkets. Every single call is routed
 * through {@link Invoker}, and anything that is not implemented falls back to a harmless default
 * instead of throwing.</p>
 */
public final class ForeignProxy {

  private ForeignProxy() {
  }

  /** Handles a call on a foreign interface. */
  @FunctionalInterface
  public interface Invoker {
    Object invoke(String name, Class<?> returnType, Class<?>[] parameterTypes, Object[] args)
        throws Throwable;
  }

  /**
   * Builds a proxy implementing the named interfaces.
   *
   * @param interfaceNames Fully qualified interface names
   * @param invoker        The dispatcher
   * @return The proxy, or {@code null} when an interface is missing
   */
  public static Object create(String[] interfaceNames, Invoker invoker) {
    Class<?>[] classes = new Class<?>[interfaceNames.length];

    for (int i = 0; i < interfaceNames.length; i++) {
      classes[i] = CompatTargets.loadClass(interfaceNames[i]);

      if (classes[i] == null) {
        return null;
      }
    }
    ClassLoader loader = ForeignProxy.class.getClassLoader();
    InvocationHandler handler = (proxy, method, args) -> {

      if (method.getDeclaringClass() == Object.class) {
        return switch (method.getName()) {
          case "toString" -> "curiosfabric bridge";
          case "hashCode" -> System.identityHashCode(proxy);
          default -> proxy == args[0];
        };
      }

      try {
        return invoker.invoke(method.getName(), method.getReturnType(),
            method.getParameterTypes(), args == null ? new Object[0] : args);
      } catch (Throwable throwable) {
        CuriosConstants.LOG.debug("Bridged call {} failed", method.getName(), throwable);
        return defaultValue(method.getReturnType());
      }
    };
    return Proxy.newProxyInstance(loader, classes, handler);
  }

  /** A type appropriate neutral value for an unimplemented bridged method. */
  public static Object defaultValue(Class<?> type) {

    if (!type.isPrimitive()) {

      if (type == Optional.class) {
        return Optional.empty();
      }

      if (type == List.class) {
        // Mutable on purpose: the foreign implementation mutates these collections (for example
        // Trinkets clears its equipped list when a player connects), and an immutable empty
        // collection turns into an UnsupportedOperationException there.
        return new java.util.ArrayList<>();
      }

      if (type == Set.class) {
        return new java.util.HashSet<>();
      }

      if (type == Map.class) {
        return new java.util.HashMap<>();
      }

      if (java.util.Collection.class.isAssignableFrom(type)) {
        // Any other collection type: hand out something mutable, foreign code often mutates it.
        return new java.util.ArrayList<>();
      }

      if (type == ItemStack.class) {
        return ItemStack.EMPTY;
      }

      if (type == CompoundTag.class) {
        return new CompoundTag();
      }
      return null;
    }

    if (type == boolean.class) {
      return Boolean.FALSE;
    }

    if (type == void.class) {
      return null;
    }

    if (type == float.class) {
      return 0.0F;
    }

    if (type == double.class) {
      return 0.0D;
    }

    if (type == long.class) {
      return 0L;
    }

    if (type == char.class) {
      return (char) 0;
    }
    return 0;
  }

  /** The enum constant of the given type, or {@code fallback} when it does not exist. */
  public static Object enumConstant(Class<?> enumType, String name, Object fallback) {

    if (enumType == null || !enumType.isEnum()) {
      return fallback;
    }

    for (Object constant : enumType.getEnumConstants()) {

      if (((Enum<?>) constant).name().equals(name)) {
        return constant;
      }
    }
    return fallback;
  }

  /** Reads a no-argument accessor reflectively, returning {@code fallback} on any problem. */
  public static Object read(Object target, String method, Object fallback) {

    if (target == null) {
      return fallback;
    }

    try {
      Method accessor = target.getClass().getMethod(method);
      return accessor.invoke(target);
    } catch (Throwable throwable) {
      return fallback;
    }
  }
}
