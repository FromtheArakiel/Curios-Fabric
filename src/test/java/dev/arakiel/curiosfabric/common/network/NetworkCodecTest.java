package dev.arakiel.curiosfabric.common.network;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import dev.arakiel.curiosfabric.common.network.client.CPacketDestroy;
import dev.arakiel.curiosfabric.common.network.client.CPacketPage;
import dev.arakiel.curiosfabric.common.network.server.SPacketPage;
import dev.arakiel.curiosfabric.common.network.server.sync.SPacketSyncActiveState;
import dev.arakiel.curiosfabric.common.network.server.sync.SPacketSyncRender;
import io.netty.buffer.Unpooled;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import org.junit.jupiter.api.Test;

class NetworkCodecTest {

  @Test
  void clientAndServerPageMessagesRoundTrip() {
    assertEquals(new CPacketPage(12, true),
        roundTrip(CPacketPage.STREAM_CODEC, new CPacketPage(12, true)));
    assertEquals(new SPacketPage(7, 3),
        roundTrip(SPacketPage.STREAM_CODEC, new SPacketPage(7, 3)));
  }

  @Test
  void renderAndActiveStateRoundTrip() {
    assertEquals(
        new SPacketSyncRender(44, "ring", 2, false),
        roundTrip(SPacketSyncRender.STREAM_CODEC,
            new SPacketSyncRender(44, "ring", 2, false)));
    assertEquals(
        new SPacketSyncActiveState(44, "necklace", 1, true),
        roundTrip(SPacketSyncActiveState.STREAM_CODEC,
            new SPacketSyncActiveState(44, "necklace", 1, true)));
  }

  @Test
  void emptyCommandHasNoTrailingData() {
    RegistryFriendlyByteBuf buffer =
        new RegistryFriendlyByteBuf(Unpooled.buffer(), RegistryAccess.EMPTY);
    try {
      CPacketDestroy.STREAM_CODEC.encode(buffer, new CPacketDestroy());
      new CPacketDestroy();
      assertFalse(buffer.isReadable());
    } finally {
      buffer.release();
    }
  }

  private static <T> T roundTrip(StreamCodec<RegistryFriendlyByteBuf, T> codec, T value) {
    RegistryFriendlyByteBuf buffer =
        new RegistryFriendlyByteBuf(Unpooled.buffer(), RegistryAccess.EMPTY);
    try {
      codec.encode(buffer, value);
      return codec.decode(buffer);
    } finally {
      buffer.release();
    }
  }
}
