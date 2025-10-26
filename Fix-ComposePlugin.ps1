
param(
    [Parameter(Mandatory=$false)]
    [string]$RepoRoot = "."
)

function Backup-File($Path) {
    if (Test-Path $Path) {
        $ts = Get-Date -Format "yyyyMMdd-HHmmss"
        $backup = "$Path.bak-$ts"
        Copy-Item -LiteralPath $Path -Destination $backup -Force
        Write-Host "Backup: $backup"
    }
}

function Replace-FileContent($Path, [scriptblock]$Transformer) {
    if (Test-Path $Path) {
        $orig = Get-Content -LiteralPath $Path -Raw
        $new  = & $Transformer $orig
        if ($new -ne $orig) {
            Set-Content -LiteralPath $Path -Value $new -Encoding UTF8
            Write-Host "Updated: $Path"
        } else {
            Write-Host "No change: $Path"
        }
    } else {
        Write-Host "Skip (not found): $Path"
    }
}

# Normalize repo root and target paths
Push-Location $RepoRoot
try {
    $AppGradle = Join-Path (Get-Location) "app/build.gradle.kts"
    $RootGradle = Join-Path (Get-Location) "build.gradle.kts"
    $LibsToml   = Join-Path (Get-Location) "gradle/libs.versions.toml"

    Write-Host "Fixing project at: $(Get-Location)"

    # 1) app/build.gradle.kts
    Backup-File $AppGradle
    Replace-FileContent $AppGradle {
        param($t)
        $txt = $t

        # Remove 'alias(libs.plugins.jetbrains.kotlin.plugin.compose)' lines in plugins{}
        $txt = [System.Text.RegularExpressions.Regex]::Replace(
            $txt,
            '^[ \t]*alias\(\s*libs\.plugins\.jetbrains\.kotlin\.plugin\.compose\s*\)\s*\r?$',
            '',
            [System.Text.RegularExpressions.RegexOptions]::Multiline
        )

        # Ensure composeOptions { kotlinCompilerExtensionVersion = "1.5.13" }
        # Strategy:
        #   - if composeOptions block exists, replace version line or add it
        #   - else insert a new composeOptions block after buildFeatures{ compose = true }
        if ($txt -match 'composeOptions\s*\{[^}]*\}') {
            # update or insert the version line
            if ($txt -match 'kotlinCompilerExtensionVersion\s*=\s*".*?"') {
                $txt = [System.Text.RegularExpressions.Regex]::Replace(
                    $txt,
                    'kotlinCompilerExtensionVersion\s*=\s*".*?"',
                    'kotlinCompilerExtensionVersion = "1.5.13"'
                )
            } else {
                $txt = [System.Text.RegularExpressions.Regex]::Replace(
                    $txt,
                    '(composeOptions\s*\{)',
                    "$1`r`n        kotlinCompilerExtensionVersion = `"1.5.13`""
                )
            }
        } elseif ($txt -match 'buildFeatures\s*\{[^}]*compose\s*=\s*true[^}]*\}') {
            # insert composeOptions after buildFeatures block
            $txt = [System.Text.RegularExpressions.Regex]::Replace(
                $txt,
                '(buildFeatures\s*\{[^}]*\}\s*)',
                "$1`r`n    composeOptions {`r`n        kotlinCompilerExtensionVersion = `"1.5.13`"`r`n    }`r`n"
            )
        } else {
            # fallback: insert into android{} block before closing brace
            $txt = [System.Text.RegularExpressions.Regex]::Replace(
                $txt,
                '(android\s*\{)',
                "$1`r`n    buildFeatures { compose = true }`r`n    composeOptions {`r`n        kotlinCompilerExtensionVersion = `"1.5.13`"`r`n    }"
            )
        }

        return $txt
    }

    # 2) root build.gradle.kts
    Backup-File $RootGradle
    Replace-FileContent $RootGradle {
        param($t)
        $txt = $t
        # Remove kotlin.compose plugin lines in root plugins{} if any
        $txt = [System.Text.RegularExpressions.Regex]::Replace(
            $txt,
            '^[ \t]*id\s*\(\s*"org\.jetbrains\.kotlin\.plugin\.compose"\s*\)\s*version\s*".*?"\s*(apply\s*=\s*false\s*)?\)?\s*\r?$',
            '',
            [System.Text.RegularExpressions.RegexOptions]::Multiline
        )
        $txt = [System.Text.RegularExpressions.Regex]::Replace(
            $txt,
            '^[ \t]*id\s*["'']org\.jetbrains\.kotlin\.plugin\.compose["'']\s*version\s*["''][^"'']+["''].*$',
            '',
            [System.Text.RegularExpressions.RegexOptions]::Multiline
        )
        $txt = [System.Text.RegularExpressions.Regex]::Replace(
            $txt,
            '^[ \t]*alias\(\s*libs\.plugins\.jetbrains\.kotlin\.plugin\.compose\s*\)\s*(apply\s*=\s*false\s*)?\s*\r?$',
            '',
            [System.Text.RegularExpressions.RegexOptions]::Multiline
        )
        return $txt
    }

    # 3) gradle/libs.versions.toml
    Backup-File $LibsToml
    Replace-FileContent $LibsToml {
        param($t)
        $txt = $t

        # Remove a plugins alias for kotlin compose if present
        # Handles either toml key names jetbrains.kotlin.plugin.compose or similar
        $txt = [System.Text.RegularExpressions.Regex]::Replace(
            $txt,
            '^[ \t]*jetbrains(\.|-)?kotlin(\.|-)?plugin(\.|-)?compose\s*=.*$',
            '',
            [System.Text.RegularExpressions.RegexOptions]::Multiline
        )
        $txt = [System.Text.RegularExpressions.Regex]::Replace(
            $txt,
            '^[ \t]*kotlin(\.|-)?plugin(\.|-)?compose\s*=.*$',
            '',
            [System.Text.RegularExpressions.RegexOptions]::Multiline
        )

        # Ensure [versions] has composeCompiler = "1.5.13"
        if ($txt -match '^\[versions\]') {
            if ($txt -match 'composeCompiler\s*=') {
                $txt = [System.Text.RegularExpressions.Regex]::Replace(
                    $txt,
                    'composeCompiler\s*=\s*".*?"',
                    'composeCompiler = "1.5.13"'
                )
            } else {
                $txt = $txt -replace '^\[versions\]\s*', "[versions]`r`ncomposeCompiler = `"1.5.13`"`r`n"
            }
        } else {
            $txt = $txt.TrimEnd() + "`r`n`r`n[versions]`r`ncomposeCompiler = `"1.5.13`"`r`n"
        }
        return $txt
    }

    Write-Host "`n=== Running Gradle clean/assemble ==="
    if (Test-Path ".\gradlew.bat") {
        & .\gradlew --stop | Out-Host
        & .\gradlew clean | Out-Host
        & .\gradlew :app:assembleDebug | Out-Host
    } else {
        Write-Host "gradlew not found in this directory. Skipping build."
    }
}
finally {
    Pop-Location
}
