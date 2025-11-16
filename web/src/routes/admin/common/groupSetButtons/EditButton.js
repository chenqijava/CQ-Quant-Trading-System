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
import { Dialog } from 'tdesign-react';

const confirm = Modal.confirm;

// 针对当前页面的基础url
const baseUrl = '/api/account';
const consumer = '/api/consumer/account';

class MyComponent extends Component {
  constructor(props) {
    super(props);
    this.state = {
    };
  }

  async componentWillMount() {
  }

  showModal = async () => {
    if (this.props.data.selectedRowKeys.length == 0) {
      message.error('请先选择数据');
      return
    }
    this.setState({editVisible: true});
  };

  async setData() {
    let filters = {};
    if (this.props.data.selectedRowKeys.indexOf('allGroup') == -1) {
      filters = {
        _id: {$in: this.props.data.selectedRowKeys}
      };
    }
    await this.props.onOk(filters);
    this.setState({editVisible: false});
  }

  render() {
    let loading = this.props.loading || this.state.loading;
    return ([
      this.props.className ? 
      <div {...this.props} onClick={this.showModal.bind(this)}>{this.props.label}</div> : <Button {...this.props} onClick={this.showModal.bind(this)} loading={loading}>{this.props.label}</Button>,
      // <Button {...this.props} onClick={this.showModal.bind(this)} loading={loading}>
      //   
      // </Button>,
      <Dialog
          header={`${this.props.label}(${this.props.data.selectedRowKeys.length}个分组)`}
          visible={this.state.editVisible}
          onConfirm={this.setData.bind(this)} confirmLoading={loading}
          onClose={()=>{this.setState({editVisible: false});}}
          onCancel={() => {
            this.setState({editVisible: false});
          }}
      >{this.props.children}</Dialog>,
      // <Modal
      //   title={`${this.props.label}(${this.props.data.selectedRowKeys.length}个分组)`}
      //   visible={this.state.editVisible}
      //   onOk={this.setData.bind(this)} confirmLoading={loading}
      //   onCancel={() => {
      //     this.setState({editVisible: false});
      //   }}
      // >{this.props.children}</Modal>
    ])
  }
}

export default MyComponent
