# 🧩 HagiaHaya Patches

Patches for use with [Morphe](https://morphe.software).

## ❓ About

Patches for the [Wykop.pl](https://wykop.pl) and [Pepper.pl](https://www.pepper.pl) apps, such as removing ads and sanitizing sharing and deal links.

### How to use these patches

Click here to add these patches to Morphe: https://morphe.software/add-source?github=HagiaHaya/morphe-patches

The **GmsCore support** patch makes signing in with Google work in the patched app, which
needs [MicroG-RE](https://github.com/MorpheApp/MicroG-RE/releases/latest) installed.
Push notifications of the patched app also go through MicroG-RE.

## 🩹 Patches list

<!-- PATCHES_START EXPANDED -->
> **[v1.3.0](https://github.com/HagiaHaya/morphe-patches/releases/tag/v1.3.0)**&nbsp;&nbsp;•&nbsp;&nbsp;`main`&nbsp;&nbsp;•&nbsp;&nbsp;7 patches total
<details open>
<summary>📦 Pepper&nbsp;&nbsp;•&nbsp;&nbsp;4 patches</summary>
<br>

**🎯 Supported versions:**

| 8.20.00 |
| :---: |

| 💊&nbsp;Patch | 📜&nbsp;Description | ⚙️&nbsp;Options |
|----------|----------------|-----------|
| [GmsCore support](#gmscore-support) | Allows signing in with Google by using MicroG-RE instead of Google Play services. |  |
| [Remove ads](#remove-ads) | Removes the ads shown between deals and on the deal pages. |  |
| [Sanitize deal links](#sanitize-deal-links) | Opens the store page of a deal directly, instead of through redirects that track the click and add affiliate links. |  |
| [Sanitize sharing links](#sanitize-sharing-links) | Shares the direct link to deals and discussions, instead of a link that redirects through a tracking service. |  |

</details>

<details open>
<summary>📦 Wykop.pl&nbsp;&nbsp;•&nbsp;&nbsp;3 patches</summary>
<br>

**🎯 Supported versions:**

| 5.1.3 |
| :---: |

| 💊&nbsp;Patch | 📜&nbsp;Description | ⚙️&nbsp;Options |
|----------|----------------|-----------|
| [GmsCore support](#gmscore-support) | Allows signing in with Google by using MicroG-RE instead of Google Play services. |  |
| [Hide newsletter prompt](#hide-newsletter-prompt) | Hides the prompt to sign up for the newsletter on the website shown by the app. |  |
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
