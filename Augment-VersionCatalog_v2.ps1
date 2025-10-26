
param([string]$RepoRoot = ".")

$catalog = Join-Path $RepoRoot "gradle\libs.versions.toml"
if (!(Test-Path $catalog)) { Write-Host "Catalog not found: $catalog"; exit 1 }

# Backup
$backup = "$catalog.bak-$(Get-Date -Format 'yyyyMMdd-HHmmss')"
Copy-Item -LiteralPath $catalog -Destination $backup -Force
Write-Host "Backup created: $backup"

# Read file
$content = Get-Content -LiteralPath $catalog -Raw

function Ensure-Block {
  param([string]$txt, [string]$header)
  if ($txt -match "^\[$header\]" ) { return $txt }
  else { return ($txt.TrimEnd() + "`r`n`r`n[$header]`r`n") }
}

# Ensure [versions] and [libraries]
$content = Ensure-Block $content "versions"
$content = Ensure-Block $content "libraries"

# Ensure version keys (only add if missing)
function Ensure-VersionKey {
  param([string]$txt, [string]$key, [string]$val)
  if ($txt -match "(?m)^\s*$key\s*=") { return $txt }
  $txt = $txt -replace '(\[versions\]\s*)', "`$1$key = `"$val`"`r`n"
  return $txt
}

$content = Ensure-VersionKey $content "composeBom" "2024.10.01"
$content = Ensure-VersionKey $content "androidxCore" "1.13.1"
$content = Ensure-VersionKey $content "appcompat" "1.7.0"
$content = Ensure-VersionKey $content "androidxLifecycle" "2.8.6"
$content = Ensure-VersionKey $content "customviewPooling" "1.0.0"

# Helper to append library entry if missing
function Ensure-LibraryKey {
  param([string]$txt, [string]$alias, [string]$module, [string]$versionRef)

  if ($txt -match "(?m)^\s*$alias\s*=") { return $txt }

  $entry = ""
  if ([string]::IsNullOrWhiteSpace($versionRef)) {
    $entry = "$alias = { module = `"$module`" }"
  } else {
    $entry = "$alias = { module = `"$module`", version.ref = `"$versionRef`" }"
  }

  # Append under [libraries]
  $txt = $txt -replace '(\[libraries\]\s*)', "`$1$entry`r`n"
  return $txt
}

# Compose BOM entry
if ($content -notmatch '(?m)^\s*androidx\.compose\.bom\s*=') {
  $content = $content -replace '(\[libraries\]\s*)', "`$1androidx.compose.bom = { module = `"androidx.compose:compose-bom`", version.ref = `"composeBom`" }`r`n"
}

# Libraries
$content = Ensure-LibraryKey $content "androidx.compose.ui.graphics" "androidx.compose.ui:ui-graphics" ""
$content = Ensure-LibraryKey $content "androidx.compose.ui.test.manifest" "androidx.compose.ui:ui-test-manifest" ""

$content = Ensure-LibraryKey $content "androidx.core.ktx" "androidx.core:core-ktx" "androidxCore"
$content = Ensure-LibraryKey $content "androidx.appcompat" "androidx.appcompat:appcompat" "appcompat"
$content = Ensure-LibraryKey $content "androidx.customview.poolingcontainer" "androidx.customview:customview-poolingcontainer" "customviewPooling"

$content = Ensure-LibraryKey $content "androidx.lifecycle.runtime.compose" "androidx.lifecycle:lifecycle-runtime-compose" "androidxLifecycle"
$content = Ensure-LibraryKey $content "androidx.lifecycle.viewmodel.compose" "androidx.lifecycle:lifecycle-viewmodel-compose" "androidxLifecycle"

# Save
Set-Content -LiteralPath $catalog -Value $content -Encoding UTF8
Write-Host "Updated catalog with missing aliases."

# Build
Push-Location $RepoRoot
try {
  if (Test-Path ".\gradlew.bat") {
    ./gradlew --stop
    ./gradlew clean
    ./gradlew :app:assembleDebug
  } else {
    Write-Host "gradlew not found in RepoRoot; skipped build."
  }
} finally {
  Pop-Location
}
