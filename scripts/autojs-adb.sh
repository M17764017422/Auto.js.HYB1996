#!/bin/bash
# Auto.js ADB Debug Tool for Linux/Mac
# Usage: ./autojs-adb.sh <command> [options]
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

# Package name - change this if using a different flavor
# Options: org.autojs.autojs (common), org.autojs.autojs.coolapk (coolapk), org.autojs.autojs.github (github)
PACKAGE_NAME="org.autojs.autojs.coolapk"
RECEIVER_CLASS="${PACKAGE_NAME}/org.autojs.autojs.external.receiver.AdbDebugReceiver"

show_help() {
    cat << EOF
Auto.js ADB Debug Tool

Usage: ./autojs-adb.sh <command> [options]

Note: Edit PACKAGE_NAME in script to match your app flavor:
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
  ./autojs-adb.sh run "toast('Hello World')"
  ./autojs-adb.sh run "toast('中文测试')"
  ./autojs-adb.sh run -f "/sdcard/脚本/test.js"
  ./autojs-adb.sh stop 12345
  ./autojs-adb.sh list
  ./autojs-adb.sh push myscript "toast('Hi')"
  ./autojs-adb.sh files
  ./autojs-adb.sh ping

EOF
}

# Base64 encode function for UTF-8 content
base64_encode() {
    echo -n "$1" | base64
}

adb_broadcast() {
    local action="$1"
    shift
    # Use explicit intent for Android 8+ compatibility
    local cmd_args="shell am broadcast -n ${RECEIVER_CLASS} -a ${PACKAGE_NAME}.${action}"
    
    while [[ $# -gt 0 ]]; do
        if [[ "$1" == "--es" ]]; then
            # String extra: --es key value
            cmd_args="$cmd_args --es $2 \"$3\""
            shift 3
        elif [[ "$1" == "--ei" ]]; then
            # Integer extra: --ei key value
            cmd_args="$cmd_args --ei $2 $3"
            shift 3
        else
            # String extra: key value
            cmd_args="$cmd_args --es $1 \"$2\""
            shift 2
        fi
    done
    
    eval adb $cmd_args
}

run_script() {
    local script=""
    local path=""
    local delay=0
    
    while [[ $# -gt 0 ]]; do
        case "$1" in
            -f)
                shift
                path="$1"
                ;;
            -d)
                shift
                delay="$1"
                ;;
            *)
                if [[ -z "$script" ]]; then
                    script="$1"
                fi
                ;;
        esac
        shift
    done
    
    if [[ -n "$path" ]]; then
        if [[ $delay -gt 0 ]]; then
            adb_broadcast "adb.RUN_SCRIPT" "path" "$path" "--ei" "delay" "$delay"
        else
            adb_broadcast "adb.RUN_SCRIPT" "path" "$path"
        fi
    elif [[ -n "$script" ]]; then
        # Use Base64 encoding to avoid shell escaping issues
        local b64_script=$(base64_encode "$script")
        if [[ $delay -gt 0 ]]; then
            adb_broadcast "adb.RUN_SCRIPT" "script" "$b64_script" "--es" "base64" "true" "--ei" "delay" "$delay"
        else
            adb_broadcast "adb.RUN_SCRIPT" "script" "$b64_script" "--es" "base64" "true"
        fi
    else
        echo "Error: Missing script content or file path"
        echo "Usage: run <script> OR run -f <path>"
    fi
}

stop_script() {
    local id="$1"
    if [[ -z "$id" ]]; then
        echo "Error: Missing script ID"
        echo "Usage: stop <id>"
        return
    fi
    adb_broadcast "adb.STOP_SCRIPT" "--ei" "id" "$id"
}

stop_all_scripts() {
    adb_broadcast "adb.STOP_ALL"
}

list_scripts() {
    adb_broadcast "adb.LIST_SCRIPTS"
}

push_script() {
    local name="$1"
    local content="$2"
    if [[ -z "$name" ]] || [[ -z "$content" ]]; then
        echo "Error: Missing name or content"
        echo "Usage: push <name> <code>"
        return
    fi
    # Use Base64 encoding for content
    local b64_content=$(base64_encode "$content")
    adb_broadcast "adb.PUSH_SCRIPT" "name" "$name" "content" "$b64_content" "--es" "base64" "true"
}

delete_script() {
    local path="$1"
    if [[ -z "$path" ]]; then
        echo "Error: Missing file path"
        echo "Usage: delete <path>"
        return
    fi
    adb_broadcast "adb.DELETE_SCRIPT" "path" "$path"
}

list_files() {
    local path="$1"
    if [[ -n "$path" ]]; then
        adb_broadcast "adb.LIST_FILES" "path" "$path"
    else
        adb_broadcast "adb.LIST_FILES"
    fi
}

ping_app() {
    adb_broadcast "adb.PING"
}

# Main command dispatcher
case "$1" in
    run)
        shift
        run_script "$@"
        ;;
    stop)
        shift
        stop_script "$@"
        ;;
    stop-all)
        stop_all_scripts
        ;;
    list)
        list_scripts
        ;;
    push)
        shift
        push_script "$@"
        ;;
    delete)
        shift
        delete_script "$@"
        ;;
    files)
        shift
        list_files "$@"
        ;;
    ping)
        ping_app
        ;;
    help|-h|--help)
        show_help
        ;;
    "")
        show_help
        ;;
    *)
        echo "Unknown command: $1"
        show_help
        ;;
esac