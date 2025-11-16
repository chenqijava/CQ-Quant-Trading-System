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
  Switch,
  Row,
  Col,
} from 'antd'
import { Pagination, Breadcrumb, Dialog, Tag, Input as TInput, Upload as TUpload, Tooltip as TTooltip, } from 'tdesign-react';
import axios from 'axios'
import { formatDate } from 'components/DateFormat'
import {download} from "components/postDownloadUtils"

const { Option } = Select;
const Search = Input.Search;
const confirm = Modal.confirm;

// 针对当前页面的基础url
const baseUrl = '/api/task';
const params = []

const colLayou = {
  span: 6,
  style: { padding: '5px' }
};

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
      pagination1: {
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
      type: '',
      usedVisiable: false,
      data1: [],
      status: '',
      taskId: ''
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
      createTime: -1
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
  }

  async reset() {
    this.filters = {}
    this.state.type = ''
    this.state.pagination.current = 1
    this.setState({ filters: {}, pagination: this.state.pagination, type: '' })
    this.reload()
  }

  async reload() {
    // 重新加载，一般是页面第一次加载的时候来一下
    this.load(this.state.pagination, this.filters, this.sorter)
  }

  async load(pagination, filters, sorter) {
    // 这里的参数是格式好的，无需修改，无阻抗失衡，直接发给服务器
    this.setState({ loading: true });
    if (filters.type === '') {
      delete filters.type
    }
    let res = await axios.post(`${baseUrl}/${pagination.pageSize}/${pagination.current}`, { filters, sorter });
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

  refSelectedCount = (ref) => {
    this.state.selectedCountRef = ref
    this.setState({ selectedCountRef: ref })
    this.handleResize()
  }

  reset1() {
    this.filters1 = {}
    this.state.pagination1.current = 1
    this.setState({ filters1: {}, pagination1: { ...this.state.pagination1 }, status: '' })
    this.load1(this.state.pagination1, this.filters1, {})
  }

  reload1() {
    this.state.pagination1.current = 1
    this.setState({ pagination1: this.state.pagination1 })
    this.load1(this.state.pagination1, this.filters1, {})
  }

  async load1(pagination1, filters1, sorter1) {
    if (filters1.status === '') {
      delete filters1.status
    }
    // 这里的参数是格式好的，无需修改，无阻抗失衡，直接发给服务器
    this.setState({ loading: true })
    let res = await axios.post(`${baseUrl}/processDetail/${this.state.taskId}/${pagination1.pageSize}/${pagination1.current}`, {
      filters: filters1
    });
    pagination1.total = res.data.data.total;
    this.setState({
      loading: false,
      data1: res.data.data.data,
      pagination1: pagination1,
    });
    this.filters1 = filters1;
  }

  async handleTableChange1(pagination, filters, sorter) {
    // 暂时不用Table的filter，不太好用
    this.load1(pagination, this.filters1, {})
  }

  // 把columns放到render中，虽然损失部分性能，但是能方便参数中的匿名回调获取实例状态
  render() {
    const columns = [
      {
        title: '任务描述',
        dataIndex: 'desc',
        key: 'desc',
        width: 200,
        ellipsis: true,
      }, {
        title: '任务总数',
        dataIndex: 'publishedCount',
        key: 'publishedCount',
        width: 120,
        ellipsis: true,
      }, {
        title: '成功',
        dataIndex: 'success',
        key: 'success',
        width: 120,
        ellipsis: true,
      }, {
        title: '失败',
        dataIndex: 'failed',
        key: 'failed',
        width: 120,
        ellipsis: true,
      },
      {
        title: '状态',
        dataIndex: 'status',
        key: 'status',
        width: 120,
        ellipsis: true,
        render: (v) => {
          return v === 'success' ? '成功' : v === 'waitPublish' ? '待执行' : '执行中'
        }
      }, {
        title: '创建时间',
        dataIndex: 'createTime',
        key: 'createTime',
        render: formatDate,
        width: 120,
        ellipsis: true,
      }, {
        title: '完成时间',
        dataIndex: 'finishTime',
        key: 'finishTime',
        render: formatDate,
        width: 120,
        ellipsis: true,
      }, {
        title: '操作',
        dataIndex: 'op',
        key: 'op',
        width: 120,
        ellipsis: true,
        render: (t, record, index) => {
          return (<div>
            <Button type="link" onClick={async () => {
              this.setState({
                usedVisiable: true,
                taskId: record._id,
              })
              this.state.taskId = record._id
              this.reset1()
            }}>进度</Button>

            { record.type === 'AccountImport' && record.failed > 0 ? <Button type="link" onClick={async () => {
              download(`${baseUrl}/exportFailedAccount/${record._id}`, {})
            }}>失败账号导出</Button> : '' }
          </div>)
        }
      }
    ];

    const columns1 = [
      {
        title: '邮箱号',
        dataIndex: 'params.addData',
        key: 'params.addData',
        width: 200,
        ellipsis: true,
      },
      {
        title: '状态',
        dataIndex: 'status',
        key: 'status',
        width: 200,
        ellipsis: true,
        render: (v) => {
          return v === 'success' ? '成功' : v === 'failed' ? '失败' : '执行中'
        }
      },{
        title: '创建时间',
        dataIndex: 'createTime',
        key: 'createTime',
        render: formatDate,
        width: 120,
        ellipsis: true,
      }, {
        title: '完成时间',
        dataIndex: 'finishTime',
        key: 'finishTime',
        render: formatDate,
        width: 120,
        ellipsis: true,
      },{
        title: '备注',
        dataIndex: 'result.msg',
        key: 'result.msg',
        width: 120,
        ellipsis: true,
      },
    ]

    return (<div>
      <Breadcrumb>
        <Breadcrumb.BreadcrumbItem>资源管理</Breadcrumb.BreadcrumbItem>
        <Breadcrumb.BreadcrumbItem>任务管理</Breadcrumb.BreadcrumbItem>
      </Breadcrumb>


      <Dialog
        header={"进度"}
        width={1200}
        // height={700}
        visible={this.state.usedVisiable}
        placement='center'
        confirmBtn={null}
        onCancel={() => {
          this.setState({ usedVisiable: false })
        }}
        onClose={() => {
          this.setState({ usedVisiable: false })
        }}
      >
        <div style={{ margin: 30, color: 'rgba(0, 0, 0, 0.90)' }}>
          <div className="search-box">
            <div className='search-item'>
              <div className="search-item-label">状态</div>
              <div className="search-item-right">
                <Select value={this.state.status} style={{ width: 200 }} placeholder="全部" onChange={v => {
                  this.setState({ status: v })
                  this.filters1['status'] = v === 'doing' ? {$nin: ['success', 'failed']} : v
                  this.reload1()
                }}>
                  <Option value="">全部</Option>
                  <Option value="success">成功</Option>
                  <Option value="failed">失败</Option>
                  <Option value="doing">执行中</Option>
                </Select>
              </div>
            </div>

            <div className='accountGroup-btn'>
              <div className="search-query-btn" onClick={() => this.reload1()}>查询</div>
              <div className="search-reset-btn" onClick={() => this.reset1()}>重置</div>
            </div>
          </div>
          <div className="tableContent accountTableContent" style={{ marginTop: 20, height: 'auto' }}>
            <Table
              size="middle"
              tableLayout="fixed"
              pagination={this.state.pagination1} columns={columns1}
              dataSource={this.state.data1}
              loading={this.state.loading}
              onChange={this.handleTableChange1.bind(this)}
            />
          </div>

          <Pagination
            showJumper
            total={this.state.pagination1.total}
            current={this.state.pagination1.current}
            pageSize={this.state.pagination1.pageSize}
            onChange={this.handleTableChange1.bind(this)}
          />
        </div>
      </Dialog>

      <div className="search-box">
        <div className='search-item' style={{ minWidth: 280 }}>
          <div className="search-item-label">任务类型</div>
          <div className="search-item-right">
            <Select value={this.state.type} style={{ width: 200 }} placeholder="请选择任务类型" onChange={v => {
              this.setState({ type: v })
              this.filters['type'] = v
              this.reload()
            }}>
              <Option value="">全部</Option>
              <Option value="AccountImport">账号导入</Option>
              <Option value="BatchSendEmailTestV2">检查邮箱发件限制</Option>
            </Select>
          </div>
        </div>

        <div className='accountGroup-btn'>
          <div className="search-query-btn" onClick={() => this.reload()}>查询</div>
          <div className="search-reset-btn" onClick={() => this.reset()}>重置</div>
        </div>
      </div>

      <div style={{ paddingTop: 40 }}>
        <div style={{ overflow: 'hidden' }}>
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

export default MyComponent
