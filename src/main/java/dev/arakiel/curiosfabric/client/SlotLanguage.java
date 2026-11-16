package dev.arakiel.curiosfabric.client;

import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import dev.arakiel.curiosfabric.CuriosFabricConstants;
import dev.arakiel.curiosfabric.api.type.ISlotType;
import dev.arakiel.curiosfabric.api.type.SlotSource;
import dev.arakiel.curiosfabric.common.data.CuriosSlotManager;
import dev.arakiel.curiosfabric.compat.trinkets.TrinketsCompat;

/** Resolves slot names through the language namespace of their source implementation. */
public final class SlotLanguage {

  private static final Set<String> REPORTED_MISSING_KEYS = ConcurrentHashMap.newKeySet();

  private SlotLanguage() {
  }

  public static String name(String slotId) {
    Optional<ISlotType> slot = findSlot(slotId);
    String key = translationKey(slotId, slot);
    reportIfMissing(slotId, key, slot);
    return I18n.get(key);
  }

  public static MutableComponent displayName(String slotId) {
    Optional<ISlotType> slot = findSlot(slotId);
    String key = translationKey(slotId, slot);
    reportIfMissing(slotId, key, slot);
    return Component.translatable(key);
  }

  /**
   * Returns the theoretical language key for the slot's source.
   *
   * <p>No cross-source fallback is attempted: a missing translation intentionally renders as the
   * raw key, matching vanilla's missing-language behaviour.</p>
   */
  public static String translationKey(String slotId) {
    return translationKey(slotId, findSlot(slotId));
  }

  private static String translationKey(String slotId, Optional<ISlotType> slot) {
    return slot.map(SlotLanguage::theoreticalKey)
        .orElseGet(() -> theoreticalKey(slotId, null));
  }

  static String theoreticalKey(ISlotType slot) {
    if (slot.getSource() == SlotSource.TRINKETS) {
      return TrinketsCompat.translationKeyFor(slot.getIdentifier())
          .orElse("trinkets.slot." + slot.getIdentifier());
    }
    return slot.getSource().translationKey(slot.getMappingIdentifier())
        .orElse("curiosfabric.identifier." + slot.getMappingIdentifier());
  }

  static String theoreticalKey(String slotId, SlotSource source) {
    if (source == SlotSource.TRINKETS) {
      return TrinketsCompat.translationKeyFor(slotId)
          .orElse("trinkets.slot." + slotId);
    }
    return (source == null ? SlotSource.CURIOSFABRIC : source)
        .translationKey(slotId)
        .orElse("curiosfabric.identifier." + slotId);
  }

  private static Optional<ISlotType> findSlot(String slotId) {
    Optional<ISlotType> exact = CuriosSlotManager.CLIENT.getSlot(slotId);
    if (exact.isPresent()) {
      return exact;
    }
    return CuriosSlotManager.CLIENT.getSlots().values().stream()
        .filter(slot -> slot.getMappingIdentifier().equals(slotId))
        .findFirst();
  }

  private static void reportIfMissing(String slotId, String key, Optional<ISlotType> slot) {
    if (!I18n.exists(key) && REPORTED_MISSING_KEYS.add(key)) {
      SlotSource source = slot.map(ISlotType::getSource).orElse(null);
      CuriosFabricConstants.LOG.warn("Missing slot translation key {} for slot {} (source {})",
          key, slotId, source == null ? "unknown" : source.id());
    }
  }
}
