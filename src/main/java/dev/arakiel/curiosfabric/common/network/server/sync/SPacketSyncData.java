package dev.arakiel.curiosfabric.common.network.server.sync;

import javax.annotation.Nonnull;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import dev.arakiel.curiosfabric.CuriosFabricConstants;

public class SPacketSyncData implements CustomPacketPayload {

  public static final Type<SPacketSyncData> TYPE =
      new Type<>(ResourceLocation.fromNamespaceAndPath(CuriosFabricConstants.MOD_ID, "sync_data"));
  public static final StreamCodec<RegistryFriendlyByteBuf, SPacketSyncData> STREAM_CODEC =
      new StreamCodec<>() {
        @Nonnull
        @Override
        public SPacketSyncData decode(@Nonnull RegistryFriendlyByteBuf buf) {
          return new SPacketSyncData(buf);
        }

        @Override
        public void encode(@Nonnull RegistryFriendlyByteBuf buf, SPacketSyncData packet) {
          CompoundTag tag = new CompoundTag();
          tag.put("SlotData", packet.slotData);
          tag.put("EntityData", packet.entityData);
          buf.writeNbt(tag);
        }
      };

  public final ListTag slotData;
  public final ListTag entityData;

  public SPacketSyncData(ListTag slotData, ListTag entityData) {
    this.slotData = slotData;
    this.entityData = entityData;
  }

  public SPacketSyncData(final FriendlyByteBuf buf) {
    CompoundTag tag = buf.readNbt();

    if (tag != null) {
      this.slotData = tag.getList("SlotData", Tag.TAG_COMPOUND);
      this.entityData = tag.getList("EntityData", Tag.TAG_COMPOUND);
    } else {
      this.slotData = new ListTag();
      this.entityData = new ListTag();
    }
  }

  @Nonnull
  @Override
  public Type<? extends CustomPacketPayload> type() {
    return TYPE;
  }
}
