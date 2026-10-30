package com.smarteq.pro.data.autoeq

import com.smarteq.pro.data.model.AutoEqDeviceType
import com.smarteq.pro.data.model.AutoEqProfile

/**
 * Ported 1:1 from src/data/autoEqDatabase.ts in the original web app.
 * Frequency order for correction15 is the standard 15-band set:
 * [25, 40, 63, 100, 160, 250, 400, 630, 1k, 1.6k, 2.5k, 4k, 6.3k, 10k, 16k]
 */
object AutoEqDatabase {

    val PROFILES: List<AutoEqProfile> = listOf(
        AutoEqProfile(
            id = "google-pixel-buds-pro-2", brand = "Google", model = "Pixel Buds Pro 2",
            type = AutoEqDeviceType.IN_EAR,
            description = "AutoEQ Harman target correction for Google Pixel Buds Pro 2. Smooths upper-mid peak & adds clean sub-bass extension.",
            correction15 = listOf(2.5f, 2.0f, 1.5f, 0.5f, -0.5f, -1.0f, -0.5f, 0.0f, 1.0f, 1.5f, -1.5f, 0.5f, 1.0f, -0.5f, 1.0f)
        ),
        AutoEqProfile(
            id = "google-pixel-buds-pro", brand = "Google", model = "Pixel Buds Pro",
            type = AutoEqDeviceType.IN_EAR,
            description = "AutoEQ target correction for Pixel Buds Pro. Tames warm mid-bass bump and enhances upper-treble detail.",
            correction15 = listOf(1.5f, 1.0f, 0.0f, -1.5f, -2.0f, -1.0f, 0.0f, 0.5f, 1.0f, 2.0f, 0.5f, -1.0f, 1.5f, 2.0f, 1.0f)
        ),
        AutoEqProfile(
            id = "sony-wh-1000xm5", brand = "Sony", model = "WH-1000XM5",
            type = AutoEqDeviceType.OVER_EAR,
            description = "AutoEQ Harman target correction for Sony WH-1000XM5. Tames heavy mid-bass boom and lifts vocal clarity at 2kHz-4kHz.",
            correction15 = listOf(-1.0f, -2.0f, -3.5f, -4.0f, -3.0f, -1.5f, 0.0f, 1.5f, 2.5f, 3.5f, 2.0f, 1.0f, 0.5f, 2.0f, 1.5f)
        ),
        AutoEqProfile(
            id = "sony-wh-1000xm4", brand = "Sony", model = "WH-1000XM4",
            type = AutoEqDeviceType.OVER_EAR,
            description = "AutoEQ Harman target correction for Sony WH-1000XM4. Corrects recessed mids and cleans up bloated upper bass.",
            correction15 = listOf(-0.5f, -2.0f, -4.0f, -4.5f, -3.5f, -2.0f, 0.5f, 2.0f, 3.0f, 4.0f, 2.5f, 0.0f, -1.0f, 1.5f, 2.0f)
        ),
        AutoEqProfile(
            id = "sony-wf-1000xm5", brand = "Sony", model = "WF-1000XM5",
            type = AutoEqDeviceType.IN_EAR,
            description = "AutoEQ target for Sony WF-1000XM5 TWS. Tightens low-end punch and opens up upper mid brilliance.",
            correction15 = listOf(1.0f, 0.5f, -1.0f, -2.0f, -1.5f, -0.5f, 0.5f, 1.0f, 2.0f, 2.5f, 1.0f, 0.0f, 1.5f, 1.0f, 0.5f)
        ),
        AutoEqProfile(
            id = "apple-airpods-pro-2", brand = "Apple", model = "AirPods Pro 2",
            type = AutoEqDeviceType.IN_EAR,
            description = "AutoEQ target correction for Apple AirPods Pro 2 (USB-C/Lightning). Near-flat target with subtle sub-bass boost.",
            correction15 = listOf(2.0f, 1.5f, 1.0f, 0.0f, -0.5f, -0.5f, 0.0f, 0.0f, 0.5f, 0.5f, -0.5f, -1.0f, 1.0f, 1.5f, 0.5f)
        ),
        AutoEqProfile(
            id = "apple-airpods-max", brand = "Apple", model = "AirPods Max",
            type = AutoEqDeviceType.OVER_EAR,
            description = "AutoEQ target correction for Apple AirPods Max. Tames minor 1.5kHz elevation and opens air above 10kHz.",
            correction15 = listOf(1.0f, 0.5f, 0.0f, -0.5f, -0.5f, 0.0f, 0.5f, 0.0f, -1.5f, -1.0f, 0.5f, 1.0f, 0.5f, 2.0f, 2.5f)
        ),
        AutoEqProfile(
            id = "sennheiser-hd-600", brand = "Sennheiser", model = "HD 600",
            type = AutoEqDeviceType.OVER_EAR,
            description = "AutoEQ target for Sennheiser HD 600 reference headphones. Sub-bass roll-off correction & treble smoothing.",
            correction15 = listOf(6.0f, 5.0f, 3.5f, 2.0f, 0.5f, 0.0f, -0.5f, 0.0f, 0.0f, 0.5f, -1.0f, 0.0f, 1.5f, -1.0f, -2.0f)
        ),
        AutoEqProfile(
            id = "sennheiser-hd-650", brand = "Sennheiser", model = "HD 650 / HD 6XX",
            type = AutoEqDeviceType.OVER_EAR,
            description = "AutoEQ target for Sennheiser HD 650 / 6XX. Elevates sub-bass and adds air shimmer to dark treble response.",
            correction15 = listOf(6.5f, 5.5f, 4.0f, 2.0f, 0.5f, 0.0f, -0.5f, 0.0f, 0.5f, 1.0f, 0.5f, 1.5f, 2.5f, 1.0f, -1.0f)
        ),
        AutoEqProfile(
            id = "audio-technica-ath-m50x", brand = "Audio-Technica", model = "ATH-M50x",
            type = AutoEqDeviceType.OVER_EAR,
            description = "AutoEQ target for ATH-M50x studio monitors. Reduces boosted bass shelf and smooths a 3-5kHz presence peak.",
            correction15 = listOf(-2.0f, -3.0f, -3.5f, -3.0f, -1.5f, 0.0f, 0.5f, 1.0f, 1.5f, -1.0f, -2.0f, -1.0f, 1.0f, 1.5f, 1.0f)
        ),
        AutoEqProfile(
            id = "beyerdynamic-dt-770-pro", brand = "Beyerdynamic", model = "DT 770 Pro",
            type = AutoEqDeviceType.OVER_EAR,
            description = "AutoEQ target for DT 770 Pro (closed-back). Reins in the well-known bright treble peak around 8-9kHz.",
            correction15 = listOf(1.0f, 0.5f, 0.0f, -0.5f, -0.5f, 0.0f, 0.5f, 0.5f, 0.0f, -1.0f, -2.0f, -1.5f, -3.5f, -3.0f, 0.5f)
        ),
        AutoEqProfile(
            id = "beyerdynamic-dt-990-pro", brand = "Beyerdynamic", model = "DT 990 Pro",
            type = AutoEqDeviceType.OVER_EAR,
            description = "AutoEQ target for DT 990 Pro (open-back). Tames its notably bright, airy treble response for long listening sessions.",
            correction15 = listOf(1.5f, 1.0f, 0.5f, 0.0f, -0.5f, 0.0f, 0.5f, 0.5f, 0.0f, -1.5f, -2.5f, -2.0f, -4.0f, -3.5f, 0.0f)
        ),
        AutoEqProfile(
            id = "bose-quietcomfort-45", brand = "Bose", model = "QuietComfort 45",
            type = AutoEqDeviceType.OVER_EAR,
            description = "AutoEQ target for Bose QC45. Smooths a warm low-mid bump and slightly recessed presence region.",
            correction15 = listOf(-1.5f, -2.5f, -2.5f, -2.0f, -0.5f, 0.0f, 0.5f, 1.0f, 1.5f, 1.0f, 0.5f, 0.5f, 1.0f, 1.5f, 1.0f)
        ),
        AutoEqProfile(
            id = "bose-quietcomfort-ultra", brand = "Bose", model = "QuietComfort Ultra",
            type = AutoEqDeviceType.OVER_EAR,
            description = "AutoEQ target for Bose QC Ultra Headphones. Balanced correction with modest immersive-mode bass compensation.",
            correction15 = listOf(-1.0f, -1.5f, -1.5f, -1.0f, 0.0f, 0.0f, 0.5f, 1.0f, 1.5f, 1.5f, 1.0f, 0.5f, 1.0f, 1.5f, 1.0f)
        ),
        AutoEqProfile(
            id = "samsung-galaxy-buds2-pro", brand = "Samsung", model = "Galaxy Buds2 Pro",
            type = AutoEqDeviceType.IN_EAR,
            description = "AutoEQ target for Galaxy Buds2 Pro. Corrects an elevated bass shelf and slightly forward upper mids.",
            correction15 = listOf(-1.0f, -1.5f, -2.0f, -2.0f, -1.0f, 0.0f, 0.5f, 0.5f, -0.5f, 0.5f, 1.0f, 0.5f, 1.0f, 1.5f, 1.0f)
        ),
        AutoEqProfile(
            id = "anker-soundcore-liberty-4", brand = "Anker", model = "Soundcore Liberty 4",
            type = AutoEqDeviceType.IN_EAR,
            description = "AutoEQ target for Soundcore Liberty 4. Reins in a consumer-tuned bass boost for a more neutral signature.",
            correction15 = listOf(-2.5f, -3.0f, -3.0f, -2.0f, -0.5f, 0.0f, 0.0f, 0.5f, 1.0f, 1.0f, 0.5f, 0.5f, 1.0f, 1.0f, 0.5f)
        ),
        AutoEqProfile(
            id = "generic-phone-speaker", brand = "Generic", model = "Phone / Laptop Speaker",
            type = AutoEqDeviceType.SPEAKERS,
            description = "General-purpose correction for small built-in speakers: compensates for weak bass extension and boxy midrange.",
            correction15 = listOf(4.0f, 3.5f, 3.0f, 2.0f, 0.0f, -1.5f, -1.0f, 0.0f, 0.5f, 1.0f, 1.5f, 1.0f, 1.5f, 1.0f, 0.5f)
        ),
        AutoEqProfile(
            id = "generic-flat-reference", brand = "Generic", model = "Flat / Reference (No Correction)",
            type = AutoEqDeviceType.SPEAKERS,
            description = "No AutoEQ correction applied — use this if your hardware is already well-measured or you prefer to tune by ear.",
            correction15 = List(15) { 0.0f }
        ),
    )

    /** Ported 1:1 from findAutoEqProfileByDeviceName in autoEqDatabase.ts. */
    fun findByDeviceName(deviceName: String): AutoEqProfile {
        val nameLower = deviceName.lowercase()

        fun byId(id: String) = PROFILES.first { it.id == id }

        return when {
            nameLower.contains("pixel buds pro 2") -> byId("google-pixel-buds-pro-2")
            nameLower.contains("pixel buds pro") -> byId("google-pixel-buds-pro")
            nameLower.contains("wh-1000xm5") || nameLower.contains("wh1000xm5") -> byId("sony-wh-1000xm5")
            nameLower.contains("wh-1000xm4") || nameLower.contains("wh1000xm4") -> byId("sony-wh-1000xm4")
            nameLower.contains("wf-1000xm5") || nameLower.contains("wf1000xm5") -> byId("sony-wf-1000xm5")
            nameLower.contains("airpods pro") -> byId("apple-airpods-pro-2")
            nameLower.contains("airpods max") -> byId("apple-airpods-max")
            nameLower.contains("hd 650") || nameLower.contains("hd650") || nameLower.contains("6xx") -> byId("sennheiser-hd-650")
            nameLower.contains("hd 600") || nameLower.contains("hd600") -> byId("sennheiser-hd-600")
            nameLower.contains("m50x") -> byId("audio-technica-ath-m50x")
            nameLower.contains("dt 770") || nameLower.contains("dt770") -> byId("beyerdynamic-dt-770-pro")
            nameLower.contains("dt 990") || nameLower.contains("dt990") -> byId("beyerdynamic-dt-990-pro")
            nameLower.contains("qc45") || nameLower.contains("qc 45") || nameLower.contains("quietcomfort 45") -> byId("bose-quietcomfort-45")
            nameLower.contains("qc ultra") || nameLower.contains("quietcomfort ultra") -> byId("bose-quietcomfort-ultra")
            nameLower.contains("galaxy buds") -> byId("samsung-galaxy-buds2-pro")
            nameLower.contains("soundcore") || nameLower.contains("liberty") -> byId("anker-soundcore-liberty-4")
            nameLower.contains("speaker") || nameLower.contains("phone") -> byId("generic-phone-speaker")
            nameLower.contains("pixel") || nameLower.contains("google") -> byId("google-pixel-buds-pro-2")
            nameLower.contains("sony") -> byId("sony-wh-1000xm5")
            nameLower.contains("apple") || nameLower.contains("airpods") -> byId("apple-airpods-pro-2")
            nameLower.contains("sennheiser") -> byId("sennheiser-hd-600")
            nameLower.contains("bose") -> byId("bose-quietcomfort-ultra")
            else -> byId("generic-flat-reference")
        }
    }
}
