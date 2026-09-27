package consumption

import org.graphiks.webgpu.GPUDevice
import org.graphiks.webgpu.suite.acid.foundationCases

/**
 * Compiles against the published suite artifacts to prove an external consumer can see the
 * portable signatures. It is a compilation check, not a native execution: a real runner chooses
 * its own isolation and manages the device lifecycle, as the browser runner does.
 */
suspend fun validateSuppliedDevice(device: GPUDevice) {
    for (case in foundationCases()) {
        check(case.requiredFeatures.all { it in device.features })
        case.run(device)
    }
}
