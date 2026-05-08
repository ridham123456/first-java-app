@echo off
setlocal

set JAVA_BIN=C:\Program Files\Eclipse Adoptium\jdk-25.0.1.8-hotspot\bin
set JAVAFX_LIB=C:\Users\FORBIT\Desktop\javafx\lib

echo Compiling HyperSync...

"%JAVA_BIN%\javac.exe" --module-path "%JAVAFX_LIB%" --add-modules javafx.controls,javafx.fxml -d bin src\server\MyServer.java src\client\App.java

if errorlevel 1 (
  echo.
  echo [ERROR] Compile failed. Check the errors above.
  pause
  exit /b 1
)

echo.
echo [SUCCESS] Compile success! All packages are linked.
pause
endlocal