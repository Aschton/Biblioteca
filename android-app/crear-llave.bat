@echo off
REM Crea la llave de firma museo-release.jks en esta carpeta (android-app).
REM keytool te pedira una clave y tus datos (nombre, organizacion, pais CL).
cd /d "%~dp0"
if exist museo-release.jks (
  echo Ya existe museo-release.jks. Si la borras, el APK quedara firmado con otra llave.
  exit /b 1
)
set "KEYTOOL=keytool"
where keytool >nul 2>nul || set "KEYTOOL=C:\Program Files\Android\Android Studio\jbr\bin\keytool.exe"
"%KEYTOOL%" -genkeypair -v -keystore museo-release.jks -alias museo -keyalg RSA -keysize 2048 -validity 10000
echo.
echo Listo. Ahora copia keystore.properties.example como keystore.properties y escribe tus claves.
