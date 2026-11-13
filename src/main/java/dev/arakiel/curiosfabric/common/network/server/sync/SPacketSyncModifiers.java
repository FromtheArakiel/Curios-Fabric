package dev.arakiel.curiosfabric.common.network.server.sync;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import javax.annotation.Nonnull;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import dev.arakiel.curiosfabric.CuriosFabricConstants;
import dev.arakiel.curiosfabric.api.type.inventory.ICurioStacksHandler;

public class SPacketSyncModifiers implements CustomPacketPayload {

  public static final Type<SPacketSyncModifiers> TYPE =
      new Type<>(ResourceLocation.fromNamespaceAndPath(CuriosFabricConstants.MOD_ID, "sync_modifiers"));
  public static final StreamCodec<RegistryFriendlyByteBuf, SPacketSyncModifiers> STREAM_CODEC =
      new StreamCodec<>() {
        @Nonnull
        @Override
        public SPacketSyncModifiers decode(@Nonnull RegistryFriendlyByteBuf buf) {
          return new SPacketSyncModifiers(buf);
        }

        @Override
        public void encode(@Nonnull RegistryFriendlyByteBuf buf, SPacketSyncModifiers packet) {
          buf.writeInt(packet.entityId);
          buf.writeInt(packet.entrySize);

          for (Map.Entry<String, CompoundTag> entry : packet.updates.entrySet()) {
            buf.writeUtf(entry.getKey());
            buf.writeNbt(entry.getValue());
          }
        }
      };

  public final int entityId;
  public final int entrySize;
  public final Map<String, CompoundTag> updates;

  public SPacketSyncModifiers(int entityId, Set<ICurioStacksHandler> updates) {
    Map<String, CompoundTag> result = new LinkedHashMap<>();

    for (ICurioStacksHandler stacksHandler : updates) {
      result.put(stacksHandler.getIdentifier(), stacksHandler.getSyncTag());
    }
    this.entityId = entityId;
    this.entrySize = result.size();
    this.updates = result;
  }

  public SPacketSyncModifiers(final FriendlyByteBuf buf) {
    int entityId = buf.readInt();
    int entrySize = buf.readInt();
    Map<String, CompoundTag> map = new LinkedHashMap<>();

    for (int i = 0; i < entrySize; i++) {
      String key = buf.readUtf();
      map.put(key, buf.readNbt());
    }
    this.entityId = entityId;
    this.entrySize = map.size();
    this.updates = map;
  }

  @Nonnull
  @Override
  public Type<? extends CustomPacketPayload> type() {
    return TYPE;
  }
}
