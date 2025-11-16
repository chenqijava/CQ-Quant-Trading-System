import React, { Component } from 'react'
import { BrowserRouter as Router, Route, Link } from 'react-router-dom'
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
  DatePicker,
  InputNumber,
  Avatar,
  Checkbox,
  Row,
  Col,
  Tooltip,
  Radio,
  Switch,
  Tag
} from 'antd'
import axios from 'axios'
import { connect } from 'dva'
import { injectIntl } from 'react-intl'
import { formatDate } from 'components/DateFormat'
import expenseType from 'components/expenseType'
import { Pagination, Breadcrumb, Dialog, Steps, Space, Input as TInput, Upload as TUpload, Tooltip as TTooltip, Button as TButton, InputAdornment } from 'tdesign-react';
import copy from 'copy-to-clipboard';
const { StepItem } = Steps;


const Search = Input.Search
const confirm = Modal.confirm
const { BreadcrumbItem } = Breadcrumb;
const { RangePicker } = DatePicker;

const steps = [
  { title: 'creatOrder', content: '创建充值订单' },
  { title: 'orderInfo', content: '充值订单信息' },
];

// 针对当前页面的基础url
const baseUrl = '/api/payment';

const rechargeMethodParam = {
  type: 'account',
  code: 'rechargeMethod',
  desc: '充值方案',
}

class MyComponent extends Component {
  constructor(props) {
    super(props);
    // data由服务器返回表格的数据
    // loading由客户端控制
    // pagination控制表格分页功能，其中数据总数pagination.total由服务器返回
    // 以上变量受控，因为需要传递给Table控件
    this.state = {
      amount: 0,
      address: "",
      arCode: "",
      paystep: 1,
      billTypes: {},
      deadTime: 30,
      rechargeMethod: [],
      value: '',
      send: 0,
      errorInfo: '最低充值金额为$5',
      orderNo: '',
    }

    this.timer = null
  }

  componentWillMount() {
    this.reloadConfig();
  }

  componentWillUnmount () {
    if (this.timer) {
      clearInterval(this.timer)
      this.timer = null
    }
  }

  returnBefore = async () => {
    this.setState({
      paystep: 1,
      amount: ""
    });
  }

  async reloadConfig() {
    let res = await axios.get(`/api/consumer/params/${rechargeMethodParam.type || pageType}/get/${rechargeMethodParam.code}`);
    if (res.data.code) {
      this.setState({ rechargeMethod: (typeof res.data.value === 'string' ? JSON.parse(res.data.value) : res.data.value).map((e, index) => ({ ...e, index: index + 1 })) });
    }
  }

  submitData = async () => {
    let val = Number(this.state.value)
    if (val < 5) {
      return
    }
    if (val > 99999999) {
      message.error('金额超过限制');
      return;
    }
    if (val < 5) {
      message.error('金额必须大于5');
      return;
    }

    let data = {
      amount: Number(this.state.value)
    }

    let res = await axios.post(`${baseUrl}/create/order`, data)
    if (res.data.code == 1) {
      this.setState({
        address: res.data.data.address,
        arCode: res.data.data.qrCode,
        amount: res.data.data.amount,
        deadTime: res.data.data.deadTime,
        paystep: 2,
        orderNo: res.data.data.orderNo
      });
      this.checkOrderStatus()
    } else {
      Modal.error({ title: res.data.message });
    }
  }

  async checkOrderStatus () {
    let res = await axios.post(`${baseUrl}/order/${this.state.orderNo}`, {})
    if (res.data.code == 1 && res.data.data.status === 'success') {
      message.success('充值成功');
      this.props.dispatch({ type: 'user/stat' });
      this.setState({
        paystep: 1,
      })
    } else {
      if (this.timer) {
        clearInterval(this.timer)
        this.timer = null
      }
      this.timer = setTimeout(() => {
        this.checkOrderStatus()
      }, 5000)
    }
  }

  render() {

    return (<div>
      <Breadcrumb>
        <BreadcrumbItem>个人中心</BreadcrumbItem>
        <BreadcrumbItem>在线充值</BreadcrumbItem>
      </Breadcrumb>
      {this.state.paystep == 1 ?
        <div style={{ width: '576px', marginLeft: 225 }}>
          <div style={{ width: '440px', margin: '35px auto 41px' }}>
            <Steps
              defaultCurrent={0}
              layout="horizontal"
              separator="line"
              sequence="positive"
              theme="default"
              readonly="true"
            >
              <StepItem
                title="创建充值订单"
              />
              <StepItem
                title="充值订单信息"
              />
            </Steps>
          </div>

          <div>
            <div style={{ color: '#000', fontSize: 16, fontWeight: 600, marginBottom: 13 }}>注：1 USDT=1 点数</div>

            <div>
              {
                this.state.rechargeMethod.filter(e => e.status === 'enable').map((e, index) => {
                  return <div onClick={() => {
                    let send = e.send
                    let value = e.value
                    this.setState({ value, send })
                  }} style={{ display: 'inline-block', margin: (index + 1) % 3 !== 0 ? '0 24px 24px 0' : '0 0 24px 0', width: 176, height: 108, borderRadius: 12, boxShadow: '0px 1px 10px 0px rgba(0, 0, 0, 0.05), 0px 4px 5px 0px rgba(0, 0, 0, 0.08), 0px 2px 4px -1px rgba(0, 0, 0, 0.12)', cursor: 'pointer' }}>
                    <div style={{ textAlign: 'center', margin: '24px 0 16px', fontSize: 24, fontWeight: 600, lineHeight: '22px', color: '#000' }}>${e.value}</div>
                    {e.send > 0 ? <div style={{ color: ' #E37318', fontSize: 14, textAlign: 'center' }}>限时再赠${e.send}</div> : <div>&nbsp;</div>}
                  </div>
                })
              }
            </div>

            <div style={{ color: '#000', fontSize: 16, margin: '40px 0 18px' }}>充值金额：</div>

            <InputAdornment append="USDT">
              <TInput
                placeholder="请输入充值金额"
                className='rechargeInput'
                clearable
                value={this.state.value}
                onChange={(value) => {
                  let send = 0
                  value = Number(value)
                  for (let i = 0; i < this.state.rechargeMethod.length; i++) {
                    let m = this.state.rechargeMethod[i]
                    if (m.status === 'enable') {
                      if (value >= m.value) {
                        send = m.send
                      } else {
                        break
                      }
                    }
                  }
                  this.setState({ value, send })
                }}
              />
            </InputAdornment>
            {Number(this.state.value) < 5 ? <div style={{ color: '#D54941', fontSize: 12 }}>{this.state.errorInfo}</div> : ''}

            {Number(this.state.value) >= 5 ? <div style={{ padding: 16 }}>
              <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: 8 }}>
                <div style={{ color: '#000', fontSize: 14, }}>充值金额</div>
                <div style={{ color: '#777', fontSize: 14 }}><span style={{ fontWeight: 600 }}>$</span>{this.state.value}</div>
              </div>

              <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: 8 }}>
                <div style={{ color: '#000', fontSize: 14, }}>赠送金额</div>
                <div style={{ color: '#777', fontSize: 14 }}><span style={{ fontWeight: 600 }}>$</span>{this.state.send}</div>
              </div>

              <div style={{ display: 'flex', justifyContent: 'space-between', color: '#000', fontSize: 18, fontWeight: 600, borderTop: '1px solid #E7E7E7', paddingTop: 10 }}>
                <div>预计总到账</div>
                <div>{(Number(this.state.send) + Number(this.state.value)).toFixed(2)}点数</div>
              </div>
            </div> : ''}

            <div onClick={() => this.submitData()} style={{ marginBottom: 24, marginTop: 24, width: 576, height: 40, background: Number(this.state.value) >= 5 ? '#3978F7' : 'rgb(181, 199, 255)', borderRadius: 3, lineHeight: '40px', textAlign: 'center', color: 'rgb(255, 255, 255,.9)', fontSize: 16, cursor: 'pointer' }}>

              提交
            </div>

            <div style={{ color: 'rgba(0, 0, 0, 0.60)', fontSize: 16 }}>
              提示：充值后将自动兑换为平台点数，仅可用于站内消费，概不支持退款，请确认后再操作。
            </div>
          </div>

        </div> : ""}

      {this.state.paystep == 2 ? <div style={{ width: '576px', marginLeft: 252, marginBottom: 20 }}>
        <div style={{ width: '440px', margin: '35px 0 56px 0' }}>
          <Steps
            defaultCurrent={1}
            layout="horizontal"
            separator="line"
            sequence="positive"
            theme="default"
            readonly="true"
          >
            <StepItem
              title="创建充值订单"
            />
            <StepItem
              title="充值订单信息"
            />
          </Steps>
        </div>

        <div style={{ marginBottom: 45 }}>
          <img src={"/api/user/qr?id=" + this.state.address} style={{ width: 180, height: 180 }} />
        </div>

        <div style={{ display: 'flex', marginBottom: '41px', color: '#000', fontSize: '16px', lineHeight: '24px' }}>
          <div>充值类型：</div>
          <div> <span>USDT_TRC20</span></div>
        </div>

        <div style={{ display: 'flex', marginBottom: '41px', color: '#000', fontSize: '16px', lineHeight: '24px' }}>
          <div>钱包地址：</div>
          <div>
            <span style={{marginRight: 10}}>{this.state.address}</span>
            <Icon type="copy" style={{ color: '#7a7d7f' }} onClick={e => {
              console.log(this.state.address)
              if (copy(this.state.address)) {
                message.success('复制成功')
              } else {
                message.error('复制失败')
              }
            }
            } />
          </div>
        </div>

        <div style={{ display: 'flex', marginBottom: '41px', color: '#000', fontSize: '16px', lineHeight: '24px' }}>
          <div>交易金额：</div>
          <div style={{ fontSize: '32px', fontStyle: 'normal', fontWeight: '600' }}>${this.state.amount}<span style={{ fontSize: '16px', fontWeight: '400', marginRight: 10 }}>USDT</span>
            <Icon type="copy" style={{ color: '#7a7d7f', fontSize: '16px', fontStyle: 'normal', fontWeight: '400', lineHeight: '24px' }} onClick={e => {
              console.log(this.state.amount)
              if (copy(this.state.amount)) {
                message.success('复制成功')
              } else {
                message.error('复制失败')
              }
            }
            } />

          </div>
        </div>

        <div style={{ display: 'flex', marginBottom: '24px',color: '#000',fontSize: '16px' }}>
          <div>说明：</div>

          <div style={{ marginBottom: '24px' }}>
            <div style={{ marginBottom: '18px' }}>
              1.到账金额【须准确为 <span style={{ color: '#D54941' }}>{this.state.amount}</span> USDT】，否则充值不成功。
            </div>

            <div style={{ marginBottom: '18px' }}>
              2.转账时请再次确认地址是否跟页面一致。
            </div>
            <div style={{ marginBottom: '18px' }}>
              3.请在<span style={{ fontWeight: '600' }}> 30分钟 </span>内完成充值。
            </div>
            <div style={{ marginBottom: '18px' }}>
              4.交易成功后会自动充值到账。
            </div>

            <div style={{ marginBottom: '18px' }}>
              5.如因转账金额错误或超时导致充值失败，请联系客服处理。
            </div>
          </div>
        </div>

        <div><div className="search-query-btn" onClick={this.returnBefore}>返回上一步</div></div>

      </div> : ""}
    </div>)
  }
}

export default connect(({ user }) => ({
  userID: user.info.userID
}))(injectIntl(MyComponent))
