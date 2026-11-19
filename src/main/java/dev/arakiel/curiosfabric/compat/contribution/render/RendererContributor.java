package dev.arakiel.curiosfabric.compat.contribution.render;

import java.util.Optional;

/** Bridge-owned access to an upstream accessory renderer registry. */
public interface RendererContributor {

  boolean supports(RendererQuery query);

  boolean render(RendererQuery query);

  /**
   * Allows a bridge to provide a last-resort renderer after all primary owners rejected the
   * stack. This is intentionally separate from {@link #supports(RendererQuery)} so fallback
   * rendering cannot affect normal renderer ownership or item qualification.
   */
  default boolean supportsFallback(RendererQuery query) {
    return false;
  }

  /** Returns whether this bridge identifies the stack as one of its own items for fallback. */
  default boolean ownsItemForFallback(RendererQuery query) {
    return false;
  }

  /** Returns a body-slot hint used only by a fallback renderer. */
  default Optional<String> fallbackSlot(RendererQuery query) {
    return Optional.empty();
  }
}
