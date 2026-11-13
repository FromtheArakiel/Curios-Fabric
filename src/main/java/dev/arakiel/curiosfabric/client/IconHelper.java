package dev.arakiel.curiosfabric.client;

import java.util.HashMap;
import java.util.Map;
import net.minecraft.resources.ResourceLocation;
import dev.arakiel.curiosfabric.api.CuriosApi;
import dev.arakiel.curiosfabric.api.type.util.IIconHelper;

public class IconHelper implements IIconHelper {

  private final Map<String, ResourceLocation> idToIcon = new HashMap<>();

  @Override
  public void clearIcons() {
    this.idToIcon.clear();
  }

  @Override
  public void addIcon(String identifier, ResourceLocation resourceLocation) {
    this.idToIcon.putIfAbsent(identifier, resourceLocation);
  }

  @Override
  public ResourceLocation getIcon(String identifier) {
    return idToIcon.getOrDefault(identifier,
        ResourceLocation.fromNamespaceAndPath(CuriosApi.MODID, "slot/empty_curio_slot"));
  }
}
