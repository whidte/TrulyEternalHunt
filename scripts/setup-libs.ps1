param(
    [string]$TbfProject = "..\trulybestfriends"
)

$ErrorActionPreference = "Stop"
$here = Split-Path -Parent $PSScriptRoot
$libs = Join-Path $here "libs"
New-Item -ItemType Directory -Force $libs | Out-Null

$tbf = Get-ChildItem (Join-Path $TbfProject "build\libs") -Filter "trulybestfriends-*-1.21.1.jar" -File -ErrorAction SilentlyContinue |
    Sort-Object LastWriteTime -Descending |
    Select-Object -First 1
if ($tbf) {
    Copy-Item $tbf.FullName (Join-Path $libs "trulybestfriends-0.2.5-1.21.1.jar") -Force
    Write-Host "TBF: $($tbf.Name)"
} else {
    Write-Warning "未找到 TBF 1.21.1 jar，请在 $TbfProject 执行 gradlew build"
}

$downloads = @(
    @{ Url = "https://cdn.modrinth.com/data/oIjj1Fan/versions/ELK5gx91/enigmaticlegacyplus-1.21.1-1.1.2.jar";
       Target = "enigmaticlegacyplus-1.21.1-1.1.2.jar" },
    @{ Url = "https://cdn.modrinth.com/data/vvuO3ImH/versions/yohfFbgD/curios-neoforge-9.5.1%2B1.21.1.jar";
       Target = "curios-neoforge-9.5.1+1.21.1.jar" }
)
foreach ($item in $downloads) {
    $target = Join-Path $libs $item.Target
    if (-not (Test-Path $target)) {
        Invoke-WebRequest -Uri $item.Url -OutFile $target -UseBasicParsing -Headers @{ "User-Agent" = "trulyeternalhunt-setup" }
        Write-Host "downloaded $($item.Target)"
    }
}
