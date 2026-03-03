@echo off
REM Auto.js ADB Debug Tool for Windows CMD
REM Usage: autojs-adb.bat <command> [options]
REM
REM Commands:
REM   run <script|path>    - Run script (content or file path)
REM   stop <id>            - Stop script by ID
REM   stop-all             - Stop all running scripts
REM   list                 - List running scripts
REM   push <name> <code>   - Push script to device
REM   delete <path>        - Delete script file
REM   files [path]         - List script files
REM   ping                 - Check if app is responding

setlocal enabledelayedexpansion

REM Package name - change this if using a different flavor
REM Options: org.autojs.autojs (common), org.autojs.autojs.coolapk (coolapk), org.autojs.autojs.github (github)
set PACKAGE_NAME=org.autojs.autojs.coolapk
set RECEIVER_CLASS=%PACKAGE_NAME%/org.autojs.autojs.external.receiver.AdbDebugReceiver

if "%~1"=="" goto :help
if "%~1"=="help" goto :help
if "%~1"=="-h" goto :help
if "%~1"=="--help" goto :help

REM Main command dispatcher
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
    echo Usage: run ^<script^> OR run -f ^<path^>
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
    
    REM Use Base64 encoding to avoid shell escaping issues with Chinese and special characters
    for /f "delims=" %%i in ('powershell -Command "[Convert]::ToBase64String([Text.Encoding]::UTF8.GetBytes('!SCRIPT_ARG!'))"') do set "B64_ARG=%%i"
    
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
    echo Usage: stop ^<id^>
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
    echo Usage: push ^<name^> ^<code^>
    goto :eof
)
if "%~2"=="" (
    echo Error: Missing content
    echo Usage: push ^<name^> ^<code^>
    goto :eof
)

REM Use Base64 encoding for content
set "PUSH_NAME=%~1"
set "PUSH_CONTENT=%~2"
for /f "delims=" %%i in ('powershell -Command "[Convert]::ToBase64String([Text.Encoding]::UTF8.GetBytes('!PUSH_CONTENT!'))"') do set "B64_CONTENT=%%i"

adb shell am broadcast -n %RECEIVER_CLASS% -a %PACKAGE_NAME%.adb.PUSH_SCRIPT --es name "!PUSH_NAME!" --es content "!B64_CONTENT!" --ez base64 true
goto :eof

:delete
shift
if "%~1"=="" (
    echo Error: Missing file path
    echo Usage: delete ^<path^>
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
echo.
echo Usage: autojs-adb.bat ^<command^> [options]
echo.
echo Note: Edit PACKAGE_NAME in script to match your app flavor:
echo   - org.autojs.autojs (common)
echo   - org.autojs.autojs.coolapk (coolapk)
echo   - org.autojs.autojs.github (github)
echo.
echo Commands:
echo   run ^<script^>          - Run script content (auto Base64 encoded)
echo   run -f ^<path^>         - Run script from file path on device
echo   run -f ^<path^> -d ^<ms^> - Run script with delay (milliseconds)
echo   stop ^<id^>             - Stop script by ID
echo   stop-all              - Stop all running scripts
echo   list                  - List running scripts
echo   push ^<name^> ^<code^>    - Push script to device (saved to /sdcard/脚本/)
echo   delete ^<path^>         - Delete script file
echo   files [path]          - List script files (default: /sdcard/脚本/)
echo   ping                  - Check if app is responding
echo.
echo Examples:
echo   autojs-adb.bat run "toast('Hello World')"
echo   autojs-adb.bat run "toast('中文测试')"
echo   autojs-adb.bat run -f "/sdcard/脚本/test.js"
echo   autojs-adb.bat stop 12345
echo   autojs-adb.bat list
echo   autojs-adb.bat push myscript "toast('Hi')"
echo   autojs-adb.bat files
echo   autojs-adb.bat ping
goto :eof

endlocal
