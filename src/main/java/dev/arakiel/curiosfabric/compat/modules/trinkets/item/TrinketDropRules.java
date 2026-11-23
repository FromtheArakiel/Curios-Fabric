package dev.arakiel.curiosfabric.compat.modules.trinkets.item;

import dev.arakiel.curiosfabric.api.type.capability.ICurio;

/** Name-based drop-rule conversion that stays loadable without the optional Trinkets classes. */
final class TrinketDropRules {

  private TrinketDropRules() {
  }

  static ICurio.DropRule toCurios(String ruleName) {
    return switch (ruleName) {
      case "KEEP" -> ICurio.DropRule.ALWAYS_KEEP;
      case "DROP" -> ICurio.DropRule.ALWAYS_DROP;
      case "DESTROY" -> ICurio.DropRule.DESTROY;
      default -> ICurio.DropRule.DEFAULT;
    };
  }
}
