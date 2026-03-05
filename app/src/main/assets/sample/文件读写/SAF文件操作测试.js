/**
 * SAF 文件操作测试 (跨版本兼容性测试)
 * 
 * 本脚本整合自 Auto.js / AutoJs6 / AutoX / Auto.js.HYB1996 内置例子，
 * 用于测试 SAF (Storage Access Framework) 模式下文件操作的跨版本兼容性。
 * 
 * 【测试覆盖内容】
 * 1. 基础目录操作 - mkdir/mkdirs/exists/isDirectory
 * 2. open() 函数 - 写入(w)/追加(a)/读取(r) 模式
 * 3. 文件编码 - UTF-8/GBK 编码转换
 * 4. files 模块 API - write/read/append/writeBytes/readBytes
 * 5. 目录操作 - listDir/remove/removeDir
 * 6. 复制和移动 - copy/rename
 * 7. 中文路径和文件名
 * 8. 特殊文件名 - 点号开头/数字/空格
 * 9. 边界情况 - 空文件/大文件
 * 10. 错误处理 - 异常捕获
 * 11. 路径操作 - join/getName/getExtension
 * 
 * 【使用方法】
 * 1. 确保已授予脚本目录的 SAF 访问权限
 * 2. 运行此脚本，观察测试结果
 * 3. 所有测试通过表示 SAF 文件操作正常
 * 
 * 【注意事项】
 * - 测试会在 /storage/emulated/0/脚本/ 目录下创建 SAF_CROSS_TEST 临时目录
 * - 测试完成后会自动清理临时目录
 * - 部分测试需要 Android 11+ 的 SAF 权限
 * 
 * @author Auto.js Community
 * @version 1.0
 */
console.show();
console.log('╔══════════════════════════════════════╗');
console.log('║   SAF 文件操作测试 (跨版本兼容)     ║');
console.log('╚══════════════════════════════════════╝');
console.log('测试时间: ' + new Date().toLocaleString());
console.log('设备: ' + device.brand + ' ' + device.model);
console.log('Android: ' + device.release);
console.log('');

// ==================== 配置 ====================

/** 测试目录路径 */
var testDir = '/storage/emulated/0/脚本/SAF_CROSS_TEST';

/** 通过测试计数 */
var passCount = 0;

/** 失败测试计数 */
var failCount = 0;

/** 测试结果记录 */
var testResults = [];

// ==================== 测试辅助函数 ====================

/**
 * 输出测试分节标题
 * @param {string} name - 分节名称
 */
function section(name) {
    console.log('');
    console.log('【' + name + '】');
}

/**
 * 执行简单断言测试
 * @param {string} name - 测试名称
 * @param {*} result - 实际结果
 * @param {*} expected - 期望结果
 * @returns {boolean} 测试是否通过
 */
function test(name, result, expected) {
    var passed = (result === expected);
    var status = passed ? '✓' : '✗';
    console.log(status + ' ' + name);
    if (passed) {
        passCount++;
    } else {
        failCount++;
        console.log('  预期: ' + expected);
        console.log('  实际: ' + result);
    }
    testResults.push({name: name, passed: passed});
    return passed;
}

/**
 * 执行函数返回值测试
 * @param {string} name - 测试名称
 * @param {Function} fn - 测试函数，应返回 true 表示成功
 * @returns {boolean} 测试是否通过
 */
function testOk(name, fn) {
    try {
        var result = fn();
        if (result) {
            console.log('✓ ' + name);
            passCount++;
            return true;
        } else {
            console.log('✗ ' + name + ' (返回false)');
            failCount++;
            return false;
        }
    } catch (e) {
        console.log('✗ ' + name + ' (异常: ' + e.message + ')');
        failCount++;
        return false;
    }
}

/**
 * 执行异常抛出测试
 * @param {string} name - 测试名称
 * @param {Function} fn - 应抛出异常的测试函数
 * @returns {boolean} 测试是否通过（抛出异常为通过）
 */
function testThrows(name, fn) {
    try {
        fn();
        console.log('✗ ' + name + ' (未抛出异常)');
        failCount++;
        return false;
    } catch (e) {
        console.log('✓ ' + name);
        passCount++;
        return true;
    }
}

// ==================== 测试开始 ====================

// ========== 1. 基础目录操作 ==========
// 测试目录的创建、存在性检查、类型判断
// 参考自各版本 Auto.js 通用 API

section('1. 基础目录操作');

// 清理旧测试目录（如果存在）
if (files.exists(testDir)) {
    files.removeDir(testDir);
}

// 创建测试目录
test('1.1 mkdirs 创建目录', files.mkdirs(testDir), true);
test('1.2 exists 目录存在', files.exists(testDir), true);
test('1.3 isDirectory', files.isDirectory(testDir), true);
test('1.4 isFile 目录非文件', files.isFile(testDir), false);

// 测试嵌套目录创建
var deepDir = testDir + '/a/b/c/d/e';
test('1.5 mkdirs 深层嵌套', files.mkdirs(deepDir), true);
test('1.6 深层目录存在', files.exists(deepDir), true);

// ========== 2. open() 函数测试 ==========
// 测试文件的打开、写入、读取操作
// 参考自官方内置例子：写入文本文件.js、读取文本文件.js、读写文本文件.js

section('2. open() 函数测试');

var openFile = testDir + '/open_test.txt';

// 测试写入模式 (mode: 'w')
// 参考例子：写入文本文件.js
testOk('2.1 open(w) 创建文件', function() {
    var file = open(openFile, 'w');
    // write() 写入字符串，不自动换行
    file.write('aaaa');
    // writeline() 写入一行，自动换行
    file.writeline('bbbbb');
    // writelines() 写入多行
    file.writelines(['ccc', 'ddd']);
    file.close();
    return true;
});

// 测试追加模式 (mode: 'a')
// 参考例子：读写文本文件.js
testOk('2.2 open(a) 追加内容', function() {
    var file = open(openFile, 'a');
    file.writeline('追加行1');
    file.writeline('追加行2');
    // flush() 刷新缓冲区
    file.flush();
    file.close();
    return true;
});

// 测试读取模式 (mode: 'r')
// 参考例子：读取文本文件.js
testOk('2.3 open(r) 读取文件', function() {
    var file = open(openFile, 'r');
    // readline() 读取一行
    var line1 = file.readline();
    // read() 读取剩余全部内容
    var rest = file.read();
    file.close();
    return line1.indexOf('aaaa') >= 0 && rest.length > 0;
});

// 测试 readlines() 按行读取
// 参考例子：读写文本文件.js
testOk('2.4 readlines 按行读取', function() {
    var file = open(openFile, 'r');
    // readlines() 返回所有行的数组
    var lines = file.readlines();
    file.close();
    return lines.length >= 4;
});

// ========== 3. 文件编码测试 ==========
// 测试不同编码的文件读写
// 参考自官方内置例子：文件编码转换.js、文件编码转换(高级).js

section('3. 文件编码测试');

var utf8File = testDir + '/utf8.txt';
var gbkFile = testDir + '/gbk.txt';

// UTF-8 编码测试
testOk('3.1 UTF-8 编码写入', function() {
    // open() 第三个参数指定编码
    var f = open(utf8File, 'w', 'utf-8');
    f.write('你好世界\nHello World\n中文测试');
    f.close();
    return true;
});

testOk('3.2 UTF-8 编码读取', function() {
    var f = open(utf8File, 'r', 'utf-8');
    var text = f.read();
    f.close();
    return text.indexOf('你好世界') >= 0;
});

// GBK 编码测试
testOk('3.3 GBK 编码写入', function() {
    var f = open(gbkFile, 'w', 'gbk');
    f.write('GBK编码测试');
    f.close();
    return true;
});

testOk('3.4 GBK 编码读取', function() {
    var f = open(gbkFile, 'r', 'gbk');
    var text = f.read();
    f.close();
    return text.indexOf('GBK') >= 0;
});

// 编码转换测试：UTF-8 -> GBK
// 参考例子：文件编码转换.js
testOk('3.5 编码转换 UTF-8 -> GBK', function() {
    var fromFile = open(utf8File, 'r', 'utf-8');
    var toFile = open(testDir + '/converted.txt', 'w', 'gbk');
    var line;
    // 逐行读取并写入
    while ((line = fromFile.readline()) != null) {
        toFile.writeline(line);
    }
    fromFile.close();
    toFile.close();
    return files.exists(testDir + '/converted.txt');
});

// ========== 4. files 模块 API 测试 ==========
// 测试 files 模块提供的便捷方法

section('4. files 模块 API 测试');

var apiFile = testDir + '/api_test.txt';

// 基本读写测试
test('4.1 files.write', files.write(apiFile, 'API测试内容'), true);
test('4.2 files.read', files.read(apiFile), 'API测试内容');
test('4.3 files.append', files.append(apiFile, '\n追加内容'), true);
test('4.4 追加后内容', files.read(apiFile).indexOf('追加内容') >= 0, true);

// 二进制读写测试 - 普通 JS 数组
testOk('4.5 files.writeBytes/readBytes', function() {
    // JavaScript 数组表示字节数组
    var bytes = [72, 101, 108, 108, 111]; // "Hello" 的 ASCII 码
    files.writeBytes(testDir + '/bytes.bin', bytes);
    var readBytes = files.readBytes(testDir + '/bytes.bin');
    return readBytes.length === 5 && readBytes[0] === 72;
});

// 二进制读写测试 - Uint8Array
testOk('4.6 writeBytes 支持 Uint8Array', function() {
    // Uint8Array 是 ES6 引入的类型化数组，适合处理二进制数据
    var arr = new Uint8Array([0x89, 0x50, 0x4E, 0x47]); // PNG 文件魔数
    var result = files.writeBytes(testDir + '/uint8array.bin', arr);
    if (!result) return false;
    // 等待 Android MediaProvider 刷新
    sleep(200);
    if (!files.exists(testDir + '/uint8array.bin')) return false;
    var readBytes = files.readBytes(testDir + '/uint8array.bin');
    // 注意：Java 字节是有符号的 (-128 ~ 127)
    // 0x89 作为有符号字节是 -119，需要用 (b & 0xFF) 转换为无符号值
    return readBytes != null && readBytes.length === 4 && 
           (readBytes[0] & 0xFF) === 0x89 && (readBytes[1] & 0xFF) === 0x50;
});

// ========== 5. 目录操作测试 ==========
// 测试目录的创建、列举、删除
// 参考自官方内置例子：删除所有空文件夹.js

section('5. 目录操作测试');

var subDir1 = testDir + '/subdir1';
var subDir2 = testDir + '/subdir2/nested';

test('5.1 mkdir 创建目录', files.mkdir(subDir1), true);
test('5.2 mkdirs 创建嵌套目录', files.mkdirs(subDir2), true);
test('5.3 listDir 列出目录', files.listDir(testDir).length >= 5, true);

// 子目录文件操作
testOk('5.4 子目录文件操作', function() {
    files.write(subDir1 + '/child.txt', '子目录文件');
    return files.exists(subDir1 + '/child.txt');
});

// 删除测试
test('5.5 remove 删除文件', files.remove(subDir1 + '/child.txt'), true);
test('5.6 删除后不存在', files.exists(subDir1 + '/child.txt'), false);

// ========== 6. 复制和移动测试 ==========

section('6. 复制和移动测试');

var copySrc = testDir + '/copy_src.txt';
var copyDst = testDir + '/copy_dst.txt';
var moveSrc = testDir + '/move_src.txt';
var moveDst = testDir + '/move_dst.txt';

files.write(copySrc, '复制测试');
files.write(moveSrc, '移动测试');

// 复制测试
test('6.1 files.copy', files.copy(copySrc, copyDst), true);
test('6.2 复制后存在', files.exists(copyDst), true);
test('6.3 复制内容一致', files.read(copyDst), '复制测试');

// 重命名测试
test('6.4 files.rename', files.rename(moveSrc, 'move_dst.txt'), true);
test('6.5 重命名后原文件不存在', files.exists(moveSrc), false);
test('6.6 重命名后新文件存在', files.exists(moveDst), true);

// ========== 7. 中文路径和文件名测试 ==========

section('7. 中文路径和文件名测试');

var chineseDir = testDir + '/中文目录';
var chineseFile = chineseDir + '/中文文件.txt';

test('7.1 创建中文目录', files.mkdirs(chineseDir), true);
testOk('7.2 写入中文文件名', function() {
    files.write(chineseFile, '中文内容测试');
    return files.exists(chineseFile);
});
test('7.3 读取中文文件', files.read(chineseFile), '中文内容测试');
test('7.4 删除中文文件', files.remove(chineseFile), true);

// ========== 8. 特殊文件名测试 ==========

section('8. 特殊文件名测试');

// 点号开头的文件（类 Unix 系统的隐藏文件）
testOk('8.1 点号开头文件名', function() {
    files.write(testDir + '/.hidden', '隐藏文件');
    return files.exists(testDir + '/.hidden');
});

// 纯数字文件名
testOk('8.2 数字文件名', function() {
    files.write(testDir + '/12345.txt', '数字文件名');
    return files.exists(testDir + '/12345.txt');
});

// 包含空格的文件名
testOk('8.3 空格文件名', function() {
    files.write(testDir + '/file with spaces.txt', '空格文件名');
    return files.exists(testDir + '/file with spaces.txt');
});

// ========== 9. 边界情况测试 ==========

section('9. 边界情况测试');

// 空文件测试
testOk('9.1 创建空文件', function() {
    files.write(testDir + '/empty.txt', '');
    return true;
});
test('9.2 空文件存在', files.exists(testDir + '/empty.txt'), true);
test('9.3 空文件读取', files.read(testDir + '/empty.txt'), '');

// 单字符文件
testOk('9.4 单字符文件', function() {
    files.write(testDir + '/single.txt', 'X');
    return files.read(testDir + '/single.txt') === 'X';
});

// 大文件测试
testOk('9.5 大文件读写', function() {
    // 生成约 28KB 的测试数据
    var bigContent = '';
    for (var i = 0; i < 1000; i++) {
        bigContent += '这是第' + i + '行测试数据\n';
    }
    files.write(testDir + '/big_file.txt', bigContent);
    var readContent = files.read(testDir + '/big_file.txt');
    return readContent.length === bigContent.length;
});

// ========== 10. 错误处理测试 ==========

section('10. 错误处理测试');

// 读取不存在的文件应抛出异常
testThrows('10.1 读取不存在的文件应抛异常', function() {
    files.read(testDir + '/nonexistent_file.txt');
});

test('10.2 判断不存在的文件', files.isFile(testDir + '/nonexistent.txt'), false);
test('10.3 判断不存在的目录', files.isDirectory(testDir + '/nonexistent'), false);

// ========== 11. 路径操作测试 ==========

section('11. 路径操作测试');

// files.join() 拼接路径
test('11.1 files.join', files.join(testDir, 'test.txt'), testDir + '/test.txt');

// files.getName() 获取文件名
test('11.2 files.getName', files.getName(testDir + '/test.txt'), 'test.txt');

// files.getExtension() 获取扩展名
test('11.3 files.getExtension', files.getExtension(testDir + '/test.txt'), 'txt');

// ========== 12. 删除目录测试 ==========

section('12. 目录删除测试');

// 创建嵌套目录和文件
var deleteDir = testDir + '/to_delete/nested/deep';
files.mkdirs(deleteDir);
files.write(deleteDir + '/file.txt', '测试');

// removeDir 递归删除目录
test('12.1 removeDir 删除嵌套目录', files.removeDir(testDir + '/to_delete'), true);
test('12.2 删除后不存在', files.exists(testDir + '/to_delete'), false);

// ========== 13. 清理测试目录 ==========

section('13. 清理测试目录');

test('13.1 清理测试目录', files.removeDir(testDir), true);
test('13.2 清理后不存在', files.exists(testDir), false);

// ==================== 测试总结 ====================

console.log('');
console.log('╔══════════════════════════════════════╗');
console.log('║             测试总结                  ║');
console.log('╠══════════════════════════════════════╣');
console.log('║  通过: ' + passCount + '                            '.slice(0, 30 - passCount.toString().length) + '║');
console.log('║  失败: ' + failCount + '                            '.slice(0, 30 - failCount.toString().length) + '║');
console.log('║  总计: ' + (passCount + failCount) + '                           '.slice(0, 30 - (passCount + failCount).toString().length) + '║');
var rate = Math.round(passCount / (passCount + failCount) * 100);
console.log('║  成功率: ' + rate + '%                         '.slice(0, 29 - rate.toString().length) + '║');
console.log('╚══════════════════════════════════════╝');

if (failCount === 0) {
    console.log('');
    console.log('★ 所有测试通过！SAF 文件操作跨版本兼容性正常 ★');
} else {
    console.log('');
    console.log('! 存在 ' + failCount + ' 个失败项，请检查日志');
}
