package dev.arakiel.curiosfabric.platform.services;

import java.util.Map;
import javax.annotation.Nullable;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.EnderMan;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import dev.arakiel.curiosfabric.api.type.ISlotType;

public interface ICuriosPlatform {

  Map<String, ISlotType> getItemStackSlots(ItemStack stack, @Nullable LivingEntity livingEntity);

  boolean makesPiglinsNeutral(ItemStack stack, LivingEntity livingEntity);

  boolean canWalkOnPowderedSnow(ItemStack stack, LivingEntity livingEntity);

  boolean isEnderMask(ItemStack stack, Player player, EnderMan enderMan);
}
