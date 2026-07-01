@echo off
setlocal

echo Compiling PlayerMS...

if not exist out mkdir out
if exist src\main\resources\ (xcopy /Y /E /I src\main\resources\ out\ >nul)

javac -cp "lib\h2-2.2.224.jar;lib\gson-2.10.1.jar" -d out src\main\java\com\playermgs\*.java src\main\java\com\playermgs\controller\*.java src\main\java\com\playermgs\dao\*.java src\main\java\com\playermgs\model\*.java src\main\java\com\playermgs\service\*.java

if %ERRORLEVEL% EQU 0 (
    echo.
    echo ✓ Compile successful! Now run: run.bat
) else (
    echo.
    echo ✗ Compile failed. Check the errors above.
)
