import React, {Component} from 'react'
import {BrowserRouter as Router, Route, Link} from 'react-router-dom'
import {
  Icon,
  Table,
  Divider,
  Upload,
  message,
  Switch,
  Card,
  Button,
  Input,
  Form,
  Spin
} from 'antd'
import axios from 'axios'
import Api from '../lib/Api'

import TimerWrapper from '../lib/TimerWrapper'

const Search = Input.Search

const {Meta} = Card
const {Item: FormItem} = Form
const {TextArea} = Input

class MyForm extends Component {
  async _handleSubmit(e) {
    e.preventDefault()
    this.props.form.validateFields((err, values) => {
      if (!err) {
        this.props.onSubmit(values)
      }
    })
  }

  render() {
    const {getFieldDecorator} = this.props.form
    return (<Form onSubmit={this._handleSubmit.bind(this)}>
      <FormItem>
        {
          getFieldDecorator('password', {
            rules: [
              {
                required: true,
                message: '密码不能为空'
              }
            ]
          })(<Input prefix={<Icon type="lock" style={{ color: 'rgba(0,0,0,.25)' }} />} type="password" placeholder="密码" />)
        }
      </FormItem>
      <Button style={{width:'100%'}} disabled={this.props.disabled} type="primary" htmlType="submit">登录</Button>
    </Form>)
  }
}

const NewForm = Form.create({
  mapPropsToFields(props) {
    let fields = {}
    for (let key in props.formState) {
      fields[key] = Form.createFormField({value: props.formState[key]})
    }
    return fields
  },
  onFieldsChange(props, fields) {
    props.onFieldsChange && props.onFieldsChange(fields)
  }
})(MyForm)

class MyComponent extends Component {
  constructor(props) {
    super(props);
    this.state = {
      loading: false,
      form: {}
    }
  }

  async onFieldsChange(fields) {
    let form = this.state.form
    for (let key in fields) {
      form[key] = fields[key].value
    }
    this.setState({form})
  }

  async _handleSubmit(form) {
    this.setState({loading: true})
    let res = await axios.post(Api.user.login, form)
    if (res.data.code == 1) {
      message.success('登录成功')
      this.props.timer.setTimeout(() => {
        window.location.href = '/'
      }, 1000)
    } else {
      this.setState({loading: false})
      message.error('登录失败')
    }
  }

  render() {
    return (
      <div style={{margin: '0 auto',marginTop: 100,width: 300}}>
        <Card title='登录'>
          <NewForm disabled={this.state.loading} formState={this.state.form} onFieldsChange={this.onFieldsChange.bind(this)} onSubmit={this._handleSubmit.bind(this)}/>
        </Card>
      </div>
    )
  }
}

export default TimerWrapper(MyComponent)
