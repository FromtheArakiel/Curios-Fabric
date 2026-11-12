package dev.arakiel.curiosfabric.api.event;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

class CuriosEventBusTest {

  @Test
  void dispatchesThroughGeminiAndFiltersByCuriosEventType() {
    AtomicInteger baseCalls = new AtomicInteger();
    AtomicInteger childCalls = new AtomicInteger();
    CuriosEventBus.register(BaseTestEvent.class, event -> baseCalls.incrementAndGet());
    CuriosEventBus.register(ChildTestEvent.class, event -> childCalls.incrementAndGet());

    ChildTestEvent event = CuriosEventBus.post(new ChildTestEvent(7));

    assertEquals(7, event.value());
    assertEquals(1, baseCalls.get());
    assertEquals(1, childCalls.get());
  }

  private static class BaseTestEvent implements ICuriosEvent {
  }

  private static final class ChildTestEvent extends BaseTestEvent {
    private final int value;

    private ChildTestEvent(int value) {
      this.value = value;
    }

    private int value() {
      return this.value;
    }
  }
}
