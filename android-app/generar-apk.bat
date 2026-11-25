@echo off
REM Genera el APK firmado (release) y lo deja en android-app\release\
cd /d "%~dp0"

if exist keystore.properties.txt (
  echo Encontre "keystore.properties.txt". Windows le agrego .txt al nombre.
  echo Renombralo a "keystore.properties" ^(sin .txt^) y vuelve a ejecutar este archivo.
  goto fin
)
if not exist keystore.properties (
  echo Falta keystore.properties.
  echo Copia keystore.properties.example, renombra la copia a keystore.properties
  echo y escribe tu clave en storePassword y keyPassword.
  goto fin
)
if not exist museo-release.jks (
  echo Falta la llave museo-release.jks. Ejecuta crear-llave.bat primero.
  goto fin
)

echo Generando el APK firmado. La primera vez puede tardar unos minutos...
echo.
call gradlew.bat assembleRelease
if errorlevel 1 (
  echo.
  echo HUBO UN ERROR al generar el APK. Revisa el mensaje de arriba.
  echo Si dice "password was incorrect", la clave de keystore.properties no coincide con la de la llave.
  goto fin
)

if not exist release mkdir release
copy /Y app\build\outputs\apk\release\app-release.apk release\MuseoDeLosMuseos-release.apk >nul
echo.
echo LISTO. APK firmado en: %~dp0release\MuseoDeLosMuseos-release.apk
explorer release

:fin
echo.
pause
