#!/usr/bin/env python3
"""
Auto.js 调试服务器 - 命令行工具
用于在电脑上调试 Auto.js 脚本，替代已下架的 VS Code 插件

使用方法:
1. 启动服务器: python autojs_debug_server.py
2. 在 Auto.js 手机端点击"连接电脑"，输入电脑 IP 地址
3. 连接成功后，可以使用以下命令:
   - run <file.js>  - 运行脚本文件
   - stop <id>      - 停止指定脚本
   - stopall        - 停止所有脚本
   - save <file.js> - 保存脚本到手机
   - logs           - 显示/隐藏日志
   - help           - 显示帮助
   - exit           - 退出程序

HTTP API 模式 (适合自动化):
   python autojs_debug_server.py --http
   然后可以通过 HTTP 请求控制:
   - POST /run {"file": "test.js"} - 运行脚本
   - POST /stop {"id": "script_1"} - 停止脚本
   - POST /stopall - 停止所有
   - GET /status - 查看状态
   - GET /list - 列出运行中的脚本

依赖: pip install websockets

================================================================================
相关源码路径 (用于扩展功能参考)
================================================================================

WebSocket 协议实现:
  app/src/main/java/org/autojs/autojs/pluginclient/
  ├── DevPluginService.java      # 服务端连接管理、状态维护
  ├── JsonWebSocket.java         # WebSocket 消息收发、JSON 解析
  └── DevPluginResponseHandler.java  # 命令路由和处理

Android 端连接入口:
  app/src/main/java/org/autojs/autojs/ui/main/drawer/DrawerFragment.java
  - mConnectionItem: "连接电脑" 菜单项
  - connectOrDisconnectToRemote(): 连接/断开处理
  - inputRemoteHost(): 输入服务器地址对话框

================================================================================
消息协议
================================================================================

握手流程:
  1. 客户端连接后发送: {"type": "hello", "data": {"device_name": ..., "client_version": 2}}
  2. 服务端回复: {"type": "hello", "data": {"server_version": ..., "name": ...}}

命令格式 (服务端 -> 客户端):
  {
    "type": "command",
    "data": {
      "command": "run|stop|stopAll|save|rerun",
      "id": "script_1",           # 脚本唯一标识
      "script": "...",            # 脚本内容 (run/save/rerun)
      "name": "test.js"           # 脚本名称 (可选)
    }
  }

响应格式 (客户端 -> 服务端):
  {"type": "log", "data": {"log": "日志内容"}}
  {"type": "command_result", "data": {"success": true, "message": "..."}}

================================================================================
命令处理器参考 (DevPluginResponseHandler.java)
================================================================================

Router mRouter = new Router.RootRouter("type")
    .handler("command", new Router("command")
        .handler("run", data -> { ... })      # 运行脚本
        .handler("stop", data -> { ... })     # 停止脚本
        .handler("save", data -> { ... })     # 保存脚本
        .handler("rerun", data -> { ... })    # 重运行
        .handler("stopAll", data -> { ... })) # 停止所有
    .handler("bytes_command", new Router("command")
        .handler("run_project", data -> { ... })   # 运行项目
        .handler("save_project", data -> { ... })); # 保存项目

================================================================================
扩展功能建议
================================================================================

1. 项目运行: 添加 bytes_command/run_project 支持，需要实现 ZIP 打包发送
2. 文件浏览: 扩展 list_files 命令，参考 AdbDebugReceiver.java
3. 实时日志: 订阅 log 类型消息，已在 handle_message 中实现
4. 断点调试: 需要扩展 Rhino Debugger 协议，参考 autojs/rhino/debug/
5. 变量监视: 参考 DebugBar.java 和 DebuggerSingleton.java

"""

import asyncio
import json
import os
import sys
import argparse
from datetime import datetime
from typing import Optional, Set, Dict
import socket
import threading
from http.server import HTTPServer, BaseHTTPRequestHandler
import urllib.parse

try:
    import websockets
    from websockets.asyncio.server import serve
except ImportError:
    print("错误: 需要安装 websockets 库")
    print("请运行: pip install websockets")
    sys.exit(1)

# 配置
DEFAULT_PORT = 9317
DEFAULT_HTTP_PORT = 9318
CLIENT_VERSION = 2
SERVER_VERSION = "1.0.0"

# 全局状态
connected_clients: Set = set()
current_client = None
show_logs = True
script_counter = 0
running_scripts: Dict = {}  # {script_id: {"name": str, "start_time": str, "file": str}}
http_server_instance = None


def get_local_ip():
    """获取本机局域网 IP 地址"""
    try:
        s = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
        s.connect(("8.8.8.8", 80))
        ip = s.getsockname()[0]
        s.close()
        return ip
    except Exception:
        return "127.0.0.1"


def log_print(level: str, message: str):
    """带时间戳的日志输出"""
    timestamp = datetime.now().strftime("%H:%M:%S")
    prefix = {
        "info": "\033[36m[INFO]\033[0m",
        "ok": "\033[32m[OK]\033[0m",
        "warn": "\033[33m[WARN]\033[0m",
        "error": "\033[31m[ERROR]\033[0m",
        "log": "\033[37m[LOG]\033[0m",
        "device": "\033[35m[DEVICE]\033[0m",
    }.get(level, "[LOG]")
    print(f"[{timestamp}] {prefix} {message}")


def build_message(msg_type: str, data: dict) -> str:
    """构建 JSON 消息"""
    return json.dumps({"type": msg_type, "data": data}, ensure_ascii=False)


async def send_command(websocket, command: str, params: dict = None):
    """发送命令到客户端"""
    global script_counter
    data = {"command": command}
    if params:
        data.update(params)
    if "id" not in data:
        script_counter += 1
        data["id"] = f"script_{script_counter}"
    
    message = json.dumps({
        "type": "command",
        "data": data
    }, ensure_ascii=False)
    
    await websocket.send(message)
    return data.get("id")


async def handle_message(websocket, message: str):
    """处理来自客户端的消息"""
    global current_client, show_logs
    
    try:
        data = json.loads(message)
        msg_type = data.get("type", "")
        
        if msg_type == "hello":
            # 握手响应
            device_info = data.get("data", {})
            device_name = device_info.get("device_name", "Unknown")
            app_version = device_info.get("app_version", "Unknown")
            
            log_print("device", f"设备连接: {device_name}")
            log_print("device", f"应用版本: {app_version}")
            
            # 回复 hello
            response = build_message("hello", {
                "server_version": SERVER_VERSION,
                "name": socket.gethostname()
            })
            await websocket.send(response)
            
            connected_clients.add(websocket)
            current_client = websocket
            log_print("ok", "握手成功，可以开始调试")
            
        elif msg_type == "log":
            # 日志消息
            if show_logs:
                log_data = data.get("data", {})
                log_content = log_data.get("log", "")
                log_print("log", log_content.rstrip())
                
        elif msg_type == "command_result":
            # 命令执行结果
            result = data.get("data", {})
            success = result.get("success", False)
            message = result.get("message", "")
            if success:
                log_print("ok", message or "命令执行成功")
            else:
                log_print("error", message or "命令执行失败")
                
    except json.JSONDecodeError as e:
        log_print("error", f"JSON 解析错误: {e}")
    except Exception as e:
        log_print("error", f"处理消息错误: {e}")


async def handle_client(websocket):
    """处理客户端连接"""
    remote_addr = websocket.remote_address
    log_print("info", f"新连接: {remote_addr}")
    
    try:
        async for message in websocket:
            await handle_message(websocket, message)
    except Exception as e:
        if "close" in str(e).lower() or "connection" in str(e).lower():
            log_print("warn", f"连接断开: {remote_addr}")
        else:
            log_print("error", f"连接错误: {e}")
    finally:
        connected_clients.discard(websocket)
        if current_client == websocket:
            current_client = None


async def run_script(websocket, file_path: str) -> str:
    """运行脚本文件"""
    global script_counter
    
    if not os.path.exists(file_path):
        log_print("error", f"文件不存在: {file_path}")
        return None
    
    try:
        with open(file_path, "r", encoding="utf-8") as f:
            script = f.read()
    except Exception as e:
        log_print("error", f"读取文件失败: {e}")
        return None
    
    name = os.path.basename(file_path)
    script_counter += 1
    script_id = f"script_{script_counter}"
    
    data = {"command": "run", "script": script, "name": name, "id": script_id}
    message = json.dumps({"type": "command", "data": data}, ensure_ascii=False)
    await websocket.send(message)
    
    # 记录运行中的脚本
    running_scripts[script_id] = {
        "name": name,
        "file": file_path,
        "start_time": datetime.now().strftime("%H:%M:%S")
    }
    
    log_print("info", f"已发送脚本: {name} (ID: {script_id})")
    return script_id


async def run_script_direct(file_path: str) -> dict:
    """直接运行脚本 (用于 HTTP API)"""
    global script_counter, current_client
    
    if current_client is None:
        return {"success": False, "error": "没有连接的设备"}
    
    if not os.path.exists(file_path):
        return {"success": False, "error": f"文件不存在: {file_path}"}
    
    try:
        with open(file_path, "r", encoding="utf-8") as f:
            script = f.read()
    except Exception as e:
        return {"success": False, "error": f"读取文件失败: {e}"}
    
    name = os.path.basename(file_path)
    script_counter += 1
    script_id = f"script_{script_counter}"
    
    data = {"command": "run", "script": script, "name": name, "id": script_id}
    message = json.dumps({"type": "command", "data": data}, ensure_ascii=False)
    await current_client.send(message)
    
    # 记录运行中的脚本
    running_scripts[script_id] = {
        "name": name,
        "file": file_path,
        "start_time": datetime.now().strftime("%H:%M:%S")
    }
    
    return {"success": True, "script_id": script_id, "name": name}


async def stop_script_direct(script_id: str) -> dict:
    """停止脚本 (用于 HTTP API)"""
    global current_client
    
    if current_client is None:
        return {"success": False, "error": "没有连接的设备"}
    
    data = {"command": "stop", "id": script_id}
    message = json.dumps({"type": "command", "data": data}, ensure_ascii=False)
    await current_client.send(message)
    
    if script_id in running_scripts:
        del running_scripts[script_id]
    
    return {"success": True, "message": f"已停止脚本: {script_id}"}


async def stop_all_scripts_direct() -> dict:
    """停止所有脚本 (用于 HTTP API)"""
    global current_client
    
    if current_client is None:
        return {"success": False, "error": "没有连接的设备"}
    
    data = {"command": "stopAll"}
    message = json.dumps({"type": "command", "data": data}, ensure_ascii=False)
    await current_client.send(message)
    
    running_scripts.clear()
    
    return {"success": True, "message": "已停止所有脚本"}


async def stop_script(websocket, script_id: str):
    """停止脚本"""
    await send_command(websocket, "stop", {"id": script_id})
    # 从记录中移除
    if script_id in running_scripts:
        del running_scripts[script_id]
    log_print("info", f"已发送停止命令: {script_id}")


async def stop_all_scripts(websocket):
    """停止所有脚本"""
    await send_command(websocket, "stopAll")
    # 清空记录
    running_scripts.clear()
    log_print("info", "已发送停止所有脚本命令")


async def save_script(websocket, file_path: str):
    """保存脚本到手机"""
    if not os.path.exists(file_path):
        log_print("error", f"文件不存在: {file_path}")
        return
    
    try:
        with open(file_path, "r", encoding="utf-8") as f:
            script = f.read()
    except Exception as e:
        log_print("error", f"读取文件失败: {e}")
        return
    
    name = os.path.basename(file_path)
    await send_command(websocket, "save", {
        "script": script,
        "name": name
    })
    log_print("info", f"已保存脚本: {name}")


def print_help():
    """打印帮助信息"""
    print("""
命令帮助:
  run <file.js>    - 运行脚本文件
  rerun <file.js>  - 重新运行脚本 (先停止再运行)
  stop <id>        - 停止指定 ID 的脚本
  stopall          - 停止所有脚本
  list / ps        - 列出运行中的脚本
  save <file.js>   - 保存脚本到手机
  logs [on|off]    - 显示/隐藏日志 (默认开启)
  status           - 显示连接状态
  help             - 显示此帮助
  exit / quit      - 退出程序
  
快捷键:
  Ctrl+C           - 退出程序
""")


async def command_loop(server):
    """命令行交互循环"""
    global show_logs, current_client
    
    print("\n" + "=" * 50)
    print("Auto.js 调试服务器已启动")
    print("=" * 50)
    print(f"本机 IP: {get_local_ip()}:{DEFAULT_PORT}")
    print("请在 Auto.js 手机端点击「连接电脑」并输入上述地址")
    print("输入 'help' 查看命令帮助\n")
    
    while True:
        try:
            # 使用 asyncio 来处理输入，避免阻塞
            loop = asyncio.get_event_loop()
            cmd_input = await loop.run_in_executor(
                None, 
                lambda: input("\033[32mdebug>\033[0m ").strip()
            )
            
            if not cmd_input:
                continue
            
            parts = cmd_input.split(maxsplit=1)
            cmd = parts[0].lower()
            arg = parts[1] if len(parts) > 1 else None
            
            if cmd in ("exit", "quit"):
                print("再见!")
                server.close()
                break
                
            elif cmd == "help":
                print_help()
                
            elif cmd == "status":
                if current_client:
                    print(f"已连接客户端数: {len(connected_clients)}")
                    print("状态: 已连接，可以调试")
                else:
                    print("状态: 等待设备连接...")
                    
            elif cmd in ("list", "ps"):
                if not running_scripts:
                    print("没有运行中的脚本")
                else:
                    print(f"\n运行中的脚本 ({len(running_scripts)} 个):")
                    print("-" * 50)
                    print(f"{'ID':<20} {'名称':<20} {'启动时间'}")
                    print("-" * 50)
                    for sid, info in running_scripts.items():
                        print(f"{sid:<20} {info['name']:<20} {info['start_time']}")
                    print("-" * 50)
                    
            elif cmd == "logs":
                if arg == "off":
                    show_logs = False
                    print("日志已关闭")
                elif arg == "on":
                    show_logs = True
                    print("日志已开启")
                else:
                    show_logs = not show_logs
                    print(f"日志: {'开启' if show_logs else '关闭'}")
                    
            elif cmd == "run":
                if not current_client:
                    print("错误: 没有连接的设备")
                    continue
                if not arg:
                    print("用法: run <file.js>")
                    continue
                await run_script(current_client, arg)
                
            elif cmd == "rerun":
                if not current_client:
                    print("错误: 没有连接的设备")
                    continue
                if not arg:
                    print("用法: rerun <file.js>")
                    continue
                    
                if not os.path.exists(arg):
                    print(f"错误: 文件不存在: {arg}")
                    continue
                    
                with open(arg, "r", encoding="utf-8") as f:
                    script = f.read()
                name = os.path.basename(arg)
                script_id = await send_command(current_client, "rerun", {
                    "script": script,
                    "name": name
                })
                # 更新记录
                running_scripts[script_id] = {
                    "name": name,
                    "file": arg,
                    "start_time": datetime.now().strftime("%H:%M:%S")
                }
                log_print("info", f"已发送重运行命令: {name}")
                
            elif cmd == "stop":
                if not current_client:
                    print("错误: 没有连接的设备")
                    continue
                if not arg:
                    print("用法: stop <id>")
                    continue
                await stop_script(current_client, arg)
                
            elif cmd == "stopall":
                if not current_client:
                    print("错误: 没有连接的设备")
                    continue
                await stop_all_scripts(current_client)
                
            elif cmd == "save":
                if not current_client:
                    print("错误: 没有连接的设备")
                    continue
                if not arg:
                    print("用法: save <file.js>")
                    continue
                await save_script(current_client, arg)
                
            else:
                print(f"未知命令: {cmd}")
                print("输入 'help' 查看帮助")
                
        except EOFError:
            # 输入流结束，退出
            print("\n再见!")
            server.close()
            break
        except KeyboardInterrupt:
            print("\n再见!")
            server.close()
            break
        except Exception as e:
            print(f"错误: {e}")


# ==================== HTTP API ====================

class HTTPRequestHandler(BaseHTTPRequestHandler):
    """HTTP 请求处理器"""
    
    def log_message(self, format, *args):
        """禁用默认日志"""
        pass
    
    def send_json_response(self, data: dict, status: int = 200):
        """发送 JSON 响应"""
        self.send_response(status)
        self.send_header('Content-Type', 'application/json; charset=utf-8')
        self.send_header('Access-Control-Allow-Origin', '*')
        self.end_headers()
        self.wfile.write(json.dumps(data, ensure_ascii=False).encode('utf-8'))
    
    def do_GET(self):
        """处理 GET 请求"""
        parsed = urllib.parse.urlparse(self.path)
        path = parsed.path
        
        if path == '/status':
            self.send_json_response({
                "connected": current_client is not None,
                "client_count": len(connected_clients),
                "local_ip": get_local_ip(),
                "ws_port": DEFAULT_PORT
            })
        elif path in ('/list', '/ps'):
            self.send_json_response({
                "scripts": running_scripts,
                "count": len(running_scripts)
            })
        elif path == '/':
            # 返回 API 说明
            self.send_json_response({
                "name": "Auto.js Debug Server API",
                "version": SERVER_VERSION,
                "endpoints": {
                    "GET /": "显示 API 说明",
                    "GET /status": "查看连接状态",
                    "GET /list": "列出运行中的脚本",
                    "POST /run": "运行脚本 {\"file\": \"path/to/script.js\"}",
                    "POST /stop": "停止脚本 {\"id\": \"script_1\"}",
                    "POST /stopall": "停止所有脚本"
                }
            })
        else:
            self.send_json_response({"error": "Not found"}, 404)
    
    def do_POST(self):
        """处理 POST 请求"""
        parsed = urllib.parse.urlparse(self.path)
        path = parsed.path
        
        # 读取请求体
        content_length = int(self.headers.get('Content-Length', 0))
        body = self.rfile.read(content_length).decode('utf-8') if content_length > 0 else '{}'
        
        try:
            data = json.loads(body) if body else {}
        except json.JSONDecodeError:
            self.send_json_response({"error": "Invalid JSON"}, 400)
            return
        
        # 获取事件循环
        loop = None
        try:
            loop = asyncio.get_event_loop()
        except RuntimeError:
            loop = asyncio.new_event_loop()
            asyncio.set_event_loop(loop)
        
        if path == '/run':
            file_path = data.get('file')
            if not file_path:
                self.send_json_response({"error": "Missing 'file' parameter"}, 400)
                return
            
            if loop.is_running():
                # 如果事件循环正在运行，使用 run_coroutine_threadsafe
                future = asyncio.run_coroutine_threadsafe(run_script_direct(file_path), loop)
                result = future.result(timeout=5)
            else:
                result = loop.run_until_complete(run_script_direct(file_path))
            
            self.send_json_response(result)
            
        elif path == '/stop':
            script_id = data.get('id')
            if not script_id:
                self.send_json_response({"error": "Missing 'id' parameter"}, 400)
                return
            
            if loop.is_running():
                future = asyncio.run_coroutine_threadsafe(stop_script_direct(script_id), loop)
                result = future.result(timeout=5)
            else:
                result = loop.run_until_complete(stop_script_direct(script_id))
            
            self.send_json_response(result)
            
        elif path == '/stopall':
            if loop.is_running():
                future = asyncio.run_coroutine_threadsafe(stop_all_scripts_direct(), loop)
                result = future.result(timeout=5)
            else:
                result = loop.run_until_complete(stop_all_scripts_direct())
            
            self.send_json_response(result)
            
        else:
            self.send_json_response({"error": "Not found"}, 404)


def run_http_server(port: int):
    """运行 HTTP 服务器"""
    global http_server_instance
    http_server_instance = HTTPServer(('0.0.0.0', port), HTTPRequestHandler)
    log_print("info", f"HTTP API 服务启动: http://{get_local_ip()}:{port}")
    http_server_instance.serve_forever()


async def run_http_mode(http_port: int):
    """HTTP API 模式"""
    print("\n" + "=" * 50)
    print("Auto.js 调试服务器 (HTTP API 模式)")
    print("=" * 50)
    print(f"WebSocket: ws://{get_local_ip()}:{DEFAULT_PORT}")
    print(f"HTTP API:  http://{get_local_ip()}:{http_port}")
    print("\n请在 Auto.js 手机端点击「连接电脑」并输入上述 WebSocket 地址")
    print("按 Ctrl+C 退出\n")
    
    # 在后台线程启动 HTTP 服务器
    http_thread = threading.Thread(target=run_http_server, args=(http_port,), daemon=True)
    http_thread.start()
    
    # 保持运行
    try:
        while True:
            await asyncio.sleep(1)
    except asyncio.CancelledError:
        pass


async def run_single_command(cmd: str, arg: str = None, timeout: int = 10) -> dict:
    """执行单个命令 (用于 -e 模式)"""
    global current_client, script_counter
    
    # 等待设备连接
    print(f"等待设备连接...")
    for _ in range(timeout):
        if current_client is not None:
            break
        await asyncio.sleep(1)
    
    if current_client is None:
        return {"success": False, "error": "设备连接超时"}
    
    if cmd == "run":
        if not arg:
            return {"success": False, "error": "缺少文件路径"}
        return await run_script_direct(arg)
    elif cmd == "stop":
        if not arg:
            return {"success": False, "error": "缺少脚本 ID"}
        return await stop_script_direct(arg)
    elif cmd == "stopall":
        return await stop_all_scripts_direct()
    elif cmd == "status":
        return {
            "success": True,
            "connected": True,
            "running_scripts": list(running_scripts.keys())
        }
    else:
        return {"success": False, "error": f"未知命令: {cmd}"}


async def main():
    """主函数"""
    global show_logs
    
    parser = argparse.ArgumentParser(description="Auto.js 调试服务器")
    parser.add_argument("-p", "--port", type=int, default=DEFAULT_PORT, 
                        help=f"WebSocket 监听端口 (默认: {DEFAULT_PORT})")
    parser.add_argument("-H", "--host", default="0.0.0.0",
                        help="监听地址 (默认: 0.0.0.0)")
    parser.add_argument("--http", action="store_true",
                        help="启用 HTTP API 模式")
    parser.add_argument("--http-port", type=int, default=DEFAULT_HTTP_PORT,
                        help=f"HTTP API 端口 (默认: {DEFAULT_HTTP_PORT})")
    parser.add_argument("-e", "--exec", type=str, metavar="COMMAND",
                        help="执行单个命令后退出 (格式: run:file.js 或 stop:id 或 stopall)")
    parser.add_argument("--timeout", type=int, default=10,
                        help="等待设备连接超时时间 (默认: 10秒)")
    parser.add_argument("--quiet", action="store_true",
                        help="安静模式，不显示日志")
    args = parser.parse_args()
    
    if args.quiet:
        show_logs = False
    
    # 单命令模式
    if args.exec:
        cmd_parts = args.exec.split(":", 1)
        cmd = cmd_parts[0].lower()
        arg = cmd_parts[1] if len(cmd_parts) > 1 else None
        
        async with serve(handle_client, args.host, args.port):
            result = await run_single_command(cmd, arg, args.timeout)
            print(json.dumps(result, ensure_ascii=False))
        return
    
    # HTTP API 模式
    if args.http:
        async with serve(handle_client, args.host, args.port):
            await run_http_mode(args.http_port)
        return
    
    # 交互模式
    print(f"启动调试服务器 on {args.host}:{args.port}")
    
    async with serve(handle_client, args.host, args.port) as server:
        await command_loop(server)


if __name__ == "__main__":
    try:
        asyncio.run(main())
    except KeyboardInterrupt:
        print("\n服务器已停止")