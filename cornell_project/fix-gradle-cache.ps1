# Script para corregir errores de cache de Gradle (metadata.bin / lock protocol)
Set-Location $PSScriptRoot

Write-Host "Deteniendo daemons de Gradle..." -ForegroundColor Yellow
& .\gradlew.bat --stop 2>$null

$depsPath = "$env:USERPROFILE\.gradle\caches\9.1.0\dependencies-accessors"
$cachePath = "$env:USERPROFILE\.gradle\caches\9.1.0"
$projectGradle = Join-Path $PSScriptRoot ".gradle"

Write-Host "`n1) Eliminando cache corrupta (dependencies-accessors)..." -ForegroundColor Yellow
if (Test-Path $depsPath) {
    Remove-Item -Recurse -Force $depsPath
    Write-Host "   OK." -ForegroundColor Green
}

Write-Host "2) Eliminando .gradle del proyecto (evita 'lock protocol')..." -ForegroundColor Yellow
if (Test-Path $projectGradle) {
    Remove-Item -Recurse -Force $projectGradle
    Write-Host "   OK." -ForegroundColor Green
}

Write-Host "3) Eliminando cache 9.1.0 completa (por si lock protocol persiste)..." -ForegroundColor Yellow
if (Test-Path $cachePath) {
    Remove-Item -Recurse -Force $cachePath
    Write-Host "   OK." -ForegroundColor Green
}

Write-Host "`nEjecutando build..." -ForegroundColor Yellow
& .\gradlew.bat clean assembleDebug
if ($LASTEXITCODE -eq 0) {
    Write-Host "`nBuild completado correctamente." -ForegroundColor Green
} else {
    Write-Host "`nBuild fallo. Prueba en Android Studio: File -> Invalidate Caches -> Invalidate and Restart" -ForegroundColor Red
}
