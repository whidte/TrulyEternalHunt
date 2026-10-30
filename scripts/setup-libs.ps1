param(
    [string]$ModpackDir = "D:\Minecraft\.minecraft\versions\Xiaolei's Optimization and Adventure 2.1.3\mods",
    [string]$TbfProject = "..\trulybestfriends-master"
)

$ErrorActionPreference = "Stop"
$here = Split-Path -Parent $PSScriptRoot
$libs = Join-Path $here "libs"
New-Item -ItemType Directory -Force $libs | Out-Null

$tbf = Get-ChildItem (Join-Path $TbfProject "build\libs") -Filter "trulybestfriends-*.jar" -File -ErrorAction SilentlyContinue |
    Where-Object { $_.Name -notmatch "fabric|1\.21" } |
    Sort-Object LastWriteTime -Descending |
    Select-Object -First 1
if ($tbf) {
    Copy-Item $tbf.FullName (Join-Path $libs "trulybestfriends-0.2.5.jar") -Force
    Write-Host "TBF: $($tbf.Name)"
} else {
    Write-Warning "未找到 TBF jar，请在 $TbfProject 执行 gradlew build"
}

$wanted = @(
    @{ Pattern = "EnigmaticLegacy-*.jar";                Target = "EnigmaticLegacy-2.30.1.jar" },
    @{ Pattern = "*enigmaticaddons-*.jar";               Target = "enigmaticaddons-1.2.6.jar" },
    @{ Pattern = "curios-forge-*1.20.1.jar";             Target = "curios-forge-5.14.1+1.20.1.jar" }
)
foreach ($item in $wanted) {
    $found = Get-ChildItem $ModpackDir -Filter $item.Pattern -File -ErrorAction SilentlyContinue |
        Sort-Object LastWriteTime -Descending |
        Select-Object -First 1
    if ($found) {
        Copy-Item $found.FullName (Join-Path $libs $item.Target) -Force
        Write-Host "$($item.Target) <- $($found.Name)"
    } else {
        Write-Warning "未在 $ModpackDir 找到 $($item.Pattern)"
    }
}

$downloads = @(
    @{ Url = "https://cdn.modrinth.com/data/OCJRPujW/versions/C7tM1LJR/relics-1.20.1-0.8.0.13.jar";
       Target = "relics-1.20.1-0.8.0.13.jar" },
    @{ Url = "https://cdn.modrinth.com/data/zU7VvEmB/versions/76qVou35/dream_relics-1.0.3-forge%2Bmc1.20.1.jar";
       Target = "dream_relics-1.0.3-forge+mc1.20.1.jar" }
)
foreach ($item in $downloads) {
    $target = Join-Path $libs $item.Target
    if (-not (Test-Path $target)) {
        Invoke-WebRequest -Uri $item.Url -OutFile $target -UseBasicParsing -Headers @{ "User-Agent" = "trulyeternalhunt-setup" }
        Write-Host "downloaded $($item.Target)"
    }
}
