import React, {Component} from 'react'
import {connect} from "dva";

const ExternalAccountFilter = ({ea_hide, externalAccount, children}) => {
  //  如果不是外部账号,直接返回
  if (externalAccount === false) return children;
  const needHide = (ea_hide) => externalAccount === true && ea_hide

  const ea_filter = (it) => it && !needHide(it.ea_hide || it.props?.ea_hide);
  const filterChildren = (children) => {
    // if(depth > 2) return children
    let changed = false;
    let tmpChildren = children instanceof Array ? children : [children];
    let newChildren = tmpChildren.filter(ea_filter).map((it, i) => {
      if (it.props) {
        let {children} = it.props;
        let key = it.key || i;
        const newProps = {key};
        if (children) {
          let newChildren = filterChildren(children)
          if (newChildren !== children) {
            changed = true;
            newProps.children = newChildren;
          }
        }
        //  处理 options 和 Table的columns
        ['columns', 'options'].forEach(key => {
          if (it.props[key] instanceof Array) {
            let oldDatas = it.props[key];
            let newDatas = oldDatas.filter(ea_filter)
            if (newDatas.length !== oldDatas.length) {
              changed = true;
              newProps[key] = newDatas;
            }
          }
        });
        if (changed) {
          it = React.cloneElement(it, newProps)
        }
      } else if (it instanceof Array) {
        let children = filterChildren(it)
        if (children !== it) {
          changed = true;
          it = children
        }
      }
      return it
    });
    //  如果有变化，或者长度不一样，返回新的children
    return (changed || newChildren.length !== tmpChildren.length) ? newChildren : children;
  }

  return needHide(ea_hide) ? "" : filterChildren(children)
}

export default connect(({user}) => ({
  externalAccount: user.info.externalAccount,
}))(ExternalAccountFilter)

