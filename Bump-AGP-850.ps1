
param([string]$RepoRoot = ".")

$catalog = Join-Path $RepoRoot "gradle\libs.versions.toml"
if (!(Test-Path $catalog)) {
  Write-Host "Catalog not found: $catalog" ; exit 1
}
$backup = "$catalog.bak-$(Get-Date -Format 'yyyyMMdd-HHmmss')"
Copy-Item -LiteralPath $catalog -Destination $backup -Force
Write-Host "Backup created: $backup"

# Replace agp version to 8.5.0
$content = Get-Content -LiteralPath $catalog -Raw
if ($content -match '(^|\n)\s*agp\s*=\s*"[0-9.]+?"') {
  $content = [regex]::Replace($content, '(^|\n)\s*agp\s*=\s*"[0-9.]+?"', "`$1agp = `"8.5.0`"")
} else {
  # ensure [versions] exists and add agp if missing
  if ($content -match '^\[versions\]') {
    $content = $content -replace '^\[versions\]\s*', "[versions]`r`nagp = `"8.5.0`"`r`n"
  } else {
    $content = $content.TrimEnd() + "`r`n`r`n[versions]`r`nagp = `"8.5.0`"`r`n"
  }
}
Set-Content -LiteralPath $catalog -Value $content -Encoding UTF8
Write-Host "Set AGP version to 8.5.0 in $catalog"

# Try clean build
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
