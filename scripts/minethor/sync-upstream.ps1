<#
.SYNOPSIS
  Pulls AngelAuraMC/Amethyst-Android updates into the MineThor fork.

.DESCRIPTION
  1. fetch upstream
  2. fast-forward the mirror branch (v3_openjdk) to upstream/v3_openjdk
  3. dry-run the rebase with `git merge-tree` and list conflicting files
  4. (unless -DryRun) tag a backup, rebase the integration branch onto the mirror
  Never pushes. Never force-anything. On conflict: `git rebase --abort` restores the previous state.

.EXAMPLE
  .\scripts\minethor\sync-upstream.ps1 -DryRun
  .\scripts\minethor\sync-upstream.ps1
#>
[CmdletBinding()]
param(
    [switch]$DryRun,
    [string]$Mirror = 'v3_openjdk',
    [string]$Integration = 'integration/dualscreen',
    [string]$UpstreamRemote = 'upstream',
    [string]$UpstreamUrl = 'https://github.com/AngelAuraMC/Amethyst-Android.git'
)

$ErrorActionPreference = 'Stop'

function Invoke-Git {
    & git @args
    if ($LASTEXITCODE -ne 0) { throw "git $($args -join ' ') failed ($LASTEXITCODE)" }
}
function Get-GitOut {
    $out = & git @args
    if ($LASTEXITCODE -ne 0) { throw "git $($args -join ' ') failed ($LASTEXITCODE)" }
    return $out
}

Set-Location (Get-GitOut rev-parse --show-toplevel)

# 0. preconditions
$remotes = (Get-GitOut remote)
if ($remotes -notcontains $UpstreamRemote) {
    Write-Host "Adding remote '$UpstreamRemote' -> $UpstreamUrl"
    Invoke-Git remote add $UpstreamRemote $UpstreamUrl
}
$url = (Get-GitOut remote get-url $UpstreamRemote)
if ($url -notmatch 'AngelAuraMC/Amethyst-Android') {
    throw "Remote '$UpstreamRemote' points to $url, expected AngelAuraMC/Amethyst-Android"
}
if (-not $DryRun) {
    $dirty = (Get-GitOut status --porcelain --untracked-files=no)
    if ($dirty) { throw "Working tree has tracked changes. Commit or stash first:`n$dirty" }
}

# 1. fetch
Invoke-Git fetch $UpstreamRemote --prune
$upstreamRef = "$UpstreamRemote/$Mirror"

# 2. history sanity: the fork must share history with upstream
& git merge-base $Integration $upstreamRef | Out-Null
if ($LASTEXITCODE -ne 0) {
    throw "'$Integration' and '$upstreamRef' have NO common ancestor (unrelated histories). See docs/GIT_WORKFLOW.md."
}

$behind = [int](Get-GitOut rev-list --count "$Integration..$upstreamRef")
$ahead = [int](Get-GitOut rev-list --count "$upstreamRef..$Integration")
Write-Host "'$Integration' is $ahead commit(s) ahead of and $behind commit(s) behind $upstreamRef."
if ($behind -eq 0) { Write-Host 'Already up to date.'; return }

# 3. dry-run: would the rebase conflict?
$base = (Get-GitOut merge-base $Integration $upstreamRef)
$mt = & git merge-tree --write-tree --name-only --no-messages "--merge-base=$base" $Integration $upstreamRef
$mtExit = $LASTEXITCODE
if ($mtExit -eq 0) {
    Write-Host 'Dry-run: no conflicts expected.'
} else {
    Write-Host 'Dry-run: CONFLICTS in these files (see the integration-points registry in docs/GIT_WORKFLOW.md):'
    $mt | Select-Object -Skip 1 | Where-Object { $_ } | ForEach-Object { Write-Host "  $_" }
}
Write-Host 'Upstream commits we are about to take:'
Invoke-Git log --oneline "$Integration..$upstreamRef" --max-count=30

if ($DryRun) { return }

# 4. mirror fast-forward (never creates merge commits)
$current = (Get-GitOut branch --show-current)
$mirrorExists = (& git rev-parse --verify --quiet "refs/heads/$Mirror")
if ($mirrorExists) {
    Invoke-Git branch -f $Mirror $upstreamRef | Out-Null
} else {
    Invoke-Git branch $Mirror $upstreamRef
}
Write-Host "Mirror '$Mirror' -> $(Get-GitOut rev-parse --short $Mirror)"

# 5. backup tag then rebase
$tag = "backup/pre-sync-$(Get-Date -Format 'yyyyMMdd-HHmmss')"
Invoke-Git tag $tag $Integration
Write-Host "Backup tag: $tag"

if ($current -ne $Integration) { Invoke-Git switch $Integration }
& git rebase $Mirror
if ($LASTEXITCODE -ne 0) {
    Write-Host ''
    Write-Host 'Rebase stopped on conflicts. Resolve, then: git add <files>; git rebase --continue'
    Write-Host "Or give up safely: git rebase --abort  (backup tag: $tag)"
    exit 1
}

Write-Host ''
Write-Host 'Rebase OK. Next:'
Write-Host '  git submodule update --init --recursive'
Write-Host '  .\gradlew.bat :app_pojavlauncher:assembleDebug'
Write-Host "  git push --force-with-lease origin $Integration     # only after the build passes"
Write-Host "  git push origin ${Mirror}                             # keep the origin mirror in sync (fast-forward)"
