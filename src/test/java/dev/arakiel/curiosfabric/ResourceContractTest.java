package dev.arakiel.curiosfabric;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class ResourceContractTest {

  @Test
  void everyLanguageUsesCuriosFabricTranslationKeys() throws Exception {
    try (var files = Files.list(Path.of("src/main/resources/assets/curiosfabric/lang"))) {
      for (Path file : files.filter(path -> path.toString().endsWith(".json")).toList()) {
        try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
          JsonObject json = JsonParser.parseReader(reader).getAsJsonObject();
          assertTrue(json.has("commands.curiosfabric.add.success"), file.toString());
          assertFalse(json.has("commands.curios.add.success"), file.toString());
          assertFalse(json.has("curios.identifier.ring"), file.toString());
        }
      }
    }
  }

  @Test
  void fabricMetadataRequiresGeminiAndUsesTheNewEntrypoint() throws Exception {
    String metadata = Files.readString(
        Path.of("src/main/resources/fabric.mod.json"), StandardCharsets.UTF_8);

    assertTrue(metadata.contains("\"gemini\": \">=1.0.4\""));
    assertTrue(metadata.contains("\"curiosfabric:inventory\""));
    assertTrue(metadata.contains("\"curiosfabric\": []"));
    assertFalse(metadata.contains("\"curios\": []"));
  }
}
