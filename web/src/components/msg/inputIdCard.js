import React, {Component} from 'react'
import {
  Icon,
  message,
  Upload,
  Alert,
  Input,
  Button,
} from 'antd'
import axios from 'axios'
//import FriendSelect from "../cs/chat/friendSelect";

const consumer = '/api/consumer/friend';

class InputVoiceMsg extends Component {

  constructor(props) {
    super(props);
    this.state = {
      friends: [],
    }
  }

  async componentWillMount() {
  }

  loadSelectAccountFriends = async () => {
    if (this.props.data.account) {
      this.setState({loading: true});
      let res = await axios.post(`${consumer}/simpleList/10000/1`, {
        filters: {ownerAccID: this.props.data.account.ownerAccID},
      });
      this.setState({
        loading: false,
        friends: res.data.data,
      });
    } else {
      message.success('任务创建成功');
    }
  };

  render() {
    return (
      <div className="clearfix">
        <Alert message={'要发送好友名片，发送账号必须与所选名片账号是好友，请提前将被发送账号添加成好友'}
               description={'加载选中好友所属账号的好友时随机加载一个账号的所有好友'}
               type="info"
        />
        <Button type="primary" onClick={this.loadSelectAccountFriends.bind(this)}>加载选中好友所属账号的好友</Button>
      </div>
    )
  }
}

export default InputVoiceMsg;
