import React, {Component} from 'react'
import {
  Icon,
  Upload,
  message,
  Modal,
  Radio,
  Input,
  Select,
} from 'antd'
import axios from 'axios'
import EditButton from './EditButton'

const confirm = Modal.confirm;

// 针对当前页面的基础url
const consumer = '/api/consumer/accountGroup';

class MyComponent extends Component {
  constructor(props) {
    super(props);
    this.state = {
      socks5Areas: [],
      selectedIds: [],
    };
    this.countMap = {}
  }

  async componentWillMount() {
    this.loadSocks5Areas();
  }

  async loadSocks5Areas() {
    let res = await axios.post(`/api/consumer/socks5/getAreas`);
    this.countMap = res.data.countMap;
    this.setState({socks5Areas: res.data.data});
  }

  reload = () => this.props.reload();

  setData = async (filters) => {
    await this.setState({loading: true});
    let data = {
      filters,
      socks5Areas: this.state.selectedIds,
    };
    let result = {data: {}};
    try {
      result = await axios.post(`${consumer}/setSocks5Areas`, data);
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

    let options = this.state.socks5Areas || [];

    return (
      <EditButton {...this.props} label={this.props.children || '设置分组socks使用的国家'}
                  onOk={this.setData.bind(this)} loading={loading}>
        <div className="clearfix">  {/*Modal 内的内容*/}
          <span>请选择一个资源：</span>
          <Select
            showSearch
            mode={'multiple'}
            placeholder="输入关键字搜索"
            style={{width: "100%", marginTop: "15px"}}
            onChange={(value) => this.setState({selectedIds: value})}
            filterOption={(input, option) => {
              return option.props.children[0].toLowerCase().indexOf(input.toLowerCase()) >= 0
            }}
          >
            {options.map(res => {
              let label = [res.countryName];
              if (res.regionName && res.regionName != res.countryName) label.push(res.regionName);
              if (res.cityName && res.cityName != res.countryName) label.push(res.cityName);
              return <Select.Option key={res._id} value={res._id}>
                {`${label.join('-')}(${res.abbr})`}{this.countMap[res._id] ? `(socks5数量：${this.countMap[res._id]})` : ''}
              </Select.Option>
            })}
          </Select>
        </div>
      </EditButton>
    )
  }
}

export default MyComponent
