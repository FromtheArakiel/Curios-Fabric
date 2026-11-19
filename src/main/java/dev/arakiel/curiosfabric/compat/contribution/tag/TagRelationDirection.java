package dev.arakiel.curiosfabric.compat.contribution.tag;

/**
 * Direction in which one compatibility tag relation should be copied.
 *
 * <p>Compatibility mappings are intentionally one-way: an upstream namespace may contribute
 * entries to a CuriosFabric semantic tag, but entries must never flow back into an upstream
 * namespace or from one upstream namespace into another.</p>
 */
public enum TagRelationDirection {
  ONE_WAY
}
