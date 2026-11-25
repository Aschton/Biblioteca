@echo off
REM Levanta los 3 microservicios, cada uno en su propia ventana de cmd.
REM Requisitos: MySQL encendido (XAMPP: Start en MySQL) y Maven (en el PATH o descomprimido en C:\maven).

set "MVN=mvn"
where mvn >nul 2>nul || set "MVN=C:\maven\bin\mvn.cmd"
if not "%MVN%"=="mvn" if not exist "%MVN%" (
  echo No se encontro Maven. Descomprimelo para que exista C:\maven\bin\mvn.cmd
  pause
  exit /b 1
)

REM Si Windows no tiene Java configurado, usa el que trae Android Studio
if not defined JAVA_HOME if exist "C:\Program Files\Android\Android Studio\jbr\bin\java.exe" set "JAVA_HOME=C:\Program Files\Android\Android Studio\jbr"

start "ms-museos :8081" cmd /k ""%MVN%" -f "%~dp0ms-museos\pom.xml" spring-boot:run"
start "ms-catalogo :8082" cmd /k ""%MVN%" -f "%~dp0ms-catalogo\pom.xml" spring-boot:run"
start "ms-pedidos :8083" cmd /k ""%MVN%" -f "%~dp0ms-pedidos\pom.xml" spring-boot:run"
