# 🧩 HagiaHaya Patches

Patches for use with [Morphe](https://morphe.software).

## ❓ About

Patches for the [Wykop.pl](https://wykop.pl) app, such as removing the ads shown on the website inside the app.

### How to use these patches

Click here to add these patches to Morphe: https://morphe.software/add-source?github=HagiaHaya/morphe-patches

The **GmsCore support** patch makes signing in with Google work in the patched app, which
needs [MicroG-RE](https://github.com/MorpheApp/MicroG-RE/releases/latest) installed.
Push notifications of the patched app also go through MicroG-RE.

## 🩹 Patches list

<!-- PATCHES_START EXPANDED -->
> **[v1.1.1](https://github.com/HagiaHaya/morphe-patches/releases/tag/v1.1.1)**&nbsp;&nbsp;•&nbsp;&nbsp;`main`&nbsp;&nbsp;•&nbsp;&nbsp;2 patches total
<details open>
<summary>📦 Wykop.pl&nbsp;&nbsp;•&nbsp;&nbsp;2 patches</summary>
<br>

**🎯 Supported versions:**

| 5.1.3 |
| :---: |

| 💊&nbsp;Patch | 📜&nbsp;Description | ⚙️&nbsp;Options |
|----------|----------------|-----------|
| [GmsCore support](#gmscore-support) | Allows signing in with Google by using MicroG-RE instead of Google Play services. |  |
| [Remove ads](#remove-ads) | Hides ads and blocks ad networks on the website shown by the app. |  |

</details>

<!-- PATCHES_END -->

### 🛠️ Building locally

- Run `./gradlew buildAndroid`
- The built patches .mpp file is found in `patches/build/libs/patches-*.mpp`
- Patch the mpp file using [Morphe-Desktop](https://github.com/MorpheApp/morphe-desktop)
  like any other patch bundle.

See the [Morphe documentation](https://github.com/MorpheApp/morphe-documentation) for more information.

## 📜 License

HagiaHaya Patches are licensed under the [GNU General Public License v3.0](LICENSE)
