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
  DatePicker
} from 'antd'
import axios from 'axios'
import { connect } from 'dva'
import { injectIntl } from 'react-intl'
import { formatDate } from 'components/DateFormat'
import billType from 'components/billType'
import expenseType from 'components/expenseType'
import { Pagination, Breadcrumb } from 'tdesign-react';

const Search = Input.Search
const confirm = Modal.confirm
const { BreadcrumbItem } = Breadcrumb;
const { RangePicker } = DatePicker;

// 针对当前页面的基础url
const baseUrl = '/api/consumer/user/balanceDetail';

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
        pageSize: 10,
        current: 1,
        showTotal: (total, range) => `共 ${total} 条`,
        showSizeChanger: true,
        pageSizeOptions: ['10', '20', '30', '40', '50', '100', '200', '300', '400', '500']
      },
      selectedRowKeys: [],
      chargeVisible: false,
      chargeValue: 0,
      showTable: false,
      scrollY: 0,
      tableContent: null,
      expenseType: '',
      type: '',
      description: '',
      createTime: null,
      expenseTypes: {},
      billTypes: {},
      userID: '',
      userIDRes: [],
      balanceChangeTotal: 0
    }

    expenseType.map(ws => {
      this.state.expenseTypes[ws.value] = ws.label;
    })
    billType.map(ws => {
      this.state.billTypes[ws.value] = ws.label;
    })
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
      createTime: -1
    }
    // 提前写在constructor则Table首次加载时生效
    // 但是因为没有放到state中，所以除sorter外，都不建议提前设置，保持不受控状态是最好的
  }

  // 首次加载数据
  async componentWillMount() {
    this.reload()
    this.loadUsers()
  }

  handleResize = () => {
    if (this.state.tableContent) {
      this.setState({ scrollY: this.state.tableContent.getBoundingClientRect().height - 80, })
    }
  }

  async componentDidMount() {
    window.addEventListener("resize", this.handleResize);
  }

  componentWillUnmount() {
    window.removeEventListener("resize", this.handleResize);
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

  async reload() {
    // 重新加载，一般是页面第一次加载的时候来一下
    this.load(this.state.pagination, this.filters, this.sorter)
  }

  async load(pagination, filters, sorter) {
    // 这里的参数是格式好的，无需修改，无阻抗失衡，直接发给服务器
    this.setState({ loading: true })
    if (this.filters.description === '') {
      delete this.filters.description
    }
    this.getBalanceChangeTotal(filters, sorter)
    let res = await axios.post(`${baseUrl}/${pagination.pageSize}/${pagination.current}`, { filters, sorter })
    pagination.total = res.data.data.total
    this.setState({ loading: false, data: res.data.data.data, pagination, selectedRowKeys: [] })
    this.selectedRows = []
    this.filters = filters
    this.sorter = sorter
  }

  async onRowSelectionChange(selectedRowKeys, selectedRows) {
    // 选中状态的数据，因为无需受控，就不记录在state里了，提高效率
    this.setState({ selectedRowKeys })
    this.selectedRows = selectedRows
  }

  async handleTableChange(pagination, filters, sorter) {
    // 此处是table控件的回调，其中的参数格式不是很合理，调整一下后，传递给load
    let sort = this.sorter
    if (sorter && sorter.field) {
      sort = {}
      sort[sorter.field] = sorter.order == 'descend'
        ? -1
        : 1
    }
    // 暂时不用Table的filter，不太好用
    this.load(pagination, this.filters, sort)
  }

  async reset() {
    this.filters = {}
    this.setState({ expenseType: '', type: '', description: '', createTime: null })
    delete this.filters.createTime
    await this.reload()
  }

  refTableContent = (ref) => {
    if (ref && ref.getBoundingClientRect) {
      this.setState({ showTable: true, scrollY: ref.getBoundingClientRect().height - 80, tableContent: ref })
    }
  }

  async getBalanceChangeTotal (filters, sorter) {
    let res = await axios.post(`/api/consumer/user/balanceChange`, { filters, sorter })
    if (res.data.code === 1) {
      this.setState({ balanceChangeTotal: res.data.data })
    }
  }

  // 把columns放到render中，虽然损失部分性能，但是能方便参数中的匿名回调获取实例状态
  render() {
    const columns = [
      {
        title: '用户',
        dataIndex: 'userID',
        key: 'userID',
        width: 100,
        ellipsis: true,
      },
      {
        title: '账单类型',
        dataIndex: 'type',
        key: 'type',
        width: 100,
        ellipsis: true,
        render: (v, r) => {
          return this.state.billTypes[v] || v;
        }
      },
      {
        title: '账户类型',
        dataIndex: 'type',
        key: 'type',
        width: 100,
        ellipsis: true,
        render: (v, r) => {
          return v && v.indexOf('send_email_count') >= 0 ? '群发账户' : '基本账户'
        }
      },
      {
        title: '支出/收入',
        dataIndex: 'expenseType',
        key: 'expenseType',
        width: 100,
        ellipsis: true,
        render: (v, r) => {
          return this.state.expenseTypes[v] || v;
        }
      }, {
        title: '变动金额',
        dataIndex: 'value',
        key: 'value',
        width: 100,
        ellipsis: true,
      }, {
        title: '变动后余额',
        dataIndex: 'balance',
        key: 'balance',
        width: 100,
        ellipsis: true,
      }, {
        title: '描述',
        dataIndex: 'description',
        key: 'description',
        width: 200,
        ellipsis: true
      }, {
        title: '关联邮箱订单编号',
        dataIndex: 'emailOrderNo',
        key: 'emailOrderNo',
        width: 130,
        ellipsis: true
      }, {
        title: '创建时间',
        dataIndex: 'createTime',
        key: 'createTime',
        width: 130,
        render: formatDate
      }
    ];

    return (<div>
      <Breadcrumb>
        <BreadcrumbItem>资源管理</BreadcrumbItem>
        <BreadcrumbItem>账单管理</BreadcrumbItem>
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
          <div className="account-search-item-label" style={{ width: '100px' }}>支出/收入</div>
          <div className="account-search-item-right">
            <Select value={this.state.expenseType} style={{ width: 200 }} onChange={(e) => {
              this.setState({ expenseType: e });
              if (e == '') {
                delete this.filters.expenseType
              } else {
                this.filters.expenseType = e;
              }
              this.reload()
            }}>
              <Option value="">全部</Option>
              {
                expenseType.map(ws => {
                  return <Option value={ws.value}>{ws.label}</Option>
                })
              }
            </Select>
          </div>
        </div>

        <div className='account-search-item'>
          <div className="account-search-item-label" style={{ width: '100px' }}>账单类型</div>
          <div className="account-search-item-right">
            <Select value={this.state.type} style={{ width: 200 }} onChange={(e) => {
              this.setState({ type: e });
              if (e == '') {
                delete this.filters.type
              } else {
                this.filters.type = e;
              }
              this.reload()
            }}>
              <Option value="">全部</Option>
              {
                billType.map(ws => {
                  return <Option value={ws.value}>{ws.label}</Option>
                })
              }
            </Select>
          </div>
        </div>

        <div className='account-search-item'>
          <div className="account-search-item-label" style={{ width: '100px' }}>描述</div>
          <div className="account-search-item-right">
            <Input placeholder='请输入内容' value={this.state.description} style={{ width: 200 }}
              onChange={(e) => {
                this.setState({ description: e.target.value });
                this.filters.description = e.target.value;
              }}
              onPressEnter={e => {
                this.setState({ description: e.target.value });
                this.filters.description = e.target.value;
                this.reload()
              }
              }
            />
          </div>
        </div>

        <div className='account-search-item'>
          <div className="account-search-item-label" style={{ width: '100px' }}>创建时间</div>
          <div className="account-search-item-right">
            <RangePicker value={this.state.createTime} style={{ width: 200 }} allowClear={true} showTime format="YYYY-MM-DD HH:mm:ss"
              placeholder={['开始时间', '结束时间']} onOpenChange={value => {
                if (!value && this.filters.createTime) {
                  this.reload();
                }
              }} onChange={value => {
                if (value.length) {
                  this.setState({ createTime: value });
                  this.filters.createTime = {
                    $gte: value[0].toISOString(),
                    $lte: value[1].toISOString()
                  };
                } else {
                  delete this.filters.createTime;
                }
              }} />
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

      <div style={{ marginTop: 20, marginLeft: 20 }}>
        合计金额：{this.state.balanceChangeTotal}
      </div>

      <div className="main-content" style={{ marginTop: '-20px' }}>
        <div className="tableContent" ref={this.refTableContent} style={{ marginTop: '60px', overflow: 'auto' }}>
          <div>
            {this.state.showTable ? <Table
              tableLayout="fixed"
              scroll={{ y: this.state.scrollY, x: 1000 }}
              pagination={this.state.pagination} columns={columns} rowKey='_id' dataSource={this.state.data} loading={this.state.loading} /> : ''}
          </div>
        </div>
        <Pagination
          total={this.state.pagination.total}
          current={this.state.pagination.current}
          pageSize={this.state.pagination.pageSize}
          onChange={this.handleTableChange.bind(this)}
        />
      </div>
    </div>)
  }
}

export default connect(({ user }) => ({
  openKeys: user.openKeys
}))(injectIntl(MyComponent))
