package org.graphiks.webgpu.suite.acid.errors

import org.graphiks.webgpu.GPUDevice
import org.graphiks.webgpu.GPUErrorFilter
import org.graphiks.webgpu.GPUFeatureName
import org.graphiks.webgpu.GPUQuerySet
import org.graphiks.webgpu.GPUQueryType
import org.graphiks.webgpu.descriptors.QuerySetDescriptor
import org.graphiks.webgpu.suite.AcidCaseId
import org.graphiks.webgpu.suite.AcidFamily
import org.graphiks.webgpu.suite.AcidTest
import org.graphiks.webgpu.suite.acid.ApiSymbols
import org.graphiks.webgpu.suite.acid.withValidationScope
import kotlin.test.assertFails
import kotlin.test.assertFalse
import kotlin.test.assertNull

/**
 * A device requested without `TimestampQuery` does not have the feature, and creating a timestamp
 * query set on it is refused synchronously on the content timeline — not as an error-scope event. An
 * ordinary occlusion query set is created normally, so the refusal is the feature's, not
 * `createQuerySet`'s. This separates adapter capability from device enablement: the sibling
 * optional cases prove the enabled side.
 */
@AcidTest(
    id = AcidCaseId.ErrorsUnenabledTimestampFeature,
    family = AcidFamily.ErrorsAsync,
    contract = [
        ApiSymbols.GPUDevice_features,
        ApiSymbols.GPUDevice_createQuerySet,
        ApiSymbols.GPUQueryType_Timestamp,
    ],
)
suspend fun unenabledTimestampFeature(device: GPUDevice) = withValidationScope(device) {
    assertFalse(
        GPUFeatureName.TimestampQuery in device.features,
        "A device requested without TimestampQuery must not report the feature",
    )

    // Control: a query set of a non-optional type is created normally and closed.
    device.createQuerySet(QuerySetDescriptor(type = GPUQueryType.Occlusion, count = 2u)).use { }

    // Probe: the timestamp query set is refused synchronously. Only the creation call is the
    // expected failure; an unexpectedly returned query set is closed outside that block so a
    // cleanup exception cannot masquerade as the refusal, and the scope stays empty.
    device.pushErrorScope(GPUErrorFilter.Validation)
    var returned: GPUQuerySet? = null
    try {
        assertFails("createQuerySet(Timestamp) without the feature must throw synchronously") {
            returned = device.createQuerySet(QuerySetDescriptor(type = GPUQueryType.Timestamp, count = 2u))
        }
    } finally {
        returned?.close()
        assertNull(
            device.popErrorScope().getOrThrow(),
            "The feature refusal is a synchronous throw, not an error-scope event",
        )
    }
}
