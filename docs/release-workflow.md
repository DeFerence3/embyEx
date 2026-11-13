# Development and release workflow

## Branches

| Branch | Role | Moves by |
| --- | --- | --- |
| `dev` (default) | Integration and beta branch. Every beta is tagged here. | Squash-merged PRs; small owner commits |
| `master` | Exactly what last shipped as stable. The install scripts are served from here. | **Promote** workflow (fast-forward) or hotfix PRs |
| `feat/*`, `fix/*` | Short-lived work branches from `dev` | You |
| `hotfix/*` | Urgent fixes for the current stable release, branched from `master` | You |

`master` is always an ancestor of `dev`, so promoting is always a fast-forward.

## Tags

- `vMAJOR.MINOR.PATCH-beta.N` (N = 1–99) is a beta. It must be on `dev`, and it's published as a GitHub pre-release.
- `vMAJOR.MINOR.PATCH` is a stable release. It must be on `master`.

`build-release.yml` refuses a tag that's on the wrong branch before it builds anything. Tags are the only version source; Android `versionCode` puts each beta between the previous release and its own.

## Day to day

1. Branch from `dev`, then open a PR into `dev`.
2. The **CI** workflow must pass before you merge. Branch rulesets require its `CI passed` job, which covers:
   - workflow lint
   - common, JVM and Android host tests, desktop tests, and Android lint for every module
   - an unsigned Android release build, to catch R8 and resource-shrinking failures before a tag exists
   - desktop builds and tests on Linux and Windows

   iOS isn't supported for now, so CI doesn't build, test or lint it. That's why CI runs explicit tasks instead of `./gradlew check`, which also compiles the iOS targets.
3. Squash-merge.

## Beta

Betas are tagged automatically. After **CI** passes for a push to `dev`, the **Auto beta** workflow counts the commits on `dev` since the previous tag.

- Squash-merged PRs and direct commits count once each.
- Merges of `master` into `dev` after a hotfix don't count.

Once the count reaches the `BETA_COMMIT_THRESHOLD` repository variable (**Settings → Secrets and variables → Actions → Variables**), the workflow tags that commit with the next beta. Set the variable to `0` to turn automatic betas off. If it's missing or not a whole number, the workflow fails.

| Highest existing tag | Next automatic beta |
| --- | --- |
| `v1.6.0-beta.2` | `v1.6.0-beta.3` |
| `v1.6.0` (stable) | `v1.7.0-beta.1` |

To ship a beta now without waiting for the threshold, run **Actions → Auto beta → Run workflow**. It tags the tip of `dev` with the next beta number.

- It still requires CI to have passed on that commit. Tick **skip_ci_check** to tag anyway.
- It fails if the tip of `dev` is already tagged.

For any other bump, such as a major version or a patch beta, tag it by hand. Automatic numbering continues from there, and a manual tag also resets the commit count:

```sh
git switch dev && git pull
git tag v2.0.0-beta.1
git push origin v2.0.0-beta.1
```

Either way, the release workflow builds every platform and publishes a pre-release. Users who opted into beta updates are offered it. Only the newest 7 beta releases are kept; their tags stay.

## Stable

Run **Actions → Promote beta to stable → Run workflow** with the beta tag to ship, for example `v1.6.0-beta.3`. The workflow:

1. checks that the beta is on `dev`, that its pre-release was published, that `v1.6.0` doesn't exist yet, and that `master` can fast-forward;
2. fast-forwards `master` to the beta's commit;
3. tags that commit `v1.6.0`, which builds and publishes the stable release.

The stable build is made from the same commit beta testers ran. Its release notes cover everything since the previous stable release.

## Hotfix

```sh
git switch -c hotfix/crash-on-start origin/master
# fix, then open a PR into master and squash-merge it
git switch master && git pull
git tag v1.6.1 && git push origin v1.6.1
git switch dev && git pull && git merge master && git push   # keeps master an ancestor of dev
```

If you skip the back-merge, the next promotion fails and tells you to do it.

## Secrets and settings

- `BETA_COMMIT_THRESHOLD` (repository variable) is how many commits on `dev` since the previous tag trigger an automatic beta; `0` turns automatic betas off.
- `PROMOTER_PAT` needs permission to push code and workflow files: the `repo` and `workflow` scopes for a classic token, or Contents and Workflows read/write for a fine-grained one. It also needs a branch-ruleset bypass on `master`. Without the workflow permission, GitHub rejects Promote's push whenever the promoted commits change `.github/workflows/`. Promote, Auto beta and the release use it because pushes made with `GITHUB_TOKEN` don't start other workflows.
- Rulesets:
  - `master`: no force push or deletion, linear history, owner bypass.
  - `dev`: no force push or deletion, `CI passed` required on PRs, owner bypass.
  - `v*` tags: no update or deletion.
