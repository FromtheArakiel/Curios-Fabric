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
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Curios.  If not, see <https://www.gnu.org/licenses/>.
 *
 */

package dev.arakiel.curios;

import net.fabricmc.api.EnvType;
import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CuriosConstants {

  /** The namespace of this mod: assets, data ({@code data/curiosfabric/...}), registries, channels. */
  public static final String MOD_ID = "curiosfabric";
  /** Same as {@link #MOD_ID}, kept as an alias for readability. */
  public static final String MODID = MOD_ID;
  /**
   * The data format namespace of the original (Forge/NeoForge) Curios edition:
   * {@code #curios:<slot>} item tags, the {@code data/<namespace>/curios/slots|entities} paths and
   * the {@code curios:<validator>} ids.
   *
   * <p>This mod uses {@link #MOD_ID} for its own data format, which is intentionally a different
   * identifier so that developers immediately see the two are not interchangeable. The
   * compatibility layer translates the legacy layout: the data managers also read those directories
   * and the item tag predicate also accepts those tags when the Forge edition is present.</p>
   */
  public static final String LEGACY_NAMESPACE = "curios";
  public static final String MOD_NAME = "Curios API";
  public static final Logger LOG = LoggerFactory.getLogger(MOD_NAME);

  /** True on the physical client. */
  public static final boolean IS_CLIENT =
      FabricLoader.getInstance().getEnvironmentType() == EnvType.CLIENT;

  /**
   * {@code true} when the resource location is in this mod's namespace or in the legacy
   * {@code curios} namespace. Used when reading identifiers that may have been written by the
   * original Curios (or by a Forge mod through the bridge).
   *
   * @param namespace The namespace to check
   */
  public static boolean isCuriosNamespace(String namespace) {
    return MOD_ID.equals(namespace) || LEGACY_NAMESPACE.equals(namespace);
  }

}
