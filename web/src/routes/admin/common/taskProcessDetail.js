import React, { Component } from 'react'
import { BrowserRouter as Router, Route, Link } from 'react-router-dom'
import {
  Icon,
  Table,
  Divider,
  message,
  Modal,
  Row,
  Col,
  Radio,
  Button,
  Avatar,
  Select
} from 'antd'
import axios from 'axios'
import { formatDate } from 'components/DateFormat'
import tTypes from 'components/taskTypes'
import taskStatuss from 'components/taskStatuss'
import { Pagination, Breadcrumb, Dialog } from 'tdesign-react';
import { Space, Tabs } from 'tdesign-react';

const { TabPanel } = Tabs;

const { BreadcrumbItem } = Breadcrumb;
const confirm = Modal.confirm;

// 针对当前页面的基础url- 发送任务进度
const baseUrl = '/api/consumer/task/quartzDetail';

//进度-账号进度页面
//D:\telegram\scrm_telegram_cloud\src\routes\admin\common\addFriend\detail.js
const addAccountUrl = '/api/consumer/task/processDetail';

const addTypes = [{
  label: '混合方式添加',
  value: '3',
}, {
  label: '通讯录添加',
  value: '2',
}, {
  label: '搜索添加',
  value: '1',
}];

const scenes = [{
  label: '通讯录添加',
  value: 13,
}, {
  label: '通讯录添加',
  value: 11,
}, {
  label: '搜索添加',
  value: 15,
}];

class MyComponent extends Component {
  constructor(props) {
    super(props);
    // this.query = this.props.taskProcessDetail.substring(1).split('&').map((v) => {
    //   let obj = {};
    //   let sp = v.split('=');
    //   obj[sp[0]] = sp[1];
    //   return obj
    // }).reduce((obj, p) => {
    //   return {
    //     ...obj,
    //     ...p
    //   }
    // });
    // data由服务器返回表格的数据
    // loading由客户端控制
    // pagination控制表格分页功能，其中数据总数pagination.total由服务器返回
    // 以上变量受控，因为需要传递给Table控件
    this.state = {
      choosenTab: 'addProcess',
      quartzDetail: this.props.quartzDetail,
      data: [],
      loading: false,
      pagination: {
        total: 0,            //添加次数
        pageSize: 10,
        current: 1,
        showTotal: (total, range) => `共 ${total} 条`,
        showSizeChanger: true,
        pageSizeOptions: ['10', '20', '30', '40', '50', '100', '200', '300', '400', '500']
      },
      sendPagination: {
        total: 0,            //添加次数
        pageSize: 10,
        current: 1,
        showTotal: (total, range) => `共 ${total} 条`,
        showSizeChanger: true,
        pageSizeOptions: ['10', '20', '30', '40', '50', '100', '200', '300', '400', '500']
      },
      selectedRowKeys: [],

      statusName: {
        'all': '全部',
      },
      statuses: [
        { label: '全部', value: 'all' },
      ],

      gtc: {},

      sendMsgVisible: false,   //发送消息任务
      params: {},               //任务参数

      imageBigVisible: false,
      imageBigUrl: '',
      showTable: false,
      scrollY: 0,
      tableContent: null,
      accounts: {},       //账号信息
      friends: {},        //账号好友信息
      statusName: {
        'all': '全部',
        'init': '执行中',
        'processing': '待执行',
        'success': '成功',
        'failed': '失败'
      },
      statuses: [
        {label: '全部', value: 'all'},
        {label: '待执行', value: 'processing'},
        {label: '执行中', value: 'init'},
        {label: '成功', value: 'success'},
        {label: '失败', value: 'failed'}
      ],
      status: 'all', //状态
      params: {},
    };
   /* Object.keys(taskStatuss).forEach(key => {
      if (key == 'waitPublish') return
      this.state.statusName[key] = taskStatuss[key];
      this.state.statuses.push(
        { label: taskStatuss[key], value: key },
      )
    })*/
    // 选中行的数据保存在selectedRows变量中
    // 之所以不放入state，因为这些变量是单向流动的，即Table控件产生这些数据，通过callback更新到变量this.selectedRows
    this.selectedRows = [];
    // filters的写法保持与mongo filter的写法一致，避免与服务器、数据库直接的数据阻抗失衡
    // 例如： filters={username: 'foo'}
    // 因为json协议无法传输regex，所以字符类型的值，都会在服务器端转成regex，以提高匹配度，损失部分查询性能
    this.filters = {
      //status: this.query.globalMessage ? 'failed' : 'all'
    };
    // sorter的写法保持与mongo sort的写法一致，避免与服务器、数据库直接的数据阻抗失衡
    // 例如：sorter={createTime: -1}
    // 注意：Table控件仅支持单列排序，不支持多列同时排序
    this.sorter = {
      createTime: -1
    };
    // 提前写在constructor则Table首次加载时生效
    // 但是因为没有放到state中，所以除sorter外，都不建议提前设置，保持不受控状态是最好的
    //this.type = this.query.type
  }

  // 首次加载数据
  async componentWillMount() {
    this.reload();
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

  async reload() {
    // 重新加载，一般是页面第一次加载的时候来一下
    this.load(this.state.pagination, this.filters, this.sorter)
  }

  async load(pagination, filters, sorter) {
      // 这里的参数是格式好的，无需修改，无阻抗失衡，直接发给服务器
    this.setState({ loading: true });
    let res = await axios.post(`${addAccountUrl}/${this.state.quartzDetail}/${pagination.pageSize}/${pagination.current}`, {
      filters,
      sorter
    });
    pagination.total = res.data.total;
    this.setState({
        loading: false,
        data: res.data.data,
        pagination,
        selectedRowKeys: [],
        accounts: res.data.accounts,
        friends: res.data.friends
    });
    this.selectedRows = [];
    this.filters = filters;
    this.sorter = sorter;
  }

  async reloadSend() {
    // 重新加载，一般是页面第一次加载的时候来一下
    this.loadSend(this.state.sendPagination, this.filters, this.sorter)
  }

  async loadSend(pagination, filters, sorter) {
    // 这里的参数是格式好的，无需修改，无阻抗失衡，直接发给服务器
    this.setState({ loading: true });
    let res = await axios.post(`${baseUrl}/${this.state.quartzDetail}/${pagination.pageSize}/${pagination.current}`, {
      filters,
      sorter,
      type: 'sendStrangerMsg2',      //accountSurvivalSearch任务有此参数
      taskType: 'sendRoomMsg',
    });
    pagination.total = res.data.total;
    this.setState({
      loading: false,
      data: res.data.data,
      pagination,
      selectedRowKeys: [],
      gtc: res.data.gtc
    });
    this.selectedRows = [];
    this.filters = filters;
    this.sorter = sorter
  }

  async handleTableChange(pagination, filters, sorter) {
    // 此处是table控件的回调，其中的参数格式不是很合理，调整一下后，传递给load
    let sort = this.sorter;
    if (sorter && sorter.field) {
      sort = {}
      sort[sorter.field] = sorter.order == 'descend'
        ? -1
        : 1
    }
    // 暂时不用Table的filter，不太好用
    this.load(pagination, this.filters, sort)
  }

  async handleTableSendChange(pagination, filters, sorter) {
    // 此处是table控件的回调，其中的参数格式不是很合理，调整一下后，传递给load
    let sort = this.sorter;
    if (sorter && sorter.field) {
      sort = {}
      sort[sorter.field] = sorter.order == 'descend'
        ? -1
        : 1
    }
    // 暂时不用Table的filter，不太好用
    this.loadSend(pagination, this.filters, sort)
  }

  async changeTaskType(e) {
    e = { target: { value: e } };
    this.setState({ status: e.target.value });
    if (e.target.value == 'all') {
      delete this.filters.status;
    } else {
      this.filters.status = e.target.value;
    }
    //this.reload()
  }

  refTableContent = (ref) => {
    if (ref && ref.getBoundingClientRect) {
      this.setState({ showTable: true, scrollY: ref.getBoundingClientRect().height - 80, tableContent: ref })
    }
  }

  async changeTab(key) {
    this.setState({ choosenTab: key })
    if (this.state.changeTab == 'addProcess') {
      this.reload();
    } else {
      this.reloadSend();
    }
  }

  async search() {
    if (this.state.choosenTab == 'addProcess') {
      this.reload();
    } else {
      this.reloadSend();
    }
  }

  async reset() {
    this.filters = {}
    this.setState({ filters: {}, status: 'all' })
    delete this.filters.status
    if (this.state.choosenTab == 'addProcess') {
      this.reload()
    } else {
      this.reloadSend()
    }
  }

  async showDetail(r) {
    this.setState({
      showParams: true,
      params: r.params
    })
  }
  // 把columns放到render中，虽然损失部分性能，但是能方便参数中的匿名回调获取实例状态
  render() {
    
    let columns = [];
    columns = [
      {
        title: '发送账号',
        dataIndex: 'sendPhone',
        key: 'sendPhone',
        width: 260
      }, {
        title: '接收账号',
        dataIndex: 'params.addData',
        key: 'params.addData',
        width: 180,
        ellipsis: true,
      }, {
        title: '状态',
        dataIndex: 'status',
        key: 'status',
        //sorter: true,
        width: 130,
        ellipsis: true,
        render: (v) => {
          return this.state.statusName[v]
        }
      }, {
        title: '创建时间',
        dataIndex: 'createTime',
        key: 'createTime',
        width: 180,
        ellipsis: true,
        render: formatDate
      }, {
        title: '完成时间',
        dataIndex: 'finishTime',
        key: 'finishTime',
        width: 180,
        ellipsis: true,
        render: formatDate
      }, {
        title: '操作',
        dataIndex: 'oper',
        key: 'oper',
        width: 130,
        ellipsis: true,
        render: (v, r) => {
          return (<div>
            <Button type="link" onClick={() => {
              this.showDetail(r)
            }}>详情</Button>
          </div>)
        }
      }, {
        title: '备注',
        dataIndex: 'result.msg',
        key: 'result.msg',
        width: 260,
        ellipsis: true,
        render: (v, r) => {
          let alertText = '';
          if (r.status == 'failed') {
            if(r.result && r.result.oldMsg) {
              alertText = r.result.oldMsg;
              return alertText;
            } else {
              if(r.result && r.result.oldErrors) {
                if(r.result.oldErrors.length == 1){
                  alertText = r.result.oldErrors[0].msg;
                } else {
                  alertText = r.result.oldErrors[r.result.oldErrors.length-2].msg;
                }
                return alertText
              } else {
                if (v) {
                  if (v.msg)
                    return v.msg;
                  else if (v.message)
                    return v.message;
                  else return v
                }
              }
            }
          } else {
            if (v) {
              if (v.msg)
                return v.msg;
              else if (v.message)
                return v.message;
              else return v
            }
          }
        }
      }
    
    ]

    return (<div>

      <div className="search-box">
        <div className='search-item'>
          <div className="search-item-label">任务状态</div>
          <div className="search-item-right">
            <Select value={this.state.status} style={{ width: 200 }} onChange={this.changeTaskType.bind(this)}>
              {this.state.statuses.map(ws => {
                return <Option value={ws.value}>{ws.label}</Option>
              })}
            </Select>
          </div>
        </div>

        <div className='accountGroup-btn' style={{marginLeft: 'auto',marginRight:'25px'}}>
          <div className="search-query-btn" onClick={() => this.reload()}>查询</div>
          <div className="search-reset-btn" onClick={() => this.reset()}>重置</div>
        </div>
      </div>

      <div class="main-content">
        <div className="tableContent" ref={this.refTableContent} style={{height: 'calc(100vh - 485px)'}}>
          <div>
            {this.state.showTable ? <Table
              tableLayout="fixed"
              scroll={{ y: this.state.scrollY, x: 1000 }}
              pagination={this.state.pagination} columns={columns} rowKey='_id' dataSource={this.state.data} loading={this.state.loading} /> : ''}
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

      <Dialog
        header="内容"
        visible={this.state.showParams}
        confirmBtn={null}
        onClose={() => { this.setState({ showParams: false }) }}
        onCancel={() => { this.setState({ showParams: false }) }}
      >
        <div className="dialog_item">

          <div>
            {
              this.state.params && this.state.params.imageFilePath ?
                <div key={`image-${this.state.params.imageFilePath}`}
                  style={{ display: 'inline-block'}}
                  onClick={() => {
                    this.setState({
                      imageBigVisible: true,
                      imageBigUrl: `/api/consumer/res/download/${this.state.params.imageFilePath}`
                    })
                  }}>
                  <Avatar shape="square" size={200}
                    src={`/api/consumer/res/download/${this.state.params.imageFilePath}`} />
                </div> : ''
            }
          </div>

          <div>
            {
              this.state.params && this.state.params.content ? <p>{this.state.params.content}</p> : ''
            }
          </div>

        </div>
      </Dialog>    
    </div>)
  }
}

export default MyComponent