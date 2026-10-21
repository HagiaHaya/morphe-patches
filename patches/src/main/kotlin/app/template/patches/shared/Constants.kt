package app.template.patches.shared

import app.morphe.patcher.patch.ApkFileType
import app.morphe.patcher.patch.AppTarget
import app.morphe.patcher.patch.Compatibility

object Constants {
    val COMPATIBILITY_WYKOP = Compatibility(
        name = "Wykop.pl", // App name as it appears in the Android launcher.
        packageName = "pl.wykop.droid",
        apkFileType = ApkFileType.APK,
        // The launcher icon background is white, so use the Wykop brand blue instead.
        appIconColor = 0x3674A1,
        targets = listOf(
            // Version the patch was developed and verified against.
            AppTarget(
                version = "5.1.3"
            ),
            // The patch hooks the flutter_inappwebview plugin, whose class names are not
            // obfuscated, so it is expected to keep working with future app versions.
            AppTarget(
                version = null,
                isExperimental = true
            )
        )
    )

    val COMPATIBILITY_PEPPER = Compatibility(
        name = "Pepper", // App name as it appears in the Android launcher.
        packageName = "com.tippingcanoe.pepperpl",
        // Not available on APKMirror or Uptodown, APKPure distributes it as a split XAPK.
        apkFileType = ApkFileType.XAPK,
        appIconColor = 0xFF3400,
        targets = listOf(
            // Version the patches were developed and verified against.
            AppTarget(
                version = "8.20.00"
            )
        )
    )
}
