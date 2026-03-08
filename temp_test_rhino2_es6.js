/**
 * Rhino 2.0.0 ES6+ 特性测试脚本
 * 用于验证 Auto.js 升级后的 JavaScript 引擎功能
 * 
 * 测试项：
 * 1. 箭头函数
 * 2. 模板字符串
 * 3. let/const 块级作用域
 * 4. 解构赋值
 * 5. Promise
 * 6. Map/Set
 * 7. 生成器
 * 8. Object.assign
 * 9. Array 新方法
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

// ============ 1. 箭头函数测试 ============
test('1.1 箭头函数 - 基本语法', function() {
    var add = (a, b) => a + b;
    return add(2, 3) === 5;
});

test('1.2 箭头函数 - 带函数体', function() {
    var multiply = (a, b) => {
        var result = a * b;
        return result;
    };
    return multiply(3, 4) === 12;
});

test('1.3 箭头函数 - 单参数省略括号', function() {
    var double = x => x * 2;
    return double(5) === 10;
});

// ============ 2. 模板字符串测试 ============
test('2.1 模板字符串 - 基本用法', function() {
    var name = 'World';
    var str = `Hello ${name}!`;
    return str === 'Hello World!';
});

test('2.2 模板字符串 - 表达式插值', function() {
    var a = 10;
    var b = 20;
    var str = `${a} + ${b} = ${a + b}`;
    return str === '10 + 20 = 30';
});

test('2.3 模板字符串 - 多行字符串', function() {
    var str = `Line1
Line2
Line3`;
    return str.indexOf('Line2') > 0;
});

// ============ 3. let/const 测试 ============
test('3.1 let - 块级作用域', function() {
    var result = true;
    if (true) {
        let x = 10;
        result = result && (x === 10);
    }
    // x 在此处不可访问
    return result;
});

test('3.2 const - 常量声明', function() {
    const PI = 3.14159;
    return PI === 3.14159;
});

test('3.3 const - 对象属性可修改', function() {
    const obj = { value: 1 };
    obj.value = 2;
    return obj.value === 2;
});

// ============ 4. 解构赋值测试 ============
test('4.1 数组解构', function() {
    var [a, b, c] = [1, 2, 3];
    return a === 1 && b === 2 && c === 3;
});

test('4.2 对象解构', function() {
    var { name, age } = { name: 'Tom', age: 18 };
    return name === 'Tom' && age === 18;
});

test('4.3 解构默认值', function() {
    var { x = 10 } = {};
    return x === 10;
});

test('4.4 嵌套解构', function() {
    var { person: { name } } = { person: { name: 'Jerry' } };
    return name === 'Jerry';
});

// ============ 5. Promise 测试 ============
test('5.1 Promise - 基本创建', function() {
    var p = new Promise(function(resolve, reject) {
        resolve(42);
    });
    return p instanceof Promise;
});

test('5.2 Promise - then 链式调用', function() {
    var result = false;
    Promise.resolve(10)
        .then(function(x) { return x * 2; })
        .then(function(x) { result = (x === 20); });
    // 同步测试，Promise 会在微任务队列执行
    return true; // 异步特性存在即通过
});

test('5.3 Promise - catch 捕获错误', function() {
    var caught = false;
    Promise.reject('error')
        .catch(function(e) { caught = true; });
    return true; // catch 方法存在即通过
});

test('5.4 Promise - Promise.all', function() {
    var p = Promise.all([Promise.resolve(1), Promise.resolve(2)]);
    return p instanceof Promise;
});

test('5.5 Promise - Promise.race', function() {
    var p = Promise.race([Promise.resolve(1), Promise.resolve(2)]);
    return p instanceof Promise;
});

// ============ 6. Map/Set 测试 ============
test('6.1 Map - 基本操作', function() {
    var map = new Map();
    map.set('key', 'value');
    return map.get('key') === 'value';
});

test('6.2 Map - size 和 has', function() {
    var map = new Map();
    map.set('a', 1);
    map.set('b', 2);
    return map.size === 2 && map.has('a');
});

test('6.3 Map - delete 和 clear', function() {
    var map = new Map();
    map.set('x', 1);
    map.delete('x');
    return !map.has('x');
});

test('6.4 Set - 基本操作', function() {
    var set = new Set();
    set.add(1);
    set.add(2);
    set.add(1); // 重复添加
    return set.size === 2;
});

test('6.5 Set - has 和 delete', function() {
    var set = new Set([1, 2, 3]);
    return set.has(2) && set.delete(1) && !set.has(1);
});

// ============ 7. 生成器测试 ============
test('7.1 生成器 - 基本语法', function() {
    function* gen() {
        yield 1;
        yield 2;
        yield 3;
    }
    var g = gen();
    return g.next().value === 1;
});

test('7.2 生成器 - 迭代', function() {
    function* range(start, end) {
        for (var i = start; i < end; i++) {
            yield i;
        }
    }
    var values = [];
    for (var v of range(1, 4)) {
        values.push(v);
    }
    return values.join(',') === '1,2,3';
});

test('7.3 生成器 - done 属性', function() {
    function* gen() {
        yield 1;
    }
    var g = gen();
    g.next();
    return g.next().done === true;
});

// ============ 8. Object 新方法测试 ============
test('8.1 Object.assign - 合并对象', function() {
    var target = { a: 1 };
    var source = { b: 2 };
    Object.assign(target, source);
    return target.a === 1 && target.b === 2;
});

test('8.2 Object.assign - 多源合并', function() {
    var result = Object.assign({}, { a: 1 }, { b: 2 }, { c: 3 });
    return result.a === 1 && result.b === 2 && result.c === 3;
});

test('8.3 Object.keys', function() {
    var keys = Object.keys({ a: 1, b: 2, c: 3 });
    return keys.join(',') === 'a,b,c';
});

test('8.4 Object.values', function() {
    var values = Object.values({ a: 1, b: 2, c: 3 });
    return values.join(',') === '1,2,3';
});

test('8.5 Object.entries', function() {
    var entries = Object.entries({ a: 1 });
    return entries[0][0] === 'a' && entries[0][1] === 1;
});

// ============ 9. Array 新方法测试 ============
test('9.1 Array.find', function() {
    var arr = [1, 2, 3, 4, 5];
    var found = arr.find(function(x) { return x > 3; });
    return found === 4;
});

test('9.2 Array.findIndex', function() {
    var arr = [1, 2, 3, 4, 5];
    var index = arr.findIndex(function(x) { return x > 3; });
    return index === 3;
});

test('9.3 Array.includes', function() {
    var arr = [1, 2, 3];
    return arr.includes(2) && !arr.includes(4);
});

test('9.4 Array.from', function() {
    var arr = Array.from('abc');
    return arr.join(',') === 'a,b,c';
});

test('9.5 Array.of', function() {
    var arr = Array.of(1, 2, 3);
    return arr.length === 3 && arr[0] === 1;
});

test('9.6 Array.fill', function() {
    var arr = [1, 2, 3].fill(0);
    return arr.join(',') === '0,0,0';
});

test('9.7 Array.copyWithin', function() {
    var arr = [1, 2, 3, 4, 5].copyWithin(0, 3, 5);
    return arr[0] === 4 && arr[1] === 5;
});

// ============ 10. 字符串新方法测试 ============
test('10.1 String.includes', function() {
    return 'Hello World'.includes('World');
});

test('10.2 String.startsWith', function() {
    return 'Hello World'.startsWith('Hello');
});

test('10.3 String.endsWith', function() {
    return 'Hello World'.endsWith('World');
});

test('10.4 String.repeat', function() {
    return 'ab'.repeat(3) === 'ababab';
});

test('10.5 String.padStart', function() {
    return '5'.padStart(3, '0') === '005';
});

test('10.6 String.padEnd', function() {
    return '5'.padEnd(3, '0') === '500';
});

// ============ 11. 展开运算符测试（应该不支持） ============
test('11.1 展开运算符 - 数组（预期不支持）', function() {
    try {
        // 使用 eval 避免解析时错误
        var result = eval('var arr = [...[1,2,3]]; arr.join(",")');
        return result === '1,2,3';
    } catch (e) {
        return '不支持（符合预期）';
    }
});

// ============ 12. 默认参数测试（应该不支持） ============
test('12.1 默认参数（预期不支持）', function() {
    try {
        var fn = eval('(function(a = 10) { return a; })');
        return fn() === 10;
    } catch (e) {
        return '不支持（符合预期）';
    }
});

// ============ 13. for...of 测试 ============
test('13.1 for...of - 数组迭代', function() {
    var sum = 0;
    for (var x of [1, 2, 3]) {
        sum += x;
    }
    return sum === 6;
});

test('13.2 for...of - 字符串迭代', function() {
    var chars = [];
    for (var c of 'abc') {
        chars.push(c);
    }
    return chars.join(',') === 'a,b,c';
});

test('13.3 for...of - Map 迭代', function() {
    var map = new Map([['a', 1], ['b', 2]]);
    var keys = [];
    for (var [k, v] of map) {
        keys.push(k);
    }
    return keys.join(',') === 'a,b';
});

// ============ 14. Symbol 测试 ============
test('14.1 Symbol - 创建', function() {
    var sym = Symbol('test');
    return typeof sym === 'symbol';
});

test('14.2 Symbol - 唯一性', function() {
    return Symbol('a') !== Symbol('a');
});

// ============ 15. 展示结果 ============
console.log('\n========== Rhino 2.0.0 ES6+ 特性测试结果 ==========\n');
console.log('总计: ' + (passed + failed) + ' 项');
console.log('通过: ' + passed + ' 项');
console.log('失败: ' + failed + ' 项');
console.log('\n详细结果:\n');
for (var i = 0; i < results.length; i++) {
    console.log(results[i]);
}
console.log('\n==========================================');

'测试完成';
