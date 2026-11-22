package dev.arakiel.curiosfabric.common;

import dev.arakiel.gemini.config.Config;
import dev.arakiel.gemini.config.ConfigEntry;
import dev.arakiel.gemini.config.ConfigHandle;
import dev.arakiel.gemini.config.GeminiConfig;
import dev.arakiel.gemini.config.Range;
import java.util.List;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.ItemStack;

/** Gemini-backed common and server configuration. */
public final class CuriosFabricConfig {

  public static ConfigHandle<Common> COMMON;
  public static ConfigHandle<Server> SERVER;

  private CuriosFabricConfig() {
  }

  public static void load() {
    GeminiConfig.init();
    if (COMMON == null) {
      COMMON = GeminiConfig.register("curiosfabric", "common", true, Common.class);
    }
    if (SERVER == null) {
      SERVER = GeminiConfig.register("curiosfabric", "server", true, Server.class);
    }
  }

  @Config(namespace = "curiosfabric", value = "common")
  public static final class Common {

    @ConfigEntry(
        value = "slots",
        comment = "Slot declarations using id=...;size=...;operation=...;order=... entries.")
    public List<String> slots = List.of();
  }

  @Config(namespace = "curiosfabric", value = "server")
  public static final class Server {

    @ConfigEntry(
        value = "keep_curios",
        comment = "ON keeps curios on death, OFF drops them, DEFAULT follows keepInventory.")
    public KeepCurios keepCurios = KeepCurios.DEFAULT;

    @ConfigEntry(value = "minimum_columns")
    @Range(min = 1, max = 8)
    public int minimumColumns = 1;

    @ConfigEntry(value = "max_slots_per_page")
    @Range(min = 1, max = 48)
    public int maxSlotsPerPage = 24;

    @ConfigEntry(
        value = "right_click_equip_blacklist",
        comment = "Full item registry names that must not auto-equip into a curio slot on right-click.")
    public List<String> rightClickEquipBlacklist = List.of();
  }

  public enum KeepCurios {
    ON,
    DEFAULT,
    OFF
  }

  public static boolean isRightClickEquipBlocked(ItemStack stack) {
    return SERVER != null && isRightClickEquipBlocked(
        stack, SERVER.value().rightClickEquipBlacklist);
  }

  static boolean isRightClickEquipBlocked(ItemStack stack, List<String> blacklist) {
    if (stack == null || stack.isEmpty()) {
      return false;
    }
    var id = BuiltInRegistries.ITEM.getKey(stack.getItem());
    return id != null && blacklist != null && blacklist.contains(id.toString());
  }
}
