@echo off
REM Deja todo listo para la defensa: microservicios, endpoints, reporte de cobertura y APK.
REM ANTES: abrir XAMPP y darle Start a MySQL.
cd /d "%~dp0"

echo ==========================================
echo   DEFENSA EP4 - Museo de los Museos
echo ==========================================
echo.
echo Recuerda: MySQL tiene que estar en Start en XAMPP.
echo.
echo [1/4] Levantando los 3 microservicios...
call "%~dp0backend\levantar-microservicios.bat"

echo [2/4] Esperando 60 segundos a que arranquen...
timeout /t 60

echo [3/4] Abriendo endpoints en el navegador...
start "" http://localhost:8081/api/museums
start "" http://localhost:8082/api/products
start "" http://localhost:8083/api/orders

echo [4/4] Abriendo reporte de cobertura y carpeta del APK...
if exist "%~dp0android-app\app\build\reports\kover\htmlDebug\index.html" (
  start "" "%~dp0android-app\app\build\reports\kover\htmlDebug\index.html"
) else (
  echo   No hay reporte de cobertura: ejecuta android-app\probar-tests.bat
)
if exist "%~dp0android-app\release" (
  start "" explorer "%~dp0android-app\release"
) else (
  echo   No hay APK: ejecuta android-app\generar-apk.bat
)

echo.
echo LISTO. Ahora abre Android Studio y presiona Run en el emulador.
echo Si algun endpoint no carga, espera un poco y recarga con F5.
pause
