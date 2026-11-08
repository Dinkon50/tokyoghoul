# Установщик сборки «Tokyo Ghoul RPG» (Forge 1.20.1) для TLauncher и обычного лаунчера.
# Скачивает моды из списка mods.txt с Modrinth (последние версии для Forge 1.20.1) вместе с их библиотеками,
# шейдеры — в shaderpacks, настройки — в config. Старые моды (кроме Tokyo Ghoul RPG) переносятся в mods_backup_*.

$ErrorActionPreference = "Stop"
try { [Console]::OutputEncoding = [System.Text.Encoding]::UTF8 } catch { }
[Net.ServicePointManager]::SecurityProtocol = [Net.SecurityProtocolType]::Tls12
$ProgressPreference = "SilentlyContinue"
$UA = @{ "User-Agent" = "tokyoghoulrpg-modpack-installer/1.0" }
$api = if ($env:TG_MODRINTH_API) { $env:TG_MODRINTH_API } else { "https://api.modrinth.com/v2" }
$here = Split-Path -Parent $MyInvocation.MyCommand.Path

Write-Host ""
Write-Host "=== Сборка Tokyo Ghoul RPG: установка модов ===" -ForegroundColor Red
Write-Host "Нужен установленный Forge 1.20.1 (в TLauncher выбери версию Forge 1.20.1 и один раз запусти игру)."
Write-Host ""
$mc = Join-Path $env:APPDATA ".minecraft"
$in = Read-Host "Папка игры (просто Enter = $mc)"
if ($in) { $mc = $in.Trim('"') }
$mods = Join-Path $mc "mods"
$shaders = Join-Path $mc "shaderpacks"
$config = Join-Path $mc "config"
New-Item -ItemType Directory -Force $mods, $shaders, $config | Out-Null

# старые моды — в запасную папку (наш мод не трогаем)
$old = Get-ChildItem $mods -Filter *.jar | Where-Object { $_.Name -notlike "tokyoghoulrpg*" }
if ($old) {
    $bk = Join-Path $mc ("mods_backup_" + (Get-Date -Format "yyyyMMdd_HHmmss"))
    New-Item -ItemType Directory $bk | Out-Null
    $old | Move-Item -Destination $bk
    Write-Host "Старые моды перенесены в $bk" -ForegroundColor Yellow
}

$gv = [uri]::EscapeDataString('["1.20.1"]')
$done = @{}
$failed = New-Object System.Collections.ArrayList

function Get-Versions([string]$id, [string]$kind) {
    $loader = if ($kind -eq "shader") { '["iris","optifine"]' } else { '["forge"]' }
    $l = [uri]::EscapeDataString($loader)
    $list = @()
    try {
        $r = Invoke-RestMethod -Headers $UA -Uri "$api/project/$id/version?loaders=$l&game_versions=$gv"
        foreach ($x in $r) { $list += $x }
    } catch { }
    return ,$list
}

function Find-Project([string]$name, [string]$kind) {
    $type = if ($kind -eq "shader") { "shader" } else { "mod" }
    $cat = if ($kind -eq "shader") { "" } else { '["categories:forge"],' }
    $facets = [uri]::EscapeDataString("[$cat[""versions:1.20.1""],[""project_type:$type""]]")
    try {
        $hits = (Invoke-RestMethod -Headers $UA -Uri "$api/search?query=$([uri]::EscapeDataString($name))&facets=$facets&limit=1").hits
        if ($hits -and $hits.Count -gt 0) { return $hits[0].project_id }
    } catch { }
    return $null
}

function Install-Project([string]$id, [string]$name, [string]$kind, [bool]$isDep) {
    if ($done.ContainsKey($id)) { return }
    $done[$id] = $true
    $versions = Get-Versions $id $kind
    if ($versions.Count -eq 0 -and -not $isDep) {
        $found = Find-Project $name $kind
        if ($found -and -not $done.ContainsKey($found)) {
            $done[$found] = $true
            $versions = Get-Versions $found $kind
        }
    }
    if ($versions.Count -eq 0) {
        [void]$failed.Add($name)
        Write-Host "  НЕ НАЙДЕН для Forge 1.20.1: $name" -ForegroundColor Red
        return
    }
    $v = $versions | Where-Object { $_.version_type -eq "release" } | Select-Object -First 1
    if (-not $v) { $v = $versions[0] }
    $done[$v.project_id] = $true
    $file = $v.files | Where-Object { $_.primary } | Select-Object -First 1
    if (-not $file) { $file = $v.files[0] }
    $target = if ($kind -eq "shader") { $shaders } else { $mods }
    $out = Join-Path $target $file.filename
    if (-not (Test-Path $out)) {
        try { Invoke-WebRequest -Headers $UA -Uri $file.url -OutFile $out -UseBasicParsing }
        catch { [void]$failed.Add($name); Write-Host "  ОШИБКА СКАЧИВАНИЯ: $name" -ForegroundColor Red; return }
    }
    $label = if ($isDep) { "    + библиотека" } else { "  OK" }
    Write-Host "$label  $($file.filename)" -ForegroundColor Green
    foreach ($d in $v.dependencies) {
        if ($d.dependency_type -eq "required" -and $d.project_id) {
            Install-Project $d.project_id $d.project_id "mod" $true
        }
    }
}

$list = Get-Content (Join-Path $here "mods.txt") -Encoding UTF8 | Where-Object { $_.Trim() -and -not $_.Trim().StartsWith("#") }
$i = 0
foreach ($line in $list) {
    $i++
    $p = $line.Split("|") | ForEach-Object { $_.Trim() }
    Write-Host "[$i/$($list.Count)] $($p[1])"
    Install-Project $p[0] $p[1] $p[2] $false
}

# настройки, подобранные под Tokyo Ghoul RPG
if (Test-Path (Join-Path $here "config")) {
    Copy-Item (Join-Path $here "config\*") $config -Recurse -Force
    Write-Host "Настройки скопированы в $config"
}

Write-Host ""
if ($failed.Count -gt 0) {
    Write-Host "Не удалось скачать: $($failed -join ', ')" -ForegroundColor Yellow
    Write-Host "Это не страшно: сборка работает и без них. Можно скачать вручную с modrinth.com (версия Forge 1.20.1)."
}
$ours = Get-ChildItem $mods -Filter "tokyoghoulrpg*.jar"
if (-not $ours) {
    Write-Host "ВАЖНО: положи сам мод tokyoghoulrpg-*.jar (из GitHub, Releases) в $mods" -ForegroundColor Red
}
Write-Host "Готово! В TLauncher выдели игре 6-8 ГБ памяти и запусти Forge 1.20.1." -ForegroundColor Green
