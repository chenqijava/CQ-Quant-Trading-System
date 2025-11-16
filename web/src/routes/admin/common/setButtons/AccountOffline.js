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
import InputImageMsg from "components/msg/inputImageMsg";

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

  reload = () => {
    this.setState({loading: false});
    this.props.reload();
  };

  setData = async (filters) => {
    let data = {
      filters,
      executeTime: this.state.executeTime,
    };

    this.setState({loading: true});
    let result = await axios.post(`${baseUrl}/offline`, data);
    if (result.data.failedType && result.data.failedType.length) {
      Modal.error({
        title: '失败信息展示',
        content: result.data.failedType.map(type => (
            <div>
              <p>{type}：</p>
              <p>accID：{result.data.failed[type].join(',')}</p>
            </div>
        )),
        okText: "关闭",
        onOk: async () => {
          // this.reload()
        }
      });
    } else {
      message.success('操作成功');
      // this.reload()
    }
    this.reload()
  };

  // 把columns放到render中，虽然损失部分性能，但是能方便参数中的匿名回调获取实例状态
  render() {
    let loading = this.props.loading || this.state.loading;

    return (
        <EditButton label={this.props.children || '下线'}
                    data={this.props.data}
                    confirmTitle={'确定要让过滤条件下所有在线账号下线'}
                    onOk={this.setData.bind(this)} loading={loading} {...this.props}>
          <div className="clearfix">  {/*Modal 内的内容*/}
            选择账号下线时间(不选立即执行)<DatePicker showTime onChange={(value) => this.setState({executeTime: value})}/>
          </div>
        </EditButton>
    )
  }
}

export default MyComponent
