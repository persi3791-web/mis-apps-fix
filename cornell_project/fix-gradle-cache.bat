@echo off
cd /d "%~dp0"

echo Deteniendo daemons de Gradle...
call gradlew.bat --stop 2>nul

echo.
echo 1) Eliminando cache corrupta (dependencies-accessors)...
set CACHE_DIR=%USERPROFILE%\.gradle\caches\9.1.0\dependencies-accessors
if exist "%CACHE_DIR%" rmdir /s /q "%CACHE_DIR%" && echo    OK.

echo 2) Eliminando .gradle del proyecto (evita "lock protocol")...
if exist ".gradle" rmdir /s /q ".gradle" && echo    OK.

echo 3) Si aun falla, elimina manualmente: %%USERPROFILE%%\.gradle\caches\9.1.0
echo.
echo Ejecutando build...
call gradlew.bat clean assembleDebug

echo.
if errorlevel 1 (
    echo Si sigue fallando, en PowerShell ejecuta:
    echo   Remove-Item -Recurse -Force "$env:USERPROFILE\.gradle\caches\9.1.0"
    echo   .\gradlew.bat clean assembleDebug
)
pause
