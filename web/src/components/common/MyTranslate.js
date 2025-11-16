import React, {Component} from 'react'
import {connect} from "dva";
import {FormattedMessage, useIntl, injectIntl} from 'react-intl'

//  强制翻译的关键词
const forcedTranslateMap = {
  "全部分组": true,
  "默认分组": true,
  "全部账号": true,
}

const MyTranslate = ({locale, children}) => {
  const formatMessages = new Set();
  const intl = useIntl()
  const formatMessage = (id = '', values = {}) => {
    if (typeof id != 'string') return id;
    id = id.trim();
    //  如果去掉空格后为空字符串,直接返回
    if (!id) return id;
    // else if (id.startsWith('connect ')) {
    //   return id
    // }
    // else if (/^[a-zA-Z0-9 ,]+$/.test(id)) {
      //  纯数字,字母,标点的暂时不翻译
      // return id
    // }
    else if(/^[0-9]+$/.test(id)) {
      //  纯数字不翻译
      return id
    }
    let tValues = {}
    Object.keys(values).forEach(key => {
      tValues[key] = formatMessage(values[key])
    })
    let message = intl.formatMessage({
      id,
      // defaultMessage: `等待翻译(${id})`,   //  开发时可以取消注释,方便查看有哪些没有翻译
    }, tValues);
    if (message.startsWith('等待翻译(')) formatMessages.add(id);
    return message;
  }
  const translateChildren = (children) => {
    let changed = false;
    let tmpChildren = children instanceof Array ? children : [children];
    let newChildren = tmpChildren.map((it, i) => {
      if (it && it.props) {
        let {children, columns, pagination, options, values = {}} = it.props;
        //  如果不是强制翻译的语句,且指定不用翻译,直接返回
        if (!forcedTranslateMap[children] && it.props.translate === false) return it;
        let key = it.key || i;
        const newProps = {key};
        if (children) {
          let children = translateChildren(it.props.children)
          if (children !== it.props.children) {
            changed = true;
            newProps.children = children;
          }
        }
        //  处理Table的columns
        if (columns) {
          columns = columns.map(c => {
            if (c.translate === false) return c;
            return ({
              ...c,
              title: <MyTranslate>{c.title}</MyTranslate>,
              //  处理render
              render: c.render &&
                  (c.translateRender !== true
                      ? c.render
                      : ((text, record, index) => <MyTranslate>{c.render(text, record, index)}</MyTranslate>)),
              //  处理filters
              filters: c.filters && c.filters.map(f => {
                if (f.translate === false) return f;
                return {
                  ...f,
                  text: formatMessage(f.text, f.values),
                }
              }),
            });
          });
          changed = true;
          newProps.pagination = {
            ...pagination,
            showTotal: (total, range) => formatMessage('共 {total} 条', {total}),
          }
          newProps.columns = columns;
        }
        //  处理options
        if (options) {
          options = options.map(o => {
            if (!forcedTranslateMap[o.label] && o.translate === false) return o;
            return {
              ...o,
              label: o.label && formatMessage(o.label, o.values),
            }
          });
          changed = true;
          newProps.options = options;
        }
        //  处理placeholder,title
        [
          'placeholder',
          'title',
          'message',
          'description',
          'header',
          'tab',
          'label',
        ].forEach(key => {
          if (it.props[key]) {
            changed = true;
            newProps[key] = typeof it.props[key] == 'string' ? formatMessage(it.props[key], values) : <MyTranslate>{it.props[key]}</MyTranslate>;
          }
        })
        if (changed) it = React.cloneElement(it, newProps);
      } else if (it instanceof Array) {
        let children = translateChildren(it)
        if (children !== it) {
          changed = true;
          it = children
        }
      } else if (it && typeof it == 'string') {
        changed = true;
        it = <span translate={false}>{formatMessage(it)}</span>
      }
      return it
    });
    //  如果有变化，返回新的children
    return (changed ? newChildren : children) || '';
  }

  try {
    return children //translateChildren(children);
  } finally {
    //  开发时可以在控制台查看有哪些文本等待翻译
    if (formatMessages.size) console.log([...formatMessages].map(msg => `"${msg}": "${msg}",`).join("\n"))
  }
}

export default connect(({locale}) => ({
  locale: locale.locale
}))(MyTranslate)

