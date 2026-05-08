@echo off
REM ── Run TEACHER (also starts the embedded server) ────────────────────────────
REM Adjust JAVAFX_LIB to your JavaFX SDK path

set JAVAFX_LIB=C:\Users\FORBIT\Desktop\javafx\lib

java --module-path %JAVAFX_LIB% ^
     --add-modules javafx.controls ^
     -cp bin ^
     client.App

pause