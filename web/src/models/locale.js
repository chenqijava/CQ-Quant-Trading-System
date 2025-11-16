import messages from '../nb-intl/messages'

// 获取浏览器默认语言
const getBrowserLang = function() {
  let browserLang = (navigator.language
      ? navigator.language
      : navigator.browserLanguage).toLowerCase();
  let defaultBrowserLang = "";
  if (
      browserLang.indexOf('en') !== -1 ||
      browserLang.indexOf('us') !== -1
  ) {
    defaultBrowserLang = "en";
  } else {
    defaultBrowserLang = "zh";
  }
  return defaultBrowserLang;
};

const loadLang = function () {
  let locale = window.localStorage.getItem("locale") || getBrowserLang();
  if (messages.ls[locale]) {
    //  判断是否支持这个语言
    return locale;
  } else {
    //  不支持,默认返回中文
    return 'zh';
  }
}

export default {
  namespace: 'locale',
  state: {
    locale: loadLang(),
  },
  reducers: {
    changeLocale(state, {locale}) {
      window.localStorage.setItem("locale", locale);
      return {
        ...state,
        locale,
      }
    }
  },
  effects: {
  }
};
