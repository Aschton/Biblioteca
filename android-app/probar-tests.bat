@echo off
REM Corre las 122 pruebas unitarias, genera el reporte de cobertura y lo abre en el navegador.
cd /d "%~dp0"
echo.
echo ===== 1/3 Ejecutando pruebas unitarias =====
call gradlew.bat testDebugUnitTest || goto error
echo.
echo ===== 2/3 Generando reporte de cobertura (Kover) =====
call gradlew.bat koverHtmlReportDebug || goto error
start "" "%~dp0app\build\reports\kover\htmlDebug\index.html"
echo.
echo ===== 3/3 Verificando que la cobertura sea de al menos 80%% =====
call gradlew.bat koverVerifyDebug || goto error
echo.
echo LISTO: todas las pruebas pasaron y la cobertura es de 80%% o mas.
echo El reporte se abrio en el navegador.
pause
exit /b 0
:error
echo.
echo HUBO UN ERROR. Revisa el mensaje de arriba.
pause
exit /b 1
