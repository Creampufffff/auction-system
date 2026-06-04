param(
    [string]$Version = "1.0.0"
)

$ErrorActionPreference = "Stop"

$repoRoot = (Resolve-Path (Join-Path $PSScriptRoot "..")).Path
$buildRoot = Join-Path $repoRoot "build\client-package"
$inputDir = Join-Path $buildRoot "input"
$distDir = Join-Path $repoRoot "dist"
$appName = "AuctionClient"
$appDir = Join-Path $distDir $appName
$archivePath = Join-Path $distDir "$appName-windows-x64-$Version.zip"

Push-Location $repoRoot
try {
    if (Test-Path $buildRoot) {
        Remove-Item -Recurse -Force $buildRoot
    }
    if (Test-Path $appDir) {
        Remove-Item -Recurse -Force $appDir
    }
    if (Test-Path $archivePath) {
        Remove-Item -Force $archivePath
    }

    New-Item -ItemType Directory -Force $inputDir, $distDir | Out-Null

    mvn -B -pl auction-client -am install -DskipTests
    if ($LASTEXITCODE -ne 0) {
        throw "Maven build failed."
    }

    mvn -B -pl auction-client dependency:copy-dependencies "-DincludeScope=runtime" "-DoutputDirectory=$inputDir"
    if ($LASTEXITCODE -ne 0) {
        throw "Could not collect client runtime dependencies."
    }

    Copy-Item "auction-client\target\auction-client-1.0-SNAPSHOT.jar" $inputDir

    jpackage `
        --type app-image `
        --name $appName `
        --app-version $Version `
        --input $inputDir `
        --dest $distDir `
        --main-jar "auction-client-1.0-SNAPSHOT.jar" `
        --main-class "com.auction.ClientLauncher"
    if ($LASTEXITCODE -ne 0) {
        throw "jpackage failed."
    }

    Copy-Item "auction-client\.env.example" (Join-Path $appDir ".env.example")
    Compress-Archive -Path $appDir -DestinationPath $archivePath -CompressionLevel Optimal

    Write-Host "Created portable client: $archivePath"
}
finally {
    Pop-Location
}
