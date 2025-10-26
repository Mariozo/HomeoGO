
param([string]$RepoRoot = ".")

$catalog = Join-Path $RepoRoot "gradle\libs.versions.toml"
if (!(Test-Path $catalog)) {
  Write-Host "Catalog not found: $catalog" ; exit 1
}

$backup = "$catalog.bak-$(Get-Date -Format 'yyyyMMdd-HHmmss')"
Copy-Item -LiteralPath $catalog -Destination $backup -Force
Write-Host "Backup created: $backup"

$content = Get-Content -LiteralPath $catalog -Raw

function Ensure-VersionsBlock {
  param([string]$txt)
  if ($txt -notmatch '^\[versions\]') {
    $txt = $txt.TrimEnd() + "`r`n`r`n[versions]`r`n"
  }
  return $txt
}

function Ensure-Version {
  param([string]$txt, [string]$key, [string]$value)
  if ($txt -match "(?m)^\s*$key\s*=") {
    # don't change existing value
    return $txt
  } else {
    return $txt -replace '(\[versions\]\s*)', "`$1$key = `"$value`"`r`n"
  }
}

function Ensure-Library {
  param([string]$txt, [string]$alias, [string]$module, [string]$versionRef)
  if ($txt -match "(?m)^\s*$alias\s*=") {
    return $txt
  } else {
    return $txt.TrimEnd() + "`r`n$alias = { module = `"$module`"`" + ($(if ($versionRef) { ", version.ref = `"$versionRef`"" } else { "" })) + " }`r`n"
  }
}

function Ensure-ComposeBom {
  param([string]$txt, [string]$versionRef)
  if ($txt -match "(?m)^\s*androidx\.compose\.bom\s*=") {
    return $txt
  } else {
    return $txt.TrimEnd() + "`r`nandroidx.compose.bom = { module = `"androidx.compose:compose-bom`", version.ref = `"$versionRef`" }`r`n"
  }
}

# Ensure blocks/versions
$content = Ensure-VersionsBlock $content
$content = Ensure-Version $content "composeBom" "2024.10.01"
$content = Ensure-Version $content "androidxCore" "1.13.1"
$content = Ensure-Version $content "appcompat" "1.7.0"
$content = Ensure-Version $content "androidxLifecycle" "2.8.6"
$content = Ensure-Version $content "customviewPooling" "1.0.0"

# Ensure libraries
# Compose BOM and modules (graphics & test-manifest use BOM)
$content = Ensure-ComposeBom $content "composeBom"
$content = Ensure-Library $content "androidx.compose.ui.graphics" "androidx.compose.ui:ui-graphics" ""
$content = Ensure-Library $content "androidx.compose.ui.test.manifest" "androidx.compose.ui:ui-test-manifest" ""

# Core/Appcompat/Customview
$content = Ensure-Library $content "androidx.core.ktx" "androidx.core:core-ktx" "androidxCore"
$content = Ensure-Library $content "androidx.appcompat" "androidx.appcompat:appcompat" "appcompat"
$content = Ensure-Library $content "androidx.customview.poolingcontainer" "androidx.customview:customview-poolingcontainer" "customviewPooling"

# Lifecycle compose
$content = Ensure-Library $content "androidx.lifecycle.runtime.compose" "androidx.lifecycle:lifecycle-runtime-compose" "androidxLifecycle"
$content = Ensure-Library $content "androidx.lifecycle.viewmodel.compose" "androidx.lifecycle:lifecycle-viewmodel-compose" "androidxLifecycle"

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
