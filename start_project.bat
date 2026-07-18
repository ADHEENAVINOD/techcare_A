@echo off
title TECH CARE Launch Controller
color 0B

echo ===================================================
echo             STARTING TECH CARE PROJECT             
echo ===================================================
echo.

echo [1/3] Starting MySQL Database Server...
start "MySQL Database" /min "C:\Program Files\MySQL\MySQL Server 8.0\bin\mysqld.exe" --datadir="D:\adheena personal thing\bachorlor\db_data" --port=3306

echo.
echo Waiting for Database to boot (5 seconds)...
timeout /t 5 /nobreak > nil

echo.
echo [2/3] Starting Flask Backend Web Server...
cd /d "D:\adheena personal thing\bachorlor"
start "Flask Backend Web Server" python backend/app.py

echo.
echo Waiting for Web Server to initialize (3 seconds)...
timeout /t 3 /nobreak > nil

echo.
echo [3/3] Opening Web Portal in Default Browser...
start http://127.0.0.1:5000

echo.
echo ===================================================
echo     Project started successfully!                  
echo     Keep the minimized windows running in taskbar. 
echo ===================================================
echo.
pause
