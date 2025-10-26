
param([string]$RepoRoot = ".")

$catalog = Join-Path $RepoRoot "gradle\libs.versions.toml"
$backup = "$catalog.bak-$(Get-Date -Format 'yyyyMMdd-HHmmss')"
$baseline = Join-Path $PSScriptRoot "baseline_libs.versions.toml"

if (!(Test-Path $catalog)) {
  Write-Host "Catalog not found at $catalog"
  exit 1
}

if (!(Test-Path $baseline)) {
  Write-Host "Baseline file not found next to this script: $baseline"
  exit 1
}

Copy-Item -LiteralPath $catalog -Destination $backup -Force
Write-Host "Backup created: $backup"

Copy-Item -LiteralPath $baseline -Destination $catalog -Force
Write-Host "Replaced catalog with baseline."

# Try a clean build
if (Test-Path (Join-Path $RepoRoot "gradlew.bat")) {
  Push-Location $RepoRoot
  try {
    ./gradlew --stop
    ./gradlew clean
    ./gradlew :app:assembleDebug
  } finally {
    Pop-Location
  }
} else {
  Write-Host "gradlew not found in RepoRoot; skipped build."
}
