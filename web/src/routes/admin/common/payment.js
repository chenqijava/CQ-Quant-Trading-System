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
import {connect} from 'dva'
import {injectIntl} from 'react-intl'
import {formatDate} from 'components/DateFormat'
import expenseType from 'components/expenseType'
import { Pagination, Breadcrumb, Dialog, Steps ,Space, Input as TInput, Upload as TUpload, Tooltip as TTooltip, Button as TButton} from 'tdesign-react';
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

class MyComponent extends Component {
  constructor(props) {
    super(props);
    // data由服务器返回表格的数据
    // loading由客户端控制
    // pagination控制表格分页功能，其中数据总数pagination.total由服务器返回
    // 以上变量受控，因为需要传递给Table控件
    this.state = {
      amount:0,
      address:"",
      arCode:"",
      paystep:1,
      billTypes: {},
      deadTime:30
    }



  }

  returnBefore = async()=> {
    this.setState({
      paystep:1,
      amount:""
    });
  }

  submitData = async()=> {
    let data = {
      amount:this.state.amount
    }
    if(this.state.amount==""){
      message.error('请填写充值金额');
      return;
    }
    if(this.state.amount > 9999999999999999999){
      message.error('金额超过限制');
      return;
    }
    if(this.state.amount < 0){
      message.error('金额必须大于0');
      return;
    }
     if(!/^[+-]?\d+$/.test(this.state.amount)){
      message.error('金额必须为整数');
      return;
     }

    let res = await axios.post(`${baseUrl}/create/order`,data)
    if (res.data.code == 1) {
      message.success('操作成功');
      this.setState({
        address:res.data.data.address,
        arCode:res.data.data.qrCode,
        amount:res.data.data.amount,
        deadTime:res.data.data.deadTime,
        paystep:2
      });
    } else {
      Modal.error({title: res.data.message});
    }
  }

  render() {
 
    return (<div>
      <Breadcrumb>
        <BreadcrumbItem>财务管理</BreadcrumbItem>
        <BreadcrumbItem>在线充值</BreadcrumbItem>
      </Breadcrumb>
      {this.state.paystep==1?
        <div style={{display: 'flex',flexDirection: 'column',alignItems: 'center',width: '439px',height:'276px',margin: '91px auto',justifyContent: 'center'}}>
          
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


          <div style={{height:'60px',display: 'flex',marginBottom: '48px'}}>
          <div style={{fontFamily: 'PingFang SC',fontSize: '16px',fontStyle: 'normal',fontWeight: '600',lineHeight: '24px',display: 'flex'}}>

          </div>
          <div style={{display: 'flex',padding: '0 16px',alignItems: 'center',gap: '16px',flex: '1 0 0',height: '24px'}}>
           
          </div>

          <div style={{color: 'var(--text-icon-font-gy-340-placeholder, rgba(0, 0, 0, 0.40))',fontFamily: 'PingFang SC',fontSize: '16px',
            fontStyle: 'normal',fontWeight: '400',lineHeight: '24px',}}> 
            
            </div>
          </div>

          <div style={{display: 'flex',marginBottom: '24px',width: '439px'}}> 
            <div style={{color: '#000',fontFamily: 'PingFang SC',fontSize: '16px',fontStyle: 'normal',fontWeight: '400',lineHeight: '24px',margin:'auto 18px'}}>充值类型：</div>
            <div> <button class="css-1p3hq3p ant-btn ant-btn-primary ant-btn-background-ghost" type="button" style={{color: 'var(--Brand-Brand7-Normal, #0052D9)',borderColor: 'var(--Brand-Brand7-Normal, #0052D9)'}}><span>USDT_TRC20</span></button></div>
          </div>


          <div style={{display: 'flex',marginBottom: '48px',width: '439px'}}>
          <div style={{color: '#000',fontFamily: 'PingFang SC',fontSize: '16px',fontStyle: 'normal',fontWeight: '400',lineHeight: '24px',margin:'auto 18px'}}>充值金额：</div>
          <div class="ant-space-item">
            <span class="ant-input-group-wrapper ant-input-search ant-input-search-large ant-input-search-with-button ant-input-group-wrapper-lg css-1p3hq3p">
              <span class="ant-input-wrapper ant-input-group css-1p3hq3p">
                <input type="text" class="ant-input " placeholder="请输入金额" onChange={(e) =>{this.setState({amount: e.target.value})}}/>
                <span class="ant-input-group-addon">USDT</span>
              </span>
            </span>
            </div>
          </div>

          <div><Button className="search-query-btn" onClick={this.submitData}>提交</Button></div>

        </div>:""}

        {this.state.paystep==2? 
        <div>
            <div style={{display: 'flex',width: '485px',height:'60px',margin: '91px auto 10px'}}>

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
            <div style={{height:'60px',display: 'flex',marginBottom: '48px'}}>
              <div style={{fontFamily: 'PingFang SC',fontSize: '16px',fontStyle: 'normal',fontWeight: '400',lineHeight: '24px',display: 'flex'}}>
                </div>
              <div style={{display: 'flex',padding: '0 16px',alignItems: 'center',gap: '16px',flex: '1 0 0',height: '24px'}}>
              </div>
                <div style={{color: 'var(--Brand-Brand7-Normal, #0052D9)',fontFamily: 'PingFang SC',fontSize: '16px',
                fontStyle: 'normal',fontWeight: '400',lineHeight: '24px',}}>
                </div>
            </div>

            </div>

            <div style={{display: 'flex',width: '485px',margin: '0 auto'}}>
                <img src={this.state.arCode} style={{width: 160, height: 160,cursor: 'pointer'}}/>
            </div>

            <div style={{display: 'flex',flexDirection: 'column',alignItems: 'center',width: '485px',height:'387px',margin: '91px auto',justifyContent: 'center'}}>
            
            <div style={{display: 'flex',marginBottom: '41px',width: '485px'}}> 
              <div style={{color: '#000',fontFamily: 'PingFang SC',fontSize: '16px',fontStyle: 'normal',fontWeight: '400',lineHeight: '24px',}}>充值类型：</div>
              <div style={{color: '#000',fontFamily: 'PingFang SC',fontSize: '16px',fontStyle: 'normal',fontWeight: '400',lineHeight: '24px'}}> <span>USDT_TRC20</span></div>
            </div>

            <div style={{display: 'flex',marginBottom: '41px',width: '485px'}}> 
              <div style={{color: '#000',fontFamily: 'PingFang SC',fontSize: '16px',fontStyle: 'normal',fontWeight: '400',lineHeight: '24px',}}>钱包地址：</div>
              <div style={{color: '#000',fontFamily: 'PingFang SC',fontSize: '16px',fontStyle: 'normal',fontWeight: '400',lineHeight: '24px'}}> 
              <span>{this.state.address}    &nbsp;&nbsp; </span>  
              <Icon type="copy" style={{color: '#7a7d7f'}} onClick={e => {
                    console.log(this.state.address)
                    if (copy(this.state.address)) {
                      message.success('复制成功')
                    } else {
                      message.error('复制失败')
                    }
                  }
                  }/>
              </div>
            </div>

            <div style={{display: 'flex',marginBottom: '41px',width: '485px'}}> 
              <div style={{color: '#000',fontFamily: 'PingFang SC',fontSize: '16px',fontStyle: 'normal',fontWeight: '400',lineHeight: '24px',}}>交易金额：</div>
              <div style={{color: '#000',fontFamily: 'PingFang SC',fontSize: '32px',fontStyle: 'normal',fontWeight: '600',lineHeight: '24px'}}> $ {this.state.amount}<span style={{fontSize: '16px',fontWeight: '400'}}>  USDT      &nbsp;&nbsp; &nbsp;&nbsp;   </span>

              <Icon type="copy" style={{color: '#7a7d7f',fontFamily: 'PingFang SC',fontSize: '16px',fontStyle: 'normal',fontWeight: '400',lineHeight: '24px'}} onClick={e => {
                    console.log(this.state.amount)
                    if (copy(this.state.amount)) {
                      message.success('复制成功')
                    } else {
                      message.error('复制失败')
                    }
                  }
                  }/>
              
              </div>
            </div>

            <div style={{display: 'flex',marginBottom: '24px',width: '485px'}}> 
              <div style={{color: '#000',fontFamily: 'PingFang SC',fontSize: '16px',fontStyle: 'normal',fontWeight: '400',lineHeight: '24px'}}>说明：</div>
            
              <div style={{marginBottom: '24px',width: '437px'}}>
                <div style={{color: '#000',fontFamily: 'PingFang SC',fontSize: '16px',fontStyle: 'normal',fontWeight: '400',lineHeight: '24px',marginBottom: '18px'}}> 
                  1.到账金额【须准确为 <span style={{color: '#D54941'}}>{this.state.amount}</span> USDT】，否则充值不成功。
                </div>

                <div style={{color: '#000',fontFamily: 'PingFang SC',fontSize: '16px',fontStyle: 'normal',fontWeight: '400',lineHeight: '24px',marginBottom: '18px'}}> 
                  2.转账时请再次确认地址是否跟页面一致。
                </div>
                <div style={{color: '#000',fontFamily: 'PingFang SC',fontSize: '16px',fontStyle: 'normal',fontWeight: '400',lineHeight: '24px',marginBottom: '18px'}}> 
                  3.请在<span style={{fontWeight: '600'}}> 30分钟 </span>内完成充值。
                </div>
                <div style={{color: '#000',fontFamily: 'PingFang SC',fontSize: '16px',fontStyle: 'normal',fontWeight: '400',lineHeight: '24px',marginBottom: '18px'}}> 
                  4.交易成功后会自动充值到账。
                </div>

                <div style={{color: '#000',fontFamily: 'PingFang SC',fontSize: '16px',fontStyle: 'normal',fontWeight: '400',lineHeight: '24px',marginBottom: '18px'}}> 
                  5.如因转账金额错误或超时导致充值失败，请联系客服处理。
                </div>
              </div>
            </div>

            <div><Button className="search-query-btn" onClick={this.returnBefore}>返回上一步</Button></div>
          </div>
          </div>:""
        }
    </div>)
  }
}

export default connect(({user}) => ({
    userID: user.info.userID
}))(injectIntl(MyComponent))
