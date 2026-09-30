// Guards that the executable catalogue and the residual "to be tested" list never share an id:
// the same id appearing in both would publish one behaviour as executed and untested at once.

/** Returns every residual id that also names an executable case. */
export function findDuplicateResidualIds(cases, uncovered) {
  const executableIds = new Set(cases.map((entry) => entry.id));
  return uncovered.filter((entry) => executableIds.has(entry.id)).map((entry) => entry.id);
}

/** Throws when any residual id collides with an executable case id. */
export function assertDisjointIds(cases, uncovered) {
  const duplicates = findDuplicateResidualIds(cases, uncovered);
  if (duplicates.length > 0) {
    throw new Error(`Residual ids duplicate executable case ids: ${duplicates.join(', ')}`);
  }
}
