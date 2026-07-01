@echo off
setlocal
echo Starting PlayerMS server...
echo.

java -cp "out;lib\h2-2.2.224.jar;lib\gson-2.10.1.jar" com.playermgs.WebServer
