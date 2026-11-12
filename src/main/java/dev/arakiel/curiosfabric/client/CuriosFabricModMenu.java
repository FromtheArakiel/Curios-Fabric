package dev.arakiel.curiosfabric.client;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import dev.arakiel.gemini.config.ConfigHandle;
import dev.arakiel.gemini.config.GeminiConfig;
import dev.arakiel.gemini.config.client.GeminiConfigClient;
import java.util.List;
import net.minecraft.client.gui.screens.Screen;

/** Exposes the Gemini-generated configuration UI through Mod Menu. */
public final class CuriosFabricModMenu implements ModMenuApi {

  @Override
  public ConfigScreenFactory<?> getModConfigScreenFactory() {
    return CuriosFabricModMenu::createScreen;
  }

  private static Screen createScreen(Screen parent) {
    List<ConfigHandle<?>> configs = GeminiConfig.all().stream()
        .filter(handle -> "curiosfabric".equals(handle.id().getNamespace()))
        .toList();
    return configs.size() == 1
        ? GeminiConfigClient.createScreen(parent, configs.getFirst())
        : GeminiConfigClient.createHubScreen(parent, configs);
  }
}
