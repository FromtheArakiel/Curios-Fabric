package dev.arakiel.curiosfabric.api.event;

/** An {@link ICuriosEvent} whose default action can be cancelled by a listener. */
public interface ICancellableCuriosEvent extends ICuriosEvent {

  boolean isCanceled();

  void setCanceled(boolean canceled);
}
