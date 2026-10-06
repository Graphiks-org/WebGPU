package org.graphiks.webgpu.arraybuffer;

import android.os.Bundle;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import org.graphiks.webgpu.benchmarks.AndroidCampaign;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Instrumented ArrayBuffer tests on device. {@code runCampaign} executes a measurement campaign and
 * hands the JSON report back through the instrumentation status stream (keyed {@code
 * arraybufferReport}) for the local Android runner; {@code arrayBufferSafetyChecks} runs the
 * business matrix and is what CI executes.
 */
@RunWith(AndroidJUnit4.class)
public class ArrayBufferAndroidTest {

    @Test
    public void runCampaign() {
        Bundle arguments = InstrumentationRegistry.getArguments();
        String profile = arguments.getString("profile", "ci");
        int runIndex = Integer.parseInt(arguments.getString("runIndex", "0"));
        String calibration = arguments.getString("calibration", null);
        boolean writers = "1".equals(arguments.getString("writers"));

        String report = AndroidCampaign.run(profile, runIndex, calibration, writers);

        Bundle status = new Bundle();
        status.putString("arraybufferReport", report);
        InstrumentationRegistry.getInstrumentation().sendStatus(0, status);
    }

    @Test
    public void arrayBufferSafetyChecks() {
        AndroidCampaign.runSafetyChecks();
    }
}
