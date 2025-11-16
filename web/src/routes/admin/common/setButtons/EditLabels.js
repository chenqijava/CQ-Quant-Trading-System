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
import {FormattedMessage, useIntl, injectIntl} from 'react-intl'

const confirm = Modal.confirm;

// 针对当前页面的基础url
const consumer = '/api/consumer/account';

class MyComponent extends Component {
  constructor(props) {
    super(props);
    this.state = {
      labels: [],

      needHiddenItems: {},    //  用来检查哪些字段需要隐藏,当对应的formItem修改时需要修改该处的值
    };
  }

  async componentWillMount() {
    this.searchAllLabel({});
  }

  async searchAllLabel(sorter) {
    // this.setState({labelLoading: true});
    // let res = await axios.post(`/api/consumer/friendLabel/10000/1`, {filters: {}, sorter});
    // let map = {};
    // for (const label of res.data.data) {
    //   map[label._id] = label.label;
    // }
    // this.setState({labelLoading: false, labels: res.data.data, labelTotal: res.data.total, labelMap: map});
  }

  reload = () => this.props.reload();

  setData = async(filters) => {
    let data = await new Promise((resolve, reject) => {
      this.formProps.form.validateFields(async (err, form) => {
        if (err) reject(err);
        resolve(form)
      })
    });
    data.labels = [data.labels];
    this.setState({loading: true});
    let result = {data: {}};
    try {
      console.log("filters:",filters)
      result = await axios.post(`${consumer}/batchSet`, {
        data,
        filters,
      });
    } catch (e) {
      console.log(e)
      throw e
    } finally {
      let state = {
        loading: false,
      };
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

    const fieldOpt = {
      rules: [
        {
          required: true,
          message: '不能为空'
        },
      ]
    };

    const formItemLayout = {
      colon: false,
      labelCol: {
        span: 0
      }, wrapperCol: {
        span: 24
      }
    };

    const formItemss = [{
      key: 'labels',
      form: {
        ...formItemLayout,
      },
      itemType: 'Select',
      options: this.state.labels.map(item => ({label: item.label, value: item._id})),
      select: {
        showSearch: true,
        allowClear: true,
        // mode: 'multiple',
        filterOption: (input, option) => option.props.children.toLowerCase().indexOf(input.toLowerCase()) >= 0,
      },
    }];

    return (
        <EditButton label={this.props.children || '设置标签'}
                    data={this.props.data}
                    onOk={this.setData.bind(this)} loading={loading}
                    {...this.props}
        >

          <div className="clearfix">
            <NewForm {...this.props} formItemLayout={{labelCol: {span: 5}}} needHiddenItems={this.state.needHiddenItems} {...{formItemss}} query={{oper: 'form'}} initForm={(formProps) => this.formProps = formProps}/>
          </div>
        </EditButton>
    )
  }
}

export default injectIntl(MyComponent)
