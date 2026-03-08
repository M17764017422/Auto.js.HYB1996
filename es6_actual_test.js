// ES6+ 特性测试 - Rhino 2.0.0
// 自动化测试脚本

var results = [];
var passed = 0;
var failed = 0;

function test(name, fn) {
    try {
        var result = fn();
        if (result === true || result === "pass") {
            results.push("✓ " + name);
            passed++;
        } else {
            results.push("✗ " + name + " (返回: " + result + ")");
            failed++;
        }
    } catch (e) {
        results.push("✗ " + name + " (异常: " + e.message + ")");
        failed++;
    }
}

console.log("========== ES6+ 特性测试开始 ==========");

// 1. 箭头函数
test("1. 箭头函数", function() {
    var add = (a, b) => a + b;
    var square = x => x * x;
    return add(2, 3) === 5 && square(4) === 16;
});

// 2. 模板字符串
test("2. 模板字符串", function() {
    var name = "World";
    var msg = `Hello, ${name}!`;
    return msg === "Hello, World!";
});

// 3. let 声明
test("3. let 声明", function() {
    let x = 1;
    if (true) {
        let x = 2;
    }
    return x === 1;
});

// 4. const 声明
test("4. const 声明", function() {
    const PI = 3.14159;
    return PI === 3.14159;
});

// 5. 解构赋值 - 对象
test("5. 对象解构", function() {
    var obj = {a: 1, b: 2};
    var {a, b} = obj;
    return a === 1 && b === 2;
});

// 6. 解构赋值 - 数组
test("6. 数组解构", function() {
    var arr = [1, 2, 3];
    var [x, y, z] = arr;
    return x === 1 && y === 2 && z === 3;
});

// 7. Promise
test("7. Promise", function() {
    var p = new Promise(function(resolve, reject) {
        resolve(42);
    });
    return p instanceof Promise;
});

// 8. Map
test("8. Map", function() {
    var map = new Map();
    map.set("key", "value");
    return map.get("key") === "value";
});

// 9. Set
test("9. Set", function() {
    var set = new Set([1, 2, 2, 3]);
    return set.size === 3 && set.has(2);
});

// 10. 生成器 function*
test("10. 生成器 function*", function() {
    function* gen() {
        yield 1;
        yield 2;
    }
    var g = gen();
    return g.next().value === 1 && g.next().value === 2;
});

// 11. Object.assign
test("11. Object.assign", function() {
    var obj = Object.assign({}, {a: 1}, {b: 2});
    return obj.a === 1 && obj.b === 2;
});

// 12. Object.values
test("12. Object.values", function() {
    var vals = Object.values({a: 1, b: 2});
    return vals.length === 2 && vals[0] === 1;
});

// 13. Object.keys
test("13. Object.keys", function() {
    var keys = Object.keys({a: 1, b: 2});
    return keys.length === 2 && keys[0] === "a";
});

// 14. Object.entries
test("14. Object.entries", function() {
    var entries = Object.entries({a: 1});
    return entries[0][0] === "a" && entries[0][1] === 1;
});

// 15. Array.find
test("15. Array.find", function() {
    var found = [1, 2, 3].find(function(x) { return x > 1; });
    return found === 2;
});

// 16. Array.findIndex
test("16. Array.findIndex", function() {
    var idx = [1, 2, 3].findIndex(function(x) { return x > 1; });
    return idx === 1;
});

// 17. Array.includes
test("17. Array.includes", function() {
    return [1, 2, 3].includes(2) && ![1, 2, 3].includes(4);
});

// 18. Array.from
test("18. Array.from", function() {
    var arr = Array.from("abc");
    return arr.length === 3 && arr[0] === "a";
});

// 19. Array.of
test("19. Array.of", function() {
    var arr = Array.of(1, 2, 3);
    return arr.length === 3 && arr[1] === 2;
});

// 20. 字符串 includes
test("20. String.includes", function() {
    return "hello".includes("ell") && !"hello".includes("xyz");
});

// 21. 字符串 startsWith
test("21. String.startsWith", function() {
    return "hello".startsWith("hel");
});

// 22. 字符串 endsWith
test("22. String.endsWith", function() {
    return "hello".endsWith("llo");
});

// 23. 字符串 repeat
test("23. String.repeat", function() {
    return "ab".repeat(3) === "ababab";
});

// 24. 展开运算符 ... (可能不支持)
test("24. 展开运算符 ...", function() {
    try {
        var arr = [1, 2, 3];
        var arr2 = [...arr, 4];
        return arr2.length === 4;
    } catch (e) {
        return "不支持: " + e.message;
    }
});

// 25. 默认参数 (可能不支持)
test("25. 默认参数", function() {
    try {
        eval("function foo(a = 1) { return a; }");
        return eval("foo()") === 1;
    } catch (e) {
        return "不支持: " + e.message;
    }
});

// 26. class 语法 (可能不支持)
test("26. class 语法", function() {
    try {
        eval("class Foo { constructor() { this.x = 1; } }");
        return "支持";
    } catch (e) {
        return "不支持: " + e.message;
    }
});

// 27. async/await (可能不支持)
test("27. async/await", function() {
    try {
        eval("async function foo() { await Promise.resolve(1); }");
        return "支持";
    } catch (e) {
        return "不支持: " + e.message;
    }
});

// 28. 可选链 ?.
test("28. 可选链 ?.", function() {
    try {
        eval("var obj = {}; return obj?.a?.b === undefined;");
    } catch (e) {
        return "不支持: " + e.message;
    }
});

// 29. 空值合并 ??
test("29. 空值合并 ??", function() {
    try {
        return eval("null ?? 'default'") === "default";
    } catch (e) {
        return "不支持: " + e.message;
    }
});

// 30. for...of
test("30. for...of", function() {
    var sum = 0;
    for (var n of [1, 2, 3]) {
        sum += n;
    }
    return sum === 6;
});

// 输出结果
console.log("========== 测试结果 ==========");
for (var i = 0; i < results.length; i++) {
    console.log(results[i]);
}
console.log("==============================");
console.log("通过: " + passed + " / 失败: " + failed + " / 总计: " + (passed + failed));
console.log("支持率: " + Math.round(passed / (passed + failed) * 100) + "%");
console.log("==============================");
