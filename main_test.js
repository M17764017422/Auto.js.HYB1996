// main_test.js - 主测试脚本
console.log("主脚本开始");

var errorModule = require("/sdcard/脚本/error_script.js");

function callError() {
    console.log("调用错误模块...");
    errorModule.nestedCall();
}

try {
    callError();
} catch (e) {
    console.error("捕获到错误: " + e.message);
    console.error(e.stack);
}

console.log("主脚本结束");
