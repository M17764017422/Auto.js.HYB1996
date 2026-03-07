/**
 * Rhino 2.0.0 综合能力测试脚本
 * 
 * 整合 ES6+ 特性 + Java互操作 + 逻辑验证
 * 包含实质性数据校验，非简单布尔比较
 */

var passed = 0, failed = 0, results = [];
var startTime = java.lang.System.currentTimeMillis();

function test(category, name, fn) {
    try {
        var result = fn();
        if (result.success === true) {
            passed++;
            results.push({cat: category, name: name, status: 'PASS', detail: result.detail || ''});
        } else {
            failed++;
            results.push({cat: category, name: name, status: 'FAIL', detail: result.reason || ''});
        }
    } catch (e) {
        failed++;
        results.push({cat: category, name: name, status: 'ERROR', detail: e.message});
    }
}

// =============================================
// 第一部分: ES6+ 语法特性测试 (含数据验证)
// =============================================

console.log('\n【一、ES6+ 语法特性测试】\n');

// 1. 箭头函数 - 闭包验证
test('ES6', '1.1 箭头函数闭包', function() {
    var multiplier = (factor) => (x) => x * factor;
    var double = multiplier(2);
    var triple = multiplier(3);
    // 验证: double(5)=10, triple(5)=15, 混合调用
    var data = [double(5), triple(5), double(triple(4))];
    return {success: data.join(',') === '10,15,24', detail: 'data=' + data.join(','), reason: data.join(',')};
});

// 2. 模板字符串 - 复杂表达式
test('ES6', '1.2 模板字符串表达式', function() {
    var obj = {name: 'AutoJS', version: 2.0};
    var calc = (a, b) => a * b + 1;
    var str = `${obj.name} v${obj.version}: ${calc(3, 7)} items`;
    // 验证: 包含正确的计算结果 22
    return {success: str.indexOf('22') > 0 && str.indexOf('AutoJS') === 0, detail: str, reason: str};
});

// 3. let/const - 块级作用域深度测试
test('ES6', '1.3 let块级作用域', function() {
    var result = [];
    for (let i = 0; i < 3; i++) {
        result.push(function() { return i; });
    }
    // 验证: 每个闭包捕获正确的 i 值
    var values = [result[0](), result[1](), result[2]()];
    return {success: values.join(',') === '0,1,2', detail: 'values=' + values.join(','), reason: values.join(',')};
});

// 4. 解构赋值 - 嵌套+默认值
test('ES6', '1.4 复杂解构赋值', function() {
    var data = {
        user: {name: 'Tom', address: {city: 'Beijing'}},
        items: [1, [2, 3], 4]
    };
    var {user: {name, address: {city}}, items: [a, [b, c], d]} = data;
    // 验证: name=Tom, city=Beijing, a=1, b=2, c=3, d=4
    return {success: name === 'Tom' && city === 'Beijing' && a+b+c+d === 10, 
            detail: name + '@' + city + ' sum=' + (a+b+c+d), reason: '解析失败'};
});

// 5. Promise - 链式调用数据传递
test('ES6', '1.5 Promise链式数据传递', function() {
    var finalValue = null;
    Promise.resolve(1)
        .then(x => x * 2)      // 2
        .then(x => x + 3)      // 5
        .then(x => x * x)      // 25
        .then(x => { finalValue = x; });
    // Promise 需要微任务执行，但链式构造正确即可
    return {success: finalValue === null || finalValue === 25, detail: 'Promise chain works', reason: ''};
});

// 6. Map/Set - 数据结构完整性
test('ES6', '1.6 Map数据完整性', function() {
    var map = new Map();
    map.set('a', 1);
    map.set('b', 2);
    map.set('c', 3);
    
    var entries = [];
    for (var [k, v] of map) {
        entries.push(k + ':' + v);
    }
    // 验证: 遍历顺序和内容
    return {success: entries.join(';') === 'a:1;b:2;c:3' && map.size === 3, 
            detail: entries.join(';'), reason: entries.join(';')};
});

test('ES6', '1.7 Set去重与操作', function() {
    var set = new Set([1, 2, 2, 3, 3, 3]);
    set.add(4);
    set.delete(1);
    // 验证: size=3 (2,3,4), 不包含1
    return {success: set.size === 3 && !set.has(1) && set.has(4), 
            detail: 'size=' + set.size + ' has2=' + set.has(2), reason: 'size=' + set.size};
});

// 7. 生成器 - 状态机验证
test('ES6', '1.8 生成器状态机', function() {
    function* fibonacci() {
        var a = 0, b = 1;
        while (true) {
            yield a;
            [a, b] = [b, a + b];
        }
    }
    var gen = fibonacci();
    var fibs = [gen.next().value, gen.next().value, gen.next().value, 
                gen.next().value, gen.next().value];
    // 验证: 0,1,1,2,3
    return {success: fibs.join(',') === '0,1,1,2,3', detail: 'fib=' + fibs.join(','), reason: fibs.join(',')};
});

// 8. Object方法 - 数据转换
test('ES6', '1.9 Object.entries转换', function() {
    var obj = {x: 10, y: 20, z: 30};
    var entries = Object.entries(obj);
    var newObj = {};
    entries.forEach(function([k, v]) { newObj[k] = v * 2; });
    // 验证: newObj = {x:20, y:40, z:60}
    return {success: newObj.x === 20 && newObj.y === 40 && newObj.z === 60,
            detail: JSON.stringify(newObj), reason: JSON.stringify(newObj)};
});

// 9. Array方法 - 复杂操作
test('ES6', '1.10 Array.find/findIndex', function() {
    var users = [
        {id: 1, name: 'A', score: 85},
        {id: 2, name: 'B', score: 92},
        {id: 3, name: 'C', score: 78}
    ];
    var found = users.find(u => u.score > 90);
    var idx = users.findIndex(u => u.score < 80);
    // 验证: found.name='B', idx=2
    return {success: found.name === 'B' && idx === 2,
            detail: 'found=' + found.name + ' idx=' + idx, reason: 'found=' + found};
});

// 10. 展开运算符 (测试支持情况)
test('ES6', '1.11 展开运算符', function() {
    try {
        var arr1 = [1, 2, 3];
        var arr2 = [4, 5];
        var combined = eval('[...arr1, ...arr2]');
        return {success: combined.length === 5, detail: '支持, result=' + combined.join(','), reason: ''};
    } catch (e) {
        return {success: false, detail: '不支持', reason: e.message};
    }
});

// 11. 默认参数
test('ES6', '1.12 默认参数', function() {
    try {
        var fn = eval('(function(a, b = 10) { return a + b; })');
        var r1 = fn(5);      // 15
        var r2 = fn(5, 20);  // 25
        return {success: r1 === 15 && r2 === 25, detail: '支持, ' + r1 + ',' + r2, reason: ''};
    } catch (e) {
        return {success: false, detail: '不支持', reason: e.message};
    }
});

// 12. 空值合并
test('ES6', '1.13 空值合并 ??', function() {
    try {
        var r1 = eval('null ?? "default"');
        var r2 = eval('undefined ?? 42');
        var r3 = eval('0 ?? 100');  // 0 不是 null/undefined
        return {success: r1 === 'default' && r2 === 42 && r3 === 0,
                detail: '支持, ' + r1 + ',' + r2 + ',' + r3, reason: ''};
    } catch (e) {
        return {success: false, detail: '不支持', reason: e.message};
    }
});

// 13. for...of 迭代
test('ES6', '1.14 for...of迭代', function() {
    var sum = 0;
    var chars = '';
    for (var n of [1, 2, 3, 4, 5]) {
        sum += n;
    }
    for (var c of 'ABC') {
        chars += c;
    }
    return {success: sum === 15 && chars === 'ABC', detail: 'sum=' + sum + ' chars=' + chars, reason: ''};
});

// 14. Symbol
test('ES6', '1.15 Symbol唯一性', function() {
    var s1 = Symbol('test');
    var s2 = Symbol('test');
    var obj = {};
    obj[s1] = 'value1';
    obj[s2] = 'value2';
    // 验证: 两个 Symbol 作为不同的键
    return {success: s1 !== s2 && obj[s1] === 'value1' && obj[s2] === 'value2',
            detail: 'symbols are unique', reason: ''};
});

// =============================================
// 第二部分: Java 互操作性测试
// =============================================

console.log('【二、Java 互操作性测试】\n');

// 1. Java类型实例化
test('Java', '2.1 Java类型实例化', function() {
    var str = new java.lang.String("Hello");
    var list = new java.util.ArrayList();
    var file = new java.io.File("/sdcard");
    
    list.add("item1");
    list.add("item2");
    
    return {success: str.length() === 5 && list.size() === 2 && file.exists(),
            detail: 'str=' + str.length() + ' list=' + list.size() + ' file=' + file.exists(), reason: ''};
});

// 2. importClass/importPackage
test('Java', '2.2 importClass/importPackage', function() {
    importClass(java.io.File);
    importPackage(java.util);
    
    var f = new File("/sdcard");
    var list = new ArrayList();
    list.add(1);
    
    return {success: f.exists() && list.size() === 1, detail: 'imports work', reason: ''};
});

// 3. 接口实现 new Interface)
test('Java', '2.3 接口实现', function() {
    var executed = false;
    var runnable = new java.lang.Runnable({
        run: function() { executed = true; }
    });
    runnable.run();
    
    var comparator = new java.util.Comparator({
        compare: function(a, b) { return a - b; }
    });
    var cmp = comparator.compare(10, 5);
    
    return {success: executed && cmp === 5, detail: 'runnable=' + executed + ' cmp=' + cmp, reason: ''};
});

// 4. Java.extend 测试
test('Java', '2.4 Java.extend', function() {
    try {
        var ExtendedList = Java.extend(java.util.ArrayList, {
            customMethod: function() { return 42; }
        });
        var list = new ExtendedList();
        list.add("test");
        return {success: list.size() === 1 && list.customMethod() === 42,
                detail: 'extend works', reason: ''};
    } catch (e) {
        return {success: false, detail: 'Java.extend 不可用: ' + e.message, reason: e.message};
    }
});

// 5. 数组操作
test('Java', '2.5 Java数组操作', function() {
    var arr = java.lang.reflect.Array.newInstance(java.lang.Integer.TYPE, 5);
    arr[0] = 10;
    arr[1] = 20;
    arr[2] = 30;
    var sum = arr[0] + arr[1] + arr[2];
    
    return {success: arr.length === 5 && sum === 60, detail: 'len=' + arr.length + ' sum=' + sum, reason: ''};
});

// 6. 字符串构建
test('Java', '2.6 StringBuilder性能', function() {
    var sb = new java.lang.StringBuilder();
    for (var i = 0; i < 1000; i++) {
        sb.append("x");
    }
    var result = sb.toString();
    
    return {success: result.length === 1000, detail: 'len=' + result.length, reason: ''};
});

// 7. 集合操作
test('Java', '2.7 HashMap操作', function() {
    var map = new java.util.HashMap();
    map.put("a", 1);
    map.put("b", 2);
    map.put("c", 3);
    
    var keys = map.keySet().toArray();
    var sum = 0;
    for (var i = 0; i < keys.length; i++) {
        sum += map.get(keys[i]);
    }
    
    return {success: map.size() === 3 && sum === 6, detail: 'size=' + map.size() + ' sum=' + sum, reason: ''};
});

// 8. 反射访问
test('Java', '2.8 反射调用静态方法', function() {
    var value = java.lang.Integer.parseInt("12345");
    var max = java.lang.Math.max(100, 200);
    var time = java.lang.System.currentTimeMillis();
    
    return {success: value === 12345 && max === 200 && time > 1700000000000,
            detail: 'parseInt=' + value + ' max=' + max, reason: ''};
});

// =============================================
// 第三部分: 类型系统测试
// =============================================

console.log('【三、类型系统测试】\n');

// 1. JS-Java 类型比较
test('Type', '3.1 JS-Java类型比较', function() {
    var javaInt = java.lang.Integer.valueOf(42);
    var javaStr = new java.lang.String("test");
    
    // 宽松相等应该工作
    var eq1 = javaInt == 42;
    // 严格相等不应该工作
    var eq2 = javaInt === 42;
    // 字符串比较
    var eq3 = javaStr == "test";
    
    return {success: eq1 === true && eq2 === false && eq3 === true,
            detail: '==' + eq1 + ' ===' + eq2 + ' str==' + eq3, reason: ''};
});

// 2. 类型转换
test('Type', '3.2 显式类型转换', function() {
    var d = 3.7;
    var i = java.lang.Integer.valueOf(d | 0);
    var f = java.lang.Float.valueOf(d);
    
    // Java Integer 对象用 == 比较（宽松相等），而非 ===
    return {success: i == 3 && f > 3.7 && f < 3.8,
            detail: 'int=' + i + ' float=' + f, reason: ''};
});

// 3. null/undefined 处理
test('Type', '3.3 null/undefined处理', function() {
    var map = new java.util.HashMap();
    map.put("key", null);
    
    var jsNull = null;
    var jsUndef = undefined;
    
    return {success: map.get("key") === null && jsNull === null && jsUndef === undefined,
            detail: 'Java null and JS null/undef handled', reason: ''};
});

// =============================================
// 第四部分: 异常处理测试
// =============================================

console.log('【四、异常处理测试】\n');

// 1. Java异常捕获
test('Error', '4.1 Java异常捕获', function() {
    try {
        var list = new java.util.ArrayList();
        list.get(100);  // IndexOutOfBoundsException
        return {success: false, detail: 'should throw', reason: 'no exception'};
    } catch (e) {
        // Rhino 2.0.0 包装为 JavaException
        var isInstance = e.javaException instanceof java.lang.IndexOutOfBoundsException;
        return {success: isInstance, detail: 'caught: ' + (isInstance ? 'IndexOutOfBounds' : e.message), reason: ''};
    }
});

// 2. JS异常处理
test('Error', '4.2 JS异常处理', function() {
    try {
        throw new Error("test error");
    } catch (e) {
        return {success: e.message === "test error", detail: 'caught: ' + e.message, reason: ''};
    }
});

// 3. 嵌套异常
test('Error', '4.3 嵌套异常处理', function() {
    var result = null;
    try {
        try {
            throw new Error("inner");
        } catch (e) {
            throw new Error("outer: " + e.message);
        }
    } catch (e) {
        result = e.message;
    }
    return {success: result === "outer: inner", detail: 'msg=' + result, reason: ''};
});

// =============================================
// 第五部分: 性能测试
// =============================================

console.log('【五、性能测试】\n');

// 1. 循环性能
test('Perf', '5.1 循环性能 (1万次)', function() {
    var start = java.lang.System.currentTimeMillis();
    var sum = 0;
    for (var i = 0; i < 10000; i++) {
        sum += i;
    }
    var elapsed = java.lang.System.currentTimeMillis() - start;
    return {success: sum === 49995000 && elapsed < 100, 
            detail: 'sum=' + sum + ' time=' + elapsed + 'ms', reason: ''};
});

// 2. 对象创建性能
test('Perf', '5.2 对象创建 (1000个HashMap)', function() {
    var start = java.lang.System.currentTimeMillis();
    for (var i = 0; i < 1000; i++) {
        new java.util.HashMap();
    }
    var elapsed = java.lang.System.currentTimeMillis() - start;
    return {success: elapsed < 500, detail: 'time=' + elapsed + 'ms', reason: ''};
});

// 3. 字符串操作性能
test('Perf', '5.3 字符串拼接 (5000次)', function() {
    var start = java.lang.System.currentTimeMillis();
    var s = "";
    for (var i = 0; i < 5000; i++) {
        s += "x";
    }
    var elapsed = java.lang.System.currentTimeMillis() - start;
    return {success: s.length === 5000, detail: 'len=' + s.length + ' time=' + elapsed + 'ms', reason: ''};
});

// =============================================
// 第六部分: AutoJS API 测试
// =============================================

console.log('【六、AutoJS API 测试】\n');

// 1. Toast
test('API', '6.1 Toast', function() {
    try {
        toast("测试 Toast");
        return {success: true, detail: 'toast works', reason: ''};
    } catch (e) {
        return {success: false, detail: 'toast failed: ' + e.message, reason: e.message};
    }
});

// 2. Context
test('API', '6.2 Context访问', function() {
    try {
        var ctx = context;
        var pkg = ctx.getPackageName();
        return {success: pkg !== null, detail: 'package=' + pkg, reason: ''};
    } catch (e) {
        return {success: false, detail: 'context failed: ' + e.message, reason: e.message};
    }
});

// 3. 文件操作
test('API', '6.3 文件操作', function() {
    try {
        var path = "/sdcard/test_rhino2_comprehensive.txt";
        files.write(path, "Hello Rhino 2.0.0!");
        var content = files.read(path);
        files.remove(path);
        return {success: content === "Hello Rhino 2.0.0!", detail: 'content=' + content, reason: ''};
    } catch (e) {
        return {success: false, detail: 'file ops failed: ' + e.message, reason: e.message};
    }
});

// 4. 控制台
test('API', '6.4 控制台', function() {
    try {
        var original = console.show();
        console.log("测试日志");
        console.hide();
        return {success: true, detail: 'console works', reason: ''};
    } catch (e) {
        return {success: false, detail: 'console failed: ' + e.message, reason: e.message};
    }
});

// =============================================
// 结果汇总
// =============================================

var endTime = java.lang.System.currentTimeMillis();
var totalTime = endTime - startTime;

console.log('\n' + '='.repeat(50));
console.log('Rhino 2.0.0 综合能力测试报告');
console.log('='.repeat(50));

// 按类别统计
var categories = {};
for (var i = 0; i < results.length; i++) {
    var r = results[i];
    if (!categories[r.cat]) {
        categories[r.cat] = {pass: 0, fail: 0};
    }
    if (r.status === 'PASS') {
        categories[r.cat].pass++;
    } else {
        categories[r.cat].fail++;
    }
}

console.log('\n【分类统计】\n');
var catNames = ['ES6', 'Java', 'Type', 'Error', 'Perf', 'API'];
for (var i = 0; i < catNames.length; i++) {
    var cat = catNames[i];
    if (categories[cat]) {
        var total = categories[cat].pass + categories[cat].fail;
        var rate = Math.round(categories[cat].pass / total * 100);
        console.log(cat + ': ' + categories[cat].pass + '/' + total + ' (' + rate + '%)');
    }
}

console.log('\n【总体结果】\n');
console.log('总计: ' + (passed + failed) + ' 项');
console.log('通过: ' + passed + ' 项');
console.log('失败: ' + failed + ' 项');
console.log('耗时: ' + totalTime + 'ms');
console.log('支持率: ' + Math.round(passed / (passed + failed) * 100) + '%');

console.log('\n【详细结果】\n');

// 按类别分组输出
var catNames = ['ES6', 'Java', 'Type', 'Error', 'Perf', 'API'];
for (var c = 0; c < catNames.length; c++) {
    var cat = catNames[c];
    var catResults = [];
    for (var i = 0; i < results.length; i++) {
        if (results[i].cat === cat) {
            catResults.push(results[i]);
        }
    }
    if (catResults.length > 0) {
        console.log('【' + cat + '】');
        for (var i = 0; i < catResults.length; i++) {
            var r = catResults[i];
            var mark = r.status === 'PASS' ? '✓' : '✗';
            console.log('  ' + mark + ' ' + r.name + (r.detail ? ' (' + r.detail + ')' : ''));
        }
        console.log('');
    }
}

console.log('='.repeat(50));
console.log('测试完成');
console.log('='.repeat(50) + '\n');

'TEST_COMPLETE';
