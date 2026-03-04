#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
Auto.js ADB Debug Tool
Usage: python autojs-adb.py <command> [options]

Commands:
  run <script|path>    - Run script (content or file path)
  stop <id>            - Stop script by ID
  stop-all             - Stop all running scripts
  list                 - List running scripts
  push <name> <code>   - Push script to device
  delete <path>        - Delete script file
  files [path]         - List script files
  read <path>          - Read file content
  mkdir <path>         - Create directory
  rename <old> <new>   - Rename/move file or directory
  output [id]          - Get script console output
  ping                 - Check if app is responding
"""

import argparse
import base64
import os
import subprocess
import sys
import shutil
from typing import Optional, Dict, List


# Package name - change this if using a different flavor
# Options: org.autojs.autojs (common), org.autojs.autojs.coolapk (coolapk), org.autojs.autojs.github (github)
PACKAGE_NAME = "org.autojs.autojs.coolapk"
RECEIVER_CLASS = f"{PACKAGE_NAME}/org.autojs.autojs.external.receiver.AdbDebugReceiver"


def find_adb() -> str:
    """Find adb executable in PATH or common locations."""
    adb = shutil.which("adb")
    if adb:
        return adb
    
    # Common paths on Windows
    common_paths = [
        r"F:\AIDE\sdk\platform-tools\adb.exe",
        r"C:\Android\sdk\platform-tools\adb.exe",
        r"%LOCALAPPDATA%\Android\Sdk\platform-tools\adb.exe",
        r"%USERPROFILE%\AppData\Local\Android\Sdk\platform-tools\adb.exe",
    ]
    
    for path in common_paths:
        expanded = os.path.expandvars(path)
        if os.path.exists(expanded):
            return expanded
    
    return "adb"


def run_adb(args: List[str]) -> tuple:
    """Run adb command and return (stdout, stderr, returncode)."""
    adb_path = find_adb()
    cmd = [adb_path] + args
    try:
        result = subprocess.run(cmd, capture_output=True, text=True, encoding='utf-8')
        return result.stdout, result.stderr, result.returncode
    except FileNotFoundError:
        print(f"Error: adb not found. Please ensure Android SDK platform-tools is in PATH.")
        sys.exit(1)


def adb_broadcast(action: str, extras: Optional[Dict] = None) -> None:
    """Send broadcast to AdbDebugReceiver."""
    cmd_args = [
        "shell", "am", "broadcast",
        "-n", RECEIVER_CLASS,
        "-a", f"{PACKAGE_NAME}.{action}"
    ]
    
    if extras:
        for key, value in extras.items():
            if key in ("delay", "id"):
                # Integer values
                cmd_args.extend(["--ei", key, str(value)])
            else:
                # String values
                cmd_args.extend(["--es", key, str(value)])
    
    stdout, stderr, code = run_adb(cmd_args)
    if stdout:
        print(stdout.strip())
    if stderr:
        print(stderr.strip(), file=sys.stderr)


def encode_base64(text: str) -> str:
    """Encode text to base64."""
    return base64.b64encode(text.encode('utf-8')).decode('ascii')


def cmd_run(args) -> None:
    """Run script command."""
    extras = {}
    
    if args.file:
        # Run from file path
        extras["path"] = args.file
    elif args.script:
        # Run script content with base64 encoding
        extras["script"] = encode_base64(args.script)
        extras["base64"] = "true"
    else:
        print("Error: Missing script content or file path")
        print("Usage: autojs-adb.py run <script> OR autojs-adb.py run -f <path>")
        return
    
    if args.delay and args.delay > 0:
        extras["delay"] = args.delay
    
    adb_broadcast("adb.RUN_SCRIPT", extras)


def cmd_stop(args) -> None:
    """Stop script by ID."""
    if args.id is None:
        print("Error: Missing script ID")
        print("Usage: autojs-adb.py stop <id>")
        return
    adb_broadcast("adb.STOP_SCRIPT", {"id": args.id})


def cmd_stop_all(args) -> None:
    """Stop all running scripts."""
    adb_broadcast("adb.STOP_ALL")


def cmd_list(args) -> None:
    """List running scripts."""
    adb_broadcast("adb.LIST_SCRIPTS")


def cmd_push(args) -> None:
    """Push script to device."""
    if not args.name or not args.content:
        print("Error: Missing name or content")
        print("Usage: autojs-adb.py push <name> <code>")
        return
    
    adb_broadcast("adb.PUSH_SCRIPT", {
        "name": args.name,
        "content": encode_base64(args.content),
        "base64": "true"
    })


def cmd_delete(args) -> None:
    """Delete script file."""
    if not args.path:
        print("Error: Missing file path")
        print("Usage: autojs-adb.py delete <path>")
        return
    adb_broadcast("adb.DELETE_SCRIPT", {"path": args.path})


def cmd_files(args) -> None:
    """List script files."""
    if args.recursive or args.tree:
        # 使用 adb shell 命令递归列出
        list_files_recursive(args.path, args.tree)
    else:
        # 使用广播列出单层目录
        extras = {}
        if args.path:
            extras["path"] = args.path
        adb_broadcast("adb.LIST_FILES", extras)


def list_files_recursive(path: Optional[str], tree_mode: bool = False) -> None:
    """递归列出文件，支持树形显示。"""
    if not path:
        path = "/sdcard/脚本"
    
    # 使用 find 命令一次性获取所有文件
    stdout, stderr, code = run_adb(["shell", f"find '{path}' -maxdepth 10 2>/dev/null | head -500"])
    
    if not stdout.strip():
        print(f"Error: Directory not found or empty: {path}")
        return
    
    lines = stdout.strip().split('\n')
    
    if tree_mode:
        _print_tree_from_lines(lines, path)
    else:
        _print_recursive_from_lines(lines)


def _print_tree_from_lines(lines: List[str], root: str) -> None:
    """从 find 输出构建树形显示。"""
    # 构建目录树结构
    tree = {}
    for line in lines:
        if not line.strip():
            continue
        # 规范化路径
        norm_line = line.rstrip('/')
        rel = os.path.relpath(norm_line, root)
        if rel == '.':
            continue
        parts = rel.replace('\\', '/').split('/')
        current = tree
        for part in parts:
            if part not in current:
                current[part] = {}
            current = current[part]
    
    print(root)
    _print_tree_node(tree, "")


def _print_tree_node(node: dict, prefix: str) -> None:
    """递归打印树节点。"""
    items = sorted(node.keys())
    for i, item in enumerate(items):
        is_last = (i == len(items) - 1)
        connector = "└── " if is_last else "├── "
        print(f"{prefix}{connector}{item}")
        
        if node[item]:  # 有子项
            new_prefix = prefix + ("    " if is_last else "│   ")
            _print_tree_node(node[item], new_prefix)


def _print_recursive_from_lines(lines: List[str]) -> None:
    """从 find 输出打印递归列表。"""
    for line in lines:
        if not line.strip():
            continue
        # 检查是否为目录（以 / 结尾或使用 test）
        rel = line.rstrip('/')
        # 简单判断：目录名通常不以扩展名结尾
        is_dir = '.' not in os.path.basename(rel) or rel.endswith('/')
        
        # 使用更准确的方式判断
        stdout, _, _ = run_adb(["shell", f"test -d '{line}' && echo D || echo F"])
        is_dir = "D" in stdout
        
        if is_dir:
            print(f"[D] {line}")
        else:
            # 获取文件大小
            stdout2, _, _ = run_adb(["shell", f"stat -c %s '{line}' 2>/dev/null || echo 0"])
            size = stdout2.strip()
            try:
                size_str = format_size(int(size))
            except:
                size_str = "?"
            print(f"[F] {line} ({size_str})")


def format_size(bytes: int) -> str:
    """格式化文件大小。"""
    if bytes < 1024:
        return f"{bytes}B"
    elif bytes < 1024 * 1024:
        return f"{bytes / 1024:.1f}KB"
    else:
        return f"{bytes / (1024 * 1024):.1f}MB"


def cmd_ping(args) -> None:
    """Check if app is responding."""
    adb_broadcast("adb.PING")


def cmd_read(args) -> None:
    """Read file content from device."""
    if not args.path:
        print("Error: Missing file path")
        print("Usage: autojs-adb.py read <path>")
        return
    
    extras = {"path": args.path}
    if args.base64:
        extras["base64"] = "true"
    
    adb_broadcast("adb.READ_FILE", extras)


def cmd_mkdir(args) -> None:
    """Create directory on device."""
    if not args.path:
        print("Error: Missing directory path")
        print("Usage: autojs-adb.py mkdir <path>")
        return
    
    adb_broadcast("adb.MKDIR", {"path": args.path})


def cmd_rename(args) -> None:
    """Rename/move file or directory."""
    if not args.oldpath or not args.newpath:
        print("Error: Missing old path or new path")
        print("Usage: autojs-adb.py rename <oldpath> <newpath>")
        return
    
    adb_broadcast("adb.RENAME_FILE", {
        "oldpath": args.oldpath,
        "newpath": args.newpath
    })


def cmd_output(args) -> None:
    """Get script console output."""
    extras = {}
    if args.id is not None:
        extras["id"] = args.id
    if args.lines:
        extras["lines"] = args.lines
    
    adb_broadcast("adb.GET_SCRIPT_OUTPUT", extras)


def cmd_logcat(args) -> None:
    """View app logs."""
    # Get app PID
    stdout, _, _ = run_adb(["shell", "pidof", PACKAGE_NAME])
    pid = stdout.strip()
    
    if not pid:
        print(f"Error: {PACKAGE_NAME} is not running")
        return
    
    print(f"App PID: {pid}")
    
    # Run logcat with PID filter
    adb_path = find_adb()
    cmd = [adb_path, "logcat", "-v", "time", "--pid", pid]
    
    if args.clear:
        cmd.insert(2, "-c")
    
    try:
        subprocess.run(cmd)
    except KeyboardInterrupt:
        print("\nLogcat stopped.")


def main():
    parser = argparse.ArgumentParser(
        description="Auto.js ADB Debug Tool",
        formatter_class=argparse.RawDescriptionHelpFormatter,
        epilog="""
Examples:
  # Run scripts
  python autojs-adb.py run "toast('Hello World')"
  python autojs-adb.py run "toast('中文测试')"
  python autojs-adb.py run -f "/sdcard/脚本/test.js"
  python autojs-adb.py run -f "/sdcard/脚本/test.js" -d 1000
  
  # Script management
  python autojs-adb.py list
  python autojs-adb.py stop 12345
  python autojs-adb.py stop-all
  python autojs-adb.py output 12345
  python autojs-adb.py output 12345 --lines 100
  
  # File operations
  python autojs-adb.py files
  python autojs-adb.py files "/sdcard/脚本/支付宝"
  python autojs-adb.py files -r
  python autojs-adb.py files --tree "/sdcard/脚本"
  python autojs-adb.py read "/sdcard/脚本/test.js"
  python autojs-adb.py read "/sdcard/test.png" --base64
  python autojs-adb.py push myscript "toast('Hi')"
  python autojs-adb.py mkdir "/sdcard/脚本/新目录"
  python autojs-adb.py rename "/sdcard/脚本/old.js" "/sdcard/脚本/new.js"
  python autojs-adb.py delete "/sdcard/脚本/test.js"
  
  # App status
  python autojs-adb.py ping
  python autojs-adb.py logcat
  python autojs-adb.py logcat -c

Note: Edit PACKAGE_NAME in script to match your app flavor:
  - org.autojs.autojs (common)
  - org.autojs.autojs.coolapk (coolapk)
  - org.autojs.autojs.github (github)
"""
    )
    
    subparsers = parser.add_subparsers(dest="command", help="Available commands")
    
    # run command
    run_parser = subparsers.add_parser("run", help="Run script content or file")
    run_parser.add_argument("script", nargs="?", help="Script content to run")
    run_parser.add_argument("-f", "--file", dest="file", help="Script file path on device")
    run_parser.add_argument("-d", "--delay", type=int, default=0, help="Delay in milliseconds")
    run_parser.set_defaults(func=cmd_run)
    
    # stop command
    stop_parser = subparsers.add_parser("stop", help="Stop script by ID")
    stop_parser.add_argument("id", type=int, help="Script ID to stop")
    stop_parser.set_defaults(func=cmd_stop)
    
    # stop-all command
    stop_all_parser = subparsers.add_parser("stop-all", help="Stop all running scripts")
    stop_all_parser.set_defaults(func=cmd_stop_all)
    
    # list command
    list_parser = subparsers.add_parser("list", help="List running scripts")
    list_parser.set_defaults(func=cmd_list)
    
    # push command
    push_parser = subparsers.add_parser("push", help="Push script to device")
    push_parser.add_argument("name", help="Script name")
    push_parser.add_argument("content", help="Script content")
    push_parser.set_defaults(func=cmd_push)
    
    # delete command
    delete_parser = subparsers.add_parser("delete", help="Delete script file")
    delete_parser.add_argument("path", help="File path to delete")
    delete_parser.set_defaults(func=cmd_delete)
    
    # files command
    files_parser = subparsers.add_parser("files", help="List script files")
    files_parser.add_argument("path", nargs="?", help="Directory path (default: /sdcard/脚本/)")
    files_parser.add_argument("-r", "--recursive", action="store_true", help="List files recursively")
    files_parser.add_argument("--tree", action="store_true", help="Display as tree structure")
    files_parser.set_defaults(func=cmd_files)
    
    # ping command
    ping_parser = subparsers.add_parser("ping", help="Check if app is responding")
    ping_parser.set_defaults(func=cmd_ping)
    
    # read command
    read_parser = subparsers.add_parser("read", help="Read file content from device")
    read_parser.add_argument("path", help="File path to read")
    read_parser.add_argument("--base64", action="store_true", help="Return base64 encoded content")
    read_parser.set_defaults(func=cmd_read)
    
    # mkdir command
    mkdir_parser = subparsers.add_parser("mkdir", help="Create directory on device")
    mkdir_parser.add_argument("path", help="Directory path to create")
    mkdir_parser.set_defaults(func=cmd_mkdir)
    
    # rename command
    rename_parser = subparsers.add_parser("rename", help="Rename/move file or directory")
    rename_parser.add_argument("oldpath", help="Old path")
    rename_parser.add_argument("newpath", help="New path")
    rename_parser.set_defaults(func=cmd_rename)
    
    # output command
    output_parser = subparsers.add_parser("output", help="Get script console output")
    output_parser.add_argument("id", nargs="?", type=int, default=None, help="Script ID (optional, shows all if not specified)")
    output_parser.add_argument("--lines", "-l", type=int, default=50, help="Number of lines to read (default: 50)")
    output_parser.set_defaults(func=cmd_output)
    
    # logcat command
    logcat_parser = subparsers.add_parser("logcat", help="View app logs in real-time")
    logcat_parser.add_argument("-c", "--clear", action="store_true", help="Clear logcat buffer first")
    logcat_parser.set_defaults(func=cmd_logcat)
    
    args = parser.parse_args()
    
    if args.command is None:
        parser.print_help()
        return
    
    args.func(args)


if __name__ == "__main__":
    main()
