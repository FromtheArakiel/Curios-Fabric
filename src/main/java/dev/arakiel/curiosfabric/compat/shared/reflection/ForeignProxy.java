package dev.arakiel.curiosfabric.compat.shared.reflection;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;

/**
 * Helpers for presenting this mod's objects as another accessory implementation's interfaces.
 *
 * <p>The bridge-specific ASM generators use {@link Invoker} as their dispatcher contract. This
 * class only retains the low-frequency default-value and no-argument accessor helpers.</p>
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
