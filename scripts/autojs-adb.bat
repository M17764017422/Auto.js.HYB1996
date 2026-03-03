@echo off
chcp 65001 >nul 2>&1
REM Auto.js ADB Debug Tool for Windows CMD
REM Usage: autojs-adb.bat ^<command^> [options]

setlocal enabledelayedexpansion

set PACKAGE_NAME=org.autojs.autojs.coolapk
set RECEIVER_CLASS=%PACKAGE_NAME%/org.autojs.autojs.external.receiver.AdbDebugReceiver

if "%~1"=="" goto :help
if "%~1"=="help" goto :help
if "%~1"=="-h" goto :help
if "%~1"=="--help" goto :help

if "%~1"=="run" goto :run
if "%~1"=="stop" goto :stop
if "%~1"=="stop-all" goto :stop_all
if "%~1"=="list" goto :list
if "%~1"=="push" goto :push
if "%~1"=="delete" goto :delete
if "%~1"=="files" goto :files
if "%~1"=="ping" goto :ping

echo Unknown command: %~1
goto :help

:run
shift
if "%~1"=="" (
    echo Error: Missing script content or file path
    goto :eof
)
if "%~1"=="-f" (
    shift
    if "%~1"=="" (
        echo Error: Missing file path
        goto :eof
    )
    set "PATH_ARG=%~1"
    shift
    if "%~1"=="-d" (
        shift
        adb shell am broadcast -n %RECEIVER_CLASS% -a %PACKAGE_NAME%.adb.RUN_SCRIPT --es path "!PATH_ARG!" --ei delay %~1
    ) else (
        adb shell am broadcast -n %RECEIVER_CLASS% -a %PACKAGE_NAME%.adb.RUN_SCRIPT --es path "!PATH_ARG!"
    )
) else (
    set "SCRIPT_ARG=%~1"
    set "TEMP_FILE=%TEMP%\autojs_b64_%RANDOM%.txt"
    <nul set /p="!SCRIPT_ARG!" > "!TEMP_FILE!"
    for /f "usebackq delims=" %%i in (powershell -NoProfile -Command "[Convert]::ToBase64String([Text.Encoding]::UTF8.GetBytes([IO.File]::ReadAllText('!TEMP_FILE!')))") do set "B64_ARG=%%i"
    del "!TEMP_FILE!" >nul 2>&1
    shift
    if "%~1"=="-d" (
        shift
        adb shell am broadcast -n %RECEIVER_CLASS% -a %PACKAGE_NAME%.adb.RUN_SCRIPT --es script "!B64_ARG!" --ez base64 true --ei delay %~1
    ) else (
        adb shell am broadcast -n %RECEIVER_CLASS% -a %PACKAGE_NAME%.adb.RUN_SCRIPT --es script "!B64_ARG!" --ez base64 true
    )
)
goto :eof

:stop
shift
if "%~1"=="" (
    echo Error: Missing script ID
    goto :eof
)
adb shell am broadcast -n %RECEIVER_CLASS% -a %PACKAGE_NAME%.adb.STOP_SCRIPT --ei id %~1
goto :eof

:stop_all
adb shell am broadcast -n %RECEIVER_CLASS% -a %PACKAGE_NAME%.adb.STOP_ALL
goto :eof

:list
adb shell am broadcast -n %RECEIVER_CLASS% -a %PACKAGE_NAME%.adb.LIST_SCRIPTS
goto :eof

:push
shift
if "%~1"=="" (
    echo Error: Missing name
    goto :eof
)
if "%~2"=="" (
    echo Error: Missing content
    goto :eof
)
set "PUSH_NAME=%~1"
set "PUSH_CONTENT=%~2"
set "TEMP_FILE=%TEMP%\autojs_b64_%RANDOM%.txt"
<nul set /p="!PUSH_CONTENT!" > "!TEMP_FILE!"
for /f "usebackq delims=" %%i in (powershell -NoProfile -Command "[Convert]::ToBase64String([Text.Encoding]::UTF8.GetBytes([IO.File]::ReadAllText('!TEMP_FILE!')))") do set "B64_CONTENT=%%i"
del "!TEMP_FILE!" >nul 2>&1
adb shell am broadcast -n %RECEIVER_CLASS% -a %PACKAGE_NAME%.adb.PUSH_SCRIPT --es name "!PUSH_NAME!" --es content "!B64_CONTENT!" --ez base64 true
goto :eof

:delete
shift
if "%~1"=="" (
    echo Error: Missing file path
    goto :eof
)
adb shell am broadcast -n %RECEIVER_CLASS% -a %PACKAGE_NAME%.adb.DELETE_SCRIPT --es path "%~1"
goto :eof

:files
shift
if "%~1"=="" (
    adb shell am broadcast -n %RECEIVER_CLASS% -a %PACKAGE_NAME%.adb.LIST_FILES
) else (
    adb shell am broadcast -n %RECEIVER_CLASS% -a %PACKAGE_NAME%.adb.LIST_FILES --es path "%~1"
)
goto :eof

:ping
adb shell am broadcast -n %RECEIVER_CLASS% -a %PACKAGE_NAME%.adb.PING
goto :eof

:help
echo Auto.js ADB Debug Tool
echo Usage: autojs-adb.bat ^<command^> [options]
echo Note: Edit PACKAGE_NAME to match your app flavor.
echo Commands: run, stop, stop-all, list, push, delete, files, ping
goto :eof

endlocal
