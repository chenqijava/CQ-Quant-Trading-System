import React, {Component} from 'react'
import {
  Icon,
  Upload,
  message,
  Button,
  Modal,
} from 'antd'
import axios from 'axios'
import EditButton from './EditButton'
import NewForm from "components/common/newForm";

const confirm = Modal.confirm;

// 针对当前页面的基础url
const consumer = '/api/consumer/accountGroup';

class MyComponent extends Component {
  constructor(props) {
    super(props);
    this.state = {
    };
  }

  async componentWillMount() {
  }

  reload = () => this.props.reload();

  setData = async (filters) => {
    let data = await new Promise((resolve, reject) => {
      this.formProps.form.validateFields(async (err, form) => {
        if (err) reject(err)
        else resolve(form)
      })
    });
    await this.setState({loading: true});
    data = {
      filters,
      data,
    };
    let result = {data: {}};
    try {
      result = await axios.post(`${consumer}/batchSet`, data);
    } catch (e) {
      console.log(e);
      throw e
    } finally {
      let state = {loading: false};
      if (result.data.code) {
        message.success('操作成功');
      } else {
        message.error(`操作失败,${result.data.message || ''}`);
      }
      await this.setState(state);
    }
    this.reload()
  };

  // 把columns放到render中，虽然损失部分性能，但是能方便参数中的匿名回调获取实例状态
  render() {
    let loading = this.props.loading || this.state.loading;

    const formItemLayout = {
      colon: false,
      labelCol: {
        span: 6
      }, wrapperCol: {
        span: 18
      }
    };

    const fieldOpt = {
      rules: [
        {
          required: true,
          message: '不能为空'
        },
      ]
    };

    const formItemss = [{
      key: 'unbindLock',
      form: {
        ...formItemLayout,
        label: '锁定'
      },
      fieldOpt: {
        initialValue: this.props.unbindLock || false
      },
      itemType: 'switch',
    }];

    return (
      <EditButton {...this.props} label={this.props.children || '设置分组解绑锁'}
                  modalUnit={"分组"}
                  data={this.props.data}
                  onOk={this.setData.bind(this)} loading={loading}>
        <div className="clearfix">  {/*Modal 内的内容*/}
          <NewForm {...this.props} formItemLayout={{labelCol: {span: 5}}} {...{formItemss}} query={{oper: 'form'}} initForm={(formProps) => this.formProps = formProps}/>
        </div>
      </EditButton>
    )
  }
}

export default MyComponent
