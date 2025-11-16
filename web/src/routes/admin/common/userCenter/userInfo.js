import React, {Component} from 'react'
import {BrowserRouter as Router, Route, Link} from 'react-router-dom'
import {
  Icon,
  Table,
  Divider,
  Upload,
  message,
  Input,
  Button,
  Modal,
  Select,
  Row,
  Col,
} from 'antd'
import { Pagination, Breadcrumb, Dialog, Tag, Input as TInput, Upload as TUpload, Tooltip as TTooltip, Switch} from 'tdesign-react';
import { connect } from 'dva'

const {Option} = Select;
const Search = Input.Search;
const confirm = Modal.confirm;

class MyComponent extends Component {
  constructor(props) {
    super(props);

    this.state = {
    };
  }

  // 首次加载数据
  async componentWillMount() {
    this.props.dispatch({ type: 'user/stat' });
  }

  copy = (text) => {
    const input = document.createElement('input');
    input.value = text;
    document.body.appendChild(input);
    input.select();
    document.execCommand('copy');
    document.body.removeChild(input);
    message.success('复制成功');
  }

  // 把columns放到render中，虽然损失部分性能，但是能方便参数中的匿名回调获取实例状态
  render() {
    return (<div>
      <Breadcrumb>
        <Breadcrumb.BreadcrumbItem>个人中心</Breadcrumb.BreadcrumbItem>
        <Breadcrumb.BreadcrumbItem>个人信息</Breadcrumb.BreadcrumbItem>
      </Breadcrumb>

      <div style={{fontSize: 20, color: 'rgba(0, 0, 0, 0.60)'}}>
        <div style={{marginTop: 57, marginLeft: 53, }}><span style={{marginRight: 24}}>登录ID:</span><span style={{color: '#000'}}>{this.props.user.info.userID}</span></div>
        <div style={{marginTop: 37, marginLeft: 53, }}><span style={{marginRight: 24}}>基本账户余额:</span><span style={{color: '#000'}}>{this.props.user.info.balance}(冻结:{this.props.user.info.frozenBalance})</span>{ this.props.openRecharge === 'open' ? <span style={{marginLeft: 24, color: '#3978F7', cursor: 'pointer'}} onClick={() => {
          let keys = [...this.props.openKeys, 'userCenter']
          this.props.dispatch({type: 'user/openKeys', openKeys: keys});
          this.props.history.push('/cloud/user/recharge')
        }}>充值</span>:''}</div>

        <div style={{marginTop: 37, marginLeft: 53, }}><span style={{marginRight: 24}}>群发账户余额:</span><span style={{color: '#000'}}>{this.props.user.info.restSendEmailCount}次</span></div>

        <div style={{marginTop: 37, marginLeft: 53, }}><span style={{marginRight: 24}}>API密钥:</span><span style={{color: '#000'}}>{this.props.user.info.userApiKey}</span><span style={{marginLeft: 24, color: '#3978F7', cursor: 'pointer'}} onClick={() => {
          this.copy(this.props.user.info.userApiKey)
        }}>复制</span></div>

      </div>
    </div>)
  }
}

export default connect(({ user }) => ({
  user,
  openKeys: user.openKeys,
  openRecharge: user.info.openRecharge,
  restSendEmailCount: user.info.restSendEmailCount,
}))(MyComponent)
