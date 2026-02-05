param(
    [Parameter(ValueFromRemainingArguments = $true)]
    [string[]] $MavenArgs
)

$ErrorActionPreference = 'Stop'

$baseDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$wrapperDir = Join-Path $baseDir ".mvn\wrapper"
$wrapperJar = Join-Path $wrapperDir "maven-wrapper.jar"
$wrapperProps = Join-Path $wrapperDir "maven-wrapper.properties"

if (-not (Test-Path -LiteralPath $wrapperProps)) {
    Write-Error "Maven Wrapper properties not found: $wrapperProps"
    exit 1
}

$wrapperUrl = $null
foreach ($line in Get-Content -LiteralPath $wrapperProps) {
    if ($line -match '^\s*#') { continue }
    if ($line -match '^wrapperUrl=(.+)$') {
        $wrapperUrl = $Matches[1].Trim()
        break
    }
}

if (-not $wrapperUrl) {
    $wrapperUrl = 'https://repo.maven.apache.org/maven2/org/apache/maven/wrapper/maven-wrapper/3.2.0/maven-wrapper-3.2.0.jar'
}

if (-not (Test-Path -LiteralPath $wrapperJar) -or ((Get-Item -LiteralPath $wrapperJar).Length -le 0)) {
    New-Item -ItemType Directory -Force -Path $wrapperDir | Out-Null
    Write-Host "Downloading Maven Wrapper JAR..."
    Write-Host "  from: $wrapperUrl"
    Write-Host "  to:   $wrapperJar"
    $ProgressPreference = 'SilentlyContinue'
    Invoke-WebRequest -Uri $wrapperUrl -OutFile $wrapperJar
}

$javaExe = $null
if ($env:JAVA_HOME) {
    $candidate = Join-Path $env:JAVA_HOME "bin\java.exe"
    if (Test-Path -LiteralPath $candidate) {
        $javaExe = $candidate
    }
}

if (-not $javaExe) {
    $cmd = Get-Command java -ErrorAction SilentlyContinue
    if ($cmd) { $javaExe = $cmd.Source }
}

if (-not $javaExe) {
    Write-Error "Java not found. Set JAVA_HOME or add java.exe to PATH."
    exit 1
}

$javaArgs = @(
    "-Dmaven.multiModuleProjectDirectory=$baseDir",
    "-classpath", $wrapperJar,
    "org.apache.maven.wrapper.MavenWrapperMain"
) + $MavenArgs

& $javaExe @javaArgs
exit $LASTEXITCODE
