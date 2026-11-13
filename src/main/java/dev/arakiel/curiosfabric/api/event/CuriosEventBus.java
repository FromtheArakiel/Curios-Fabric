package dev.arakiel.curiosfabric.api.event;

import dev.arakiel.gemini.event.GeminiEvent;
import dev.arakiel.gemini.event.GeminiEvents;
import java.util.function.Consumer;

/**
 * Curios API event facade backed by Gemini's event bus.
 *
 * <p>The public Curios event types remain unchanged. Each event is wrapped in a Gemini event
 * envelope so listeners still use the established Curios API while all dispatch goes through the
 * shared Gemini event implementation.</p>
 */
public final class CuriosEventBus {

  private CuriosEventBus() {
  }

  public static <T extends ICuriosEvent> void register(Class<T> eventType,
                                                       Consumer<T> listener) {
    GeminiEvents.listen(CuriosEventEnvelope.class, envelope -> {
      if (eventType.isInstance(envelope.event)) {
        listener.accept(eventType.cast(envelope.event));
      }
    });
  }

  public static <T extends ICuriosEvent> T post(T event) {
    GeminiEvents.post(new CuriosEventEnvelope(event));
    return event;
  }

  private static final class CuriosEventEnvelope extends GeminiEvent {
    private final ICuriosEvent event;

    private CuriosEventEnvelope(ICuriosEvent event) {
      this.event = event;
    }
  }
}
