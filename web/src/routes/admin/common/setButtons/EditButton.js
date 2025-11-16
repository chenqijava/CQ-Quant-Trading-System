import React, {Component} from 'react'
import {BrowserRouter as Router, Route, Link} from 'react-router-dom'
import {
  Icon,
  Table,
  Divider,
  Upload,
  message,
  Input,
  InputNumber,
  Button,
  Modal,
  Cascader,
  Select,
  Tabs,
  Radio,
  Avatar,
  Tooltip
} from 'antd'
import axios from 'axios'
import accountStatus from 'components/accountStatus'
import {formatDate} from 'components/DateFormat'
import WarningDialog from '../dialog/warningDialog'
import { Dialog } from 'tdesign-react';

const confirm = Modal.confirm;

// 针对当前页面的基础url
const baseUrl = '/api/account';
const consumer = '/api/consumer/account';

class MyComponent extends Component {
  constructor(props) {
    super(props);
    this.state = {
      editVisible: false,
      showConfirm: false,
      confirmTitle: '',
      confirmContent: '',
      step: 0,
    };
  }

  async componentWillMount() {
  }

  async confirm(option) {
    return new Promise((resolve, reject) => {
      Object.keys(option).forEach(key => {
        if (typeof option[key] === 'function') {
          let fun = option[key];
          option[key] = () => {
            fun(resolve, reject)
          }
        }
      })
      confirm(option)
    })
  }

  async checkFilters2 () {
    if (typeof this.props.checkFilters == 'function') return this.props.checkFilters();
    if (this.props.data && this.props.data.selectedRowKeys.length == 0) {
      if ( this.props.bannedEmptySelectedRowKeys ) {
        message.error(`请选择数据`);
        return false;
      }
      if (Object.keys(this.props.data.filters).length === 0 && this.state.step == 0) {
        console.log(this.props.label.props)
        this.setState({showConfirm: true, confirmTitle: this.props.confirmTitle || `确定要修改所有账号的${typeof this.props.label == 'string' ? this.props.label : this.props.label.length > 0 ? this.props.label[0].props.children : this.props.label.props.children}`, confirmContent: 
        <h3 style={{color: 'red'}}>
          {this.props.confirmContent || ""}
          <br/>
          没有筛选条件,确定要操作所有数据吗?
        </h3>, step: 1})
      } else {
        this.setState({showConfirm: true, confirmTitle: this.props.confirmTitle || `确定要修改过滤条件下所有账号的${typeof this.props.label == 'string' ? this.props.label : this.props.label.length > 0 ? this.props.label[0].props.children : this.props.label.props.children}`, confirmContent: this.props.confirmContent || '', step: 2})
      }
      return false
    }
      
    return true
  }

  async checkFilters() {
    if (typeof this.props.checkFilters == 'function') return this.props.checkFilters();
    if (this.props.data && this.props.data.selectedRowKeys.length == 0) {
      if ( this.props.bannedEmptySelectedRowKeys ) {
        message.error(`请选择数据`);
        return false;
      }
      if (Object.keys(this.props.data.filters).length === 0) {
        let res = await this.confirm({
          title: this.props.confirmTitle || `确定要修改所有账号的${this.props.label}`,
          content: <h3 style={{color: 'red'}}>
            {this.props.confirmContent || ""}
            <br/>
            没有筛选条件,确定要操作所有数据吗?
          </h3>,
          okText: '确定',
          okType: 'danger',
          cancelText: '取消',
          onOk: async (resolve) => {
            return resolve(true);
          },
          onCancel(resolve) {
            return resolve(false);
          }
        })
        if (!res) return res
      }
      return await this.confirm({
        title: this.props.confirmTitle || `确定要修改过滤条件下所有账号的${this.props.label}`,
        content: this.props.confirmContent || '',
        okText: '确定',
        okType: 'danger',
        cancelText: '取消',
        onOk: async (resolve) => {
          return resolve(true);
        },
        onCancel(resolve) {
          return resolve(false);
        }
      })
    } else {
      return true;
    }
  }

  showModal = async () => {
    if (typeof this.props.showModal == 'function')
      if (await this.props.showModal() == false) return;
    this.setState({editVisible: true});
  };

  async setData() {
    let filters = this.props.data && this.props.data.filters;
      if (this.props.data && this.props.data.selectedRowKeys && this.props.data.selectedRowKeys.length !== 0) {
      //目标类型是群组ID获取账号
      if (this.props.converter === "group2Account") {
        filters = {
          groupID: {$in: this.props.data.selectedRowKeys}
        }
      } else {
        filters = {
          _id: {$in: this.props.data.selectedRowKeys}
        }
      }
    }
    try {
      if (await this.props.onOk(filters) == false) return;
      await this.onCancel();
    } catch (e) {
      console.log(e)
    }
  }

  async onCancel() {
    if (typeof this.props.onCancel == 'function')
      if (await this.props.onCancel() == false) return;
    await this.setState({editVisible: false});
  }

  render() {
    let loading = this.props.loading || this.state.loading;

    return ([
      this.props.className ? 
      <div {...this.props} onClick={async () => {
        if (await this.checkFilters2()) {
          if (!this.props.children) {
            this.setData();
          } else {
            this.showModal();
          }
        }
      }}>{this.props.label}</div> : 
      <Button {...this.props} onClick={async () => {
        if (await this.checkFilters2()) {
          if (!this.props.children) {
            this.setData();
          } else {
            this.showModal();
          }
        }
      }} loading={loading}>
        {this.props.label}
      </Button>,
        <Dialog
            header={<dev>{this.props.label} ({this.props.data && this.props.data.selectedRowKeys && this.props.data.selectedRowKeys.length ? `${this.props.data.selectedRowKeys.length}个` : '所有'}{this.props.modalUnit || '账号'})</dev>}
            visible={this.state.editVisible}
            onConfirm={this.setData.bind(this)} confirmLoading={loading}
            onClose={()=>{this.onCancel();}}
            onCancel={() => {
              this.onCancel();
            }}
        >{this.props.children}</Dialog>,
        <WarningDialog title={this.state.confirmTitle}
        visible={this.state.showConfirm}
        onOkTxt={'确定'}
        onCancelTxt={'取消'}
        onOk={() => {
          if (this.state.step == 2) {
            this.setState({showConfirm: false,step: 0})
            if (!this.props.children) {
              this.setData();
            } else {
              this.showModal();
            }
          } else {
            this.checkFilters2()
          }
        }}
        onCancel={() => {
          this.setState({showConfirm: false, step: 0})
        }}>
          {this.state.confirmContent}
        </WarningDialog>
    ])
  }
}

export default MyComponent
