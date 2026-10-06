/*
 * Copyright (c) 2018-2024 C4
 *
 * This file is part of Curios, a mod made for Minecraft.
 *
 * Curios is free software: you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as published
 * by the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Curios is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR PARTICULAR PURPOSE.  See the GNU Lesser General Public
 * License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Curios.  If not, see <https://www.gnu.org/licenses/>.
 *
 */

package dev.arakiel.curios.common;

import net.minecraft.commands.synchronization.ArgumentTypeInfo;
import net.minecraft.commands.synchronization.ArgumentTypeInfos;
import net.minecraft.commands.synchronization.SingletonArgumentInfo;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.level.storage.loot.functions.LootItemFunctionType;
import dev.arakiel.curios.api.CurioAttributeModifiers;
import dev.arakiel.curios.api.CuriosApi;
import dev.arakiel.curios.common.inventory.container.CuriosContainer;
import dev.arakiel.curios.common.util.Deferred;
import dev.arakiel.curios.common.util.EquipCurioTrigger;
import dev.arakiel.curios.common.util.SetCurioAttributesFunction;
import dev.arakiel.curios.server.command.CurioArgumentType;

/**
 * Registry bootstrap of the Fabric build.
 *
 * <p>NeoForge's {@code DeferredRegister} is replaced by direct {@link Registry#register} calls. The
 * identifiers keep the {@code curios} namespace so that existing worlds (item components, loot
 * functions, the curios menu) stay readable.</p>
 */
public class CuriosRegistry {

  public static final Deferred<ArgumentTypeInfo<?, ?>> CURIO_SLOT_ARGUMENT = new Deferred<>();
  public static final Deferred<MenuType<CuriosContainer>> CURIO_MENU = new Deferred<>();
  public static final Deferred<LootItemFunctionType<SetCurioAttributesFunction>> CURIO_ATTRIBUTES =
      new Deferred<>();
  public static final Deferred<EquipCurioTrigger> EQUIP_TRIGGER = new Deferred<>();

  /** The data component that stores the curio attribute modifiers of an item stack. */
  public static DataComponentType<CurioAttributeModifiers> CURIO_ATTRIBUTE_MODIFIERS;

  private CuriosRegistry() {
  }

  private static ResourceLocation id(String path) {
    return ResourceLocation.fromNamespaceAndPath(CuriosApi.MODID, path);
  }

  public static void init() {
    SingletonArgumentInfo<CurioArgumentType> slotArgument =
        SingletonArgumentInfo.contextFree(CurioArgumentType::slot);
    ArgumentTypeInfos.register(BuiltInRegistries.COMMAND_ARGUMENT_TYPE, "curios:slot_type",
        CurioArgumentType.class, slotArgument);
    CURIO_SLOT_ARGUMENT.set(slotArgument);

    MenuType<CuriosContainer> menuType =
        new MenuType<>(CuriosContainer::new, FeatureFlags.VANILLA_SET);
    CURIO_MENU.set(menuType);
    Registry.register(BuiltInRegistries.MENU, id("curios_container"), menuType);

    LootItemFunctionType<SetCurioAttributesFunction> attributeFunction =
        new LootItemFunctionType<>(SetCurioAttributesFunction.CODEC);
    CURIO_ATTRIBUTES.set(attributeFunction);
    Registry.register(BuiltInRegistries.LOOT_FUNCTION_TYPE, id("set_curio_attributes"),
        attributeFunction);

    EQUIP_TRIGGER.set(EquipCurioTrigger.INSTANCE);
    Registry.register(BuiltInRegistries.TRIGGER_TYPES, id("equip_curio"),
        EquipCurioTrigger.INSTANCE);

    CURIO_ATTRIBUTE_MODIFIERS =
        Registry.register(BuiltInRegistries.DATA_COMPONENT_TYPE, id("attribute_modifiers"),
            DataComponentType.<CurioAttributeModifiers>builder()
                .persistent(CurioAttributeModifiers.CODEC)
                .networkSynchronized(CurioAttributeModifiers.STREAM_CODEC)
                .cacheEncoding()
                .build());
  }
}
