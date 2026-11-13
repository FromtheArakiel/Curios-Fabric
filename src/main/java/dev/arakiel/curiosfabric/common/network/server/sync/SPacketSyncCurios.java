package dev.arakiel.curiosfabric.common.network.server.sync;

import java.util.LinkedHashMap;
import java.util.Map;
import javax.annotation.Nonnull;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import dev.arakiel.curiosfabric.CuriosFabricConstants;
import dev.arakiel.curiosfabric.api.type.inventory.ICurioStacksHandler;

public class SPacketSyncCurios implements CustomPacketPayload {

  public static final Type<SPacketSyncCurios> TYPE =
      new Type<>(ResourceLocation.fromNamespaceAndPath(CuriosFabricConstants.MOD_ID, "sync_curios"));
  public static final StreamCodec<RegistryFriendlyByteBuf, SPacketSyncCurios> STREAM_CODEC =
      new StreamCodec<>() {
        @Nonnull
        @Override
        public SPacketSyncCurios decode(@Nonnull RegistryFriendlyByteBuf buf) {
          return new SPacketSyncCurios(buf);
        }

        @Override
        public void encode(@Nonnull RegistryFriendlyByteBuf buf, SPacketSyncCurios packet) {
          buf.writeInt(packet.entityId);
          buf.writeInt(packet.entrySize);

          for (Map.Entry<String, CompoundTag> entry : packet.map.entrySet()) {
            buf.writeUtf(entry.getKey());
            buf.writeNbt(entry.getValue());
          }
        }
      };

  public final int entityId;
  public final int entrySize;
  public final Map<String, CompoundTag> map;

  public SPacketSyncCurios(int entityId, Map<String, ICurioStacksHandler> map) {
    Map<String, CompoundTag> result = new LinkedHashMap<>();

    for (Map.Entry<String, ICurioStacksHandler> entry : map.entrySet()) {
      result.put(entry.getKey(), entry.getValue().getSyncTag());
    }
    this.entityId = entityId;
    this.entrySize = map.size();
    this.map = result;
  }

  public SPacketSyncCurios(final FriendlyByteBuf buf) {
    int entityId = buf.readInt();
    int entrySize = buf.readInt();
    Map<String, CompoundTag> map = new LinkedHashMap<>();

    for (int i = 0; i < entrySize; i++) {
      String key = buf.readUtf();
      map.put(key, buf.readNbt());
    }
    this.entityId = entityId;
    this.entrySize = map.size();
    this.map = map;
  }

  @Nonnull
  @Override
  public Type<? extends CustomPacketPayload> type() {
    return TYPE;
  }
}
