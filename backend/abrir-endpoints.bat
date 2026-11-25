@echo off
REM Abre en el navegador los endpoints principales de los 3 microservicios.
start "" http://localhost:8081/api/museums
start "" http://localhost:8082/api/products
start "" http://localhost:8083/api/orders
