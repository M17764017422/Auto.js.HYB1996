# Auto.js ADB Debug Tool for Windows PowerShell
# Usage: .\autojs-adb.ps1 <command> [options]
# 
# Commands:
#   run <script|path>    - Run script (content or file path)
#   stop <id>            - Stop script by ID
#   stop-all             - Stop all running scripts
#   list                 - List running scripts
#   push <name> <code>   - Push script to device
#   delete <path>        - Delete script file
#   files [path]         - List script files
#   ping                 - Check if app is responding

param(
    [Parameter(Position=0)]
    [string]$Command,
    
    [Parameter(Position=1, ValueFromRemainingArguments=$true)]
    [string[]]$Args
)

# Package name - change this if using a different flavor
# Options: org.autojs.autojs (common), org.autojs.autojs.coolapk (coolapk), org.autojs.autojs.github (github)
$PackageName = "org.autojs.autojs.coolapk"
$ReceiverClass = "$PackageName/org.autojs.autojs.external.receiver.AdbDebugReceiver"

function Show-Help {
    Write-Host @"
Auto.js ADB Debug Tool

Usage: .\autojs-adb.ps1 <command> [options]

Note: Edit `$PackageName in script to match your app flavor:
  - org.autojs.autojs (common)
  - org.autojs.autojs.coolapk (coolapk)  
  - org.autojs.autojs.github (github)

Commands:
  run <script>          - Run script content (auto Base64 encoded)
  run -f <path>         - Run script from file path on device
  run -f <path> -d <ms> - Run script with delay (milliseconds)
  stop <id>             - Stop script by ID
  stop-all              - Stop all running scripts
  list                  - List running scripts
  push <name> <code>    - Push script to device (saved to /sdcard/脚本/)
  delete <path>         - Delete script file
  files [path]          - List script files (default: /sdcard/脚本/)
  ping                  - Check if app is responding

Examples:
  .\autojs-adb.ps1 run "toast('Hello World')"
  .\autojs-adb.ps1 run "toast('中文测试')"
  .\autojs-adb.ps1 run -f "/sdcard/脚本/test.js"
  .\autojs-adb.ps1 stop 12345
  .\autojs-adb.ps1 list
  .\autojs-adb.ps1 push myscript "toast('Hi')"
  .\autojs-adb.ps1 files
  .\autojs-adb.ps1 ping

"@
}

function Invoke-AdbBroadcast {
    param(
        [string]$Action,
        [hashtable]$Extras = @{}
    )
    
    # Use explicit intent for Android 8+ compatibility
    $cmdArgs = @("shell", "am", "broadcast", "-n", $ReceiverClass, "-a", "$PackageName.$Action")
    
    foreach ($key in $Extras.Keys) {
        $value = $Extras[$key]
        if ($key -eq "base64") {
            # Boolean value
            $cmdArgs += "--ez"
            $cmdArgs += $key
            $cmdArgs += $value
        } elseif ($key -eq "delay" -or $key -eq "id") {
            # Integer values
            $cmdArgs += "--ei"
            $cmdArgs += $key
            $cmdArgs += $value
        } else {
            # String values
            $cmdArgs += "--es"
            $cmdArgs += $key
            $cmdArgs += $value
        }
    }
    
    $result = & adb $cmdArgs 2>&1
    Write-Host $result
}

function Run-Script {
    param([string[]]$ScriptArgs)
    
    if ($ScriptArgs.Count -eq 0) {
        Write-Host "Error: Missing script content or file path"
        Write-Host "Usage: run <script> OR run -f <path>"
        return
    }
    
    $extras = @{}
    $isFile = $false
    $delay = 0
    
    for ($i = 0; $i -lt $ScriptArgs.Count; $i++) {
        if ($ScriptArgs[$i] -eq "-f") {
            $isFile = $true
            $i++
            if ($i -lt $ScriptArgs.Count) {
                $extras["path"] = $ScriptArgs[$i]
            }
        } elseif ($ScriptArgs[$i] -eq "-d") {
            $i++
            if ($i -lt $ScriptArgs.Count) {
                $delay = [long]$ScriptArgs[$i]
            }
        } elseif (-not $isFile) {
            # 使用 base64 编码脚本内容，避免 shell 转义问题
            $scriptContent = $ScriptArgs[$i]
            $bytes = [System.Text.Encoding]::UTF8.GetBytes($scriptContent)
            $base64 = [Convert]::ToBase64String($bytes)
            $extras["script"] = $base64
            $extras["base64"] = "true"
        }
    }
    
    if ($delay -gt 0) {
        $extras["delay"] = $delay.ToString()
    }
    
    Invoke-AdbBroadcast -Action "adb.RUN_SCRIPT" -Extras $extras
}

function Stop-Script {
    param([string]$Id)
    
    if ([string]::IsNullOrEmpty($Id)) {
        Write-Host "Error: Missing script ID"
        Write-Host "Usage: stop <id>"
        return
    }
    
    Invoke-AdbBroadcast -Action "adb.STOP_SCRIPT" -Extras @{ id = $Id }
}

function Stop-AllScripts {
    Invoke-AdbBroadcast -Action "adb.STOP_ALL"
}

function List-Scripts {
    Invoke-AdbBroadcast -Action "adb.LIST_SCRIPTS"
}

function Push-Script {
    param([string]$Name, [string]$Content)
    
    if ([string]::IsNullOrEmpty($Name) -or [string]::IsNullOrEmpty($Content)) {
        Write-Host "Error: Missing name or content"
        Write-Host "Usage: push <name> <code>"
        return
    }
    
    # 使用 base64 编码内容
    $bytes = [System.Text.Encoding]::UTF8.GetBytes($Content)
    $base64 = [Convert]::ToBase64String($bytes)
    
    Invoke-AdbBroadcast -Action "adb.PUSH_SCRIPT" -Extras @{ name = $Name; content = $base64; base64 = "true" }
}

function Delete-Script {
    param([string]$Path)
    
    if ([string]::IsNullOrEmpty($Path)) {
        Write-Host "Error: Missing file path"
        Write-Host "Usage: delete <path>"
        return
    }
    
    Invoke-AdbBroadcast -Action "adb.DELETE_SCRIPT" -Extras @{ path = $Path }
}

function List-Files {
    param([string]$Path)
    
    $extras = @{}
    if (-not [string]::IsNullOrEmpty($Path)) {
        $extras["path"] = $Path
    }
    
    Invoke-AdbBroadcast -Action "adb.LIST_FILES" -Extras $extras
}

function Ping-App {
    Invoke-AdbBroadcast -Action "adb.PING"
}

# Main command dispatcher
switch ($Command.ToLower()) {
    "run"       { Run-Script -ScriptArgs $Args }
    "stop"      { Stop-Script -Id $Args[0] }
    "stop-all"  { Stop-AllScripts }
    "list"      { List-Scripts }
    "push"      { Push-Script -Name $Args[0] -Content $Args[1] }
    "delete"    { Delete-Script -Path $Args[0] }
    "files"     { 
        if ($Args -and $Args.Count -gt 0) {
            List-Files -Path $Args[0]
        } else {
            List-Files
        }
    }
    "ping"      { Ping-App }
    "help"      { Show-Help }
    "-h"        { Show-Help }
    "--help"    { Show-Help }
    default     { 
        if ([string]::IsNullOrEmpty($Command)) {
            Show-Help
        } else {
            Write-Host "Unknown command: $Command"
            Show-Help
        }
    }
}
