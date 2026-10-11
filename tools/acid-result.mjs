// An unsupported result must identify a capability explicitly allowed for that case.
export function isAcceptedAcidResult(entry, declaredFeatures = []) {
  if (entry.status === 'passed') return true;
  if (entry.status !== 'unsupported') return false;
  if (entry.id === 'adapter.feature-level-compatibility'
      && entry.unsupportedReason === 'compatibility-mode-core-fallback') return true;
  const reported = entry.missingFeatures ?? [];
  return declaredFeatures.length > 0 && reported.length > 0
    && reported.every((feature) => declaredFeatures.includes(feature));
}
