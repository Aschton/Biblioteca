@echo off
REM Muestra los datos de la llave y verifica que el APK este firmado con ella.
cd /d "%~dp0"

set "KEYTOOL=keytool"
where keytool >nul 2>nul || set "KEYTOOL=C:\Program Files\Android\Android Studio\jbr\bin\keytool.exe"

if not exist museo-release.jks (
  echo Falta museo-release.jks. Ejecuta crear-llave.bat primero.
  pause
  exit /b 1
)

echo ===== 1/2 Datos de la llave museo-release.jks =====
echo (escribe la clave de la llave y presiona Enter)
"%KEYTOOL%" -J-Duser.language=en -list -v -keystore museo-release.jks
echo.

set "APKSIGNER="
for /d %%D in ("%LOCALAPPDATA%\Android\Sdk\build-tools\*") do if exist "%%D\apksigner.bat" set "APKSIGNER=%%D\apksigner.bat"
if not defined APKSIGNER (
  echo No se encontro apksigner. Instala Build-Tools desde Android Studio: Tools - SDK Manager - SDK Tools.
  pause
  exit /b 1
)
if not exist release\MuseoDeLosMuseos-release.apk (
  echo Falta el APK. Ejecuta generar-apk.bat primero.
  pause
  exit /b 1
)

echo ===== 2/2 Firma del APK release\MuseoDeLosMuseos-release.apk =====
call "%APKSIGNER%" verify --verbose --print-certs release\MuseoDeLosMuseos-release.apk | findstr /i "Verifies Signer #1 certificate DN v2 v3"
echo.
echo Si los datos (CN=...) de la llave y del APK coinciden, el APK esta firmado con tu llave.
pause
