package dev.arakiel.curiosfabric.compat.contribution.render;

import dev.arakiel.curiosfabric.api.type.SlotSource;
import java.util.Optional;

/** Result of selecting the exactly-owned renderer for a slot source. */
public record RendererDecision(
    State state, Optional<SlotSource> source, String detail) {

  public enum State {
    NONE,
    UNIQUE,
    AMBIGUOUS,
    FAILED
  }

  public RendererDecision {
    if (state == null || source == null || detail == null) {
      throw new IllegalArgumentException("renderer decision fields must not be null");
    }
  }

  public static RendererDecision none() {
    return none("");
  }

  public static RendererDecision none(String detail) {
    return new RendererDecision(State.NONE, Optional.empty(), detail);
  }

  public static RendererDecision unique(SlotSource source) {
    return unique(source, "");
  }

  public static RendererDecision unique(SlotSource source, String detail) {
    return new RendererDecision(State.UNIQUE, Optional.of(source), detail);
  }

  public static RendererDecision ambiguous(String detail) {
    return new RendererDecision(State.AMBIGUOUS, Optional.empty(), detail);
  }

  public static RendererDecision failed(String detail) {
    return new RendererDecision(State.FAILED, Optional.empty(), detail);
  }
}
