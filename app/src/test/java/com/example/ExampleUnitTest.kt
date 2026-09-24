package com.example

import com.example.util.DeveloperLicenseGenerator
import com.example.util.FeatureGate
import org.junit.Assert.*
import org.junit.Test

class ExampleUnitTest {
  @Test
  fun testFeatureGateSerialNumberGenerationAndVerification() {
    val sampleDeviceId = "A1B2C3D4E5F67890"

    // 1. Generate 30D key
    val gen30D = DeveloperLicenseGenerator.generateLicense(sampleDeviceId, "30D")
    assertTrue(gen30D.serialNumber.startsWith("BQPRO-30D-"))

    // 2. Verify key for this device
    val validation = FeatureGate.verifyLicenseKey(gen30D.serialNumber, sampleDeviceId)
    assertTrue("Key should be valid for this device", validation.isValid)
    assertEquals(30, validation.durationDays)
    assertFalse(validation.isLifetime)

    // 3. Verify key fails on different device
    val wrongDeviceValidation = FeatureGate.verifyLicenseKey(gen30D.serialNumber, "DIFFERENT_DEV_ID_999")
    assertFalse("Key should be invalid on different device", wrongDeviceValidation.isValid)

    // 4. Test 1D plan (1000/hari)
    val gen1D = DeveloperLicenseGenerator.generateLicense(sampleDeviceId, "1D")
    val val1D = FeatureGate.verifyLicenseKey(gen1D.serialNumber, sampleDeviceId)
    assertTrue(val1D.isValid)
    assertEquals(1, val1D.durationDays)

    // 5. Test Lifetime plan
    val genLife = DeveloperLicenseGenerator.generateLicense(sampleDeviceId, "LIFE")
    val valLife = FeatureGate.verifyLicenseKey(genLife.serialNumber, sampleDeviceId)
    assertTrue(valLife.isValid)
    assertTrue(valLife.isLifetime)
  }
}
