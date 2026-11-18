package dev.arakiel.curiosfabric.compat.contribution.render;

/** Bridge-owned access to an upstream accessory renderer registry. */
public interface RendererContributor {

  boolean supports(RendererQuery query);

  boolean render(RendererQuery query);
}
