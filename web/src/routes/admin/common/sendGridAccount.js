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
    action: `/api/account/importSendGrid`,
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
      pagination1: {
        pageSize: 10,
        current: 1,
        showTotal: (total, range) => `共 ${total} 条`,
        showSizeChanger: true,
        pageSizeOptions: ['10', '20', '30', '40', '50', '100', '200', '300', '400', '500']
      },
      pagination2: {
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

      allPlatforms: [],
      allGroups: [],
      platform: '',
      platform2: '',
      onlineStatus: '',
      createTimeRange: ['', ''],
      addVisible: false,
      files: [],
      usedVisiable: false,
      platform1: '',
      used: '',
      data1: [],
      accid: '',
      exportVisible: false,
      password: false,
      cookie: false,
      platform2: '',
      stock: 0,
      count: 0,

      userID: '',
      platform3: '',
      exportTimeRange: ['', ''],
      data2: [],
      recordVisible: false,
      phone: '',
      sendGridApiKey: '',
      email: '',
      email2: '',
      apiKey: '',
      editUser: null,
    };
    // 选中行的数据保存在selectedRows变量中
    // 之所以不放入state，因为这些变量是单向流动的，即Table控件产生这些数据，通过callback更新到变量this.selectedRows
    this.selectedRows = []
    // filters的写法保持与mongo filter的写法一致，避免与服务器、数据库直接的数据阻抗失衡
    // 例如： filters={username: 'foo'}
    // 因为json协议无法传输regex，所以字符类型的值，都会在服务器端转成regex，以提高匹配度，损失部分查询性能
    this.filters = {}
    this.filters1 = {},
      this.filters2 = {},
      // sorter的写法保持与mongo sort的写法一致，避免与服务器、数据库直接的数据阻抗失衡
      // 例如：sorter={createTime: -1}
      // 注意：Table控件仅支持单列排序，不支持多列同时排序
      this.sorter = {
        createTime: -1,
      },
      this.sorter2 = {
        createTime: -1,
      }
    // 提前写在constructor则Table首次加载时生效
    // 但是因为没有放到state中，所以除sorter外，都不建议提前设置，保持不受控状态是最好的
  }

  // 首次加载数据
  async componentWillMount() {
    await this.reload();
    await this.getAllPlatform();
    await this.getAllAccountGroup()
  }

  async getAllPlatform() {
    let res = await axios.post(`/api/consumer/platform/10000/1`, { filters: {}, sorter: { createTime: -1 } });
    if (res.data.code === 1) {
      this.setState({
        allPlatforms: res.data.data.data
      })
    }
  }

  async getAllAccountGroup() {
    let res = await axios.post(`/api/consumer/accountGroup/10000/1`, { filters: {}, sorter: { createTime: -1 } });
    if (res.data.code === 1) {
      this.setState({
        allGroups: res.data.data.data
      })
    }
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
    this.setState({ filters: {}, pagination: { ...this.state.pagination }, createTimeRange: ['', ''], email: '', platform: '', onlineStatus: '', platform2: '', phone: '', groupID: '' })
    this.load(this.state.pagination, this.filters, this.sorter)
  }

  async load(pagination, filters, sorter) {
    if (filters.onlineStatus === '') {
      delete filters.onlineStatus
    }
    if (filters.email === '') {
      delete filters.email
    }
    if (filters.platform === '') {
      delete filters.platform
    }
    if (filters.platform2 === '') {
      delete filters.platform2
    }
    // 这里的参数是格式好的，无需修改，无阻抗失衡，直接发给服务器
    this.setState({ loading: true })
    let res = await axios.post(`${baseUrl}/${pagination.pageSize}/${pagination.current}`, {
      filters: { ...filters, type: 'sendgrid' }, sorter
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

  async handleTableChange1(pagination, filters, sorter) {
    // 暂时不用Table的filter，不太好用
    this.load1(pagination, this.filters1, {})
  }

  async handleTableChange2(pagination, filters, sorter) {
    // 暂时不用Table的filter，不太好用
    this.load2(pagination, this.filters2, this.sorter2)
  }

  refSelectedCount = (ref) => {
    this.state.selectedCountRef = ref
    this.setState({ selectedCountRef: ref })
    this.handleResize()
  }

  deleteAccount(r) {
    let keys = [];
    if (r) {
      keys.push(r._id);
    } else {
      if (this.state.selectedRowKeys.length == 0) {
        message.error('请先选择数据');
        return
      }

      for (const row of this.selectedRows) {
        keys.push(row._id);
      }
    }
    if (keys.length == 0) return;
    DialogApi.warning({
      title: '确定要删除这些账号吗？',
      content: '上线的账号请先下线再进行删除',
      onOkTxt: '确认删除',
      onCancelTxt: '取消',
      onOk: async () => {
        this.setState({ loading: true });
        let result = await axios.post(`${baseUrl}/delete`, { ids: keys });
        if (result.data.code == 1) {
          message.success('操作成功');
          this.getAllPlatform()
          this.reload()
          this.setState({ loading: false });
          return;
        } else {
          message.error(result.data.message);
          this.setState({ loading: false });
          return;
        }
      },
      onCancel() {
      }
    })
  }

  retryCheck(r) {
    let keys = [];
    if (r) {
      keys.push(r._id);
    } else {
      if (this.state.selectedRowKeys.length == 0) {
        message.error('请先选择数据');
        return
      }

      for (const row of this.selectedRows) {
        keys.push(row._id);
      }
    }
    if (keys.length == 0) return;
    DialogApi.warning({
      title: '确定要重新检查这些账号吗？',
      content: '',
      onOkTxt: '确认',
      onCancelTxt: '取消',
      onOk: async () => {
        this.setState({ loading: true });
        let result = await axios.post(`${baseUrl}/retryCheck`, { ids: keys });
        if (result.data.code == 1) {
          message.success('操作成功');
          this.reload()
          this.setState({ loading: false });
          return;
        } else {
          message.error(result.data.message);
          this.setState({ loading: false });
          return;
        }
      },
      onCancel() {
      }
    })
  }


  setGroup(r) {
    let keys = [];
    if (r) {
      keys.push(r._id);
    } else {
      if (this.state.selectedRowKeys.length == 0) {
        message.error('请先选择数据');
        return
      }

      for (const row of this.selectedRows) {
        keys.push(row._id);
      }
    }
    if (keys.length == 0) return;
    let groupID = ''
    DialogApi.info({
      title: '请选择分组',
      content: <div style={{ display: 'flex', alignItems: 'center', gap: 10 }}>
        <div className="account-search-item-label">分组</div>
        <div className="account-search-item-right">
          <Select style={{ width: 200 }} placeholder="请选择内容" onChange={v => {
            groupID = v
          }}>
            {
              this.state.allGroups.map(e => <Option value={e._id}>{e.groupName}</Option>)
            }
          </Select>
        </div>
      </div>,
      onOkTxt: '确认',
      onCancelTxt: '取消',
      onOk: async () => {
        this.setState({ loading: true });
        let result = await axios.post(`${baseUrl}/setGroup`, { groupId: groupID, ids: keys, count: -1,  });
        if (result.data.code == 1) {
          message.success('操作成功');
          this.getAllPlatform()
          this.reload()
          this.setState({ loading: false });
          return;
        } else {
          message.error(result.data.message);
          this.setState({ loading: false });
          return;
        }
      },
      onCancel() {
      }
    })
  }

  openAdd () {
    this.setState({
      addVisible: true,
      editUser: null,
      email2: '',
      apiKey: '',
    })
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
        title: '邮箱',
        dataIndex: 'email',
        key: 'email',
        width: 191,
        ellipsis: true,
        render: (v, r) => {
          return v ? <div onClick={() => this.copyTxt(v, '邮箱已复制到剪切板')}>{v}</div> : '-'
        }
      },
      {
        title: 'API KEY',
        dataIndex: 'sendGridApiKey',
        key: 'sendGridApiKey',
        width: 191,
        ellipsis: true,
        render: (v, r) => {
          return v ? <div onClick={() => this.copyTxt(v, 'API KEY已复制到剪切板')}>{v}</div> : '-'
        }
      },
      {
        title: '分组',
        dataIndex: 'groupName',
        key: 'groupName',
        width: 80,
        ellipsis: true,
      },
      {
        title: '状态',
        dataIndex: 'onlineStatus',
        key: 'onlineStatus',
        width: 80,
        ellipsis: true,
        render: (v, r) => {
          return v === '1' ? '可用' : '不可用'
        }
      },
      {
        title: '当天已发邮件数',
        dataIndex: 'sendEmailNumByDay',
        key: 'sendEmailNumByDay',
        width: 140,
        ellipsis: true,
        render: (v, r) => {
          return (v || 0) + (r.limitSendEmail ? '(限制)' : '')
        }
      },
      {
        title: '累计发件数',
        dataIndex: 'sendEmailTotal',
        key: 'sendEmailTotal',
        width: 120,
        ellipsis: true,
      },
      {
        title: '创建时间',
        dataIndex: 'createTime',
        key: 'createTime',
        render: formatDate,
        width: 160,
        sorter: true,
        ellipsis: true,
      },
      {
        title: '被封时间',
        dataIndex: 'changeOnlineStatusTime',
        key: 'changeOnlineStatusTime',
        width: 160,
        ellipsis: true,
        render: (v, r) => {
          return r.onlineStatus !== '1' ? formatDate(v) : '--'
        }
      },
      {
        title: '错误信息',
        dataIndex: 'loginError',
        key: 'loginError',
        width: 160,
        ellipsis: true,
      },
      {
        title: '操作',
        dataIndex: 'oper',
        key: 'oper',
        width: 150,
        // fixed: 'right',
        render: (v, r) => {
          return (
            <div style={{
              display: 'flex',
              alignItems: 'center',
              gap: '12px', // 按钮间距
              padding: '8px 0 8px 8px', // 上下内边距8px，左侧内边距16px
              marginLeft: '8px' // 增加左侧外边距
            }}>
              <Button
                type="link"
                style={{
                  padding: 0,
                  minWidth: 'auto',
                }}
                onClick={() => this.deleteAccount(r)}
              >删除
              </Button>
            </div>
          )
        }
      },
    ];
    
    const customRowStyle = {
      padding: '50px' // 调整此值以改变行高
    };

    return (<MyTranslate><ExternalAccountFilter>
      <Breadcrumb>
        <Breadcrumb.BreadcrumbItem>资源管理</Breadcrumb.BreadcrumbItem>
        <Breadcrumb.BreadcrumbItem>API资源管理</Breadcrumb.BreadcrumbItem>
      </Breadcrumb>


      <Dialog
        header={this.state.editUser ? "编辑" : "新增"}
        width={854}
        height={566}
        visible={this.state.addVisible}
        placement='center'
        onConfirm={async () => {
          if (!this.state.email2) {
            message.error('请输入邮箱')
            return
          }
          if (!this.state.apiKey) {
            message.error('请输入API Key')
            return
          }
          let form = {
            email: this.state.email2,
            apiKey: this.state.apiKey,
          };
          if (this.state.editUser) {
            form._id = this.state.editUser._id
          }
          this.setState({ loading: true });
          // 过滤掉null
          let res = await axios.post(`${baseUrl}/saveSendGrid`, form)
          if (res.data.code == 1) {
            message.success('操作成功')
            this.setState({ addVisible: false })
            this.reload()
          } else {
            message.error(res.data.message)
            this.setState({ loading: false })
          }
        }} confirmLoading={this.state.loading}
        onCancel={() => {
          this.setState({ addVisible: false })
        }}
        onClose={() => {
          this.setState({ addVisible: false })
        }}
      >
        <div style={{ marginLeft: 127, marginTop: 26, color: 'rgba(0, 0, 0, 0.90)' }}>
          <div style={{ display: 'flex', marginBottom: 24 }}>
            <div style={{ width: 100, textAlign: 'right', marginRight: 16, lineHeight: '30px' }}>
              <span style={{ color: '#D54941' }}>*</span>邮箱
            </div>
            <div>
              <Input value={this.state.email2}
                onChange={(e) => this.setState({ email2: e.target.value })} style={{ width: 400 }}
                placeholder='请输入' />
            </div>
          </div>

          <div style={{ display: 'flex', marginBottom: 24 }}>
            <div style={{ width: 100, textAlign: 'right', marginRight: 16, lineHeight: '30px' }}>
              <span style={{ color: '#D54941' }}>*</span>Api Key
            </div>
            <div>
              <Input value={this.state.apiKey}
                onChange={(e) => this.setState({ apiKey: e.target.value })} style={{ width: 400 }}
                placeholder='请输入' />
            </div>
          </div>
        </div>
      </Dialog>

      <div className="account-search-box">
        <div className='account-search-item'>
          <div className="account-search-item-label">分组</div>
          <div className="account-search-item-right">
            <Select value={this.state.groupID} style={{ width: 200 }} placeholder="请选择内容" onChange={v => {
              this.setState({ groupID: v })
              this.filters['groupID'] = v
              this.reload()
            }}>
              {
                this.state.allGroups.map(e => <Option value={e._id}>{e.groupName}</Option>)
              }
            </Select>
          </div>
        </div>

        <div className='account-search-item' style={{ minWidth: 280 }}>
          <div className="account-search-item-label">邮箱</div>
          <div className="account-search-item-right">
            <Input
              allowClear
              style={{ width: 200 }}
              placeholder="请输入"
              value={this.state.email}
              onChange={e => {
                this.setState({ email: e.target.value })
                this.filters['email'] = e.target.value
              }
              }
              onPressEnter={e => {
                this.setState({ email: e.target.value })
                this.filters['email'] = e.target.value
                this.reload()
              }
              }
            />
          </div>
        </div>

        <div className='account-search-item' style={{ minWidth: 280 }}>
          <div className="account-search-item-label">API KEY</div>
          <div className="account-search-item-right">
            <Input
              allowClear
              style={{ width: 200 }}
              placeholder="请输入"
              value={this.state.sendGridApiKey}
              onChange={e => {
                this.setState({ sendGridApiKey: e.target.value })
                this.filters['sendGridApiKey'] = e.target.value
              }
              }
              onPressEnter={e => {
                this.setState({ sendGridApiKey: e.target.value })
                this.filters['sendGridApiKey'] = e.target.value
                this.reload()
              }
              }
            />
          </div>
        </div>

        <div className='account-search-item'>
          <div className="account-search-item-label">状态</div>
          <div className="account-search-item-right">
            <Select value={this.state.onlineStatus} style={{ width: 200 }} onChange={v => {
              this.setState({ onlineStatus: v })
              this.filters['onlineStatus'] = v
              this.reload()
            }}>
              <Option value="">全部</Option>
              <Option value="1">可用</Option>
              <Option value="0">不可用</Option>
            </Select>
          </div>
        </div>

        {/* <div className='account-search-item'>
          <div className="account-search-item-label">创建时间</div>
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
        </div> */}

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
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
            <div style={{ display: 'flex' }}>
              <div className={"search-query-btn"} onClick={() => {
                this.openAdd()
              }}>新增
              </div>

              <TUpload
                {...getUploadImageProps()}
                showUploadProgress={false}
                files={this.state.files}
                onChange={(info) => {
                  if (info.length > 0) {
                    message.success("导入成功")
                    this.reload()
                  }
                }}
                onRemove={() => this.setState({ files: [] })}
              >
                <div className={"search-query-btn"}>导入</div>
              </TUpload>

              <div
                className={this.state.selectedRowKeys && this.state.selectedRowKeys.length > 0 ? "search-delete-btn" : "search-reset-btn"}
                onClick={() => this.deleteAccount()}>批量删除
              </div>

              <div
                className={this.state.selectedRowKeys && this.state.selectedRowKeys.length > 0 ? "search-query-btn" : "search-reset-btn"}
                onClick={() => this.setGroup()}>设置分组
              </div>
            </div>
          </div>
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
