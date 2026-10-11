package org.graphiks.webgpu.suite

/** Only signals the spec-permitted Core fallback observed by the Compatibility feature-level case. */
class CompatibilityModeUnavailable : Exception("Compatibility request defaulted to Core; stricter validation is unavailable")
