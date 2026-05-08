@echo off
setlocal

set JAVA_BIN=C:\Program Files\Eclipse Adoptium\jdk-25.0.1.8-hotspot\bin
set JAVAFX_LIB=C:\Users\FORBIT\Desktop\javafx\lib
set JAVAFX_NATIVE=C:\Users\FORBIT\Desktop\javafx\bin

echo Running...
"%JAVA_BIN%\java.exe" --module-path "%JAVAFX_LIB%" --add-modules javafx.controls,javafx.fxml --enable-native-access=javafx.graphics "-Dprism.order=sw" "-Djava.library.path=%JAVAFX_NATIVE%" -cp bin client.App
if errorlevel 1 (
  echo Runtime failed.
  pause
  exit /b 1
)

echo Run success.
pause
endlocal