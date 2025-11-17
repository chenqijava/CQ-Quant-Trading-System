import React, {Component} from 'react'
import {BrowserRouter as Router, Route, Link} from 'react-router-dom'
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
  Spin
} from 'antd'
import {connect} from 'dva';
import {Redirect} from 'dva/router'
import Head from "../head";
import BackgroundImage from "../images/slices/login-bg.jpg";
import CenterLogo from "../images/slices/Logo-white@2x.png";
import Style from "./login.css";
import ChangeLocale from '../nb-intl/ChangeLocale'
import { Input as TInput, InputAdornment,Button, loading } from 'tdesign-react'
import {UserIcon, LockOnIcon} from 'tdesign-icons-react'
import {FormattedMessage, useIntl, injectIntl} from 'react-intl'

const {Meta} = Card
const {Item: FormItem} = Form
const {TextArea} = Input

const pageStyle = {
   width: "50%",
  // height: "100%",
  minHeight: 850,
  background: '#EDEEF0',
  height: '100vh',
  backgroundImage: `url(/NBG.jpg)`,
  backgroundRepeat: "no-repeat",
  backgroundSize: '100%',
  backgroundPosition: 'center center',
}

class MyForm extends Component {
  constructor(props) {
    super(props);
    this.state = {
      username: '', 
      password: '',
      loading: false,
      showPassword: false,
      isFirst: true,
    }
  }

  async componentWillMount() {
    if (!this.props.user.checked) {
      this.props.dispatch({type: 'user/stat'});
    }
  }

  async _handleSubmit() {
    if (this.state.loading) return;
    try {
      console.log("加载")
      this.setState({loading: true,isFirst: true})
      await this.props.dispatch({type: 'user/login', form: {userID: this.state.username, password: this.state.password}})
      this.setState({loading: false, isFirst: false,})
    } finally {
      this.setState({loading: false}) 
    }
  }

  render() {
    // 如果已经登录状态的话，就跳转
    if (this.props.user.info) {
      if (this.props.user.info.userID == "admin")
        return <Redirect exact from="/" to="/admin/"/>;
      else
        return <Redirect exact from="/" to="/cloud/"/>
    } else {
      if (!this.state.isFirst) {
        // message.error('账号密码错误！')
        this.state.isFirst = true
      }
    }
    return (
      <div style={{marginTop:200,marginLeft:'25%'}}>
        <div style={{fontFamily: 'PingFang SC',fontStyle: 'normal',fontWeight: 600,lineHeight: '44px',fontSize: '36px',color: 'rgba(0, 0, 0, 0.90)', marginTop: 19}}><FormattedMessage id='欢迎使用' ></FormattedMessage></div>
        <div style={{marginTop: 16, fontSize: '16px', lineHeight: '24px',color: '#86909C'}}><FormattedMessage id='请使用您的账号密码登录系统' ></FormattedMessage></div>
        <div style={{marginTop: 30,width: 400}} className='loginUsername'>
        <UserIcon style={{width: 18,height:18, margin:'14px 3px 14px 12px',}} />
        <input placeholder={this.props.intl.formatMessage({id: "请输入用户名"})}
            value={this.state.username}
            onChange={(value) => {
              this.setState({username: value.target.value})
            }}></input>
        </div>
        <div style={{marginTop: 19,width: 400}} className='loginUsername'>
          <LockOnIcon style={{width: 18,height:18, margin:'14px 3px 14px 12px',}} />
          <input type="password" onKeyDown={(e) => {
            if (e.key === 'Enter') {
              this._handleSubmit() 
            }
          }} placeholder={this.props.intl.formatMessage({id: "请输入密码"})}
              value={this.state.password}
              onChange={(value) => {
                this.setState({password: value.target.value})
              }}></input>
        </div>
        <div style={{marginTop: 50}}>
        <Button
          shape="rectangle"
          size="medium"
          type="button"
          variant="base"
          style={{width: 400, height: 60, fontSize: 18, fontWeight: 600, color: 'rgba(255, 255, 255, 0.90)'}}
          loading={this.state.loading}
          onClick={() => this._handleSubmit()}
        >
          <FormattedMessage id='登录' ></FormattedMessage>
        </Button>
        </div>
      </div>  
    )
  }
}

const NewForm = Form.create()(connect(({user, loading}) => ({user, loading: loading.models.user}))(injectIntl(MyForm)))

class MyComponent extends Component {
  render() {
    return (
      <div style={{
        display: "flex",width: '100%'
      }}>
      <div style={pageStyle}> 
      <div > </div>
      
      <div style={{display:'flex',width: 615,justifyContent: 'space-between'}}>

      </div>
    </div>
          <div style={{
            width: '50%',
            height: '100%',
            marginLeft: 50,
            backgroundColor: 'white',
          }}>
            <div style={{display: "inline-flex"}}>
              <div className={Style.logo2} style={{marginTop: 30,marginLeft:16}}></div> 
              <div style={{marginTop: 40,marginLeft: 10}}><div style={{color:'#000',fontFamily: 'PingFang SC',fontSize: '28px',fontWeighttyle: 'normal',fontWeight: '600'}}>Quant Trading</div>
              <ChangeLocale/>
              </div>
              </div>
            <Head/>
            <NewForm/>
          </div>
      </div>
  )
  }
}

export default injectIntl(MyComponent)
