@echo off
REM ── Run STUDENT CLIENT ───────────────────────────────────────────────────────
REM Run this on each student machine on the same LAN.
REM Student will be prompted to enter the teacher's IP.

set JAVAFX_LIB=C:\Users\FORBIT\Desktop\javafx\lib

java --module-path %JAVAFX_LIB% ^
     --add-modules javafx.controls ^
     -cp bin ^
     client.MyClient

pause