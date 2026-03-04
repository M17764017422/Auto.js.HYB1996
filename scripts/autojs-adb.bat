@echo off
chcp 65001 >nul 2>&1
setlocal enabledelayedexpansion

set PN=org.autojs.autojs.coolapk
set RC=%PN%/org.autojs.autojs.external.receiver.AdbDebugReceiver

if "%~1"=="" goto help
if "%~1"=="run" goto run
if "%~1"=="stop" goto stop
if "%~1"=="stop-all" goto stop_all
if "%~1"=="list" goto list
if "%~1"=="push" goto push
if "%~1"=="delete" goto delete
if "%~1"=="files" goto files
if "%~1"=="ping" goto ping
echo Unknown: %~1
goto help

:run
shift
if "%~1"=="" echo Error: Missing script&& goto eof
if "%~1"=="-f" (
  shift
  set "P=%~1"
  shift
  if "%~1"=="-d" (shift && adb shell am broadcast -n %RC% -a %PN%.adb.RUN_SCRIPT --es path "!P!" --ei delay %~1) else (adb shell am broadcast -n %RC% -a %PN%.adb.RUN_SCRIPT --es path "!P!")
  goto eof
)
set "S=%~1"
echo DEBUG S=[!S!]
set "TF=%TEMP%\ajs%RANDOM%"
echo !S!>"!TF!.txt"
certutil -encode "!TF!.txt" "!TF!.b64" >nul 2>&1
set "B="
for /f "skip=1 tokens=* eol=-" %%i in (!TF!.b64) do set "B=!B!%%i"
echo DEBUG B=[!B!]
del "!TF!.txt" "!TF!.b64" >nul 2>&1
echo DEBUG CMD=adb shell am broadcast -n %RC% -a %PN%.adb.RUN_SCRIPT --es script "!B!" --es base64 true
adb shell am broadcast -n %RC% -a %PN%.adb.RUN_SCRIPT --es script "!B!" --es base64 true
goto eof

:stop
shift
if "%~1"=="" echo Error: Missing ID&& goto eof
adb shell am broadcast -n %RC% -a %PN%.adb.STOP_SCRIPT --ei id %~1
goto eof

:stop_all
adb shell am broadcast -n %RC% -a %PN%.adb.STOP_ALL
goto eof

:list
adb shell am broadcast -n %RC% -a %PN%.adb.LIST_SCRIPTS
goto eof

:push
shift
if "%~1"=="" echo Error: Missing name&& goto eof
if "%~2"=="" echo Error: Missing content&& goto eof
set "N=%~1"
set "C=%~2"
set "TF=%TEMP%\ajs%RANDOM%"
echo !C!>"!TF!.txt"
certutil -encode "!TF!.txt" "!TF!.b64" >nul 2>&1
set "B="
for /f "skip=1 tokens=* eol=-" %%i in (!TF!.b64) do set "B=!B!%%i"
del "!TF!.txt" "!TF!.b64" >nul 2>&1
adb shell am broadcast -n %RC% -a %PN%.adb.PUSH_SCRIPT --es name "!N!" --es content "!B!" --es base64 true
goto eof

:delete
shift
if "%~1"=="" echo Error: Missing path&& goto eof
adb shell am broadcast -n %RC% -a %PN%.adb.DELETE_SCRIPT --es path "%~1"
goto eof

:files
shift
if "%~1"=="" (adb shell am broadcast -n %RC% -a %PN%.adb.LIST_FILES) else (adb shell am broadcast -n %RC% -a %PN%.adb.LIST_FILES --es path "%~1")
goto eof

:ping
adb shell am broadcast -n %RC% -a %PN%.adb.PING
goto eof

:help
echo Auto.js ADB Debug Tool
echo Usage: autojs-adb.bat command [options]
echo Commands: run, stop, stop-all, list, push, delete, files, ping
:eof