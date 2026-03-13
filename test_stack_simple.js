// 简单堆栈测试 - 不使用require
console.log("=== 开始堆栈跳转测试 ===");

function level3() {
    console.log("level3: 即将抛出错误");
    throw new Error("测试错误 - 从level3抛出");
}

function level2() {
    console.log("level2: 调用level3");
    level3();
}

function level1() {
    console.log("level1: 调用level2");
    level2();
}

try {
    level1();
} catch (e) {
    console.error("捕获错误: " + e.message);
    console.error("堆栈追踪:");
    console.error(e.stack);
}

console.log("=== 测试结束 ===");
