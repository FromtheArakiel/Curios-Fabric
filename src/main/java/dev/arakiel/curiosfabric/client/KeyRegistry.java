package dev.arakiel.curiosfabric.client;

import net.minecraft.client.KeyMapping;
import org.lwjgl.glfw.GLFW;

public class KeyRegistry {

  public static KeyMapping openCurios =
      new KeyMapping("key.curiosfabric.open.desc", GLFW.GLFW_KEY_G,
          "key.curiosfabric.category");
}
