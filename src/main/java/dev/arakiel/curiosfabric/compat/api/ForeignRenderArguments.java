package dev.arakiel.curiosfabric.compat.api;

/**
 * Adapts the shared render argument list to each foreign renderer contract.
 *
 * <p>Shared layout: model, render-layer parent, pose stack, buffer source, light, entity, limb swing,
 * limb amount, partial ticks, age in ticks, head yaw, head pitch.</p>
 */
final class ForeignRenderArguments {

  private ForeignRenderArguments() {
  }

  static Object[] trinkets(Object[] shared) {
    return select(shared, 0, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11);
  }

  static Object[] neoforge(Object[] shared) {
    return select(shared, 2, 1, 3, 4, 6, 7, 8, 9, 10, 11);
  }

  private static Object[] select(Object[] shared, int... indices) {
    Object[] result = new Object[indices.length];
    for (int index = 0; index < indices.length; index++) {
      result[index] = shared[indices[index]];
    }
    return result;
  }
}
