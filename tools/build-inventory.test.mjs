import { test } from 'node:test';
import assert from 'node:assert/strict';
import { findDuplicateResidualIds } from './inventory-ids.mjs';

test('a residual id that collides with an executable case id is detected', () => {
  const cases = [{ id: 'a.b' }];
  const uncovered = [{ id: 'a.b' }, { id: 'c.d' }];
  assert.deepEqual(findDuplicateResidualIds(cases, uncovered), ['a.b']);
});

test('disjoint ids are accepted', () => {
  const cases = [{ id: 'a.b' }];
  const uncovered = [{ id: 'c.d' }];
  assert.deepEqual(findDuplicateResidualIds(cases, uncovered), []);
});
