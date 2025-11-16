import React, { Component } from 'react'
import { Dialog, Popup, List, ListProps, Space, Button as TButton } from 'tdesign-react'
import { NotificationIcon } from 'tdesign-icons-react'
import { Layout, Menu, Icon, message, Input, Modal, Button, Badge, Row, Col, Dropdown, Empty, Checkbox, notification } from 'antd'
import { BrowserRouter as Router, Route, Switch, withRouter } from 'react-router-dom'
import 'quill/dist/quill.snow.css'; // 引入 Quill 基础样式

import 'moment/locale/zh-cn'
import { connect } from 'dva'
import ChangeLocale from '../nb-intl/ChangeLocale'
import { FormattedMessage, useIntl, injectIntl } from 'react-intl'
import Style from './index.css'
import TimerWrapper from '../components/TimerWrapper'
import axios from 'axios'

import Home from './admin/home'
import User from './admin/account/user'
import Socks5 from './admin/ac/account/socks5'
import Vps from './admin/ac/account/vps'
import Params from './admin/ac/account/params'
import Index from './admin/index.js'
import BalanceDetail from './admin/common/BalanceDetail'
import Payment from './admin/common/payment'
import UserVps from './admin/common/vps'
import UserAccount from './admin/common/account'
import StrangerSendMsg from './admin/common/strangerSendMsg'
import UserAccountGroup from './admin/common/accountGroup'
import roleList from './admin/common/role/list'
import ApiKeyList from './admin/common/apiKey/list'
import proxyAccountList from './admin/common/proxyAccount/list'
import PlatformList from './admin/common/platform/list'
import googleStudioList from './admin/common/googleStudio/list'
import chatgptList from './admin/common/googleStudio/listChatgpt.js'
import TaskList from './admin/common/task/list'
import Receive from './admin/common/receiveEmail.js'
import aiModelList from './admin/common/aiModel/list'

import UserInfo from './admin/common/userCenter/userInfo.js'
import MyOrder from './admin/common/userCenter/myOrder.js'
import MyOrderV2 from './admin/common/userCenter/myOrderV2.js'
import MyEmail from './admin/common/userCenter/myEmail.js'
import MyBill from './admin/common/userCenter/myBill.js'
import Recharge from './admin/common/userCenter/recharge.js'
import ownerUser from './admin/common/userCenter/ownerUser.js'
import batchSendEmail from './admin/common/userCenter/batchSendEmail.js'
import batchSendEmailAdd from './admin/common/userCenter/batchSendEmailAdd.js'
import linkCheck from './admin/common/userCenter/linkCheck.js'
import contentAIGen from './admin/common/userCenter/contentAiGen.js'
import tgnetToSession from './admin/common/userCenter/tgnetToSession.js'
import systemMailTemplate from './admin/common/userCenter/adminSystemMailTemplate.js'
import myMailTemplate from './admin/common/userCenter/myMailTemplate.js'


import billManage from './admin/common/billManage.js'
import orderManage from './admin/common/orderManage.js'
import orderManageV2 from './admin/common/orderManageV2.js'
import emailManage from './admin/common/emailManage.js'

import aiServer from './admin/common/googleStudio/aiServer.js'

import SysPermissionButton from '../components/common/SysPermissionButton.js'

import sendGridAccount from './admin/common/sendGridAccount.js'
import emailCheckActive from './admin/common/emailCheckActive.js'
import sieveActive from './admin/common/sieveActive.js'
import aiTokenStatistics from './admin/common/aiTokenStatistics.js'

import dnsRecordList from  './admin/common/dnsRecord/list.js'

import taskTypes from 'components/taskTypes'
import Head from '../head'

const { SubMenu, ItemGroup: MenuItemGroup } = Menu;
const { Header, Content, Sider } = Layout;
const confirm = Modal.confirm;
const InputGroup = Input.Group;
const { ListItem, ListItemMeta } = List;

const capitalInitial = (name) => name.charAt(0).toUpperCase() + name.slice(1);
// 通用全局参数设置
// const params = [{
//   type: 'webConfig',
//   code: 'head',
// }];
const params = []
class BackStage extends Component {

  constructor(props) {
    super(props);
    this.state = {
      editPassword: false,
      oldPassword: '',
      newPassword: '',
      newPwd: '',

      editName: false,
      newName: '',

      menus: null,
      noticeMessage: '',
      dialogMsg: [],
      stopNotice: false,

      types: {
        ...taskTypes.tTypeName,
        snspost_1: '发朋友圈消息',
      },

      config: {},
      openKeys: [],
      active: '',
      notification: false, //系统通知
      hasNotification: false, //是否有通知
      systemNoticeList: [],//系统通知列表
      noReadNoticeList: [],//未读通知
      balance: 0, //账号余额
      edit2FA: false,
    };
  }

  // 首次加载数据
  async componentWillMount() {
    this.props.dispatch({ type: 'user/loadPermissions' })
    // this.loadPermissions();
    // this.componentMount();
    this.reloadConfig();
    this.loadBalance()
    // this.loadSystemNotice();

    if (!this.props.setSecretKey && this.props.userID === 'admin') {
      this.setState({edit2FA: true});
      await this.getAuthenticationUrl()
    }
  }

  async loadBalance() {
    // 这里的参数是格式好的，无需修改，无阻抗失衡，直接发给服务器
    let res1 = await axios.get(`/api/consumer/user/balance`);
    this.setState({balance: res1.data.data});
  }

  async reloadConfig() {
    let config = {};
    for (let i in params) {
      let param = params[i];
      let res = await axios.get(`/api/consumer/params/${param.type || pageType}/get/${param.code}`);
      if (res.data.code) {
        config[`${param.type}-${param.code}`] = param.unit ? res.data.value / (param.unit || 1) : res.data.value;
      }
    }
    this.setState({ config });
  }

  chargeOpen = async () => {
    // window.location.href='/cloud/user/payment';
    let keys = [...this.props.openKeys, 'balanceMenu']
    this.props.dispatch({type: 'user/openKeys', openKeys: keys});
    this.props.history.push('/cloud/user/payment')
  };

  getToken ()  {
    axios.post('/api/chatroom/login').then(res => {
      message.info(res.data.data.token)
    })
  }

  async loadPermissions() {
    let menus = [];
    try {
      let result = await axios.get('/api/common/user/loadPermissions');
      if (result.data.code) {
        let submenusMap = {};     // 用来记录各菜单的子菜单
        result.data.data.filter(pm => pm.type !== 'button').forEach(pm => {
          if (!pm.parent) {       // 没有parent的是一级菜单
            menus.push(pm);
          } else {                // 将菜单放到父菜单的数组中
            let id = pm.parent;
            if (!submenusMap[id]) {
              submenusMap[id] = [];
            }
            submenusMap[id].push(pm)
          }
          let id = pm._id;
          if (!submenusMap[id]) {
            submenusMap[id] = [];
          }
          pm.submenus = submenusMap[id];  // 链接子菜单数组
        })
      }
    } finally {
      console.log(menus);
      this.setState({ menus });
    }
  }

  // 定义柯里化函数，先接收 id，返回处理 checked 的函数
  // 正确的参数顺序：先接收 id（通过闭包固定），再接收 checked（onChange 传递的参数）
  handleCheckboxChange = (id) => async (checked) => {
    // 保存本地存储
    checked ?
      localStorage.setItem('hideEmergencyDialog', 'true') :
      localStorage.removeItem('hideEmergencyDialog');

    // 发起请求（确保 id 存在）
    if (id) {
      try {
        // 建议使用请求体传递参数（更规范）
        const res = await axios.post(`/api/consumer/systemDialog/stopNotice`, { id });
        console.log('请求成功:', res.data);
      } catch (error) {
        console.error('请求失败:', error.response || error);
        // 可选：提示用户错误
      }
    }
  };

  async loadSystemNotice() {
    let res = await axios.post(`/api/consumer/systemNoticeUser/list`);
    let noticeList = [];
    let gtc = res.data.gtc || [];
    for (let c of res.data.data) {
      noticeList.push(c);
    }
    this.setState({ systemNoticeList: noticeList })

    //未读
    if (gtc && gtc.length > 0) {
      this.setState({ hasNotification: true, noReadNoticeList: gtc })
    }
  }

  closeSystemDialog = async () => {
    if (this.state.dialogMsg.length > 0) {
      const newDialogMsg = [...this.state.dialogMsg];
      const firstMsg = newDialogMsg.shift();
      if (this.state.stopNotice) {
        await axios.post(`/api/consumer/systemDialog/stopNotice`, { id: firstMsg._id, status: 1 });
      }
      console.log('弹出的第一个元素:', firstMsg);
      this.setState({ dialogMsg: newDialogMsg, stopNotice: false });
    }
  };


  readGroupTask = async (id) => {
    await axios.post(`/api/consumer/task/readGroupTask`, { id });
  };

  onMenuClick({ item, key, keyPath }) {
    console.log('onMenuClick', item, key, keyPath);
    this.props.history.push(key)
  };

  onOpenChange(openKeys) {
    console.log('onOpenChange', openKeys);
    this.props.dispatch({ type: 'user/openKeys', openKeys: openKeys });
    this.setState({ openKeys })
  }

  handleClick = ({ key }) => {
    if (key == 'logout') {
      this.props.dispatch({ type: 'user/' + key })
    } else if (key == 'customer') {
    } else {
      let data = {};
      data[key] = true;
      this.setState(data);
    }
  };

  edit = ({ }) => {
    let form;
    if (this.state.editPassword) {
      if (!this.state.oldPassword || this.state.oldPassword == "") {
        message.warning(this.props.intl.formatMessage({ id: '请输入{value}' }, { value: intl.formatMessage({ id: '原密码' }) }));
        return
      }
      if (!this.state.newPassword || this.state.newPassword == "") {
        message.warning(this.props.intl.formatMessage({ id: '请输入{value}' }, { value: intl.formatMessage({ id: '新密码' }) }));
        return
      }
      if (!this.state.newPwd || this.state.newPwd == "") {
        message.warning(this.props.intl.formatMessage({ id: '请确认{value}' }, { value: intl.formatMessage({ id: '新密码' }) }));
        return
      }
      if (this.state.newPassword != this.state.newPwd) {
        message.warning('两次输入的密码不相等，请重新输入');
        return
      }
      form = {
        userID: this.props.userID,
        password: this.state.oldPassword,
        newPassword: this.state.newPassword
      };
    } else if (this.state.editName) {
      if (!this.state.editName || this.state.editName == "") {
        message.warning(this.props.intl.formatMessage({ id: '请输入{value}' }, { value: intl.formatMessage({ id: '昵称' }) }));
        return
      }
      form = {
        userID: this.props.userID,
        name: this.props.name,
        newName: this.state.newName
      };
    }
    this.props.dispatch({ type: 'user/edit', form });
    this.setState({ editPassword: false });
    this.setState({ editName: false });
  };

  async componentMount() {
    this.timer = setInterval(async () => {
      let res = await axios.post('/api/consumer/emegencyNotice/findMessage');
      if (res != null && res != '' && res.data.data != '') {
        let messageCount = res.data.data.messageCount
        if (res.data.data.message != null && res.data.data.message != '' && messageCount > 0) {
          let id = res.data.data._id
          this.setState({
            noticeMessage: res.data.data.message
          })
          this.openNotification('bottomRight');
          messageCount = messageCount - 1;
          await axios.post('/api/consumer/emegencyNotice/updateCount', {id: id, messageCount: messageCount});
        }
      } else {
        notification.close(`sd`)
      }
    }, 5000)

    this.timer2 = setInterval(async () => {
      if(this.state.dialogMsg.length > 0){
        return
      }
      let res = await axios.post('/api/consumer/systemDialog/find');
      if (res != null && res != '' && res.data.data != '') {
        // console.log("准备弹窗的数据为：",JSON.stringify(res.data.data))
        let messageCount = res.data.data.length
        console.log("数据数量为：",res.data.data.length)
        if (messageCount > 0) {
          this.setState({
            dialogMsg: res.data.data
          })
        }
      }
    },5000)
  }

  openNotification = (placement) => {
    let key = `sd`
    let message = (
      <div>
        <span><FormattedMessage id={'消息提示'} /></span>
        <audio autoplay="autoplay" loop="loop"><FormattedMessage id={'预警提示'} /></audio>
      </div>
    )
    let description = (
      <span>{this.state.noticeMessage}</span>
    )

    const args = {
      message,
      description,
      placement,
      duration: 3,
      key,
      onClose: () => {
        //flag = false
      }
    };
    //notification.open(args)
  };

  handleClickNotification = async () => {
    if (this.state.notification) {
      this.setState({ notification: false, hasNotification: false })
    } else {
      this.setState({ notification: true, hasNotification: false })

      //已读
      if (this.state.noReadNoticeList.length > 0) {
        await axios.post(`/api/consumer/systemNoticeUser/handle`, { ids: this.state.noReadNoticeList });
      }
    }
  }

  async getAuthenticationUrl () {
    let res = await axios.post('/api/user/getAuthenticationUrl');
    this.setState({authUrl: res.data.data});
    // console.log(res)
  }


  edit2FA = async () => {
    confirm({
      title: `确认已经扫描成功？`,
      content: '',
      okText: '确定',
      okType: 'danger',
      cancelText: '取消',
      onOk: async() => {
        try {
          let res = await axios.post('/api/user/setSecretKey');
          if (res.data.code == 1) {
            message.success('设置成功');
          } else {
            message.error(res.data.message);
          }
        } catch (e) {
          message.error(res.data.message);
        } finally {
          this.setState({edit2FA: false,});
        }
      },
      onCancel() {
      }
    })

  }

  render() {
    const { intl, location, permissions } = this.props;
    let menus = [];
    let submenusMap = {};     // 用来记录各菜单的子菜单
    permissions && permissions.filter(pm => pm.type !== 'button').forEach(pm => {
      if (!pm.parent) {       // 没有parent的是一级菜单
        menus.push(pm);
      } else {                // 将菜单放到父菜单的数组中
        let id = pm.parent;
        if (!submenusMap[id]) {
          submenusMap[id] = [];
        }
        submenusMap[id].push(pm)
      }
      let id = pm._id;
      if (!submenusMap[id]) {
        submenusMap[id] = [];
      }
      pm.submenus = submenusMap[id];  // 链接子菜单数组
    })

    return (
      <Layout className={Style.body}>
        <Head />

          <Modal
            title="设置2FA"
            visible={this.state.edit2FA}
            onOk={this.edit2FA}
            onCancel={()=>{this.setState({edit2FA:false})}}
          >
            <div style={{width: 250, margin: "50px auto", textAlign: "center"}}>
            <div>请使用谷歌authenticator APP或者其他身份验证码扫描下面二维码：</div>
            <div style={{margin:"20px auto"}}> {this.state.authUrl ? <img src={"/api/user/qr?id=" + this.state.authUrl} /> : '' }</div>
            </div>
          </Modal>

        <Dialog width={"32%"} className={Style.changeProfile} header={intl.formatMessage({ id: "修改{value}" }, { value: intl.formatMessage({ id: '密码' }) })} visible={this.state.editPassword}
          onConfirm={this.edit} onCancel={() => {
            this.setState({ editPassword: false })
          }} onClose={() => {
            this.setState({ editPassword: false })
          }}>
          <InputGroup>
            <Row className={Style.row} gutter={8}> <Col span={2} className={Style.redPoint}>*</Col>
              <Col span={4}><p> <FormattedMessage id={'请输入{value}'} values={{ value: intl.formatMessage({ id: '原密码' }) }} /> </p></Col> <Col span={18}> <Input onChange={(e) => {
                this.setState({ oldPassword: e.target.value.trim() })
              }} value={this.state.oldPassword} /> </Col> </Row>
          </InputGroup>

          <InputGroup>
            <Row className={Style.row} gutter={8}> <Col span={2} className={Style.redPoint}>*</Col>
              <Col span={4}><p> <FormattedMessage id={'请输入{value}'} values={{ value: intl.formatMessage({ id: '新密码' }) }} /> </p></Col> <Col span={18}> <Input onChange={(e) => {
                this.setState({ newPassword: e.target.value.trim() })
              }} value={this.state.newPassword} /> </Col> </Row>
          </InputGroup>

          <InputGroup>
            <Row className={Style.row} gutter={8}> <Col span={2} className={Style.redPoint}>*</Col>
              <Col span={4}><p> <FormattedMessage id={'请确认{value}'} values={{ value: intl.formatMessage({ id: '新密码' }) }} /> </p></Col> <Col span={18}> <Input onChange={(e) => {
                this.setState({ newPwd: e.target.value.trim() })
              }} value={this.state.newPwd} /> </Col> </Row>
          </InputGroup>
        </Dialog>

        <Dialog width={"32%"} className={Style.changeProfile} header={intl.formatMessage({ id: "修改{value}" }, { value: intl.formatMessage({ id: '信息' }) })}
          visible={this.state.editName} onConfirm={this.edit} onCancel={() => {
            this.setState({ editName: false })
          }} onClose={() => {
            this.setState({ editName: false })
          }}>
          <InputGroup>
            <Row className={Style.row} gutter={8}> <Col span={2} className={Style.redPoint}>*</Col>
              <Col span={4}><p> <FormattedMessage id={'请输入{value}'} values={{ value: intl.formatMessage({ id: '昵称' }) }} /> </p></Col> <Col span={18}> <Input onChange={(e) => {
                this.setState({ newName: e.target.value.trim() })
              }} value={this.state.newName} /> </Col> </Row>
          </InputGroup>
        </Dialog>


        <Dialog
          header={this.state.dialogMsg && this.state.dialogMsg.length > 0 ? this.state.dialogMsg[0].title : "紧急公告"}
          width={800}
          visible={this.state.dialogMsg.length > 0}
          forceRender={true}
          destroyOnClose={true}
          onConfirm={async () => {
          }} confirmLoading={this.state.loading}
          style={{
            height: 'auto',
            //maxHeight: '660px',
          }}
          placement='center'
          footer={null}
          onClose={this.closeSystemDialog}
          onCancel={this.closeSystemDialog}
          className="custom-dialog" // 添加自定义类名
        >
          <div style={{ fontSize: 'inherit' }}>
            <div
              style={{ maxHeight: '480px', overflowY: 'auto', fontFamily: 'inherit', whiteSpace: 'pre-wrap' }} // 继承页面字体，避免 Ant Design 默认字体干扰
              dangerouslySetInnerHTML={{ __html: this.state.dialogMsg[0]?.content || '' }}
            />
          </div>
          {this.state.dialogMsg.length > 0 && this.state.dialogMsg[0].stopNotice === 1 ? <>
            <div style={{ display: 'flex', justifyContent: 'flex-start', padding: '10px 16px' }}>
              <Checkbox
                onChange={(e) => { this.setState({ stopNotice: e.target.checked }) }}
              >
                不再显示该弹窗
              </Checkbox>
            </div>
          </> : <div style={{ height: '70px' }} />}
        </Dialog>


        <Header className={Style.header}>
          <div style={{display: 'flex', color: '#000', fontFamily: "MiSans VF", fontStyle: 'normal', fontWeight: 330}}>
            <div className={Style.logo} />
            <div style={{fontSize: 36, fontWeight: 520, lineHeight: '80px'}}>TNT 邮件</div>

            <div style={{fontSize: 20, lineHeight: '80px', marginLeft: 100, cursor: 'pointer'}} onClick={()=>{this.props.history.push('/main')}}>首页</div>
            <div style={{fontSize: 20, lineHeight: '80px', marginLeft: 47, cursor: 'pointer'}} onClick={() => {window.open('https://tnt-2.gitbook.io/tntmail/', '_blank')}}>操作指南</div>
            <div style={{fontSize: 20, lineHeight: '80px', marginLeft: 47, cursor: 'pointer'}} onClick={()=>{window.open('https://tnt-2.gitbook.io/tntmail/api-wen-dang', "_blank")}}>API</div>
            {/* <div style={{fontSize: 20, lineHeight: '80px', marginLeft: 47, cursor: 'pointer'}}>联系我们</div> */}
          </div>
          <div className={Style.headerRight}>
            <div style={{display: 'flex', marginRight: 60}}>
              <img src='/wallet.png' alt='wallet' style={{ width: 64, height: 64, aspectRatio: '1/1' }}></img>
              <div>
                <div onClick={() => {
                  if (this.props.openRecharge !== 'open') {
                    return
                  }
                  let keys = [...this.props.openKeys, 'userCenter']
                  this.props.dispatch({type: 'user/openKeys', openKeys: keys});
                  this.props.history.push('/cloud/user/recharge')
                }} style={{display: 'flex', lineHeight: '22px', alignItems: 'center', gap: 5, fontSize: 20, fontWeight: 600, marginTop: 10, color: '#000', cursor: 'pointer'}}>
                  <div>{this.props.balance}</div>
                  { this.props.openRecharge === 'open' ? <svg xmlns="http://www.w3.org/2000/svg" width="20" height="20" viewBox="0 0 20 20" fill="none">
                    <g clip-path="url(#clip0_853_18507)">
                      <path d="M10.0007 0.833344C4.95898 0.833344 0.833984 4.95834 0.833984 10C0.833984 15.0417 4.95898 19.1667 10.0007 19.1667C15.0423 19.1667 19.1673 15.0417 19.1673 10C19.1673 4.95834 15.0423 0.833344 10.0007 0.833344ZM14.1673 10.8333H10.834V14.1667C10.834 14.625 10.459 15 10.0007 15C9.54232 15 9.16732 14.625 9.16732 14.1667V10.8333H5.83398C5.37565 10.8333 5.00065 10.4583 5.00065 10C5.00065 9.54168 5.37565 9.16668 5.83398 9.16668H9.16732V5.83334C9.16732 5.37501 9.54232 5.00001 10.0007 5.00001C10.459 5.00001 10.834 5.37501 10.834 5.83334V9.16668H14.1673C14.6257 9.16668 15.0007 9.54168 15.0007 10C15.0007 10.4583 14.6257 10.8333 14.1673 10.8333Z" fill="#3978F7" />
                    </g>
                    <defs>
                      <clipPath id="clip0_853_18507">
                        <rect width="20" height="20" fill="white" />
                      </clipPath>
                    </defs>
                  </svg> : '' }
                </div>
                <div style={{display: 'flex', lineHeight: '22px', color: '#8B8B8B'}}>{`(冻结:${this.props.frozenBalance || 0})`}</div>
              </div>

            </div>
            <Popup
              content={
                <div style={{ borderRadius: '12px', background: '#FFF' }}>
                  {this.state.systemNoticeList.length == 0 && (
                    <List asyncLoading={false ? 'loading' : ''} size="small" split>
                      <ListItem
                        action={
                          <Space>
                            <a style={{ color: '#3978F7', fontSize: '14px', fontWeight: '400', lineHeight: '22px' }}>全部已读</a>
                          </Space>
                        }
                        style={{ marginLeft: '20px' }}
                      >
                        <ListItemMeta title={<div style={{ color: '#000', fontSize: '16px', fontWeight: '600', lineHeight: '22px' }}>通知</div>} />
                      </ListItem>
                      <Empty image={Empty.PRESENTED_IMAGE_SIMPLE} description="暂无数据" />
                    </List>
                  )}

                  {this.state.systemNoticeList.length > 0 && (
                    <List asyncLoading={false ? 'loading' : ''} size="small" split>
                      <ListItem
                        action={
                          <Space>
                            <a style={{ color: '#3978F7', fontSize: '14px', fontWeight: '400', lineHeight: '22px' }}>全部已读</a>
                          </Space>
                        }
                        style={{ marginLeft: '20px' }}
                      >
                        <ListItemMeta title={<div style={{ color: '#000', fontSize: '16px', fontWeight: '600', lineHeight: '22px' }}>通知</div>} />
                      </ListItem>
                      <div style={{ height: '500px', overflowY: 'auto' }}>
                        {this.state.systemNoticeList.map((item) => (
                          <ListItem key={item.id} style={{ marginLeft: '20px' }}>
                            <ListItemMeta title={<div style={{ color: '#000', fontSize: '14px', fontWeight: '600', lineHeight: '22px' }}>{item.title}</div>}
                              description={<div style={{ color: '#4B4B4B', fontSize: '14px', fontWeight: '400', lineHeight: '22px', whiteSpace: 'pre-line' }}>{item.content} </div>} />
                            {/* <span class="item-time">{formatDate(item.updateTime)}</span> */}
                          </ListItem>
                        ))}
                      </div>
                    </List>
                  )}
                </div>
              }
              visible={this.state.notification}
              placement="left"
              trigger="click"
              overlayStyle={{ width: '20%', overflow: 'auto', top: '55px', right: '-44px', boxShadow: '-10px 0 6px -5px rgba(0, 0, 0, 0.04), 0 10px 3px -5px rgba(0, 0, 0, 0.08)' }}
              onVisibleChange={this.handleClickNotification}
              showArrow={false}
              destroyOnClose
            >
              <div style={{ marginTop: -4 }}>
                <Badge dot={this.state.hasNotification}>
                  <NotificationIcon size="24px" style={{ cursor: 'pointer' }} />
                </Badge>
              </div>
            </Popup>
            <Dropdown overlay={<Menu onClick={this.handleClick} mode="horizontal" className={Style.menu}>
              <Menu.Item key="editPassword"><FormattedMessage id={'修改{value}'} values={{ value: intl.formatMessage({ id: '密码' }) }} /></Menu.Item>
              <Menu.Item key="editName"><FormattedMessage id={'修改{value}'} values={{ value: intl.formatMessage({ id: '名称' }) }} /></Menu.Item>
              <Menu.Divider />
              <Menu.Item key="logout" style={{ color: "red" }}><FormattedMessage id={'退出'} /></Menu.Item>
            </Menu>}>

              <a className={`ant-dropdown-link ${Style.title1}`} onClick={e => e.preventDefault()}>
                {this.props.name} <Icon type="down" className={Style.icon} />
              </a>
            </Dropdown>
          </div>
          {/*
            <div className={Style.version}>
              <div>{this.props.serverVersion.server}</div>
              <div>{this.props.serverVersion.gateway}</div>
            </div> */}
        </Header>
        <Layout>



          <Sider width={220} className='sider-origin' style={{borderRight: '10px solid #F7F7F7'}}>
            <Menu selectedKeys={[location.pathname]} subMenuCloseDelay={0} mode="inline" openKeys={this.props.openKeys} onClick={this.onMenuClick.bind(this)} onOpenChange={this.onOpenChange.bind(this)}
              className={Style.left_menu}>
              {(menus || []).sort((a, b) => a.index - b.index).map(menu =>
                <SubMenu key={menu.key} onTitleMouseEnter={() => { this.setState({ active: menu.key }) }} onTitleMouseLeave={() => this.setState({ active: '' })} title={<span> <img src={(/*this.state.openKeys.indexOf(menu.key) >= 0 ||*/ this.state.active === menu.key ? "/icons2/" : "/icons/") + menu.icon.type} className={Style.iconImg} /><FormattedMessage id={menu.name} /> <span className={this.state.openKeys.indexOf(menu.key) >= 0 ? this.state.active === menu.key ? Style.arrowImg2 : Style.arrowImg1 : this.state.active === menu.key ? Style.arrowImg4 : Style.arrowImg3} ></span> </span>}>
                  {
                    menu.submenus.filter(e => e.key !== 'recharge' || this.props.openRecharge === 'open').sort((a, b) => a.index - b.index).map(submenu => {
                      let badge = '';
                      switch (submenu.key) {
                      }
                      return <Menu.Item key={submenu.url || submenu.key}> <FormattedMessage id={submenu.name} /> {badge} </Menu.Item>
                    })}
                </SubMenu>
              )}
            </Menu>
          </Sider>


          <Layout>
            <Content className={Style.content}>
              <Switch>
                <Route exact={true} path="/cloud2/" component={Index} />
                <Route exact={true} path="/cloud/" component={UserInfo} />


                <Route exact={true} path="/admin/" component={Home} />
                {/* <Route path="/cloud/user/machine" component={UserVps} /> */}
                <Route path="/cloud/account/socks5/list" component={Socks5} />
                <Route path="/cloud/user/proxyAccount/list" component={proxyAccountList}/>
                <Route path="/cloud/account/platform/list" component={PlatformList} />
                {/* <Route path="/admin/account/sendMessageTask"cloud component={StrangerSendMsg} /> */}
                <Route path="/cloud/account/email/list" component={UserAccount} />
                <Route path="/cloud/account/task/list" component={TaskList} />
                <Route path="/cloud/account/receive/list" component={Receive} />
                <Route path="/cloud/account/accountGroup" component={UserAccountGroup} />
                {/* <Route path="/cloud/user/balanceDetail" component={BalanceDetail} /> */}
                {/* <Route path="/cloud/user/payment" component={Payment} /> */}
                <Route path="/admin/account/role/list" component={roleList} />
                <Route path="/admin/account/user/list" component={User} />
                {/* <Route path="/admin/ac/account/vps/list" component={Vps} /> */}
                <Route path="/admin/ac/account/params/globalParams" component={Params} />

                <Route path="/cloud/user/apiKey/list" component={ApiKeyList}/>
                <Route path="/cloud/account/googleStudio/list" component={googleStudioList}/>
                <Route path="/cloud/account/chatgpt/list" component={chatgptList}/>
                <Route path="/cloud/account/aiServer/list" component={aiServer}/>


                <Route path="/cloud/user/center" component={UserInfo} />
                <Route path="/cloud/user/order" component={MyOrder} />
                <Route path="/cloud/user/orderV2" component={MyOrderV2} />
                <Route path="/cloud/user/email" component={MyEmail} />
                <Route path="/cloud/user/bill" component={MyBill} />
                <Route path="/cloud/user/recharge" component={Recharge} />
                <Route path="/cloud/user/ownerUser" component={ownerUser} />
                <Route path="/cloud/user/batchSendEmail" component={batchSendEmail} />
                <Route path="/cloud/user/batchSendEmailAdd" component={batchSendEmailAdd} />
                <Route path="/cloud/user/linkCheck" component={linkCheck} />
                <Route path="/cloud/user/contentAIGen" component={contentAIGen} />
                <Route path="/cloud/user/tgnetToSession" component={tgnetToSession} />
                <Route path="/cloud/user/systemMailTemplate" component={systemMailTemplate}/>
                <Route path="/cloud/user/myMailTemplate" component={myMailTemplate}/>


                <Route path="/cloud/account/billManage/list" component={billManage} />
                <Route path="/cloud/account/order/list" component={orderManage} />
                <Route path="/cloud/account/orderV2/list" component={orderManageV2} />
                <Route path="/cloud/account/emailOrder/list" component={emailManage} />

                <Route path="/cloud/account/apiResource/list" component={sendGridAccount}/>

                <Route path="/cloud/account/emailCheckActive/list" component={emailCheckActive}/>
                <Route path="/cloud/account/sieveActive/list" component={sieveActive}/>
                <Route path="/cloud/account/aiTokenStatistics/list" component={aiTokenStatistics}/>

                <Route path="/admin/account/aiModel/list" component={aiModelList}/>

                <Route path="/admin/common/dnsRecord/list" component={dnsRecordList}/>

              </Switch>
            </Content>
          </Layout>
        </Layout>
      </Layout>
    )
  }
}

export default connect(({ user }) => ({
  userID: user.info.userID,
  name: user.info.name,
  group: user.info.group,
  label: user.info.label,
  customer: user.info.customer,
  serverVersion: user.info.serverVersion || {},
  balance: user.info.balance,
  frozenBalance: user.info.frozenBalance,
  openKeys: user.openKeys,
  permissions: user.permissions,
  setSecretKey: user.info.setSecretKey,
  openRecharge: user.info.openRecharge
}))(TimerWrapper(injectIntl(withRouter(BackStage))))
