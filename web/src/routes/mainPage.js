import React, { Component } from 'react'
import { BrowserRouter as Router, Route, Link } from 'react-router-dom'
import {
  // Icon,
  Table,
  Divider,
  Upload,
  message,
  Switch,
  Avatar,
  Card,
  // Button,
  Input,
  Form,
  Spin,
  Dropdown,
  Menu,
  Icon,
  Row,
  Col,
} from 'antd'
import { connect } from 'dva';
import { Redirect } from 'dva/router'
import ChangeLocale from '../nb-intl/ChangeLocale'
import { Input as TInput, InputAdornment, Button, loading, Dialog, InputNumber as TInputNumber } from 'tdesign-react'
import { SearchIcon } from 'tdesign-icons-react'
import { FormattedMessage, useIntl, injectIntl } from 'react-intl'
import Head from "../head";
import Style from './index.css'
import axios from 'axios'
import DialogApi from './admin/common/dialog/DialogApi';

const InputGroup = Input.Group;
const Search = Input.Search;

class MyComponent extends Component {

  constructor(props) {
    super(props);
    this.state = {
      editPassword: false,
      oldPassword: '',
      newPassword: '',
      newPwd: '',

      editName: false,
      newName: '',

      balance: 0, //账号余额
      allPlatforms: [],
      searchWord: '',
      buyVisible: false,
      platform: {},
      buyCount: '',
      loading: false
    };
  }
  async componentWillMount() {
    if (!this.props.user.checked) {
      this.props.dispatch({ type: 'user/stat' });
    }
    this.getAllPlatform()
  }

  async getAllPlatform() {
    let res = await axios.post(`/api/consumer/platform/common/10000/1`, { filters: {}, sorter: { sortNo: -1, createTime: -1 } });
    if (res.data.code === 1) {
      this.setState({
        allPlatforms: res.data.data.data,
        // platform: res.data.data.data[0],
        // buyVisible: true
      })
    }
  }

  handleClick = ({ key }) => {
    this.setState({ newName: '', newPassword: '' });
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
        message.warning(this.props.intl.formatMessage({ id: '请输入{value}' }, { value: this.props.intl.formatMessage({ id: '原密码' }) }));
        return
      }
      if (!this.state.newPassword || this.state.newPassword == "") {
        message.warning(this.props.intl.formatMessage({ id: '请输入{value}' }, { value: this.props.intl.formatMessage({ id: '新密码' }) }));
        return
      }
      if (!this.state.newPwd || this.state.newPwd == "") {
        message.warning(this.props.intl.formatMessage({ id: '请确认{value}' }, { value: this.props.intl.formatMessage({ id: '新密码' }) }));
        return
      }
      if (this.state.newPassword != this.state.newPwd) {
        message.warning('两次输入的密码不相等，请重新输入');
        return
      }
      form = {
        userID: this.props.user.info.userID,
        password: this.state.oldPassword,
        newPassword: this.state.newPassword
      };
    } else if (this.state.editName) {
      if (!this.state.editName || this.state.editName == "") {
        message.warning(this.props.intl.formatMessage({ id: '请输入{value}' }, { value: this.props.intl.formatMessage({ id: '昵称' }) }));
        return
      }
      form = {
        userID: this.props.user.info.userID,
        name: this.props.user.info.name,
        newName: this.state.newName
      };
    }
    this.props.dispatch({ type: 'user/edit', form }).then(() => {
      this.props.dispatch({ type: 'user/stat' });
    });
    this.setState({ editPassword: false });
    this.setState({ editName: false });
  };

  buy(v) {
    if (!this.props.user.info) {
      this.props.history.push('/login')
      return
    }
    this.setState({
      buyVisible: true,
      platform: v
    })
  }

  render() {
    return (
      <div style={{ background: '#F5F5F5' }}>
        <Dialog width={"32%"} className={Style.changeProfile} header={this.props.intl.formatMessage({ id: "修改{value}" }, { value: this.props.intl.formatMessage({ id: '密码' }) })} visible={this.state.editPassword}
          onConfirm={this.edit} onCancel={() => {
            this.setState({ editPassword: false })
          }} onClose={() => {
            this.setState({ editPassword: false })
          }}>
          <InputGroup>
            <Row className={Style.row} gutter={8}> <Col span={2} className={Style.redPoint}>*</Col>
              <Col span={4}><p> <FormattedMessage id={'请输入{value}'} values={{ value: this.props.intl.formatMessage({ id: '原密码' }) }} /> </p></Col> <Col span={18}> <Input onChange={(e) => {
                this.setState({ oldPassword: e.target.value.trim() })
              }} value={this.state.oldPassword} /> </Col> </Row>
          </InputGroup>

          <InputGroup>
            <Row className={Style.row} gutter={8}> <Col span={2} className={Style.redPoint}>*</Col>
              <Col span={4}><p> <FormattedMessage id={'请输入{value}'} values={{ value: this.props.intl.formatMessage({ id: '新密码' }) }} /> </p></Col> <Col span={18}> <Input onChange={(e) => {
                this.setState({ newPassword: e.target.value.trim() })
              }} value={this.state.newPassword} /> </Col> </Row>
          </InputGroup>

          <InputGroup>
            <Row className={Style.row} gutter={8}> <Col span={2} className={Style.redPoint}>*</Col>
              <Col span={4}><p> <FormattedMessage id={'请确认{value}'} values={{ value: this.props.intl.formatMessage({ id: '新密码' }) }} /> </p></Col> <Col span={18}> <Input onChange={(e) => {
                this.setState({ newPwd: e.target.value.trim() })
              }} value={this.state.newPwd} /> </Col> </Row>
          </InputGroup>
        </Dialog>

        <Dialog width={"32%"} className={Style.changeProfile} header={this.props.intl.formatMessage({ id: "修改{value}" }, { value: this.props.intl.formatMessage({ id: '信息' }) })}
          visible={this.state.editName} onConfirm={this.edit} onCancel={() => {
            this.setState({ editName: false })
          }} onClose={() => {
            this.setState({ editName: false })
          }}>
          <InputGroup>
            <Row className={Style.row} gutter={8}> <Col span={2} className={Style.redPoint}>*</Col>
              <Col span={4}><p> <FormattedMessage id={'请输入{value}'} values={{ value: this.props.intl.formatMessage({ id: '昵称' }) }} /> </p></Col> <Col span={18}> <Input onChange={(e) => {
                this.setState({ newName: e.target.value.trim() })
              }} value={this.state.newName} /> </Col> </Row>
          </InputGroup>
        </Dialog>

        <div className='mainPage-header'>
          <div style={{ display: 'flex', alignItems: 'center' }}>
            <img src='/logo.png' alt='logo' style={{ width: 80, height: 80, aspectRatio: '1/1' }}></img>
            <div style={{ color: '#000', fontSize: 36, fontWeight: 520, fontFamily: 'MiSans VF', marginLeft: 10, marginRight: 100 }}>TNT 邮件</div>

            <div className='header-nav' onClick={()=>{this.props.history.push('/main')}}>首页</div>
            <div className='header-nav' onClick={() => {window.open('https://tnt-2.gitbook.io/tntmail/', '_blank')}}>操作指南</div>
            <div className='header-nav' onClick={()=>{window.open('https://tnt-2.gitbook.io/tntmail/api-wen-dang', "_blank")}}>API</div>
            {/* <div className='header-nav'>联系我们</div> */}
          </div>

          <div style={{ display: 'flex', alignItems: 'center' }}>
            {
              this.props.user.info ? (
                <>
                  <div className='header-nav' onClick={() => {
                    let keys = [...this.props.openKeys, 'userCenter']
                    this.props.dispatch({ type: 'user/openKeys', openKeys: keys });
                    this.props.history.push('/cloud/user/center')
                  }}>个人中心</div>
                  <div style={{ display: 'flex', marginRight: 60 }}>
                    <img src='/wallet.png' alt='wallet' style={{ width: 64, height: 64, aspectRatio: '1/1' }}></img>
                    <div>
                      <div onClick={() => {
                        if (this.props.openRecharge !== 'open') {
                          return
                        }
                        let keys = [...this.props.openKeys, 'userCenter']
                        this.props.dispatch({ type: 'user/openKeys', openKeys: keys });
                        this.props.history.push('/cloud/user/recharge')
                      }} style={{ display: 'flex', lineHeight: '22px', alignItems: 'center', gap: 5, fontSize: 20, fontWeight: 600, marginTop: 10, color: '#000', cursor: 'pointer' }}>
                        <div>{this.props.user.info.balance}</div>
                        { this.props.openRecharge === 'open' ?<svg xmlns="http://www.w3.org/2000/svg" width="20" height="20" viewBox="0 0 20 20" fill="none">
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
                      <div style={{ display: 'flex', lineHeight: '22px', color: '#8B8B8B' }}>{`(冻结:${this.props.user.info.frozenBalance || 0})`}</div>
                    </div>

                  </div>
                  <div>
                    <Dropdown overlay={<Menu onClick={this.handleClick} mode="horizontal" className={Style.menu}>
                      <Menu.Item key="editPassword"><FormattedMessage id={'修改{value}'} values={{ value: this.props.intl.formatMessage({ id: '密码' }) }} /></Menu.Item>
                      <Menu.Item key="editName"><FormattedMessage id={'修改{value}'} values={{ value: this.props.intl.formatMessage({ id: '名称' }) }} /></Menu.Item>
                      <Menu.Divider />
                      <Menu.Item key="logout" style={{ color: "red" }}><FormattedMessage id={'退出'} /></Menu.Item>
                    </Menu>}>

                      <a className={`ant-dropdown-link ${Style.title1}`} style={{ display: 'flex', gap: 10 }} onClick={e => e.preventDefault()}>
                        <div style={{ display: 'flex', alignItems: 'center' }}>
                          <svg xmlns="http://www.w3.org/2000/svg" width="32" height="32" viewBox="0 0 32 32" fill="none">
                            <g opacity="0.9">
                              <path d="M16.0007 3.99967C9.37323 3.99967 4.00065 9.37226 4.00065 15.9997C4.00065 18.6805 4.87883 21.1554 6.36506 23.1535C7.26808 20.5419 9.74867 18.6663 12.6673 18.6663H19.334C22.2526 18.6663 24.7332 20.5419 25.6362 23.1535C27.1225 21.1554 28.0007 18.6805 28.0007 15.9997C28.0007 9.37226 22.6281 3.99967 16.0007 3.99967ZM23.334 25.499V25.333C23.334 23.1239 21.5431 21.333 19.334 21.333H12.6673C10.4582 21.333 8.66732 23.1239 8.66732 25.333V25.499C10.6956 27.0674 13.2378 27.9997 16.0007 27.9997C18.7635 27.9997 21.3057 27.0674 23.334 25.499ZM1.33398 15.9997C1.33398 7.8995 7.90047 1.33301 16.0007 1.33301C24.1008 1.33301 30.6673 7.8995 30.6673 15.9997C30.6673 20.4606 28.6741 24.4574 25.5344 27.1453C22.9713 29.3397 19.6392 30.6663 16.0007 30.6663C12.3621 30.6663 9.03 29.3397 6.46686 27.1453C3.32716 24.4574 1.33398 20.4606 1.33398 15.9997ZM16.0007 7.99967C14.1597 7.99967 12.6673 9.49206 12.6673 11.333C12.6673 13.174 14.1597 14.6663 16.0007 14.6663C17.8416 14.6663 19.334 13.174 19.334 11.333C19.334 9.49206 17.8416 7.99967 16.0007 7.99967ZM10.0007 11.333C10.0007 8.0193 12.6869 5.33301 16.0007 5.33301C19.3144 5.33301 22.0007 8.0193 22.0007 11.333C22.0007 14.6467 19.3144 17.333 16.0007 17.333C12.6869 17.333 10.0007 14.6467 10.0007 11.333Z" fill="black" fill-opacity="0.9" />
                            </g>
                          </svg>
                        </div>
                        <div style={{ display: 'flex', alignItems: 'center', gap: 10, color: 'rgba(0, 0, 0, 0.90)' }}>
                          {this.props.user.info.name} <Icon type="down" className={Style.icon} />
                        </div>
                      </a>
                    </Dropdown>
                  </div>
                </>
              ) : (
                <div onClick={() => {
                  this.props.history.push('/login')
                }} style={{ color: 'rgba(255,255,255,0.9)', cursor: 'pointer', display: 'flex', padding: '8px 24px', justifyContent: 'center', alignItems: 'center', gap: 10, borderRadius: 20, background: '#1A73E8' }}>
                  登录
                </div>
              )
            }
          </div>
        </div>

        <div className='mainPage-content'>
          <div>
            <TInput
              placeholder="搜索"
              onChange={value => this.setState({ searchWord: value })}
              className='mainPage-search'
              suffix={<SearchIcon />}
            />
          </div>
          <div style={{ marginTop: 55 }}>
            {
              this.state.allPlatforms.filter(e => e.name.toLowerCase().indexOf(this.state.searchWord.toLowerCase()) >= 0).map(e =>
              (<div style={{ display: 'inline-block', width: 390, height: 152, padding: 16, margin: '0 28px 30px 0', background: '#FFF', borderRadius: '12px' }}>
                <div style={{ display: 'flex' }}>
                  <div style={{ marginRight: 24 }}>
                    <img src={`/api/consumer/res/download/${e.icon}`} style={{ width: 120, height: 120, aspectRatio: '1/1', borderRadius: '5px' }} />
                  </div>
                  <div>
                    <div style={{ color: '#000', fontSize: 24, fontWeight: 600, lineHeight: '24px', marginBottom: '12px', marginTop: '10px' }}>{e.name}</div>
                    <div style={{ color: 'rgba(0, 0, 0, 0.60)', fontSize: 16, marginBottom: '12px', lineHeight: '24px' }}>{`库存:${e.canUseAccountNumber}`}</div>
                    <div style={{ display: 'flex', justifyContent: 'space-between', width: 212 }}>
                      <div style={{ color: 'rgba(0, 0, 0, 0.60)', fontSize: 16 }}><span style={{ color: '#F00', fontSize: 20, fontWeight: 600, lineHeight: '24px' }}>{e.price}</span>&nbsp;/个</div>
                      {e.canUseAccountNumber > 0 ? <div onClick={() => this.buy(e)} style={{ borderRadius: '3px', background: '#3978F7', padding: '5px 16px', color: 'rgba(255,255,255,.9)', cursor: 'pointer' }}>购买</div> :
                        <div style={{ borderRadius: '3px', background: '#B5C7FF', padding: '5px 16px', color: 'rgba(255,255,255,.9)', cursor: 'pointer' }}>售罄</div>}
                    </div>
                  </div>
                </div>
              </div>)
              )
            }
          </div>
        </div>
        <div className='mainPage-footer'>
          <div style={{ height: 223, display: 'flex' }}>
            <div style={{ marginLeft: 100 }}>
              <div style={{ color: '#000', fontWeight: 600, fontSize: 20, margin: '48px 0 42px 0' }}>关于我们</div>
              <div style={{ color: 'rgba(0, 0, 0, 0.60)', fontSize: 16 }}>专注于 Gmail 账号的邮箱接码服务，为用户提供高质量的资源和便捷的交易体验</div>
            </div>

            {/* <div style={{ marginLeft: '600px' }}>
              <div style={{ color: '#000', fontWeight: 600, fontSize: 20, margin: '48px 0 25px 0' }}>帮助中心</div>
              <div style={{ color: 'rgba(0, 0, 0, 0.60)', fontSize: 16, marginBottom: 16, lineHeight: '22px', cursor: 'pointer' }}>如何购买</div>
              <div style={{ color: 'rgba(0, 0, 0, 0.60)', fontSize: 16, marginBottom: 16, lineHeight: '22px', cursor: 'pointer' }}>账号充值</div>
              <div style={{ color: 'rgba(0, 0, 0, 0.60)', fontSize: 16, lineHeight: '22px', cursor: 'pointer' }}>联系客服</div>
            </div> */}
          </div>
          <div style={{ height: 53, lineHeight: '53px', textAlign: 'center', borderTop: '1px solid #E7E7E7' }}>&copy;2025 TNT 邮件  All rights reserved.</div>
        </div>

        <Dialog width={617} header={'下单'} visible={this.state.buyVisible}
          placement='center'
          loading={this.state.loading}
          onConfirm={() => {
            if (this.state.buyCount <= 0) {
              message.error('购买数量不能小于0')
              return
            }
            if (this.state.buyCount > 99999) {
              message.error('购买数量不能大于99999')
              return
            }
            if (this.state.loading) {
              return
            }
            this.state.loading = true
            this.setState({ loading: true })
            axios.post('/api/buyEmailOrder/buy', {
              platformId: this.state.platform._id,
              buyNum: this.state.buyCount
            }).then(async res => {
              if (res.data.code === 1) {
                message.success('下单成功')
                await new Promise(v => setTimeout(() => {
                  this.props.dispatch({ type: 'user/stat' });
                  this.setState({ buyVisible: false })
                  let keys = [...this.props.openKeys, 'userCenter']
                  this.props.dispatch({ type: 'user/openKeys', openKeys: keys });
                  this.props.history.push('/cloud/user/email?orderNo=' + res.data.data)
                  v()
                }, 3000))
              } else {
                if (res.data.message === 'user balance insufficient') {
                  DialogApi.warning({
                    title: ' 账户余额不足，请先充值',
                    onOk: this.props.openRecharge === 'open' ? () => {
                      let keys = [...this.props.openKeys, 'userCenter']
                      this.props.dispatch({ type: 'user/openKeys', openKeys: keys });
                      this.props.history.push('/cloud/user/recharge')
                    } : null,
                    onCancel: () => {

                    },
                    onOkTxt: '立即充值',
                    width: 457
                  })
                } else {
                  message.error(res.data.message)
                }
              }
            }).finally(() => {
              this.state.loading = false
              this.setState({ loading: false })
            })
          }} onCancel={() => {
            this.setState({ buyVisible: false })
          }} onClose={() => {
            this.setState({ buyVisible: false })
          }}>
          {this.state.platform ? <div style={{ height: 541 }}>
            <div style={{ margin: '24px 32px', paddingLeft: '16px' }}>
              <div style={{ display: 'flex' }}>
                <div style={{ marginRight: 8 }}>
                  <img src={`/api/consumer/res/download/${this.state.platform.icon}`} style={{ width: 120, height: 120, aspectRatio: '1/1', borderRadius: '5px' }} />
                </div>
                <div>
                  <div style={{ color: 'rgba(0,0,0,.9)', fontSize: 18, fontWeight: 600, lineHeight: '32px' }}>{this.state.platform.name}</div>

                  <div style={{ color: 'rgba(0,0,0,.6)', fontSize: 14, margin: '8px 0' }}>库存：{this.state.platform.canUseAccountNumber}</div>
                  <div style={{ color: 'rgba(0,0,0,.6)', fontSize: 14 }}>单价：<span style={{ color: '#D54941', fontSize: 18, fontWeight: 600, lineHeight: '26px' }}>{this.state.platform.price}点</span> /个</div>
                </div>
              </div>
              <div style={{ color: 'rgba(0, 0, 0, 0.90)', fontSize: 14, marginTop: 16 }}>
                <span style={{ color: '#D54941' }}>*</span>购买数量
              </div>
              <div style={{marginTop: 16}}>
                <TInputNumber
                  style={{width: 200}}
                  size="medium"
                  theme="row"
                  value={this.state.buyCount}
                  min={1}
                  max={99999}
                  onChange={(v) => {
                    this.setState({buyCount: v})
                  }}
                />
              </div>

              <div style={{marginTop: 16, padding: 16, background: '#F9FAFB', borderRadius: 8}}>
                  <div style={{display: 'flex', justifyContent: 'space-between'}}>
                    <div style={{color: '#000', lineHeight: '22px', fontSize: 14}}>单价</div>
                    <div style={{color: '#777', lineHeight: '22px', fontSize: 14}}>{this.state.platform.price}点</div>
                  </div>
                  <div style={{display: 'flex', justifyContent: 'space-between', marginTop: 8}}>
                    <div style={{color: '#000', lineHeight: '22px', fontSize: 14}}>数量</div>
                    <div style={{color: '#777', lineHeight: '22px', fontSize: 14}}>{this.state.buyCount || 0}个</div>
                  </div>

                  <div style={{display: 'flex', justifyContent: 'space-between', marginTop: 10, paddingTop: 10, borderTop: '1px solid #E7E7E7'}}>
                    <div style={{color: '#000', lineHeight: '26px', fontSize: 18, fontWeight: 600}}>应付总额</div>
                    <div style={{color: '#D54941', lineHeight: '28px', fontSize: 20, fontWeight: 600}}>{((this.state.buyCount || 0) * (this.state.platform.price || 0)).toFixed(2)}点</div>
                  </div>
              </div>

              <div>
                <div style={{color: '#000', lineHeight: '20px', fontSize: 12, fontWeight: 600, marginBottom: 8, marginTop: 16}}>购买须知</div>
                <div class="dot-list">每个邮箱资源有效期为20min，请尽快使用或手动释放。超时未处理系统将自动扣除点数，视为资源已使用。</div>
                <div class="dot-list">20min内邮箱失效平台将自动释放并退还点数。</div>
                <div class="dot-list">请确认平台和数量无误再下单。</div>
              </div>
            </div>
          </div> : ''}
        </Dialog>
        <Head />
      </div>
    )
  }
}

export default injectIntl(connect(({ user, loading }) => ({ user, openKeys: user.openKeys,openRecharge: user.info && user.info.openRecharge }))(injectIntl(MyComponent)))
