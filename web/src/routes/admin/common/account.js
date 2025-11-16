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
    action: `/api/consumer/res/uploadTxt/importAccount`,
    beforeUpload(file, fileList) {
      const isLt2M = file.size / 1024 / 1024 < 300;
      if (!isLt2M) {
        message.error('文件必须小于300MB!');
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
      type: 'mobile',
      limitSendEmail: '',
      importGroupId: '',
      openExportReceiveCode: '0',

      email2: '',
      clientId: '',
      clientSecret: '',
      refreshToken: '',
      emailFiles: [],
      jsonFile: [],
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
    this.setState({ filters: {}, pagination: { ...this.state.pagination }, createTimeRange: ['', ''], email: '', platform: '', onlineStatus: '', platform2: '', phone: '', groupID: '', limitSendEmail: '' })

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
      filters: { ...filters, type: {$ne: 'sendgrid'} }, sorter
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
      // if (this.state.selectedRowKeys.length == 0) {
      //   message.error('请先选择数据');
      //   return
      // }

      for (const row of this.selectedRows) {
        keys.push(row._id);
      }
    }
    // if (keys.length == 0) return;
    let groupID = ''
    let count = 0
    DialogApi.info({
      title: '请选择分组',
      content: <><div style={{ display: 'flex', alignItems: 'center', gap: 10 }}>
        <div className="account-search-item-label" style={{ width: 100, textAlign: 'right' }}>分组</div>
        <div className="account-search-item-right">
          <Select style={{ width: 200 }} placeholder="请选择内容" onChange={v => {
            groupID = v
          }}>
            {
              this.state.allGroups.map(e => <Option value={e._id}>{e.groupName}</Option>)
            }
          </Select>
        </div>
      </div>
      <div style={{ display: 'flex', alignItems: 'center', gap: 10, marginTop: 10 }}>
        <div className="account-search-item-label" style={{ width: 100, textAlign: 'right' }}>需要设置的数量</div>
        <div className="account-search-item-right">
          <Input
            allowClear
            style={{ width: 200 }}
            placeholder="请输入"
            defaultValue={count}
            onChange={e => {
              count = e.target.value
            }
            }
          />
        </div>
      </div>
      <div style={{fontSize: 12, color: '#999'}}>为0时默认全部</div>
      </>,
      onOkTxt: '确认',
      onCancelTxt: '取消',
      onOk: async () => {
        if (keys.length == 0 && Number(count) <= 0) {
          setTimeout(() => {
            DialogApi.warning({
            title: '确定要修改当前过滤条件下的所有账号的分组吗？',
            content: '',
            onOkTxt: '确认',
            onCancelTxt: '取消',
            onOk: async () => {
              this.setState({ loading: true });
              let params = {
                count: Number(count),
                groupId: groupID
              }
              if (keys.length == 0) {
                params.filters = this.filters
              } else {
                params.ids = keys
              }

              let result = await axios.post(`${baseUrl}/setGroup`, params);
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
            onCancel: () => {}
          })
          }, 100)

          return
        }
        this.setState({ loading: true });
        let params = {
          count: Number(count),
          groupId: groupID
        }
        if (keys.length == 0) {
          params.filters = this.filters
        } else {
          params.ids = keys
        }

        let result = await axios.post(`${baseUrl}/setGroup`, params);
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

  testV2(r) {
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
    DialogApi.info({
      title: '检查邮箱发件限制',
      content: '',
      onOkTxt: '确认',
      onCancelTxt: '取消',
      onOk: async () => {
        this.setState({ loading: true });
        let result = await axios.post(`/api/batchSendEmail/testV2`, { ids: keys });
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

  onUploadAccountAvatarChange = async (info) => {
    if (info.length > 0) {
      this.setState({ files: info });
      return
    }
  }

  reset1() {
    this.filters1 = {}
    this.state.pagination1.current = 1
    this.setState({ filters1: {}, pagination1: { ...this.state.pagination1 }, platform1: '', used: '' })
    this.load1(this.state.pagination1, this.filters1, {})
  }

  reload1() {
    this.state.pagination1.current = 1
    this.setState({ pagination1: this.state.pagination1 })
    this.load1(this.state.pagination1, this.filters1, {})
  }

  async load1(pagination1, filters1, sorter1) {
    // 这里的参数是格式好的，无需修改，无阻抗失衡，直接发给服务器
    this.setState({ loading: true })
    let res = await axios.post(`${baseUrl}/usedInfo/${pagination1.pageSize}/${pagination1.current}`, {
      ...filters1, accId: this.state.accid
    });
    pagination1.total = res.data.data.total;
    this.setState({
      loading: false,
      data1: res.data.data.data,
      pagination1: pagination1,
    });
    this.filters1 = filters1;
  }

  reset2() {
    this.filters2 = {}
    this.state.pagination2.current = 1
    this.setState({ filters2: {}, pagination2: { ...this.state.pagination2 }, platform3: '', userID: '', exportTimeRange: ['', ''] })
    this.load2(this.state.pagination2, this.filters2, this.sorter2)
  }

  reload2() {
    this.state.pagination2.current = 1
    this.setState({ pagination2: this.state.pagination2 })
    this.load2(this.state.pagination2, this.filters2, this.sorter2)
  }

  async load2(pagination2, filters2, sorter2) {
    // 这里的参数是格式好的，无需修改，无阻抗失衡，直接发给服务器
    if (filters2.userID === '') {
      delete filters2.userID
    }
    this.setState({ loading: true })
    let res = await axios.post(`${baseUrl}/exportAccount/${pagination2.pageSize}/${pagination2.current}`, {
      filters: filters2, sorter: sorter2,
    });
    pagination2.total = res.data.data.total;
    this.setState({
      loading: false,
      data2: res.data.data.data,
      pagination2: pagination2,
    });
    this.filters2 = filters2;
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
        render: (v, r) => {
          return v ? <div onClick={() => this.copyTxt(v, '邮箱已复制到剪切板')}>{v}</div> : '-'

        }
      },
      {
        title: '密码',
        dataIndex: 'password',
        key: 'password',
        width: 100,
        render: (v, r) => {
          return v ? <div onClick={() => this.copyTxt(v, '密码已复制到剪切板')}>{v}</div> : '-'
        }
      },
      {
        title: '导入格式',
        dataIndex: 'type',
        key: 'type',
        width: 100,
        render: (v, r) => {
          return v ? v === 'web' ? 'Gmail网页端' : v === 'mobile' ? 'Gmail移动端' : v === 'outlook_graph' ? 'Outlook Graph' : v === 'workspace_service_account' ? 'workspace服务账号' : v === 'workspace_second_hand_account' ? 'workspace二手账号' : v === 'yahoo' ? 'yahoo邮箱' : '-' : '-'
        }
      },
      {
        title: '分组',
        dataIndex: 'groupName',
        key: 'groupName',
        width: 80,
      },
      {
        title: '手机号',
        dataIndex: 'phone',
        key: 'phone',
        width: 130,
      },
      {
        title: '已接码/已导出/总平台数',
        dataIndex: 'used',
        key: 'used',
        width: 200,
        ellipsis: true,
        translateRender: true,
        sorter: true,
        render: (v, r) => {
          return r.openExportReceiveCode === '1' ? <>{r.realUsedPlatformIds ? r.realUsedPlatformIds.length : 0}/{v || 0}/{this.state.allPlatforms.length || '-'}</> : '不支持接码'
        }
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
        title: '是否已检测',
        dataIndex: 'isCheck',
        key: 'isCheck',
        width: 120,
        ellipsis: true,
        sorter: true,
        render: (v, r) => {
          return v === true ? '已检测' : '未检测'
        }
      },
      {
        title: '当天已发邮件数',
        dataIndex: 'sendEmailNumByDayDisplay',
        key: 'sendEmailNumByDayDisplay',
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
        title: '登录错误信息',
        dataIndex: 'loginError',
        key: 'loginError',
        width: 160,
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
        title: '创建时间',
        dataIndex: 'createTime',
        key: 'createTime',
        render: formatDate,
        width: 160,
        sorter: true,
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
                style={{ padding: 0, minWidth: 'auto' }}
                onClick={() => {
                  this.state.accid = r._id
                  this.setState({
                    usedVisiable: true,
                    accid: r._id
                  })
                  this.reload1()
                }}
              >查看
              </Button>

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
    const columns1 = [
      {
        title: '平台名称',
        dataIndex: 'platformName',
        key: 'platformName',
        width: 191,
      },
      {
        title: '状态',
        dataIndex: 'used',
        key: 'used',
        width: 140,
        render: (v) => {
          return v ? '已使用' : '未使用'
        }
      },
      {
        title: '是否接码',
        dataIndex: 'realUsed',
        key: 'realUsed',
        width: 140,
        render: (v) => {
          return v ? '已接码' : '未接码'
        }
      },
      {
        title: '使用时间',
        dataIndex: 'usedTime',
        key: 'usedTime',
        width: 191,
        render: (v, r) => {
          return r.used ? formatDate(v) : '--'
        },
      },
      {
        title: '接码时间',
        dataIndex: 'realUsedTime',
        key: 'realUsedTime',
        width: 191,
        render: (v, r) => {
          return r.used ? formatDate(v) : '--'
        },
      },
      {
        title: '操作',
        dataIndex: 'op',
        key: 'op',
        width: 191,
        render: (v, r) => {
          return <Button
            type="link"
            onClick={async () => {
              let res = await axios.post(`${baseUrl}/resetPlatform`, {
                accId: this.state.accid,
                platformId: r.platformId
              })
              if (res.data.code == 1) {
                message.success('操作成功')
                this.reload1()
              } else {
                message.error(res.data.message)
              }
            }}
          >重置
          </Button>
        }
      }
    ]


    const columns2 = [
      {
        title: '平台名称',
        dataIndex: 'platformName',
        key: 'platformName',
        width: 191,
      },
      {
        title: '导出数量',
        dataIndex: 'number',
        key: 'number',
        width: 191
      },
      {
        title: '操作人',
        dataIndex: 'userID',
        key: 'userID',
        width: 191
      },
      {
        title: '导出时间',
        dataIndex: 'createTime',
        key: 'createTime',
        width: 191,
        render: formatDate
      },
      {
        title: '操作',
        dataIndex: 'op',
        key: 'op',
        width: 191,
        render: (v, r) => {
          return r.filepath ? <Button
            type="link"
            onClick={async () => {
              window.open('/api/consumer/res/download/' + r.filepath, '_blank')
            }}
          >下载
          </Button> : ''
        }
      }
    ]
    const customRowStyle = {
      padding: '50px' // 调整此值以改变行高
    };

    return (<MyTranslate><ExternalAccountFilter>

      <Dialog
        header={"导入"}
        width={600}
        height={566}
        visible={this.state.addVisible}
        placement='center'
        onConfirm={async () => {
          if (this.state.type != "workspace_service_account" && this.state.type != "workspace_second_hand_account") {
            if (!this.state.files && this.state.files.length <= 0) {
              message.error('请上传文件')
              return
            }
            let form = {
              filepath: this.state.files[0].response.data.filepath,
              type: this.state.type,
              groupId: this.state.importGroupId,
              openExportReceiveCode: this.state.openExportReceiveCode
            }
            this.setState({ loading: true });
            // 过滤掉null
            let res = await axios.post(this.state.type === "yahoo" ? `${baseUrl}/saveYahooAccount` : `${baseUrl}/importAccount`, form)
            if (res.data.code == 1) {
              message.success('操作成功')
              this.setState({ addVisible: false })
              this.reload()
            } else {
              message.error(res.data.message)
              this.setState({ loading: false })
            }
          } else {
                    let form = {}
                    if (this.state.type === "workspace_service_account") {
                      if (!this.state.jsonFile || this.state.jsonFile.length <= 0) {
                        message.error('请输入身份凭证')
                        return
                      }
                      if (!this.state.files || this.state.files.length <= 0) {
                        message.error('请输入发件邮箱')
                        return
                      }
                      form = {
                        jsonFilePath: this.state.jsonFile.length > 0 ? this.state.jsonFile[0].response.data.filepath : '',
                        emailFilePath: this.state.files.length > 0 ? this.state.files[0].response.data.filepath : '',
                        groupId: this.state.importGroupId,
                        type: "workspace_service_account"
                      };
                    } else {
                      if (!this.state.email2) {
                        message.error('请输入邮箱')
                        return
                      }
                      if (!this.state.clientId) {
                        message.error('请输入clientId')
                        return
                      }
                      if (!this.state.clientSecret) {
                        message.error('请输入clientSecret')
                        return
                      }
                      if (!this.state.refreshToken) {
                        message.error('请输入refreshToken')
                        return
                      }
                      form = {
                        email: this.state.email2,
                        clientId: this.state.clientId,
                        clientSecret: this.state.clientSecret,
                        refreshToken: this.state.refreshToken,
                        groupId: this.state.importGroupId,
                        type: "workspace_second_hand_account"
                      };
                    }
                      if (this.state.editUser) {
                        form._id = this.state.editUser._id
                      }
                      this.setState({ loading: true });
                      let res = await axios.post(`${baseUrl}/saveWorkspaceAccount`, form)
                      if (res.data.code == 1) {
                        message.success('操作成功')
                        this.setState({ addVisible: false })
                        this.reload()
                      } else {
                        message.error(res.data.message)
                        this.setState({ loading: false })
                      }
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
          <div style={{ display: 'flex' }}>
            <div style={{ width: 84, textAlign: 'right', marginRight: 16, lineHeight: '30px' }}>导入格式</div>
            <div style={{ color: 'rgba(0, 0, 0, 0.40)' }}>
              <Select value={this.state.type} style={{ width: 200 }} placeholder="请选择内容" onChange={async v => {
                this.setState({ type: v })
              }}>
                <Option value='web'>Gmail网页端</Option>
                <Option value='mobile'>Gmail移动端</Option>
                <Option value='outlook_graph'>Outlook Graph</Option>
                <Option value='workspace_service_account'>workspace服务账号</Option>
                <Option value='workspace_second_hand_account'>workspace二手账号</Option>
                <Option value='yahoo'>yahoo邮箱</Option>
                <Option value='smtp'>Smtp</Option>
              </Select>
            </div>
          </div>

          <div style={{ display: this.state.type != "workspace_service_account" && this.state.type != "workspace_second_hand_account" ? 'flex': 'none', marginTop: 20 }}>
            <div style={{ width: 84, textAlign: 'right', marginRight: 16, lineHeight: '30px' }}>导入邮箱</div>
            <div style={{ color: 'rgba(0, 0, 0, 0.40)' }}>
              <TUpload
                {...getUploadImageProps()}
                files={this.state.files}
                onChange={this.onUploadAccountAvatarChange.bind(this)}
                onRemove={() => this.setState({ files: [] })}
              />
              {/* <InputImageMsg value={this.state.images} onUploadChange={this.onUploadChange.bind(this)} hidePlus={false} /> */}
              <div style={{ fontSize: 12 }}>请选择txt格式的文件上传，文件小于300M</div>
            </div>
          </div>

          <div style={{ display: 'flex', marginTop: 20 }}>
            <div style={{ width: 84, textAlign: 'right', marginRight: 16, lineHeight: '30px' }}>导入分组</div>
            <div style={{ color: 'rgba(0, 0, 0, 0.40)' }}>
              <Select value={this.state.importGroupId} style={{ width: 200 }} placeholder="请选择内容" onChange={async v => {
                this.setState({ importGroupId: v })
              }}>
                {this.state.allGroups.map(r => <Option key={r._id} value={r._id}>{r.groupName}</Option>)}
              </Select>
            </div>
          </div>

          <div style={{ display: this.state.type != "workspace_service_account" && this.state.type != "workspace_second_hand_account" ? 'flex': 'none', marginTop: 20 }}>
            <div style={{ width: 84, textAlign: 'right', marginRight: 16, lineHeight: '30px' }}>支持接码</div>
            <div style={{ color: 'rgba(0, 0, 0, 0.40)' }}>
              <Radio.Group onChange={(e) => this.setState({openExportReceiveCode: e.target.value})} value={this.state.openExportReceiveCode}>
                <Radio value={"1"}>支持</Radio>
                <Radio value={"0"}>不支持</Radio>
              </Radio.Group>
            </div>
          </div>
        <div style={{ display: this.state.type === "workspace_service_account" ? 'flex': 'none', marginTop: 20 }}>
                    <div style={{ width: 84, textAlign: 'right', marginRight: 16, lineHeight: '30px' }}>
                      <span style={{ color: '#D54941' }}>*</span>账号身份凭证
                    </div>
                    <div>
                      <TUpload
                        {...getUploadImageProps("json")}
                        showUploadProgress={false}
                        files={this.state.jsonFile}
                        onChange={(info) => {
                                          if (info.length > 0) {
                                            this.setState({ jsonFile: info });
                                            return
                                          }
                                        }}
                        onRemove={() => this.setState({ jsonFile: [] })}
                       >
                       </TUpload>
                    </div>
                  </div>

                  <div style={{ display: this.state.type === "workspace_service_account" ? 'flex': 'none', marginTop: 20 }}>
                    <div style={{ width: 84, textAlign: 'right', marginRight: 16, lineHeight: '30px' }}>
                      <span style={{ color: '#D54941' }}>*</span>发件邮箱列表
                    </div>
                    <div>
                      <TUpload
                        {...getUploadImageProps("csv")}
                        showUploadProgress={false}
                        files={this.state.files}
                        onChange={(info) => {
                                                          if (info.length > 0) {
                                                            this.setState({ files: info });
                                                            return
                                                          }
                                                        }}
                        onRemove={() => this.setState({ files: [] })}
                      >
                      </TUpload>
                    </div>
                  </div>

                  <div style={{ display: this.state.type === "workspace_second_hand_account" ? 'flex': 'none', marginTop: 20 }}>
                    <div style={{ width: 90, textAlign: 'right', marginRight: 16, lineHeight: '30px' }}>
                      <span style={{ color: '#D54941' }}>*</span>邮箱名
                    </div>
                    <div>
                      <Input value={this.state.email2}
                        onChange={(e) => this.setState({ email2: e.target.value })} style={{ width: 200 }}
                        placeholder='请输入' />
                    </div>
                  </div>

                  <div style={{ display: this.state.type === "workspace_second_hand_account" ? 'flex': 'none', marginTop: 20 }}>
                    <div style={{ width: 90, textAlign: 'right', marginRight: 16, lineHeight: '30px' }}>
                      <span style={{ color: '#D54941' }}>*</span>ClientId
                    </div>
                    <div>
                      <Input value={this.state.clientId}
                        onChange={(e) => this.setState({ clientId: e.target.value })} style={{ width: 200 }}
                        placeholder='请输入' />
                    </div>
                  </div>

                  <div style={{ display: this.state.type === "workspace_second_hand_account" ? 'flex': 'none', marginTop: 20 }}>
                    <div style={{ width: 90, textAlign: 'right', marginRight: 16, lineHeight: '30px' }}>
                      <span style={{ color: '#D54941' }}>*</span>ClientSecret
                    </div>
                    <div>
                      <Input value={this.state.clientSecret}
                        onChange={(e) => this.setState({ clientSecret: e.target.value })} style={{ width: 200 }}
                        placeholder='请输入' />
                    </div>
                  </div>

                  <div style={{ display: this.state.type === "workspace_second_hand_account" ? 'flex': 'none', marginTop: 20 }}>
                    <div style={{ width: 90, textAlign: 'right', marginRight: 16, lineHeight: '30px' }}>
                      <span style={{ color: '#D54941' }}>*</span>Refresh Token
                    </div>
                    <div>
                      <Input value={this.state.refreshToken}
                        onChange={(e) => this.setState({ refreshToken: e.target.value })} style={{ width: 200 }}
                        placeholder='请输入' />
                    </div>
                  </div>
        </div>
      </Dialog>


      <Dialog
        header={"导出"}
        width={854}
        height={566}
        visible={this.state.exportVisible}
        placement='center'
        onConfirm={async () => {
          if (!this.state.platform2) {
            message.error('请选择平台')
            return
          }
          if (this.state.stock <= 0) {
            message.error('库存不足')
            return
          }
          if (this.state.count > this.state.stock) {
            message.error('库存不足')
            return
          }
          let form = {
            platformId: this.state.platform2,
            count: this.state.count,
            ids: this.state.selectedRowKeys.length > 0 ? this.state.selectedRowKeys : undefined,
            exportType: 'origin' + (this.state.password ? ',password' : '') + (this.state.cookie ? ',cookie' : ''),
          }
          this.setState({ loading: true });
          // 过滤掉null
          let res = await axios.post(`${baseUrl}/exportAccount`, form)
          if (res.data.code == 1) {
            message.success('操作成功')
            this.setState({ exportVisible: false })
            this.reload()
          } else {
            message.error(res.data.message)
            this.setState({ loading: false })
          }
        }} confirmLoading={this.state.loading}
        onCancel={() => {
          this.setState({ exportVisible: false })
        }}
        onClose={() => {
          this.setState({ exportVisible: false })
        }}
      >
        <div style={{ marginLeft: 127, marginTop: 26, color: 'rgba(0, 0, 0, 0.90)' }}>
          <div style={{ display: 'flex', marginBottom: 24 }}>
            <div style={{ width: 100, textAlign: 'right', marginRight: 16, lineHeight: '30px' }}>
              <span style={{ color: '#D54941' }}>*</span>平台
            </div>
            <div>
              <Select value={this.state.platform2} style={{ width: 200 }} placeholder="请选择内容" onChange={async v => {
                this.setState({ platform2: v, loading: true })
                let res = await axios.post(`${baseUrl}/queryPlatformCount`, { platformId: v, ids: this.state.selectedRowKeys })
                if (res.data.code === 1) {
                  let update = { stock: res.data.data, loading: false }
                  if (this.state.selectedRowKeys.length > 0) {
                    update.count = res.data.data
                  }
                  this.setState(update)
                } else {
                  this.setState({ stock: 0, loading: false, count: 0 })
                }
              }}>
                {
                  this.state.allPlatforms.map(e => <Option value={e._id}>{e.name}</Option>)
                }
              </Select>
            </div>
          </div>

          <div style={{ display: 'flex', marginBottom: 24 }}>
            <div style={{ width: 100, textAlign: 'right', marginRight: 16, lineHeight: '30px' }}>
              库存数量
            </div>
            <div>
              <Input value={this.state.stock} disabled
                style={{ width: 400 }}
                placeholder='--' />
            </div>
          </div>

          <div style={{ display: 'flex', marginBottom: 24 }}>
            <div style={{ width: 100, textAlign: 'right', marginRight: 16, lineHeight: '30px' }}>
              <span style={{ color: '#D54941' }}>*</span>数量
            </div>
            <div>
              <Input value={this.state.count} disabled={this.state.selectedRowKeys.length > 0}
                onChange={(e) => this.setState({ count: e.target.value })} style={{ width: 400 }}
                placeholder='请输入' />
            </div>
          </div>

          <div style={{ display: 'flex', marginBottom: 24 }}>
            <div style={{ width: 100, textAlign: 'right', marginRight: 16, lineHeight: '30px' }}>
              其他数据
            </div>
            <div>
              <Checkbox style={{ marginRight: 10 }} checked={this.state.password} onChange={(e) => this.setState({ password: e })}>
                邮箱密码
              </Checkbox>
              <Checkbox checked={this.state.cookie} onChange={(e) => this.setState({ cookie: e })}>
                cookie
              </Checkbox>
            </div>
          </div>
        </div>
      </Dialog>


      <Dialog
        header={"使用详情"}
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
            <div className='search-item' style={{ minWidth: 280 }}>
              <div className="search-item-label">平台</div>
              <div className="search-item-right">
                <Input
                  allowClear
                  style={{ width: 200 }}
                  placeholder="请输入"
                  value={this.state.platform1}
                  onChange={e => {
                    this.setState({ platform1: e.target.value })
                    this.filters1['platformName'] = e.target.value
                  }
                  }
                  onPressEnter={e => {
                    this.setState({ platform1: e.target.value })
                    this.filters1['platformName'] = e.target.value
                    this.reload1()
                  }
                  }
                />
              </div>
            </div>

            <div className='search-item'>
              <div className="search-item-label">状态</div>
              <div className="search-item-right">
                <Select value={this.state.used} style={{ width: 200 }} placeholder="全部" onChange={v => {
                  this.setState({ used: v })
                  this.filters1['used'] = v
                  this.reload1()
                }}>
                  <Option value={true}>已使用</Option>
                  <Option value={false}>未使用</Option>
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
          // components={{
          //   body: {
          //     row: (props) => (
          //       <tr {...props} style={{ ...props.style, ...customRowStyle }} />
          //     ),
          //   },
          // }}
          />
        </div>
      </Dialog>


      <Dialog
        header={"导出记录"}
        width={1200}
        // height={700}
        visible={this.state.recordVisible}
        placement='center'
        confirmBtn={null}
        onCancel={() => {
          this.setState({ recordVisible: false })
        }}
        onClose={() => {
          this.setState({ recordVisible: false })
        }}
      >
        <div style={{ margin: 30, color: 'rgba(0, 0, 0, 0.90)' }}>
          <div className="account-search-box">
            <div className='account-search-item' style={{ minWidth: 280 }}>
              <div className="account-search-item-label">操作人</div>
              <div className="account-search-item-right">
                <Input
                  allowClear
                  style={{ width: 200 }}
                  placeholder="请输入"
                  value={this.state.userID}
                  onChange={e => {
                    this.setState({ userID: e.target.value })
                    this.filters2['userID'] = e.target.value
                  }
                  }
                  onPressEnter={e => {
                    this.setState({ userID: e.target.value })
                    this.filters2['userID'] = e.target.value
                    this.reload2()
                  }
                  }
                />
              </div>
            </div>
            <div className='account-search-item'>
              <div className="account-search-item-label">平台</div>
              <div className="account-search-item-right">
                <Input
                  allowClear
                  style={{ width: 200 }}
                  placeholder="请输入"
                  value={this.state.platform3}
                  onChange={e => {
                    this.setState({ platform3: e.target.value })
                    this.filters2['platformName'] = e.target.value
                  }
                  }
                  onPressEnter={e => {
                    this.setState({ platform3: e.target.value })
                    this.filters2['platformName'] = e.target.value
                    this.reload2()
                  }
                  }
                />
              </div>
            </div>

            <div className='account-search-item'>
              <div className="account-search-item-label">导出时间</div>
              <div className="account-search-item-right" style={{ width: 500 }}>
                <DateRangePicker
                  mode="date"
                  presetsPlacement="bottom"
                  enableTimePicker
                  value={this.state.exportTimeRange}
                  onChange={(e) => {
                    this.setState({
                      exportTimeRange: e
                    })
                    this.filters2['exportTimeRange'] = e
                    this.reload2()
                  }}
                />
              </div>
            </div>

            <div className='account-btn-no-expand' style={{ width: '136px' }}>
              <div style={{ display: 'flex', justifyContent: 'right', alignItems: 'center' }}>
                <div className="search-query-btn" onClick={() => this.reload2()}>查询</div>
                <div className="search-reset-btn" onClick={() => this.reset2()}>重置</div>
              </div>
            </div>
            <div style={{ clear: 'both' }}></div>
          </div>
          <div className="tableContent accountTableContent" style={{ marginTop: 20, height: 'auto' }}>
            <Table
              size="middle"
              tableLayout="fixed"
              pagination={this.state.pagination2} columns={columns2}
              dataSource={this.state.data2}
              loading={this.state.loading}
              onChange={this.handleTableChange2.bind(this)}
            />
          </div>

          <Pagination
            showJumper
            total={this.state.pagination2.total}
            current={this.state.pagination2.current}
            pageSize={this.state.pagination2.pageSize}
            onChange={this.handleTableChange2.bind(this)}
          // components={{
          //   body: {
          //     row: (props) => (
          //       <tr {...props} style={{ ...props.style, ...customRowStyle }} />
          //     ),
          //   },
          // }}
          />
        </div>
      </Dialog>

      <Breadcrumb>
        <Breadcrumb.BreadcrumbItem>资源管理</Breadcrumb.BreadcrumbItem>
        <Breadcrumb.BreadcrumbItem>邮箱资源管理</Breadcrumb.BreadcrumbItem>
      </Breadcrumb>

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
        <div className='account-search-item'>
          <div className="account-search-item-label">可用平台</div>
          <div className="account-search-item-right">
            <Select value={this.state.platform} style={{ width: 200 }} placeholder="请选择内容" onChange={v => {
              this.setState({ platform: v })
              this.filters['platform'] = v
              this.reload()
            }}>
              {
                this.state.allPlatforms.map(e => <Option value={e._id}>{e.name}</Option>)
              }
            </Select>
          </div>
        </div>

        <div className='account-search-item'>
          <div className="account-search-item-label">已使用平台</div>
          <div className="account-search-item-right">
            <Select value={this.state.platform2} style={{ width: 200 }} placeholder="请选择内容" onChange={v => {
              this.setState({ platform2: v })
              this.filters['platform2'] = v
              this.reload()
            }}>
              {
                this.state.allPlatforms.map(e => <Option value={e._id}>{e.name}</Option>)
              }
            </Select>
          </div>
        </div>

        <div className='account-search-item'>
          <div className="account-search-item-label">存在手机号</div>
          <div className="account-search-item-right">
            <Select value={this.state.phone} style={{ width: 200 }} onChange={v => {
              this.setState({ phone: v })
              if (v === "0") {
                this.filters['phone'] = { $eq: null }
              } else if (v === "1") {
                this.filters['phone'] = { $ne: null }
              } else {
                delete this.filters['phone']
              }
              this.reload()
            }}>
              <Option value="">全部</Option>
              <Option value="1">存在</Option>
              <Option value="0">不存在</Option>
            </Select>
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

        <div className='account-search-item'>
          <div className="account-search-item-label">发件限制</div>
          <div className="account-search-item-right">
            <Select value={this.state.limitSendEmail} style={{ width: 200 }} onChange={v => {
              this.setState({ limitSendEmail: v })
              if (v) {
                this.filters['limitSendEmail'] = v === "1" ? true : {$in: [null, false]}
              } else {
                delete this.filters['limitSendEmail']
              }

              this.reload()
            }}>
              <Option value="">全部</Option>
              <Option value="1">已限制</Option>
              <Option value="0">未限制</Option>
            </Select>
          </div>
        </div>

        <div className='account-search-item'>
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
        </div>

        <div className='account-btn-no-expand' style={{ width: '136px' }}>
          <div style={{ display: 'flex', justifyContent: 'right', alignItems: 'center' }}>
            <div className="search-query-btn" onClick={() => this.reload()}>查询</div>
            <div className="search-reset-btn" onClick={() => this.reset()}>重置</div>
          </div>
        </div>
        <div style={{ clear: 'both' }}></div>

        {/* <div className='accountGroup-btn'>
          <div className="search-query-btn" onClick={() => this.reload()}>查询</div>
          <div className="search-reset-btn" onClick={() => this.reset()}>重置</div>
        </div> */}
      </div>

      <div className="account-main-content">
        <div className="account-main-content-right">
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
            <div>
              <div className="search-query-btn" onClick={() => {
                this.setState({
                  addVisible: true,
                  files: []
                })
              }}>导入</div>
              <div
                className={this.state.selectedRowKeys && this.state.selectedRowKeys.length > 0 ? "search-delete-btn" : "search-reset-btn"}
                onClick={() => this.deleteAccount()}>批量删除
              </div>
              <div
                className={this.state.selectedRowKeys && this.state.selectedRowKeys.length > 0 ? "search-query-btn" : "search-reset-btn"}
                onClick={() => download(`${baseUrl}/export`, {
                  ...this.filters,
                  phone: this.state.phone
                })}>导出表格
              </div>

              <div
                className={this.state.selectedRowKeys && this.state.selectedRowKeys.length > 0 ? "search-query-btn" : "search-reset-btn"}
                onClick={() => this.retryCheck()}>重新检查
              </div>

              <div
                className={this.state.selectedRowKeys && this.state.selectedRowKeys.length > 0 ? "search-query-btn" : "search-reset-btn"}
                onClick={() => this.setGroup()}>设置分组
              </div>

              <div
                className={this.state.selectedRowKeys && this.state.selectedRowKeys.length > 0 ? "search-query-btn" : "search-reset-btn"}
                onClick={() => this.testV2()}>检查邮箱发件限制
              </div>
            </div>
            <div>
              <TButton theme="primary" style={{ marginRight: 8 }} onClick={() => {
                this.setState({
                  exportVisible: true,
                  platform2: '',
                  stock: 0,
                  count: 0,
                  password: false,
                  cookie: false,
                })
              }}>
                导出{this.state.selectedRowKeys.length <= 0 ? '' : `(${this.state.selectedRowKeys.length})`}
              </TButton>
              <TButton theme="primary" variant="outline" onClick={() => {
                this.reset2()
                this.setState({
                  recordVisible: true
                })
              }}>
                导出记录
              </TButton>
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
