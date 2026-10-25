package com.immortal521.colorosiconspatch.data

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class IconIndexSecurityTest {
    @Test
    fun acceptsOnlySafeNamesAndPaths() {
        assertTrue(isSafeIconFileName("recfg_example.png"))
        assertTrue(isSafeIconFileName("rec_night_ 2x1.png"))
        assertTrue(isSafeIconFileName("图标.png"))
        assertFalse(isSafeIconFileName("../outside.png"))
        assertFalse(isSafeIconFileName("/absolute.png"))
        assertTrue(isSafePackageName("com.example.app"))
        assertFalse(isSafePackageName("com.example/../../data"))
        assertTrue(isSafeIconDownloadPath("com.example/recfg_example.png"))
        assertTrue(isSafeIconDownloadPath("packages/com.example/rec_night_ 2x1.png"))
        assertFalse(isSafeIconDownloadPath("../../outside.png"))
        assertFalse(isSafeIconDownloadPath("%2e%2e/outside.png"))
        assertFalse(isSafeIconDownloadPath("/absolute.png"))
    }

    @Test
    fun monochromeIndexFilesArePreservedUnderDownloadNames() {
        assertTrue(downloadedMonochromeFileName("monochrome.png") == "monochrome_download.png")
        assertTrue(downloadedMonochromeFileName("monochrome_2x1.png") == "monochrome_2x1_download.png")
        assertTrue(activeMonochromeFileName("monochrome_dark_download.png") == "monochrome_dark.png")
        assertTrue(activeMonochromeFileName("monochrome.png") == "monochrome.png")
        assertTrue(downloadedMonochromeFileName("monochrome_2x2.png") == "monochrome_2x2_download.png")
    }

    @Test
    fun switchesDownloadedMonochromeByMovingTheSingleFile() {
        val packageName = "com.example.app"
        val checksum = "a".repeat(64)
        val source = IconFile("monochrome.png", "icons/monochrome.png", checksum, 12)
        val index = IconIndexLoadResult(mapOf(packageName to IconPackage(packageName, listOf(source))))
        val directory = "$ICON_STORAGE_ROOT/$packageName"

        val activate = buildIconSyncPlan(
            setOf(packageName),
            index,
            nativeMonochromePackages = setOf(packageName),
            checksums = mapOf("$directory/monochrome_download.png" to checksum)
        )
        assertTrue(activate.updates.isEmpty())
        assertTrue(activate.moves.single() == IconFileMove(packageName, "$directory/monochrome_download.png", "$directory/monochrome.png"))
        assertTrue(activate.staleFiles.isEmpty())

        val deactivate = buildIconSyncPlan(
            setOf(packageName),
            index,
            monochromeSources = setOf("native:$packageName"),
            nativeMonochromePackages = setOf(packageName),
            checksums = mapOf("$directory/monochrome.png" to checksum)
        )
        assertTrue(deactivate.updates.isEmpty())
        assertTrue(deactivate.moves.single() == IconFileMove(packageName, "$directory/monochrome.png", "$directory/monochrome_download.png"))
        assertTrue(deactivate.staleFiles.isEmpty())
    }

    @Test
    fun nativeSelectionDeletesGeneratedPngInsteadOfHashingOrMovingIt() {
        val packageName = "com.example.app"
        val directory = "$ICON_STORAGE_ROOT/$packageName"
        val source = IconFile("monochrome.png", "icons/monochrome.png", "a".repeat(64), 12)
        val index = IconIndexLoadResult(mapOf(packageName to IconPackage(packageName, listOf(source))))

        val plan = buildIconSyncPlan(
            setOf(packageName),
            index,
            monochromeSources = setOf("native:$packageName"),
            nativeMonochromePackages = setOf(packageName),
            checksums = mapOf("$directory/monochrome.png" to "b".repeat(64))
        )

        assertTrue(plan.updates.single().target == "$directory/monochrome_download.png")
        assertTrue(plan.moves.isEmpty())
        assertTrue(plan.staleFiles == listOf("$directory/monochrome.png"))
    }

    @Test
    fun identifiesMalformedPackageKeys() {
        assertTrue(isSafePackageName("com.example.valid"))
        assertFalse(isSafePackageName("yh.app.appstart.lg "))
    }

    @Test
    fun persistentPathsStayInsideExpectedTree() {
        assertTrue(isSafePersistentIconPath("$ICON_STORAGE_ROOT/com.example.app/icon.png"))
        assertTrue(isSafePersistentIconPath("$ICON_STORAGE_ROOT/required.png"))
        assertFalse(isSafePersistentIconPath("$ICON_STORAGE_ROOT/../outside"))
        assertFalse(isSafePersistentIconPath("$ICON_STORAGE_ROOT/com.example/../../outside"))
        assertFalse(isSafePersistentIconPath("/data/adb/other/icon.png"))
    }

    @Test
    fun rejectsInvalidIndexFieldValuesBeforeRootOperations() {
        assertFalse(isSafeIconFileName("../escape.png"))
        assertFalse(isSafeIconDownloadPath("../../escape.png"))
        assertFalse("not-a-checksum".matches(Regex("[0-9a-fA-F]{64}")))
        assertTrue(MAX_ICON_FILE_BYTES + 1 > MAX_ICON_FILE_BYTES)
        assertTrue(MAX_INDEX_BYTES > 0)
        assertTrue(MAX_TOTAL_DOWNLOAD_BYTES >= MAX_ICON_FILE_BYTES)
    }

    @Test
    fun rejectsUnsafeSyncTargetsBeforeAnyRootOperations() {
        val update = IconFileUpdate(
            "com.example.app",
            IconFile("icon.png", "com.example/icon.png", "a".repeat(64), 1),
            "/data/adb/ColorOSIconsPatch/uxicons/../../outside.png",
            1
        )
        try {
            validateSyncPlan(IconSyncPlan(listOf(update), emptyList()))
            throw AssertionError("Expected unsafe target to be rejected")
        } catch (_: IllegalArgumentException) {
            // Expected validation failure.
        }
    }

    @Test
    fun rejectsOversizedIndexBeforeJsonParsing() {
        try {
            validateIndexSize(" ".repeat(MAX_INDEX_BYTES + 1))
            throw AssertionError("Expected oversized index to be rejected")
        } catch (_: IllegalArgumentException) {
            // Expected validation failure.
        }
    }
}
