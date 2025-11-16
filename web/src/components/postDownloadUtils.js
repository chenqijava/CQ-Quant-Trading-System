/**
 * post方式下载文件
 * @param  {string} url  接口
 * @param  {json} data 数据
 * @return
 */
function download(url, data) {
  var body = document.getElementsByTagName('body')[0];
  var form = document.createElement('form');
  form.method = 'POST';
  form.action = url;
  appendParams(form, '', data);
  body.appendChild(form);
  form.submit();
  body.removeChild(form);
}

function appendParams(form, name, data) {
  if (typeof data == 'object') {
    for (let key in data) {
      let keyName = key;
      if (name) keyName = `${name}.${key}`;
      if (data[key] != undefined) appendParams(form, keyName, data[key])
    }
  } else {
    let param = document.createElement('input');
    param.type = "hidden";
    param.name = name;
    param.value = data;
    form.appendChild(param);
  }
}

module.exports = {
  download,
};
