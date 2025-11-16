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
import { Pagination, Breadcrumb, Dialog, Tag, Input as TInput, Upload as TUpload, Tooltip as TTooltip,} from 'tdesign-react';
import axios from 'axios'
import {formatDate} from 'components/DateFormat'

const {Option} = Select;
const Search = Input.Search;
const confirm = Modal.confirm;

// 针对当前页面的基础url
const baseUrl = '/api/consumer/proxyAccount';
const params = []
// [{
//   type: 'userProxy',
//   code: 'checkLength',
//   desc: '代理ip检查长度',
//   // unit: 0.01,
//   suffix: '段'
// }, {
//   type: 'userProxy',
//   code: 'checkMaxNum',
//   desc: '同段ip最多数量',
//   // unit: 0.01,
//   suffix: '个'
// }, {
//   type: 'disableIp',
//   code: 'maxCount',
//   desc: '同段ip最多封禁账号数量',
//   // unit: 0.01,
//   suffix: '个'
// }, {
//   type: 'disableIp',
//   code: 'disableLength',
//   desc: '同段ip封禁时长',
//   unit: 1000 * 60 * 60,
//   suffix: '小时'
// }];

const colLayou = {
  span: 6,
  style: {padding: '5px'}
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
    this.reloadConfig();
    this.loadPlatform()
  }

  async loadPlatform () {
    let res = await axios.get('/api/consumer/proxyAccount/getIpPlatforms')
    this.setState({
      platforms: res.data
    })
  }

  async reloadConfig() {
    let config = {};
    for (let i in params) {
      let param = params[i];
      let res = await axios.get(`/api/consumer/userParams/${param.type || pageType}/get/${param.code}`);
      if (res.data.code) {
        config[`${param.type}-${param.code}`] = res.data.data / (param.unit || 1);
      }
    }
    this.setState({config});
  }

  /**
   * 修改参数
   **/
  async onParamsSetChange(param, value) {
    this.setState({loading: true});
    let res = await axios.post(`/api/consumer/userParams/${param.type || pageType}/set/${param.code}`, {value: Number(value) * (param.unit || 1)});
    if (res.data.code == 1) {
      // 修改配置后重新加载所有配置
      await this.reloadConfig();
      message.success('操作成功');
    } else {
      Modal.error({title: res.data.message});
    }
    this.setState({loading: false})
  }

  async reset () {
    this.filters = {}
    this.state.searchDesc = ''
    this.state.pagination.current = 1
    this.setState({ filters: {}, searchDesc: '', pagination: this.state.pagination })
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

  // 比较通用的回调
  // async create() {
  //   this.props.history.push('card?oper=create')
  // }

  async edit() {
    if (this.state.selectedRowKeys.length == 0) {
      message.error('请先选择数据');
      return
    }
    if (this.state.selectedRowKeys.length > 1) {
      message.error('只能选择一条数据');
      return
    }
    let proxy = this.state.data.filter(e => e._id === this.state.selectedRowKeys[0])[0]
    this.setState({
      addVisible: true,
      editUser: {
        _id: this.state.selectedRowKeys[0]
      },
      desc: proxy.desc,
      account: proxy.account,
      token: proxy.token,
      maxVpsNum: proxy.maxVpsNum,
    })
  }

  async view() {
    if (this.state.selectedRowKeys.length == 0) {
      message.error('请先选择数据');
      return
    }
    if (this.state.selectedRowKeys.length > 1) {
      message.error('只能选择一条数据');
      return
    }
    this.props.history.push('card?oper=view&_id=' + this.state.selectedRowKeys[0])
  }

  async delete() {
    if (this.state.selectedRowKeys.length == 0) {
      message.error('请先选择数据');
      return
    }
    confirm({
      title: '确定要删除这些数据？',
      content: '',
      okText: '确定',
      okType: 'danger',
      cancelText: '取消',
      onOk: async() => {
        this.setState({loading: true});
        await axios.post(`${baseUrl}/delete`, this.state.selectedRowKeys);
        message.success('操作成功');
        this.reload()
      },
      onCancel() {
      }
    })
  }

  async enableBtnClick(record, enable) {
    this.setState({loading: true});
    await axios.post(`${baseUrl}/changeEnable/${record._id}`, {enable});
    message.success('操作成功');
    this.reload()
  }

  refSelectedCount = (ref) => {
    this.state.selectedCountRef = ref
    this.setState({selectedCountRef: ref})
    this.handleResize()
  }

  openAdd () {
    this.setState({
      addVisible: true,
      editUser: null,
      desc: '',
      platform: 'aggregationPlatform',
      maxVpsNum: '',
      token: '',
    })
  }

  // 把columns放到render中，虽然损失部分性能，但是能方便参数中的匿名回调获取实例状态
  render() {
    const columns = [
      {
        title: '描述',
        dataIndex: 'desc',
        key: 'desc',
        width: 120,
        ellipsis: true,
      }, {
        title: '代理账号',
        dataIndex: 'token',
        key: 'token',
        width: 120,
        ellipsis: true,
        render: (v, r) => {
          return `${r.account}(${v})`
        }
      },{
        title: 'IP重用次数',
        dataIndex: 'maxVpsNum',
        key: 'maxVpsNum',
        width: 120,
        ellipsis: true,
      }, {
        title: '创建时间',
        dataIndex: 'createTime',
        key: 'createTime',
        render: formatDate,
        width: 120,
        ellipsis: true,
      }, {
        title: '启用',
        dataIndex: 'enable',
        key: 'enable',
        width: 120,
        ellipsis: true,
        render: (t, record, index) => {
          return (<Switch style={{width: '56px'}} checkedChildren="选定" unCheckedChildren="" checked={t} onChange={(enable) => {
            this.enableBtnClick(record, enable)
          }}/>)
        }
      }
    ];

    return (<div>
      <Breadcrumb>
        <Breadcrumb.BreadcrumbItem>系统设置</Breadcrumb.BreadcrumbItem>
        <Breadcrumb.BreadcrumbItem>代理账号管理</Breadcrumb.BreadcrumbItem>
      </Breadcrumb>


      <Dialog
        header={this.state.editUser ? "编辑" : "新增"}
        width={854}
        height={566}
        visible={this.state.addVisible}
        placement='center'
        onConfirm={async () => {
          if (!this.state.token) {
            message.error('请选择代理账号')
            return
          }
          let form = {
            desc: this.state.desc,
            token: this.state.token,
            account: this.state.platforms.filter(e => e.value === this.state.token)[0].label,
            maxVpsNum: this.state.maxVpsNum,
            enable: false,
          };
          if (this.state.editUser) {
            form._id = this.state.editUser._id
          }
          this.setState({loading: true});
          // 过滤掉null
          let res = await axios.post(`${baseUrl}/card/save`, form)
          if (res.data.code == 1) {
            message.success('操作成功')
            this.setState({addVisible: false})
            this.reload()
          } else {
            message.error(res.data.message)
            this.setState({loading: false})
          }
        }} confirmLoading={this.state.loading}
        onCancel={() => {
          this.setState({addVisible: false})
        }}
        onClose={() => {
          this.setState({addVisible: false})
        }}
      >
        <div style={{marginLeft: 127, marginTop: 26, color: 'rgba(0, 0, 0, 0.90)'}}>
          <div style={{display: 'flex', marginBottom: 24}}>
            <div style={{width: 100, textAlign: 'right', marginRight: 16, lineHeight: '30px'}}>
              描述
            </div>
            <div>
              <Input value={this.state.desc}
                     onChange={(e) => this.setState({desc: e.target.value})} style={{width: 400}}
                     placeholder='请输入'/>
            </div>
          </div>

          <div style={{display: 'flex', marginBottom: 24}}>
            <div
              style={{width: 100, textAlign: 'right', marginRight: 16, lineHeight: '30px'}}>
              <span style={{color: '#D54941'}}>*</span>选择账号
            </div>
            <Select value={this.state.token} style={{width: 400}} placeholder="请选择"
                      onChange={v => this.setState({token: v})}>
                { this.state.platforms.map(ws => <Option value={ws.value}>{ws.label}({ws.value})</Option>) }
            </Select>
          </div>

          <div style={{display: 'flex', marginBottom: 24}}>
            <div style={{width: 100, textAlign: 'right', marginRight: 16, lineHeight: '30px'}}>
              IP重用次数
            </div>
            <div>
              <Input value={this.state.maxVpsNum}
                     onChange={(e) => this.setState({maxVpsNum: e.target.value})} style={{width: 400}}
                     placeholder='请输入'/>
            </div>
          </div>
        </div>
      </Dialog>

      <div className="search-box">
        <div className='search-item' style={{minWidth: 280}}>
          <div className="search-item-label">描述</div>
          <div className="search-item-right">
            <Input
              allowClear
              style={{width: 200}}
              placeholder="请输入描述"
              // 补充事件绑定 ↓
              value={this.state.searchDesc}
              onChange={e => {
                this.filters.desc = e.target.value;
                this.setState({
                  searchDesc: e.target.value
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

        <div className='accountGroup-btn'>
          <div className="search-query-btn" onClick={() => this.reload()}>查询</div>
          <div className="search-reset-btn" onClick={() => this.reset()}>重置</div>
        </div>
      </div>

      <div style={{paddingTop: 40}}>
        <div style={{overflow: 'hidden'}}>
          <div>
            <div className={"search-query-btn"} onClick={() => {
              this.openAdd()
            }}>新增
            </div>
            <div className={this.state.selectedRowKeys && this.state.selectedRowKeys.length > 0 ? "search-query-btn" : "search-reset-btn"} onClick={() => {
              this.edit()
            }}>修改
            </div>
            <div className={this.state.selectedRowKeys && this.state.selectedRowKeys.length > 0 ? "search-delete-btn" : "search-reset-btn"} onClick={() => {
              this.delete()
            }}>删除
            </div>
          </div>
          <div className="tableSelectedCount"
              ref={this.refSelectedCount}>{`已选${this.state.selectedRowKeys.length}项`}</div>
          <div className="tableContent" style={{height: this.state.tableContentHeight}}>
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
