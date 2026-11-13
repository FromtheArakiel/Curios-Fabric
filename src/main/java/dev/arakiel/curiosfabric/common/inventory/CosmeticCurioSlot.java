package dev.arakiel.curiosfabric.common.inventory;

import net.minecraft.client.resources.language.I18n;
import net.minecraft.core.NonNullList;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.InventoryMenu;
import dev.arakiel.curiosfabric.CuriosFabricConstants;
import dev.arakiel.curiosfabric.api.type.inventory.IDynamicStackHandler;

public class CosmeticCurioSlot extends CurioSlot {

  public CosmeticCurioSlot(Player player, IDynamicStackHandler handler, int index,
                           String identifier, int xPosition, int yPosition) {
    super(player, handler, index, identifier, xPosition, yPosition, NonNullList.create(), true);
    this.setBackground(InventoryMenu.BLOCK_ATLAS,
        ResourceLocation.fromNamespaceAndPath(CuriosFabricConstants.MOD_ID, "slot/empty_cosmetic_slot"));
  }

  @Override
  public boolean getRenderStatus() {
    return true;
  }

  @Override
  public String getSlotName() {
    return I18n.get("curiosfabric.cosmetic") + " " + super.getSlotName();
  }
}
