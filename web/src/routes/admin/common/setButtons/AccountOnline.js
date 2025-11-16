import React, {Component} from 'react'
import {
  Icon,
  Upload,
  message,
  Modal,
  Radio,
  Input,
  DatePicker,
} from 'antd'
import axios from 'axios'
import EditButton from './EditButton'
import NewForm from "components/common/newForm";
import DialogApi from '../dialog/DialogApi'

const confirm = Modal.confirm;

// 针对当前页面的基础url
const baseUrl = '/api/account';
const consumer = '/api/consumer/account';

class MyComponent extends Component {
  constructor(props) {
    super(props);
    this.state = {
      showErrorDialog: false,
    };
  }

  async componentWillMount() {
  }

  reload = () => {
    this.setState({loading: false});
    this.props.reload();
  };

  setData = async (filters) => {
    // let data = await new Promise((resolve, reject) => {
    //   this.formProps.form.validateFields(async (err, form) => {
    //     if (err) reject(err);
    //     resolve(form)
    //   })
    // });

    let data = {
      filters,
      groupName: '',
      clearGroups: false,
      checkNickname: false,
      skip_history: 'discarding',
    };

    this.setState({loading: true});
    let result = await axios.post(`${this.props.baseUrl || baseUrl}/batchOnline`, data);
    if (result.data.failedType && result.data.failedType.length) {
      // Modal.error({
      //   title: '失败信息展示',
      //   content: result.data.failedType.map(type => (
      //       <div>
      //         <p>{type}：</p>
      //         <p>accID：{result.data.failed[type].join(',')}</p>
      //       </div>
      //   )),
      //   okText: "关闭",
      //   onOk: async () => {
      //     // this.reload()
      //   }
      // });
      DialogApi.error({
        title: '失败信息展示',
        content: result.data.failedType.map(type => (
            <div>
              <p>{type}：</p>
              <p>accID：{result.data.failed[type].join(',')}</p>
            </div>
        )),
        onOkTxt: "关闭",
        onOk: async () => {
        }
      })
    } else {
      message.success('操作成功');
      // this.reload()
    }
    this.reload()
  };

  // 把columns放到render中，虽然损失部分性能，但是能方便参数中的匿名回调获取实例状态
  render() {
    let loading = this.props.loading || this.state.loading;

    const formItemss = [
      {
        key: 'groupName',
        form: {
          colon: false,
          labelCol: {
            span: 3
          }, wrapperCol: {
            span: 9
          },
          label: ' ',
        },
        fieldOpt: {
          initialValue: '',
        },
        itemType: 'Input',
        input: {
          placeholder: "包含字符串",
          addonBefore:"除" ,
          suffix:"外"
        },
        item: {},
      },
      {
      key: 'clearGroups',
      form: {
        label: '退出所有群聊',
      },
      fieldOpt: {
        initialValue: false,
      },
      itemType: 'switch',
      item: {},
    },
      {
        key: 'checkNickname',
        form: {
          label: '检测中文昵称并修改',
        },
        fieldOpt: {
          initialValue: false,
        },
        itemType: 'switch',
        item: {},
      }, {
        key: 'skip_history',
        form: {
          label: '跳过历史消息',
        },
        fieldOpt: {
          initialValue: 'discarding',
        },
        itemType: 'Radio',
        options: [
          {value: 'discarding', label: '根据账号消息过滤设置'},
          {value: true, label: '跳过'},
          {value: false, label: '不跳过'},
        ],
      },
    ];
    console.log(this.props.children)
    return (
        <EditButton className={this.props.className} label={this.props.children || '上线'}
                    data={this.props.data}
                    confirmTitle={'只有连接中的账号才能执行批量上线操作,确定要让过滤条件下所有账号上线'}
                    onOk={this.setData.bind(this)} loading={loading} />
    )
  }
}

export default MyComponent
