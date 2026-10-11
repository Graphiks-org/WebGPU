import { test } from 'node:test';
import assert from 'node:assert/strict';
import { isAcceptedAcidResult } from './acid-result.mjs';

test('only the Compatibility case may report the spec-permitted Core fallback', () => {
  assert.equal(isAcceptedAcidResult({
    id: 'adapter.feature-level-compatibility', status: 'unsupported',
    unsupportedReason: 'compatibility-mode-core-fallback',
  }), true);
  assert.equal(isAcceptedAcidResult({
    id: 'adapter.feature-level-core', status: 'unsupported',
    unsupportedReason: 'compatibility-mode-core-fallback',
  }), false);
});

test('Compatibility failures and unclassified unsupported results stay failures', () => {
  for (const status of ['failed', 'not-run', 'unsupported']) {
    assert.equal(isAcceptedAcidResult({ id: 'adapter.feature-level-compatibility', status }), false);
  }
  assert.equal(isAcceptedAcidResult({
    id: 'adapter.feature-level-compatibility', status: 'failed',
    unsupportedReason: 'compatibility-mode-core-fallback',
  }), false);
});

test('optional-feature skips require a matching declaration and nonempty missing features', () => {
  assert.equal(isAcceptedAcidResult({ status: 'passed' }), true);
  assert.equal(isAcceptedAcidResult({ status: 'unsupported', missingFeatures: ['ShaderF16'] }, ['ShaderF16']), true);
  assert.equal(isAcceptedAcidResult({ status: 'unsupported', missingFeatures: ['ShaderF16'] }), false);
  assert.equal(isAcceptedAcidResult({ status: 'unsupported', missingFeatures: [] }, ['ShaderF16']), false);
  assert.equal(isAcceptedAcidResult({ status: 'unsupported', missingFeatures: ['TimestampQuery'] }, ['ShaderF16']), false);
});
