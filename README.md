# 🧩 HagiaHaya Patches

Patches for use with [Morphe](https://morphe.software).

## ❓ About

Patches for the [Wykop.pl](https://wykop.pl), [Pepper.pl](https://www.pepper.pl) and [Slack](https://slack.com) apps, such as removing ads, sanitizing sharing and deal links, and keeping Slack messages older than the free plan shows.

### How to use these patches

Click here to add these patches to Morphe: https://morphe.software/add-source?github=HagiaHaya/morphe-patches

The **GmsCore support** patch makes signing in with Google work in the patched app, which
needs [MicroG-RE](https://github.com/MorpheApp/MicroG-RE/releases/latest) installed.
Push notifications of the patched app also go through MicroG-RE.

## 🩹 Patches list

<!-- PATCHES_START EXPANDED -->
> **[v1.5.0-dev.7](https://github.com/HagiaHaya/morphe-patches/releases/tag/v1.5.0-dev.7)**&nbsp;&nbsp;•&nbsp;&nbsp;`dev`&nbsp;&nbsp;•&nbsp;&nbsp;31 patches total
<details open>
<summary>📦 Pepper&nbsp;&nbsp;•&nbsp;&nbsp;15 patches</summary>
<br>

**🎯 Supported versions:**

| 8.20.00 |
| :---: |

| 💊&nbsp;Patch | 📜&nbsp;Description | ⚙️&nbsp;Options |
|----------|----------------|-----------|
| [Add deals to calendar](#add-deals-to-calendar) | Adds "Add to calendar" to the menu of a deal, which creates an event when the deal starts, or before it expires. |  |
| [Block tracking](#block-tracking) | Blocks the analytics of Pepper, such as reports of the deals visited and shared and tracking pixels, Firebase Analytics, Facebook app events, Adjust, Iterable and the start of the SDKs of ads. Adds a setting to send a random device ID instead of one derived from the Android ID. |  |
| [Catch Flamedeer](#catch-flamedeer) | Adds a setting to get a notification when a Flamedeer ("Ognisty Renifer") of the Flamedeer hunt shows up, or to catch it at once by opening its prize screen. Flamedeers found while the app is in the background get a notification instead of being missed. Optionally looks for Flamedeers while the app is idle or in the background, and links the collection of Flamedeers in the settings. |  |
| [Enable debug menu](#enable-debug-menu) | Adds a setting to show the hidden debug menu of the developers of Pepper at the top right of the home screen. Some of its actions can break the app. |  |
| [Filter comments](#filter-comments) | Adds a setting to hide comments, and the replies to them, that contain certain words. |  |
| [Filter deals](#filter-deals) | Adds settings to hide deals and discussions from all lists by keyword, store, member, category, highest price or lowest temperature. |  |
| [GmsCore support](#gmscore-support) | Allows signing in with Google by using MicroG-RE instead of Google Play services. |  |
| [Price error alerts](#price-error-alerts) | Adds a setting to get a notification of new deals with a discount of at least 70 to 95%, such as price errors. Checks every 10 minutes, also in the background. |  |
| [Remove ads](#remove-ads) | Removes the ads shown between deals and on the deal pages. |  |
| [Sanitize deal links](#sanitize-deal-links) | Opens the store page of a deal directly, instead of through redirects that track the click and add affiliate links. |  |
| [Sanitize sharing links](#sanitize-sharing-links) | Shares the direct link to deals and discussions, instead of a link that redirects through a tracking service. |  |
| [Search comments](#search-comments) | Adds "Search comments" to the menu of a deal, which loads all its comments and replies, and finds the ones with the words entered. |  |
| [Show price with shipping](#show-price-with-shipping) | Adds a setting to show the price with shipping next to the shipping costs of deals, such as "+ 9,99zł = 59,98zł". |  |
| [Spoof contributor level](#spoof-contributor-level) | Adds a setting to show Silver, Gold or Platinum in the app, such as on the profile, instead of the contributor level earned. It unlocks the app icons of the level, but rewards granted by Pepper, such as more deal stats, are not unlocked. |  |
| [Unlock custom app icons](#unlock-custom-app-icons) | Unlocks all app icons regardless of the contributor level, and offers the seasonal icons, such as the Black Friday one, all year round. |  |

</details>

<details open>
<summary>📦 Slack&nbsp;&nbsp;•&nbsp;&nbsp;12 patches</summary>
<br>

**🎯 Supported versions:**

| 26.10.20.0 |
| :---: |

| 💊&nbsp;Patch | 📜&nbsp;Description | ⚙️&nbsp;Options |
|----------|----------------|-----------|
| [Anonymize file names](#anonymize-file-names) | Adds an option to upload files under a random name, keeping the extension, so the name does not reveal anything about the file or the device. |  |
| [Block telemetry](#block-telemetry) | Adds an option to block the usage tracking, performance traces and crash reports Slack sends to its servers and to Sentry. |  |
| [Feature flags](#feature-flags) | Adds a screen to the patch settings to turn the feature flags of Slack on or off, which can enable features and experiments Slack has not released yet. |  |
| [Fix Google sign-in](#fix-google-sign-in) | Fixes signing in with Google staying on a loading button after choosing the account. |  |
| [GmsCore support](#gmscore-support) | Allows signing in with Google and receiving notifications by using MicroG-RE instead of Google Play services. |  |
| [Hide history limit banner](#hide-history-limit-banner) | Hides the banner at the start of conversations that says messages older than 90 days are hidden on the free plan and offers a Pro trial. |  |
| [Keep message history](#keep-message-history) | Keeps a copy of the messages Slack loads on the device, and shows the copies where the server hides older messages, such as after 90 days on the free plan. Only the messages loaded while the patched app is used are kept. Long press the app icon and choose "Search archive" to search the copies, or "Message archive" to turn it off, export or delete them. |  |
| [Message logger](#message-logger) | Adds options to keep the messages others delete, marked as deleted, and to show the earlier versions of the messages others edit below them. Patch settings list them in a message log. |  |
| [Nicknames](#nicknames) | Adds a screen to the patch settings to give other members nicknames, which Slack shows instead of their names, only on this device. |  |
| [Sanitize links](#sanitize-links) | Adds an option to remove tracking parameters, such as utm_source or fbclid, from the links in the messages you send. |  |
| [Silent typing](#silent-typing) | Adds an option to not show others that you are typing, while still showing when they are. |  |
| [Standard Markdown](#standard-markdown) | Adds an option to format messages as you type standard Markdown, such as **bold**, *italic*, ~~strikethrough~~ and [text](link), instead of Slack markup. |  |

</details>

<details open>
<summary>📦 Wykop.pl&nbsp;&nbsp;•&nbsp;&nbsp;4 patches</summary>
<br>

**🎯 Supported versions:**

| 5.1.3 |
| :---: |

| 💊&nbsp;Patch | 📜&nbsp;Description | ⚙️&nbsp;Options |
|----------|----------------|-----------|
| [Fix sidebar layout](#fix-sidebar-layout) | Keeps the sidebar of the website shown by the app clear of the status bar and the navigation bar. |  |
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
