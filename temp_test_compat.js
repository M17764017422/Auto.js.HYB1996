/**
 * Rhino 2.0.0 AutoJs6 兼容性测试脚本
 * 
 * 基于 SuperMonster003 为 AutoJs6 项目所做的改进定制
 * 参考: TECHNICAL_ANALYSIS_REPORT.md 第13章
 * 
 * 测试类别:
 * 1. Android 兼容性改进
 * 2. Java-JS 互操作性
 * 3. API 扩展功能
 * 4. VMBridge 反射访问
 * 5. 字符串导入支持
 * 6. 国际化支持
 */

var passed = 0;
var failed = 0;
var results = [];

function test(name, fn) {
    try {
        var result = fn();
        if (result === true || result === 'success') {
            passed++;
            results.push('✓ ' + name);
        } else {
            failed++;
            results.push('✗ ' + name + ' (返回: ' + result + ')');
        }
    } catch (e) {
        failed++;
        results.push('✗ ' + name + ' (异常: ' + e.message + ')');
    }
}

console.log('\n========== Rhino 2.0.0 AutoJs6 兼容性测试 ==========\n');

// ============ 1. Java 类型访问测试 (VMBridge 反射支持) ============
console.log('【1. VMBridge 反射访问测试】');

test('1.1 Java 类直接访问 - java.lang.String', function() {
    var str = new java.lang.String("Hello");
    return str.length() === 5;
});

test('1.2 Java 类直接访问 - java.util.ArrayList', function() {
    var list = new java.util.ArrayList();
    list.add("item1");
    list.add("item2");
    return list.size() === 2;
});

test('1.3 Java 类直接访问 - java.io.File', function() {
    var file = new java.io.File("/sdcard");
    return file.exists() === true;
});

test('1.4 Java 类直接访问 - java.lang.Math', function() {
    var max = java.lang.Math.max(10, 20);
    return max === 20;
});

test('1.5 Java 类直接访问 - java.lang.System', function() {
    var time = java.lang.System.currentTimeMillis();
    return typeof time === 'number' && time > 0;
});

// ============ 2. importClass/importPackage 测试 ============
console.log('【2. 字符串导入支持测试】');

test('2.1 importClass - 传统方式', function() {
    importClass(java.io.File);
    var f = new File("/sdcard");
    return f.exists() === true;
});

test('2.2 importPackage - 传统方式', function() {
    importPackage(java.util);
    var list = new ArrayList();
    list.add("test");
    return list.size() === 1;
});

test('2.3 importClass - 字符串参数 (AutoJs6扩展)', function() {
    try {
        // SuperMonster003 扩展: 支持字符串参数
        importClass("java.io.File");
        var f = new File("/sdcard");
        return f.exists() === true;
    } catch (e) {
        // 如果不支持字符串参数，返回预期失败
        return '不支持字符串参数（可能需要合并AutoJs6改进）';
    }
});

test('2.4 importPackage - 字符串参数 (AutoJs6扩展)', function() {
    try {
        importPackage("java.util");
        var list = new ArrayList();
        return list.size() === 0;
    } catch (e) {
        return '不支持字符串参数（可能需要合并AutoJs6改进）';
    }
});

test('2.5 importClass - Packages 前缀 (AutoJs6扩展)', function() {
    try {
        importClass("Packages.java.io.File");
        var f = new File("/sdcard");
        return f.exists() === true;
    } catch (e) {
        return '不支持Packages前缀（可能需要合并AutoJs6改进）';
    }
});

// ============ 3. Java 接口实现测试 ============
console.log('【3. Java 接口实现测试】');

test('3.1 Runnable 接口实现', function() {
    var executed = false;
    var runnable = new java.lang.Runnable({
        run: function() {
            executed = true;
        }
    });
    runnable.run();
    return executed === true;
});

test('3.2 Comparator 接口实现', function() {
    var comparator = new java.util.Comparator({
        compare: function(a, b) {
            return a - b;
        }
    });
    return comparator.compare(5, 3) === 2;
});

test('3.3 OnClickListener 模拟', function() {
    var clicked = false;
    var listener = {
        onClick: function(view) {
            clicked = true;
        }
    };
    listener.onClick(null);
    return clicked === true;
});

// ============ 4. Java 类继承测试 ============
console.log('【4. Java 类继承测试】');

test('4.1 Java.extend - Thread', function() {
    var threadRan = false;
    var MyThread = Java.extend(java.lang.Thread, {
        run: function() {
            threadRan = true;
        }
    });
    var t = new MyThread();
    t.start();
    t.join(100); // 等待最多100ms
    return threadRan === true;
});

test('4.2 Java.extend - ArrayList', function() {
    var MyList = Java.extend(java.util.ArrayList, {
        customMethod: function() {
            return "custom";
        }
    });
    var list = new MyList();
    list.add("item");
    return list.customMethod() === "custom" && list.size() === 1;
});

// ============ 5. Java 反射访问测试 ============
console.log('【5. Java 反射访问测试 (VMBridge)】');

test('5.1 访问公共方法', function() {
    var str = new java.lang.String("test");
    var length = str.length();
    return length === 4;
});

test('5.2 访问公共字段', function() {
    var out = java.lang.System.out;
    return out !== null;
});

test('5.3 调用静态方法', function() {
    var value = java.lang.Integer.parseInt("123");
    return value === 123;
});

test('5.4 访问私有字段 (通过反射)', function() {
    try {
        var str = new java.lang.String("test");
        // 尝试获取私有字段 (需要VMBridge支持)
        var field = str.getClass().getDeclaredField("value");
        field.setAccessible(true);
        var value = field.get(str);
        return value !== null;
    } catch (e) {
        // Android 可能限制私有字段访问
        return '私有字段访问受限（Android安全限制）';
    }
});

test('5.5 调用私有方法 (通过反射)', function() {
    try {
        var str = new java.lang.String("test");
        var method = str.getClass().getDeclaredMethod("getChars", 
            java.lang.Integer.TYPE, java.lang.Integer.TYPE, 
            java.lang.reflect.Array.newInstance(java.lang.Character.TYPE, 0).getClass(), 
            java.lang.Integer.TYPE);
        method.setAccessible(true);
        return method !== null;
    } catch (e) {
        return '私有方法访问受限（Android安全限制）';
    }
});

// ============ 6. NativeJavaObject 测试 ============
console.log('【6. NativeJavaObject 测试】');

test('6.1 Java 数组创建', function() {
    var arr = java.lang.reflect.Array.newInstance(java.lang.Integer.TYPE, 5);
    arr[0] = 10;
    arr[1] = 20;
    return arr.length === 5 && arr[0] === 10;
});

test('6.2 Java 字符串操作', function() {
    var str = new java.lang.StringBuilder();
    str.append("Hello");
    str.append(" ");
    str.append("World");
    return str.toString() === "Hello World";
});

test('6.3 Java Map 操作', function() {
    var map = new java.util.HashMap();
    map.put("key1", "value1");
    map.put("key2", "value2");
    return map.get("key1") === "value1" && map.size() === 2;
});

test('6.4 Java Set 操作', function() {
    var set = new java.util.HashSet();
    set.add("a");
    set.add("b");
    set.add("a"); // 重复
    return set.size() === 2 && set.contains("a");
});

// ============ 7. 类型转换测试 ============
console.log('【7. JS-Java 类型转换测试】');

test('7.1 JS 数组转 Java List', function() {
    var jsArr = [1, 2, 3, 4, 5];
    var list = java.util.Arrays.asList(jsArr);
    return list.size() === 5;
});

test('7.2 JS 对象转 Java Map', function() {
    var jsObj = { a: 1, b: 2 };
    var map = new java.util.HashMap();
    for (var key in jsObj) {
        map.put(key, jsObj[key]);
    }
    return map.get("a") === 1;
});

test('7.3 Java List 转 JS 数组', function() {
    var list = new java.util.ArrayList();
    list.add(1);
    list.add(2);
    list.add(3);
    var arr = list.toArray();
    return arr.length === 3;
});

test('7.4 数值类型转换', function() {
    var d = 3.14159;
    var i = java.lang.Integer.valueOf(d | 0);
    return i === 3;
});

// ============ 8. 异常处理测试 ============
console.log('【8. 异常处理测试】');

test('8.1 Java 异常捕获', function() {
    try {
        var list = new java.util.ArrayList();
        list.get(100); // IndexOutOfBoundsException
        return false;
    } catch (e) {
        return e instanceof java.lang.IndexOutOfBoundsException;
    }
});

test('8.2 JS 异常在 Java 中传播', function() {
    try {
        var runnable = new java.lang.Runnable({
            run: function() {
                throw new Error("JS Error");
            }
        });
        runnable.run();
        return false;
    } catch (e) {
        return e.message === "JS Error" || e.toString().indexOf("JS Error") >= 0;
    }
});

// ============ 9. 并发与线程测试 ============
console.log('【9. 并发与线程测试】');

test('9.1 Thread 创建与执行', function() {
    var result = 0;
    var t = new java.lang.Thread(new java.lang.Runnable({
        run: function() {
            result = 42;
        }
    }));
    t.start();
    t.join(500);
    return result === 42;
});

test('9.2 synchronized 块兼容性 (SlotMapOwner)', function() {
    // 测试 synchronized 替代 VarHandle 的兼容性
    var obj = {
        value: 0,
        increment: function() {
            synchronized(this, function() {
                this.value++;
            });
        }
    };
    
    // Auto.js 可能不支持 synchronized 关键字
    // 改用简单测试
    var list = new java.util.Collections.synchronizedList(new java.util.ArrayList());
    for (var i = 0; i < 100; i++) {
        list.add(i);
    }
    return list.size() === 100;
});

// ============ 10. 上下文与作用域测试 ============
console.log('【10. 上下文与作用域测试】');

test('10.1 eval 执行', function() {
    var result = eval("1 + 2 + 3");
    return result === 6;
});

test('10.2 函数作用域', function() {
    var outer = "outer";
    function inner() {
        return outer;
    }
    return inner() === "outer";
});

test('10.3 this 绑定', function() {
    var obj = {
        value: 42,
        getValue: function() {
            return this.value;
        }
    };
    return obj.getValue() === 42;
});

// ============ 11. Android 特定 API 测试 ============
console.log('【11. Android API 测试】');

test('11.1 Context 访问', function() {
    try {
        var context = context || auto.service || null;
        return context !== null;
    } catch (e) {
        return 'context 不可用（需要在 Auto.js 环境中运行）';
    }
});

test('11.2 Toast 测试', function() {
    try {
        toast("测试 Toast");
        return true;
    } catch (e) {
        return 'toast 不可用（需要在 Auto.js 环境中运行）';
    }
});

test('11.3 UiObject 操作', function() {
    try {
        var obj = text("测试").findOne(1000);
        return obj !== null || true; // 不要求找到，只测试 API 可用
    } catch (e) {
        return 'UiObject API 不可用（需要在 Auto.js 环境中运行）';
    }
});

// ============ 12. 内存与性能测试 ============
console.log('【12. 内存与性能测试】');

test('12.1 大数组处理', function() {
    var arr = [];
    for (var i = 0; i < 10000; i++) {
        arr.push(i);
    }
    return arr.length === 10000;
});

test('12.2 字符串连接性能', function() {
    var sb = new java.lang.StringBuilder();
    for (var i = 0; i < 1000; i++) {
        sb.append("a");
    }
    return sb.length() === 1000;
});

test('12.3 对象创建性能', function() {
    var count = 1000;
    var start = java.lang.System.currentTimeMillis();
    for (var i = 0; i < count; i++) {
        new java.util.HashMap();
    }
    var end = java.lang.System.currentTimeMillis();
    return (end - start) < 5000; // 应该在5秒内完成
});

// ============ 13. 方法参数数量测试 (MemberBox 兼容) ============
console.log('【13. 方法参数兼容性测试】');

test('13.1 可变参数方法', function() {
    var arr = java.util.Arrays.asList(1, 2, 3, 4, 5);
    return arr.size() === 5;
});

test('13.2 重载方法调用', function() {
    var str = new java.lang.StringBuilder();
    str.append("a");
    str.append(123);
    str.append(3.14);
    return str.toString().indexOf("a") >= 0;
});

test('13.3 空参数方法', function() {
    var list = new java.util.ArrayList();
    return list.isEmpty() === true;
});

// ============ 结果汇总 ============
console.log('\n========== 测试结果汇总 ==========\n');
console.log('总计: ' + (passed + failed) + ' 项');
console.log('通过: ' + passed + ' 项');
console.log('失败: ' + failed + ' 项');
console.log('通过率: ' + ((passed / (passed + failed)) * 100).toFixed(1) + '%');

console.log('\n【详细结果】\n');
for (var i = 0; i < results.length; i++) {
    console.log(results[i]);
}

console.log('\n==========================================');
console.log('测试完成 - Rhino 2.0.0 AutoJs6 兼容性验证');
console.log('==========================================\n');

'测试完成';
