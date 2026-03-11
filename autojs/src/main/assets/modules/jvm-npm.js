/**
 *  Copyright 2014-2016 Red Hat, Inc.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License")
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
// Since we intend to use the Function constructor.
/* jshint evil: true */

module = (typeof module === 'undefined') ? {} : module;

(function () {
  var builtInModules = ["lodash.js"];

  var System = java.lang.System;
  var Scanner = java.util.Scanner;
  var File = java.io.File;

  NativeRequire = (typeof NativeRequire === 'undefined') ? {} : NativeRequire;
  if (typeof require === 'function' && !NativeRequire.require) {
    NativeRequire.require = require;
  }

  function Module(id, parent, core) {
    this.id = id;
    this.core = core;
    this.parent = parent;
    this.children = [];
    this.filename = id;
    this.loaded = false;

    Object.defineProperty(this, 'exports', {
      get: function () {
        return this._exports;
      }.bind(this),
      set: function (val) {
        Require.cache[this.filename] = val;
        this._exports = val;
      }.bind(this)
    });
    this.exports = {};

    if (parent && parent.children) parent.children.push(this);

    this.require = function (id) {
      return Require(id, this);
    }.bind(this);
  }

  Module._load = function _load(file, parent, core, main) {
    return NativeRequire.require(file);
  };

  Module.runMain = function runMain(main) {
    var file = Require.resolve(main);
    Module._load(file, undefined, false, true);
  };

  function Require(id, parent) {
    var normalizePath = normalizeName(id);
    if (builtInModules.indexOf(normalizePath) >= 0 && !files.exists(normalizePath)) {
      return NativeRequire.require(normalizePath);
    }
    if (id === "events") {
      return events;
    }
    if (id.startsWith("http://") || id.startsWith("https://")) {
      return NativeRequire.require(id);
    }

    var core;
    var native_;
    var file = Require.resolve(id, parent);

    if (!file) {
      if (typeof NativeRequire.require === 'function') {
        if (Require.debug) {
          System.out.println(['Cannot resolve', id, 'defaulting to native'].join(' '));
        }
        native_ = NativeRequire.require(id);
        if (native_) return native_;
      }
      System.err.println('Cannot find module ' + id);
      throw new ModuleError('Cannot find module ' + id, 'MODULE_NOT_FOUND');
    }

    if (file.core) {
      file = file.path;
      core = true;
    }
    if (Require.cache[file]) {
      return Require.cache[file];
    } else if (file.endsWith('.js')) {
      return Module._load(file, parent, core);
    } else if (file.endsWith('.json')) {
      return loadJSON(file);
    }
  }

  Require.resolve = function (id, parent) {
    var roots = findRoots(parent);
    for (var i = 0; i < roots.length; ++i) {
      var root = roots[i];
      var result = resolveCoreModule(id, root) ||
        resolveAsFile(id, root, '.js') ||
        resolveAsFile(id, root, '.json') ||
        resolveAsDirectory(id, root) ||
        resolveAsNodeModule(id, root);
      if (result) {
        return result;
      }
    }
    return false;
  };

  Require.root = files.cwd();//System.getProperty('user.dir');
  Require.NODE_PATH = undefined;

  function findRoots(parent) {
    var r = [];
    r.push(findRoot(parent));
    return r.concat(Require.paths());
  }

  function parsePaths(paths) {
    if (!paths) {
      return [];
    }
    if (paths === '') {
      return [];
    }
    var osName = java.lang.System.getProperty('os.name').toLowerCase();
    var separator;

    if (osName.indexOf('win') >= 0) {
      separator = ';';
    } else {
      separator = ':';
    }

    return paths.split(separator);
  }

  Require.paths = function () {
    var r = [];
    r.push(java.lang.System.getProperty('user.home') + '/.node_modules');
    r.push(java.lang.System.getProperty('user.home') + '/.node_libraries');

    if (Require.NODE_PATH) {
      r = r.concat(parsePaths(Require.NODE_PATH));
    } else {
      var NODE_PATH = java.lang.System.getenv().NODE_PATH;
      if (NODE_PATH) {
        r = r.concat(parsePaths(NODE_PATH));
      }
    }
    // r.push( $PREFIX + "/node/library" )
    return r;
  };

  function findRoot(parent) {
    if (!parent || !parent.id) {
      // 动态获取 cwd，支持 SAF 模式下正确解析相对路径
      // files.cwd() 在脚本运行时会返回脚本所在目录
      var cwd = files.cwd();
      java.lang.System.out.println('[jvm-npm] findRoot: cwd=' + cwd);
      return cwd;
    }
    var pathParts = parent.id.split(/[/\\,]+/g);
    pathParts.pop();
    return pathParts.join('/');
  }

  Require.debug = true;
  Require.cache = {};
  Require.extensions = {};
  require = Require;

  Module.require = require;
  module.exports = Module;

  function loadJSON(file) {
    var json = JSON.parse(readFile(file));
    Require.cache[file] = json;
    return json;
  }

  function resolveAsNodeModule(id, root) {
    var base = [root, 'node_modules'].join('/');
    return resolveAsFile(id, base) ||
      resolveAsDirectory(id, base) ||
      (root ? resolveAsNodeModule(id, getParentPath(root)) : false);
  }
  
  // 获取父目录路径（替代 new File(path).getParent()）
  function getParentPath(path) {
    if (!path) return null;
    // 移除末尾的 /
    if (path.endsWith('/')) {
      path = path.slice(0, -1);
    }
    var lastSlash = path.lastIndexOf('/');
    if (lastSlash <= 0) return null;
    return path.substring(0, lastSlash);
  }

  function resolveAsDirectory(id, root) {
    var base = simplifyPath([root, id].join('/'));
    var packagePath = [base, 'package.json'].join('/');
    // 使用 files.exists() 支持 SAF 模式
    if (files.exists(packagePath)) {
      try {
        // 使用 files.read() 支持 SAF 模式
        var body = files.read(packagePath);
        var package_ = JSON.parse(body);
        if (package_.main) {
          return (resolveAsFile(package_.main, base) ||
            resolveAsDirectory(package_.main, base));
        }
        // if no package.main exists, look for index.js
        return resolveAsFile('index.js', base);
      } catch (ex) {
        throw new ModuleError('Cannot load JSON file', 'PARSE_ERROR', ex);
      }
    }
    return resolveAsFile('index.js', base);
  }

  function resolveAsFile(id, root, ext) {
    var filePath;
    if (id.length > 0 && id[0] === '/') {
      filePath = normalizeName(id, ext);
    } else {
      filePath = [root, normalizeName(id, ext)].join('/');
    }
    // 先规范化路径，移除 ./ 和 ../，支持 SAF 模式
    filePath = simplifyPath(filePath);
    
    // 使用 files.exists() 支持 SAF 模式
    if (files.exists(filePath)) {
      return filePath;
    }
    // 如果是绝对路径且不存在，尝试作为目录解析
    if (id.length > 0 && id[0] === '/') {
      return resolveAsDirectory(id);
    }
  }
  
  // 路径规范化函数（替代 File.getCanonicalPath）
  function simplifyPath(path) {
    var parts = path.split('/');
    var result = [];
    for (var i = 0; i < parts.length; i++) {
      if (parts[i] === '' || parts[i] === '.') continue;
      if (parts[i] === '..') {
        if (result.length > 0) {
          result.pop();
        }
      } else {
        result.push(parts[i]);
      }
    }
    var simplified = result.join('/');
    // path 以 / 开头时，结果也要以 / 开头
    return path.startsWith('/') ? '/' + simplified : simplified;
  }

  function resolveCoreModule(id, root) {
    var name = normalizeName(id);
    var classloader = java.lang.Thread.currentThread().getContextClassLoader();
    if (classloader.getResource(name)) {
      return { path: name, core: true };
    }
  }

  function normalizeName(fileName, ext) {
    if (fileName.endsWith('.json')) {
      return fileName;
    }
    var extension = ext || '.js';
    if (fileName.endsWith(extension)) {
      return fileName;
    }
    return fileName + extension;
  }

  function readFile(filename, core) {
    try {
      if (core) {
        // 内置模块从 classpath 读取
        var classloader = java.lang.Thread.currentThread().getContextClassLoader();
        var input = classloader.getResourceAsStream(filename);
        return new Scanner(input).useDelimiter('\\A').next();
      } else {
        // 外部模块使用 files.read() 支持 SAF 模式
        return files.read(filename);
      }
    } catch (e) {
      throw new ModuleError('Cannot read file [' + filename + ']: ', 'IO_ERROR', e);
    }
  }

  function ModuleError(message, code, cause) {
    this.code = code || 'UNDEFINED';
    this.message = message || 'Error loading module';
    this.cause = cause;
  }

  // Helper function until ECMAScript 6 is complete
  if (typeof String.prototype.endsWith !== 'function') {
    String.prototype.endsWith = function (suffix) {
      if (!suffix) return false;
      return this.indexOf(suffix, this.length - suffix.length) !== -1;
    };
  }

  ModuleError.prototype = new Error();
  ModuleError.prototype.constructor = ModuleError;
}());