import {delay} from 'dva/saga'
import axios from 'axios'
import {message} from 'antd'

export default {
  namespace: 'user',
  state: {
    checked: false,
    openKeys: [],
    permissions: [],
  },
  reducers: {
    update(state, {info}) {
      return {
        ...state,
        checked: true,
        info
      }
    },
    updateOpenKey(state, {openKeys}) {
      return {
        ...state,
        checked: true,
        openKeys: openKeys
      }
    },
    updatePermissions(state, {permissions}) {
      let pathname = window.location.pathname

      let p = permissions.filter(e => e.url === pathname)
      if (p.length > 0) {
        p = permissions.filter(e => e._id === p[0].parent)
      }

      return {
        ...state,
        permissions: permissions,
        openKeys: p.length > 0 ? [p[0].key] : []
      } 
    }
  },
  effects: {
    *stat(action, {call, put}) {
      let res = yield call(axios.get, '/api/common/user/stat')
      if (res.data.code == 0) {
        yield put.resolve({
          type: 'update',
          info: null
        })
      } else {
        yield put.resolve({
          type: 'update',
          info: res.data.data,
        })
      }
    },
    *openKeys(action, {call, put}) {
      yield put.resolve({
        type: 'updateOpenKey',
        openKeys: action.openKeys
      })
    },
    *login(action, {call, put}) {
      let res = yield call(axios.post, '/api/common/user/login', action.form)
      if (res.data.code == 1) {
        message.success('登录成功')
        yield put.resolve({
          type: 'update',
          info: res.data.data,
        })
      } else {
        message.error(res.data.message)
      }
    },
    *logout(action, {call, put}) {
      console.log('logout here')
      let res = yield call(axios.post, '/api/common/user/logout', {})
      if (res.data.code == 1) {
        yield put.resolve({
          type: 'update',
          info: {
            userID: null,
            name: null
          }
        })
        window.location.href = '/'
      }
    },
    *edit(action, {call, put}) {
      let res = yield call(axios.post, '/api/common/user/edit', action.form)
      if (res.data.code == 1) {
        message.success('修改成功');
        yield put.resolve({
          type: 'update',
          info: {
            userID: res.data.userID,
            name: res.data.name
          }
        })
      } else {
        message.error(res.data.message);
      }
    },
    *loadPermissions (action, {call, put}) {
      let result = yield call(axios.get, '/api/common/user/loadPermissions')
      if (result.data.code) {
        yield put.resolve({
          type: 'updatePermissions',
          permissions: result.data.data 
        })
      }
    }
  }
};
