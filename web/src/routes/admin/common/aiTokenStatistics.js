import React, { Component } from 'react'
import { BrowserRouter as Router, Route, Link } from 'react-router-dom'
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
  Switch,
  Form,
  Tabs,
  Row,
  Col,
  Radio,
  Avatar,
  Tooltip,
  Alert,
  // Checkbox,
  Collapse,
  DatePicker,
} from 'antd'
import axios from 'axios'
import { FormattedMessage, useIntl, injectIntl } from 'react-intl'
import { formatDate } from 'components/DateFormat'
import accountStatus from 'components/accountStatus'
import ExternalAccountFilter from "components/common/ExternalAccountFilter";
import MyTranslate from "components/common/MyTranslate";
import { Pagination, Breadcrumb, Dialog, Tag, Input as TInput, Upload as TUpload, Tooltip as TTooltip, DateRangePicker, Button as TButton, loading, Checkbox } from 'tdesign-react';
import { SearchIcon } from 'tdesign-icons-react'
import DialogApi from './dialog/DialogApi.js'
import { download } from "components/postDownloadUtils"
import { status } from 'nprogress'

const { RangePicker } = DatePicker;
const { Panel } = Collapse;
const { TextArea } = Input;
const { Option } = Select;
const Search = Input.Search;
const confirm = Modal.confirm;
const { TabPane } = Tabs;
const { Item: FormItem } = Form;

// import '../../../mock'

// 针对当前页面的基础url
const baseUrl = '/api/account';
const consumer = '/api/consumer/account';

const searchItems = [
  // {desc: '昵称', key: 'nickname'},
  // {desc: '用户名', key: 'username'},
  // {desc: '手机号', key: 'phone'},
  // {desc: 'accID', key: 'accID'},
  // {desc: '被封提示', key: 'errorMessage'},
  // {desc: '群ID', key: 'chatroomID'},
  // {desc: '新设备', key: 'loginedNewDevice'},
];

const nowDate = new Date().Format("yyyy-MM-dd");

const getUploadImageProps = (regExpStr = 'txt') => {
  let regExp = new RegExp(`^.+\\.(${regExpStr})$`)
  const uploadProps = {
    name: 'file',
    multiple: false,
    action: `/api/consumer/res/uploadTxt/emailCheckActive`,
    beforeUpload(file, fileList) {
      const isLt2M = file.size / 1024 / 1024 < 30;
      if (!isLt2M) {
        message.error('文件必须小于30MB!');
        return false
      }
      if (!regExp.test(file.name.toLowerCase())) {
        message.error(`文件名不合法,文件后缀为( ${regExpStr} )`)
        return false
      }
      return true
    }
  };
  return uploadProps
}

class MyComponent extends Component {
  constructor(props) {
    super(props);
    this.query = this.props.location.search.substring(1).split('&').map((v) => {
      let obj = {};
      let sp = v.split('=');
      obj[sp[0]] = sp[1];
      return obj
    }).reduce((obj, p) => {
      return {
        ...obj,
        ...p
      }
    });
    // data由服务器返回表格的数据
    // loading由客户端控制
    // pagination控制表格分页功能，其中数据总数pagination.total由服务器返回
    // 以上变量受控，因为需要传递给Table控件
    this.state = {
      data: [],
      loading: false,
      pagination: {
        pageSize: 10,
        current: 1,
        showTotal: (total, range) => `共 ${total} 条`,
        showSizeChanger: true,
        pageSizeOptions: ['10', '20', '30', '40', '50', '100', '200', '300', '400', '500']
      },
      selectedRowKeys: [],

      tableContent: null,
      mainContent: null,
      scrollY: 0,
      mainContentHeight: 0,
      searchRef: null,
      tableContentHeight: 0,

      userID: '',
      createTimeRange: ['', ''],
    },
    // 选中行的数据保存在selectedRows变量中
    // 之所以不放入state，因为这些变量是单向流动的，即Table控件产生这些数据，通过callback更新到变量this.selectedRows
    this.selectedRows = []
    // filters的写法保持与mongo filter的写法一致，避免与服务器、数据库直接的数据阻抗失衡
    // 例如： filters={username: 'foo'}
    // 因为json协议无法传输regex，所以字符类型的值，都会在服务器端转成regex，以提高匹配度，损失部分查询性能
    this.filters = {}
    // sorter的写法保持与mongo sort的写法一致，避免与服务器、数据库直接的数据阻抗失衡
    // 例如：sorter={createTime: -1}
    // 注意：Table控件仅支持单列排序，不支持多列同时排序
    this.sorter = {
      createTime: -1,
    }
    // 提前写在constructor则Table首次加载时生效
    // 但是因为没有放到state中，所以除sorter外，都不建议提前设置，保持不受控状态是最好的
  }

  // 首次加载数据
  async componentWillMount() {
    await this.reload();
  }

  refSearch = (ref) => {
    this.state.searchRef = ref
    this.setState({ searchRef: ref })
    this.handleResize()
  }

  handleResize = () => {
    let height = document.body.getBoundingClientRect().height;
    if (this.state.tableContent) {
      this.setState({ scrollY: this.state.tableContent.getBoundingClientRect().height - 80, })
    }
    if (this.state.mainContent) {
      this.setState({ mainContentHeight: this.state.mainContent.getBoundingClientRect().top })
    }
    if (this.state.searchRef) {
      setTimeout(() => {
        if (this.state.searchRef) {
          this.setState({ searchRef: this.state.searchRef })
        }
      }, 100)
    }
    if (this.state.selectedCountRef) {
      setTimeout(() => {
        if (this.state.selectedCountRef) {
          this.setState({ selectedCountRef: this.state.selectedCountRef, tableContentHeight: height - this.state.selectedCountRef.getBoundingClientRect().top - 84, scrollY: height - this.state.selectedCountRef.getBoundingClientRect().top - 84 - 80 })
        }
      }, 100)
    }
  }

  async componentDidMount() {
    window.addEventListener("resize", this.handleResize);
  }

  componentWillUnmount() {
    window.removeEventListener("resize", this.handleResize);
  }

  async reload() {
    // 重新加载，一般是页面第一次加载的时候来一下
    this.state.pagination.current = 1
    this.setState({ pagination: this.state.pagination })
    this.load(this.state.pagination, this.filters, this.sorter)
  }

  async reset() {
    this.filters = {}
    this.state.pagination.current = 1
    this.setState({ filters: {}, pagination: { ...this.state.pagination }, userID: '', createTimeRange: ['', ''] })
    this.load(this.state.pagination, this.filters, this.sorter)
  }

  async load(pagination, filters, sorter) {
    // 这里的参数是格式好的，无需修改，无阻抗失衡，直接发给服务器
    this.setState({ loading: true })
    let res = await axios.post(`/api/userApiKeyTokenStatistics/list/${pagination.pageSize}/${pagination.current}`, {
      filters: { ...filters }, sorter
    });
    pagination.total = res.data.data.total;
    this.setState({
      loading: false,
      data: res.data.data.data,
      pagination,
      selectedRowKeys: []
    });
    this.selectedRows = [];
    this.filters = filters;
    this.sorter = sorter;
  }

  async onRowSelectionChange(selectedRowKeys, selectedRows) {
    // 选中状态的数据，因为无需受控，就不记录在state里了，提高效率
    this.setState({ selectedRowKeys });
    this.selectedRows = selectedRows
  }

  async handleTableChange(pagination, filters, sorter) {
    // 此处是table控件的回调，其中的参数格式不是很合理，调整一下后，传递给load
    let sort = this.sorter;
    if (sorter && sorter.field) {
      sort = {};
      sort[sorter.field] = sorter.order == 'descend' ? -1 : 1
    } else {
      sort = {};
      sort['createTime'] = -1
    }
    let oldFilters = this.filters;
    for (let key in filters) {
      if (filters[key].length > 0) {
        let filter = filters[key];
        oldFilters[key] = { $in: filter };
      } else {
        delete oldFilters[key];
      }
    }
    // 暂时不用Table的filter，不太好用
    this.load(pagination, this.filters, sort)
  }

  refSelectedCount = (ref) => {
    this.state.selectedCountRef = ref
    this.setState({ selectedCountRef: ref })
    this.handleResize()
  }

  // 复制文本
  async copyTxt(txt, msg) {
    if (txt) {
      await navigator.clipboard.writeText(txt);
      message.success(msg || '复制成功')
    }
  }

  // 把columns放到render中，虽然损失部分性能，但是能方便参数中的匿名回调获取实例状态
  render() {
    const { intl } = this.props
    const columns = [
      {
        title: 'userID',
        dataIndex: 'userID',
        key: 'userID',
        width: 100,
        ellipsis: true,
        render: (v, r) => {
          return v ? <div onClick={() => this.copyTxt(v, '已复制到剪切板')}>{v}</div> : '-'
        }
      },
      {
        title: 'apiKey',
        dataIndex: 'apiKey',
        key: 'apiKey',
        width: 200,
        ellipsis: true,
      },
      {
        title: '调用次数',
        dataIndex: 'callCount',
        key: 'callCount',
        width: 150,
        ellipsis: true,
      },
      {
        title: '输入Token总数',
        dataIndex: 'promptTokenCount',
        key: 'promptTokenCount',
        width: 150,
        ellipsis: true,
      },
      {
        title: '输出Token总数',
        dataIndex: 'outputTokenCount',
        key: 'outputTokenCount',
        width: 150,
        ellipsis: true,
      },
      {
        title: '合计Token总数',
        dataIndex: 'totalTokenCount',
        key: 'totalTokenCount',
        width: 150,
        ellipsis: true
      }
    ];

    const customRowStyle = {
      padding: '50px' // 调整此值以改变行高
    };

    return (<MyTranslate><ExternalAccountFilter>
      <Breadcrumb>
        <Breadcrumb.BreadcrumbItem>资源管理</Breadcrumb.BreadcrumbItem>
        <Breadcrumb.BreadcrumbItem>AI token统计</Breadcrumb.BreadcrumbItem>
      </Breadcrumb>

      <div className="account-search-box">
        <div className='account-search-item' style={{ minWidth: 280 }}>
          <div className="account-search-item-label">userID</div>
          <div className="account-search-item-right">
            <Input
              allowClear
              style={{ width: 200 }}
              placeholder="请输入"
              value={this.state.userID}
              onChange={e => {
                this.setState({ userID: e.target.value })
                this.filters['userID'] = e.target.value
              }
              }
              onPressEnter={e => {
                this.setState({ userID: e.target.value })
                this.filters['userID'] = e.target.value
                this.reload()
              }
              }
            />
          </div>
        </div>

        <div className='account-search-item'>
          <div className="account-search-item-label">使用时间</div>
          <div className="account-search-item-right" style={{ width: 500 }}>
            <DateRangePicker
              mode="date"
              presetsPlacement="bottom"
              enableTimePicker
              value={this.state.createTimeRange}
              onChange={(e) => {
                this.setState({
                  createTimeRange: e
                })
                this.filters['createTimeRange'] = e
                this.reload()
              }}
            />
          </div>
        </div>

        <div className='account-btn-no-expand' style={{ width: '136px' }}>
          <div style={{ display: 'flex', justifyContent: 'right', alignItems: 'center' }}>
            <div className="search-query-btn" onClick={() => this.reload()}>查询</div>
            <div className="search-reset-btn" onClick={() => this.reset()}>重置</div>
          </div>
        </div>
        <div style={{ clear: 'both' }}></div>

      </div>

      <div className="account-main-content">
        <div className="account-main-content-right">
          <div className="tableSelectedCount"
            ref={this.refSelectedCount}>{`已选${this.state.selectedRowKeys.length}项`}</div>
          <div className="tableContent accountTableContent" style={{ height: this.state.tableContentHeight }}>
            <div>
              <Table
                size="middle"
                tableLayout="fixed"
                scroll={{
                  y: this.state.scrollY,
                  x: columns.filter(e => e.width).map(e => e.width).reduce((a, b) => a + b)
                }}
                pagination={this.state.pagination} rowSelection={{
                  selectedRowKeys: this.state.selectedRowKeys,
                  onChange: this.onRowSelectionChange.bind(this)
                }} columns={columns}
                rowKey='_id'
                dataSource={this.state.data}
                loading={this.state.loading}
                onChange={this.handleTableChange.bind(this)}
              />
            </div>
          </div>
          <Pagination
            showJumper
            total={this.state.pagination.total}
            current={this.state.pagination.current}
            pageSize={this.state.pagination.pageSize}
            onChange={this.handleTableChange.bind(this)}
            components={{
              body: {
                row: (props) => (
                  <tr {...props} style={{ ...props.style, ...customRowStyle }} />
                ),
              },
            }}
          />
        </div>
      </div>
    </ExternalAccountFilter></MyTranslate>
    )
  }
}

export default injectIntl(MyComponent)
