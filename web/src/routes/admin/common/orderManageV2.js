import React, { Component } from 'react'
import { BrowserRouter as Router, Route, Link } from 'react-router-dom'
import {
  Icon,
  Table,
  Divider,
  Upload,
  message,
  Input,
  Button,
  Modal,
  Select,
  Row,
  Col,
} from 'antd'
import { Pagination, Breadcrumb, Dialog, Tag, Input as TInput, Upload as TUpload, Tooltip as TTooltip, Switch, DateRangePicker } from 'tdesign-react';
import axios from 'axios'
import { formatDate } from 'components/DateFormat'
import { download } from "components/postDownloadUtils"
import { connect } from 'dva'
import { injectIntl } from 'react-intl'

const { Option } = Select;
const Search = Input.Search;
const confirm = Modal.confirm;

// 针对当前页面的基础url
const baseUrl = '/api/consumer/platform';
const params = []

class MyComponent extends Component {
  constructor(props) {
    super(props);
    // data由服务器返回表格的数据
    // loading由客户端控制
    // pagination控制表格分页功能，其中数据总数pagination.total由服务器返回
    // 以上变量受控，因为需要传递给Table控件
    this.state = {
      data: [],
      loading: false,
      pagination: {
        total: 0,
        pageSize: 10,
        current: 1,
        showTotal: (total, range) => `共 ${total} 条`,
        showSizeChanger: true,
        pageSizeOptions: ['10', '20', '30', '40', '50', '100', '200', '300', '400', '500']
      },
      selectedRowKeys: [],

      config: {},   // 用户参数存储
      selectedCountRef: null,
      tableContentHeight: 0,
      scrollY: 0,
      addVisible: false,
      editUser: null,
      platforms: [],
      platform: 'aggregationPlatform',
      searchDesc: '',
      files: [],
      image: '',
      displayStatus: false,
      price: '',
      allPlatforms: [],
      orderNo: '',
      platformId: '',
      status: '',
      createTimeRange: ['', ''],
      userIDRes: []
    };
    // 选中行的数据保存在selectedRows变量中
    // 之所以不放入state，因为这些变量是单向流动的，即Table控件产生这些数据，通过callback更新到变量this.selectedRows
    this.selectedRows = [];
    // filters的写法保持与mongo filter的写法一致，避免与服务器、数据库直接的数据阻抗失衡
    // 例如： filters={username: 'foo'}
    // 因为json协议无法传输regex，所以字符类型的值，都会在服务器端转成regex，以提高匹配度，损失部分查询性能
    this.filters = {};
    // sorter的写法保持与mongo sort的写法一致，避免与服务器、数据库直接的数据阻抗失衡
    // 例如：sorter={createTime: -1}
    // 注意：Table控件仅支持单列排序，不支持多列同时排序
    this.sorter = {
      createTime: -1,
      sortNo: -1,
    }
    // 提前写在constructor则Table首次加载时生效
    // 但是因为没有放到state中，所以除sorter外，都不建议提前设置，保持不受控状态是最好的
  }

  handleResize = () => {
    let height = document.body.getBoundingClientRect().height;
    if (this.state.selectedCountRef) {
      // console.log('===CQ', this.state.searchRef.getBoundingClientRect(), height,height - this.state.searchRef.getBoundingClientRect().top - 100)
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

  // 首次加载数据
  async componentWillMount() {
    this.reload()
    this.getAllPlatform()
    this.loadUsers()
  }

  async getAllPlatform() {
    let res = await axios.post(`/api/consumer/platform/common/10000/1`, { filters: {}, sorter: { sortNo: -1, createTime: -1 } });
    if (res.data.code === 1) {
      this.setState({
        allPlatforms: res.data.data.data
      })
    }
  }

  async loadUsers() {
    let res = await axios.post(`/api/consumer/user/10000/1`, { filters: {}, sorter: { createTime: -1 } })
    let userIDRes = res.data.data.data.map(ws => {
      return ({
        value: ws.userID,
        label: ws.userID
      })
    })
    this.setState({ userIDRes })
  }

  async reset() {
    this.filters = {}
    this.state.orderNo = ''
    this.state.status = ''
    this.state.platformId = ''
    this.state.createTimeRange = ['', '']
    this.state.pagination.current = 1
    this.setState({
      filters: {}, pagination: this.state.pagination, orderNo: '',
      platformId: '',
      status: '',
      createTimeRange: ['', '']
    })
    this.reload()
  }

  async reload() {
    // 重新加载，一般是页面第一次加载的时候来一下
    this.load(this.state.pagination, this.filters, this.sorter)
  }

  async load(pagination, filters, sorter) {
    // 这里的参数是格式好的，无需修改，无阻抗失衡，直接发给服务器
    this.setState({ loading: true });
    let res = await axios.post(`/api/buyEmailOrder/list/${pagination.pageSize}/${pagination.current}`, { filters: { ...filters, type: 'v2' }, sorter });
    pagination.total = res.data.data.total;
    this.setState({ loading: false, data: res.data.data.data, pagination, selectedRowKeys: [] });
    this.selectedRows = [];
    this.filters = filters;
    this.sorter = sorter
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
      sort[sorter.field] = sorter.order == 'descend'
        ? -1
        : 1
    }
    // 暂时不用Table的filter，不太好用
    this.load(pagination, this.filters, sort)
  }

  async delete(r) {
    if (!r) {
      if (this.state.selectedRowKeys.length == 0) {
        message.error('请先选择数据');
        return
      }
    }

    confirm({
      title: '确定要删除这些数据？',
      content: '',
      okText: '确定',
      okType: 'danger',
      cancelText: '取消',
      onOk: async () => {
        this.setState({ loading: true });
        await axios.post(`${baseUrl}/delete`, r ? [r._id] : this.state.selectedRowKeys);
        message.success('操作成功');
        this.reload()
      },
      onCancel() {
      }
    })
  }

  async enableBtnClick(record, enable) {
    this.setState({ loading: true });
    await axios.post(`${baseUrl}/changeEnable/${record._id}`, { enable });
    message.success('操作成功');
    this.reload()
  }

  refSelectedCount = (ref) => {
    this.state.selectedCountRef = ref
    this.setState({ selectedCountRef: ref })
    this.handleResize()
  }

  async copyTxt(txt, msg) {
    if (txt) {
      await navigator.clipboard.writeText(txt);
      message.success(msg || '复制成功')
    }
  }

  // 把columns放到render中，虽然损失部分性能，但是能方便参数中的匿名回调获取实例状态
  render() {
    const columns = [
      {
        title: '用户',
        dataIndex: 'userID',
        key: 'userID',
        width: 80,
        ellipsis: true,
      },
      {
        title: '订单编号',
        dataIndex: 'orderNo',
        key: 'orderNo',
        width: 200,
        ellipsis: true,
        render: (v, record, index) => {
          return v ? <div style={{ cursor: 'pointer' }} onClick={() => this.copyTxt(v, '订单编号已复制到剪切板')}>{v}</div> : '-'
        }
      }, {
        title: '平台',
        dataIndex: 'platformName',
        key: 'platformName',
        width: 120,
        ellipsis: true,
      }, {
        title: '价格(点)',
        dataIndex: 'unitPrice',
        key: 'unitPrice',
        width: 80,
        ellipsis: true,
      }, {
        title: '下单数',
        dataIndex: 'buyNum',
        key: 'buyNum',
        width: 80,
        ellipsis: true,
      }, {
        title: '金额(点)',
        dataIndex: 'payPrice',
        key: 'payPrice',
        width: 80,
        ellipsis: true,
      },
      // {
      //   title: '订单状态',
      //   dataIndex: 'status',
      //   key: 'status',
      //   width: 100,
      //   ellipsis: true,
      //   render: (v) => {
      //     if (v === 'doing') {
      //       return <div>进行中</div>
      //     }
      //     if (v === 'finish') {
      //       return <div>已完成</div>
      //     }
      //     if (v === 'failed') {
      //       return <div>失败</div>
      //     }
      //   }
      // },
      {
        title: '下单时间',
        dataIndex: 'createTime',
        key: 'createTime',
        render: formatDate,
        width: 120,
        ellipsis: true,
      }, {
        title: '操作',
        dataIndex: 'op',
        key: 'op',
        width: 80,
        ellipsis: true,
        render: (t, record, index) => {
          return (<div>
            {record.downloadUrl ? <Button type="link" onClick={async () => {
              window.open(record.downloadUrl, '_blank')
            }}>下载</Button> : ''}
          </div>)
        }
      }
    ];

    return (<div>
      <Breadcrumb>
        <Breadcrumb.BreadcrumbItem>个人中心</Breadcrumb.BreadcrumbItem>
        <Breadcrumb.BreadcrumbItem>订单管理V2</Breadcrumb.BreadcrumbItem>
      </Breadcrumb>

      <div className="account-search-box">
        <div className='account-search-item'>
          <div className="account-search-item-label" style={{ width: '100px' }}>用户</div>
          <div className="account-search-item-right">
            <Select value={this.state.userID} style={{ width: 200 }} onChange={(e) => {
              this.setState({ userID: e });
              if (e == '') {
                delete this.filters.userID
              } else {
                this.filters.userID = e;
              }
              this.reload()
            }}>
              <Option value="">全部</Option>
              {
                this.state.userIDRes.map(ws => {
                  return <Option value={ws.value}>{ws.label}</Option>
                })
              }
            </Select>
          </div>
        </div>

        <div className='account-search-item'>
          <div className="account-search-item-label">订单编号</div>
          <div className="account-search-item-right">
            <Input
              allowClear
              style={{ width: 200 }}
              placeholder="请输入"
              value={this.state.orderNo}
              onChange={e => {
                this.setState({ orderNo: e.target.value })
                this.filters['orderNo'] = e.target.value
              }
              }
              onPressEnter={e => {
                this.setState({ orderNo: e.target.value })
                this.filters['orderNo'] = e.target.value
                this.reload()
              }
              }
            />
          </div>
        </div>

        <div className='account-search-item'>
          <div className="account-search-item-label">平台</div>
          <div className="account-search-item-right">
            <Select value={this.state.platformId} style={{ width: 200 }} placeholder="请选择内容" onChange={v => {
              this.setState({ platformId: v })
              this.filters['platformId'] = v
              this.reload()
            }}>
              {
                this.state.allPlatforms.map(e => <Option value={e._id}>{e.name}</Option>)
              }
            </Select>
          </div>
        </div>

        {/* <div className='account-search-item'>
          <div className="account-search-item-label">订单状态</div>
          <div className="account-search-item-right">
            <Select value={this.state.status} style={{ width: 200 }} onChange={v => {
              this.setState({ status: v })
              if (v) {
                this.filters['status'] = v
              } else {
                delete this.filters['status']
              }

              this.reload()
            }}>
              <Option value="">全部</Option>
              <Option value="doing">进行中</Option>
              <Option value="finish">已完成</Option>
              <Option value="failed">失败</Option>
            </Select>
          </div>
        </div> */}

        <div className='account-search-item'>
          <div className="account-search-item-label">下单时间</div>
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

      <div style={{ paddingTop: 40 }}>
        <div style={{ overflow: 'hidden' }}>
          <div style={{ display: 'flex' }}>
            {/* <div className={"search-query-btn"} onClick={() => {
              download(`${baseUrl}/export`)
            }}>导出
            </div> */}
          </div>
          <div className="tableSelectedCount"
            ref={this.refSelectedCount}>{`已选${this.state.selectedRowKeys.length}项`}</div>
          <div className="tableContent" style={{ height: this.state.tableContentHeight }}>
            <div>
              <Table
                tableLayout="fixed"
                scroll={{
                  y: this.state.scrollY,
                  x: columns.filter(e => e.width).map(e => e.width).reduce((a, b) => a + b)
                }}
                pagination={this.state.pagination} rowSelection={{
                  selectedRowKeys: this.state.selectedRowKeys,
                  onChange: this.onRowSelectionChange.bind(this)
                }} columns={columns} rowKey='_id' dataSource={this.state.data} loading={this.state.loading}
                onChange={this.handleTableChange.bind(this)} />
            </div>
          </div>
          <Pagination
            showJumper
            total={this.state.pagination.total}
            current={this.state.pagination.current}
            pageSize={this.state.pagination.pageSize}
            onChange={this.handleTableChange.bind(this)}
          />
        </div>
      </div>
    </div>)
  }
}


export default connect(({ user }) => ({
  openKeys: user.openKeys,
  userID: user.info.userID
}))(injectIntl(MyComponent))
