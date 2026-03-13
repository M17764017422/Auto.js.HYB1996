// error_script.js - 错误脚本
console.log("错误脚本加载");

function causeError() {
    console.log("即将抛出错误...");
    throw new Error("这是一个测试错误！");
}

function nestedCall() {
    console.log("嵌套调用中...");
    causeError();
}

module.exports = {
    causeError: causeError,
    nestedCall: nestedCall
};
