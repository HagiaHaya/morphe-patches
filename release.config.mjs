import { readFileSync } from "node:fs";
import createPreset from "conventional-changelog-conventionalcommits";

const presetConfig = {
  types: [
    { type: "fix", section: "🐛 Bug Fixes", hidden: false },
    { type: "feat", section: "✨ New Features", hidden: false },
    { type: "bump", section: "🚀 Updated App Support", hidden: false },
    { type: "perf", section: "🔧 Improvements", hidden: false },
    { type: "build", hidden: true },
  ],
};

/**
 * Names of the apps of the bundle, as their compatibility declares them.
 */
const appNames = (() => {
  try {
    const constants = readFileSync(
      new URL("patches/src/main/kotlin/app/template/patches/shared/Constants.kt", import.meta.url),
      "utf8",
    );
    return [...constants.matchAll(/Compatibility\(\s*name\s*=\s*"([^"]+)"/g)].map((match) => match[1]);
  } catch {
    return [];
  }
})();

const mentionOf = (app) => {
  const name = app.replace(/[.*+?^${}()|[\]\\]/g, "\\$&");
  return new RegExp(`(?<![\\p{L}\\p{N}])${name}(?![\\p{L}\\p{N}])`, "u");
};

const suffixOf = (app) =>
  new RegExp(`\\s+(?:for|in|to)\\s+${app.replace(/[.*+?^${}()|[\]\\]/g, "\\$&")}$`);

/**
 * Morphe Manager shows the changes of a release for an app, and offers to patch it again, only
 * when a change is scoped to the app or names it. A change that names a patch the apps share,
 * such as `GmsCore support`, counts for all of them. So a commit without a scope that names
 * exactly one app is scoped to it, and its subject loses a trailing "for", "in" or "to" the app.
 */
const scopeToApp = (commit) => {
  if (commit.scope || typeof commit.subject !== "string") return commit;
  const named = appNames.filter((app) => mentionOf(app).test(commit.subject));
  if (named.length !== 1) return commit;
  const [app] = named;
  return { ...commit, scope: app, subject: commit.subject.replace(suffixOf(app), "") };
};

const presetWriter = createPreset(presetConfig).writer;

export default {
  branches: ["main", { name: "dev", prerelease: true }],
  plugins: [
    [
      "@semantic-release/commit-analyzer",
      {
        releaseRules: [
          { type: "fix", release: "patch" },
          { type: "feat", release: "minor" },
          { type: "bump", release: "patch" },
          { type: "perf", release: "patch" },
          { type: "build", scope: "Needs bump", release: "patch" },
        ],
      },
    ],
    [
      "@semantic-release/release-notes-generator",
      {
        preset: "conventionalcommits",
        presetConfig,
        writerOpts: {
          transform: (commit, context) => presetWriter.transform(scopeToApp(commit), context),
        },
      },
    ],
    [
      "@MorpheApp/changelog",
      {
        releaseJson: {
          path: "patches-bundle.json",
          downloadUrlTemplate:
            "https://github.com/${owner}/${repo}/releases/download/v${version}/patches-${version}.mpp",
          signatureUrlTemplate: "",
        },
      },
    ],
    "gradle-semantic-release-plugin",
    [
      "@semantic-release/exec",
      {
        prepareCmd:
          "./gradlew generatePatchesList" +
          " && jq '.version=\"${nextRelease.version}\"' patches-list.json > patches-list.json.tmp" +
          " && mv patches-list.json.tmp patches-list.json" +
          " && python3 .github/scripts/generate_patches_readme.py $GITHUB_REPOSITORY $GITHUB_REF_NAME patches-list.json README.md",
      },
    ],
    [
      "@semantic-release/git",
      {
        assets: ["CHANGELOG.md", "gradle.properties", "patches-bundle.json", "patches-list.json", "README.md"],
        message: "chore: Release v${nextRelease.version} [skip ci]\n\n${nextRelease.notes}",
      },
    ],
    [
      "@semantic-release/github",
      {
        assets: [{ path: "patches/build/libs/patches-!(*sources*|*javadoc*).mpp?(.asc)" }],
        successComment: false,
      },
    ],
    [
      "@cleyrop-org/semantic-release-backmerge",
      {
        backmergeBranches: [{ from: "main", to: "dev" }],
        clearWorkspace: true,
      },
    ],
  ],
};
