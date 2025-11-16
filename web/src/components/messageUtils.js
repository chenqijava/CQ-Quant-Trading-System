module.exports = {
  msg2params: (msg) => {
    console.log(msg)
    switch (msg.msgType) {
      case 1:
        return {
          msgType: msg.msgType,
          content: msg.content,
        };
      case 3:
        return {
          msgType: msg.msgType,
          ...msg.content,
          msgId: msg._id,
        };
      default:
        throw {message: '所选消息类型暂不支持转发'}
    }
  },
  /**
   * 检测通配符
   * @param text
   * @param data
   * @returns {string}
   */
  checkTextReplaceRules(state,text, data) {
    //  如果有通配符,且没有替换数据
    if (text.indexOf('__replaceContent__') !== -1 && !data.templateValues) {
      let value = state.templateValues.trim();
      if (!value) return '请输入替换内容';
      let targets = [];
      let ads = value.split("\n");
      for (let ad of ads) {
        if (ad.trim()) {
          targets.push(ad.trim());
        }
      }
      if (targets.length < 1) {
        return '请输入替换内容';
      }
      data.templateValues = targets;
    }

    let content = text;
    let specialContent = '__replace0__';
    let params = content.match(/(__replace\d+__)/g);
    if (params) {
      if (params.indexOf(specialContent) !== -1) {
        return "__replacei__变量,i从1开始";
      }
      const regexp = /__replace(\d+)__/g;
      let ms = content.matchAll(regexp);
      let maxParamIndex = 0;//最大变量下标
      for (let m of ms) {
        if (parseInt(m[1]) > maxParamIndex) {
          maxParamIndex = m[1]
        }
      }
      if (maxParamIndex != state.uploadParamFiles.length) {
        return `使用变量__replacei__模板,需要上传${maxParamIndex}个变量文件`;
      }
    }
  },
  checkText(state,data){
    data.text = state.text;
    let error = this.checkTextReplaceRules(state,data.text, data);
    if (error) {
      return error;
    }
    if (state.beEditText) {
      let beEditText = state.beEditText.trim();
      if (beEditText) {
        let error = this.checkTextReplaceRules(state,beEditText, data);
        if (error) {
          return error
        }
        data.data = {
          beEditText,
          editInterval: state.editInterval || 0,
        }
      }
    }
    return false;
  }
};
