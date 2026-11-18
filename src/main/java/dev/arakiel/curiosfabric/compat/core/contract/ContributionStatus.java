package dev.arakiel.curiosfabric.compat.core.contract;

/** Result state of a bridge contribution; empty and failed are deliberately distinct. */
public enum ContributionStatus {
  NOT_APPLICABLE,
  NOT_READY,
  EMPTY,
  AVAILABLE,
  PARTIAL,
  FAILED
}
