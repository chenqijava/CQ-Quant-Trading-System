import React, {Component} from 'react'
import {
  Icon,
  Upload,
  message,
  Modal,
  Radio,
  Input,
  Switch
} from 'antd'
import axios from 'axios'
import EditButton from './EditButton'
import InputImageMsg from "components/msg/inputImageMsg";

const confirm = Modal.confirm;

// 针对当前页面的基础url
const baseUrl = '/api/account';
const consumer = '/api/consumer/account';

/**
 * 账号清理聊天记录
 */
class MyComponent extends Component {
  constructor(props) {
    super(props);
    this.state = {
      deleteAll:true,
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
      deleteAll: this.state.deleteAll,
    };
    this.setState({loading: true});
    let res = await axios.post(`${baseUrl}/clearChats`, data);
    this.setState({loading: false});
    if(res.data.code == 1){
      message.success('操作成功');
      this.reload()
    }else {
      message.error(res.data.message)
    }
    this.reload()
  };

  // 把columns放到render中，虽然损失部分性能，但是能方便参数中的匿名回调获取实例状态
  render() {
    let loading = this.props.loading || this.state.loading;

    return (
        <EditButton label={this.props.children || ''}
                    data={this.props.data}
                    confirmTitle={'确定要让过滤条件下所有在线账号删除聊天记录'}
                    onOk={this.setData.bind(this)} loading={loading}
                    {...this.props}
        >
          <div className="clearfix">  {/*Modal 内的内容*/}
            是否同时清除对方记录:
            <Switch checkedChildren="是" unCheckedChildren="否" defaultChecked  onChange={(val)=>{
              this.setState(
                {deleteAll: val}
              )
              // console.log(data)
            }
            }/>
          </div>
        </EditButton>
    )
  }
}

export default MyComponent
