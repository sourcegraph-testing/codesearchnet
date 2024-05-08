/**
 * 读取本地mock数据入口
 */

const path = require('path');
const fs = require('fs');
const url = require('url');
const jsonc = require('jsonc-parser');
const colors = require('colors');

const cwd = process.cwd();

/**
 * 延迟
 * @param {number} time 延迟时间
 */
function delay(time = 0) {
  return new Promise((resolve) => {
    setTimeout(() => {
      resolve();
    }, time);
  });
}

/**
 * 解析json字符串，返回兼容解析后的字符串，如果解析错误，返回原始data。
 * 解析器做兼容处理，允许json字符串中存在注释、结束逗号
 * @param {string} data 原始json字符串数据
 */
function parseJson(data) {
  const parseError = [];
  const parseOptions = {
    disallowComments: false,
    allowTrailingComma: true
  };
  // 使用微软的node-jsonc-parser组件做兼容注释、结束逗号数据，
  // 详细见https://github.com/Microsoft/node-jsonc-parser
  const jsonData = jsonc.parse(data, parseError, parseOptions);
  if (parseError.length !== 0) {
    // 解析错误，直接返回原始数据，以便前端获知错误及原始信息
    return data;
  }
  return JSON.stringify(jsonData);
}

/**
 * 跨域支持
 */
function cors(req, res) {
  const { origin } = req.headers;
  res.set('Access-Control-Allow-Origin', origin); // 注意这里不能使用* ,否则请求include时候报错
  res.set('Access-Control-Allow-Headers', 'Origin, X-Requested-With, Content-Type, Accept,Content-Range, Content-Disposition, Content-Description,Set-Cookie,, Access-Control-Request-Method, Access-Control-Request-Headers,Authorization,Authentication');
  res.set('Access-Control-Allow-Credentials', true); // 告诉客户端可以在HTTP请求中带上Cookie
  res.set('Access-Control-Allow-Methods', 'GET,POST,PUT,DELETE,PATCH,HEAD,OPTIONS');
  res.set('Access-Control-Expose-Headers', 'Set-Cookie,Authorization,Authentication');
}

function res404(req, res) {
  res.status(404);
  res.end('Not Found');
}

function res500(res, error) {
  res.status(500);
  res.set('Content-Type', 'text/html;charset=utf-8');
  return res.end(error.message);
}

function resJSON(req, res, filePath) {
  const data = fs.readFileSync(filePath, 'utf-8');
  const json = parseJson(data);
  res.set('Content-Type', 'application/json;charset=utf-8');
  return res.end(json);
}

function resHtml(req, res, filePath) {
  const data = fs.readFileSync(filePath, 'utf-8');
  res.set('Content-Type', 'text/html;charset=utf-8');
  return res.end(data);
}

function resJSONData(res, data) {
  // any other data structure
  res.set('Content-Type', 'application/json;charset=utf-8');

  if (typeof data === 'string') {
    return res.end(data);
  }

  return res.end(JSON.stringify(data, null, 2));
}

function resJs(req, res, filePath) {
  delete require.cache[filePath];
  const code = require(filePath);

  // if exports a function, just call it
  if (typeof code === 'function') {
    const result = code(req, res, url.parse(req.url, true));

    // if return a promise
    if (result && typeof result.then === 'function') {
      return result.then(
        data => resJSONData(res, data),
        error => res500(res, error)
      );
    }

    // any other data structure
    return resJSONData(res, result);
  }

  return resJSONData(res, code);
}

function processRequest(req, res, filePath) {
  const extList = [
    ['.js', resJs],
    ['.json', resJSON],
    ['.htm', resHtml],
    ['.html', resHtml]
  ];

  const currentExt = path.extname(filePath);
  const basePath = filePath.replace(currentExt, '');
  const newExt = extList.find(ext => fs.existsSync(basePath + ext[0]));

  if (!newExt) {
    return res404(req, res);
  }

  const finalPath = basePath + newExt[0];
  const finalProcesser = newExt[1];

  // 检测是否存在
  if (!fs.existsSync(finalPath)) {
    console.log('SERVED[404]: ', colors.red(finalPath));
    console.log();
    return res404(req, res);
  }

  console.log('SERVED: ', colors.green(finalPath));
  console.log();

  return finalProcesser(req, res, finalPath);
}

function request(
  req,
  res,
  options = {
    mockPath: '/mock',
    delay: 100,
    cors: true,
    exact: false
  }) {
  const urlObj = url.parse(req.url, true);
  let filePath = path.join(cwd, options.mockPath);
  let fileName = '';
  if (!options.exact) {
    fileName = urlObj.pathname;
    filePath = path.join(filePath, fileName);
  }

  if (options.cors) {
    cors(req, res);
  }

  return delay(options.delay)
    .then(() => processRequest(req, res, filePath))
    .catch(err => res500(res, err));
}


module.exports = request;
