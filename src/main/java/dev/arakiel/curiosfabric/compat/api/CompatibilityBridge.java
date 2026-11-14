package dev.arakiel.curiosfabric.compat.api;

/** One compatibility target split into independent data and behavior bridges. */
public interface CompatibilityBridge {

  String modId();

  boolean isLoaded();

  SlotMetadataBridge metadata();

  ItemTagBridge tags();

  EntityBindingBridge entityBindings();

  ItemBehaviorBridge itemBehavior();

  RendererBridge renderer();
}
