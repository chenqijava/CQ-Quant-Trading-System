import React, {Component} from 'react'
import {BrowserRouter as Router, Route, Link} from 'react-router-dom'
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
import { Pagination, Breadcrumb, Dialog, Tag, Input as TInput, Upload as TUpload, Tooltip as TTooltip, DateRangePicker} from 'tdesign-react';
import axios from 'axios'
import {formatDate} from 'components/DateFormat'
import {download} from "components/postDownloadUtils"

const {Option} = Select;
const Search = Input.Search;
const confirm = Modal.confirm;

// 针对当前页面的基础url
const baseUrl = '/api/consumer/receive';
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
      allPlatforms: [],
      email: '',
      platformId: '',
      receiveTimeRange: ['',''],
      record: null,
      lookVisible: false,
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
          this.setState({selectedCountRef: this.state.selectedCountRef, tableContentHeight: height - this.state.selectedCountRef.getBoundingClientRect().top - 84, scrollY: height - this.state.selectedCountRef.getBoundingClientRect().top - 84 - 80})
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
  }

  async getAllPlatform() {
    let res = await axios.post(`/api/consumer/platform/10000/1`, { filters: {}, sorter: { createTime: -1 } });
    if (res.data.code === 1) {
      this.setState({
        allPlatforms: res.data.data.data
      })
    }
  }

  async reset () {
    this.filters = {}
    this.state.email = ''
    this.state.platformId = ''
    this.state.receiveTimeRange = ['','']
    this.state.pagination.current = 1
    this.setState({ filters: {}, pagination: this.state.pagination, email: '', platformId: '', receiveTimeRange: ['', ''] })
    this.reload()
  }

  async reload() {
    // 重新加载，一般是页面第一次加载的时候来一下
    this.load(this.state.pagination, this.filters, this.sorter)
  }

  async load(pagination, filters, sorter) {
    // 这里的参数是格式好的，无需修改，无阻抗失衡，直接发给服务器
    this.setState({loading: true});
    let res = await axios.post(`${baseUrl}/${pagination.pageSize}/${pagination.current}`, {filters, sorter});
    pagination.total = res.data.data.total;
    this.setState({loading: false, data: res.data.data.data, pagination, selectedRowKeys: []});
    this.selectedRows = [];
    this.filters = filters;
    this.sorter = sorter
  }

  async onRowSelectionChange(selectedRowKeys, selectedRows) {
    // 选中状态的数据，因为无需受控，就不记录在state里了，提高效率
    this.setState({selectedRowKeys});
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
    this.setState({selectedCountRef: ref})
    this.handleResize()
  }

  async look (r) {
    this.setState({
      lookVisible: true,
      record: r
    })
  }

  // 把columns放到render中，虽然损失部分性能，但是能方便参数中的匿名回调获取实例状态
  render() {
    const columns = [
      {
        title: '邮箱',
        dataIndex: 'email',
        key: 'email',
        width: 200,
        ellipsis: true,
      }, {
        title: '平台',
        dataIndex: 'platformName',
        key: 'platformName',
        width: 120,
        ellipsis: true,
      },{
        title: '创建时间',
        dataIndex: 'receiveTime',
        key: 'receiveTime',
        render: formatDate,
        width: 120,
        ellipsis: true,
      },{
        title: '操作',
        dataIndex: 'op',
        key: 'op',
        width: 120,
        ellipsis: true,
        render: (t, record, index) => {
          return (<div>
            <Button type="link" onClick={async () => {
              this.look(record)
            }}>查看</Button>
          </div>)
        }
      }
    ];

    return (<div>
      <Breadcrumb>
        <Breadcrumb.BreadcrumbItem>资源管理</Breadcrumb.BreadcrumbItem>
        <Breadcrumb.BreadcrumbItem>接码记录</Breadcrumb.BreadcrumbItem>
      </Breadcrumb>


      <Dialog
        header={"接码内容"}
        width={1200}
        // height={700}
        visible={this.state.lookVisible}
        placement='center'
        confirmBtn={null}
        onCancel={() => {
          this.setState({ lookVisible: false })
        }}
        onClose={() => {
          this.setState({ lookVisible: false })
        }}
      >
        <div style={{height: 600, overflow: 'auto'}}>
         { this.state.record ? <div dangerouslySetInnerHTML={{__html: this.state.record.allText || this.state.record.text || ''}}></div> : '' }
        </div>
      </Dialog>

      <div className="search-box">
        <div className='search-item' style={{minWidth: 280}}>
          <div className="search-item-label">邮箱</div>
          <div className="search-item-right">
            <Input
              allowClear
              style={{width: 200}}
              placeholder="请输入"
              // 补充事件绑定 ↓
              value={this.state.email}
              onChange={e => {
                this.filters.email = e.target.value;
                this.setState({
                  email: e.target.value
                })
              }
              }
              onPressEnter={e => {
                this.reload()
              }
              }
            />
          </div>
        </div>

        <div className='search-item' style={{minWidth: 280}}>
          <div className="search-item-label">平台</div>
          <div className="search-item-right">
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

        <div className='search-item' style={{minWidth: 280}}>
          <div className="search-item-label">接码时间</div>
          <div className="search-item-right" style={{width: 500}}>
            <DateRangePicker
              mode="date"
              presetsPlacement="bottom"
              enableTimePicker
              value={this.state.receiveTimeRange}
              onChange={(e) => {
                this.setState({
                  receiveTimeRange: e
                })
                this.filters['receiveTimeRange'] = e
                this.reload()
              }}
            />
          </div>
        </div>

        <div className='accountGroup-btn'>
          <div className="search-query-btn" onClick={() => this.reload()}>查询</div>
          <div className="search-reset-btn" onClick={() => this.reset()}>重置</div>
        </div>
      </div>

      <div style={{paddingTop: 40}}>
        <div style={{overflow: 'hidden'}}>
        <div className="tableSelectedCount"
            ref={this.refSelectedCount}></div>
          <div className="tableContent" style={{height: this.state.tableContentHeight}}>
            <div>
              <Table
                tableLayout="fixed"
                scroll={{
                  y: this.state.scrollY,
                  x: columns.filter(e => e.width).map(e => e.width).reduce((a, b) => a + b)
                }}
                pagination={this.state.pagination} columns={columns} dataSource={this.state.data} loading={this.state.loading}
                onChange={this.handleTableChange.bind(this)}/>
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
