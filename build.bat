@echo off
echo Building HyperSync...

REM ── Adjust these paths for your system ──────────────────────────────────────
set JAVAFX_LIB=C:\Users\FORBIT\Desktop\javafx\lib
set SRC=src
set OUT=bin

REM ── Clean ───────────────────────────────────────────────────────────────────
if exist %OUT% rmdir /s /q %OUT%
mkdir %OUT%

REM ── Compile (server first, then client which references server) ──────────────
javac --module-path %JAVAFX_LIB% ^
      --add-modules javafx.controls ^
      -d %OUT% ^
      %SRC%\server\MyServer.java ^
      %SRC%\server\ClientHandler.java ^
      %SRC%\server\QuizEngine.java ^
      %SRC%\server\LobbyManager.java ^
      %SRC%\server\ScoreTracker.java ^
      %SRC%\client\App.java ^
      %SRC%\client\MyClient.java

if %ERRORLEVEL% NEQ 0 (
    echo BUILD FAILED.
    pause
    exit /b 1
)

echo BUILD SUCCESS.
pause