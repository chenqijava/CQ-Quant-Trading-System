import React, { Component } from 'react'
import { BrowserRouter as Router, Route, Link } from 'react-router-dom'
import {
  DatePicker,
  Icon,
  Table,
  Divider,
  Upload,
  message,
  Input,
  Button,
  // Radio,
  // Switch,
  Modal,
  Select,
} from 'antd'
import axios from 'axios'
import moment from 'moment';
import userStatus from 'components/userStatus'
import { formatDate } from 'components/DateFormat'
import TimerWrapper from "../../../components/TimerWrapper";
import { connect } from 'dva'
import { download } from "components/postDownloadUtils"
import { FormattedMessage, useIntl, injectIntl } from 'react-intl'
import ExternalAccountFilter from "components/common/ExternalAccountFilter";
import MyTranslate from "components/common/MyTranslate";
import { Pagination, Breadcrumb, Dialog, Tag, Input as TInput, Upload as TUpload, Tooltip as TTooltip, Radio, Switch } from 'tdesign-react';
import DialogApi from '../common/dialog/DialogApi.js'

const { RangePicker } = DatePicker;
const Search = Input.Search;
const { Option } = Select;
const confirm = Modal.confirm;

// 针对当前页面的基础url
const baseUrl = '/api/consumer/user';

class MyComponent extends Component {
  constructor(props) {
    super(props);
    // data由服务器返回表格的数据
    // loading由客户端控制
    // pagination控制表格分页功能，其中数据总数pagination.total由服务器返回
    // 以上变量受控，因为需要传递给Table控件
    this.state = {
      data: [],
      data1: [],
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
      chargeSendEmailVisible: false,
      deductVisible: false,
      priceVisible: false,
      chargeValue: null,
      deductValue: null,
      code: '',

      editPwdVisible: false,
      editPwdValue: '',
      selectedCountRef: null,
      tableContentHeight: 0,
      scrollY: 0,
      addVisible: false,
      userID: '',
      searchUserID: '',
      status: '',
      password: '',
      newPassword: '',
      name: '',
      roleId: undefined,
      editUser: null,
      allRole: [],
      vipLevelVisible: false,
      vipLevel: 0, //普通会员
      vipLevels: [], //会员等级
      vipLevelMap: {},
      clearCreateTime: null,
      filters: {},
      data2: [],
      referVisible: false,
    };
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
    this.isAdmin = this.props.userID == 'admin';
  }

  // 首次加载数据
  async componentWillMount() {
    this.reload()
  }

  async loadRoles() {
    let res = await axios.post('/api/consumer/role/10000/1');
    this.setState({ allRole: res.data.data.data })
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


  async reload() {
    this.state.pagination.current = 1
    this.setState({ pagination: this.state.pagination })
    // 重新加载，一般是页面第一次加载的时候来一下
    this.load(this.state.pagination, this.filters, this.sorter)
    this.loadRoles()
  }

  async load(pagination, filters, sorter) {
    // 这里的参数是格式好的，无需修改，无阻抗失衡，直接发给服务器
    this.setState({ loading: true })

    if (filters.status === 'all') {
      delete filters.status
    }
    if (filters.userID === '') {
      delete filters.userID
    }

    let res = await axios.post(`${baseUrl}/${pagination.pageSize}/${pagination.current}`, { filters, sorter })
    pagination.total = res.data.data.total
    this.setState({ loading: false, data: res.data.data.data, pagination, selectedRowKeys: [] })
    this.selectedRows = []
    this.filters = filters
    this.sorter = sorter
  }

  async reset() {
    this.filters = {}
    this.setState({ filters: {}, userID: '', status: 'all' })
    this.load(this.state.pagination, this.filters, this.sorter)
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

  // 比较通用的回调
  async create() {
    this.props.history.push('card?oper=create')
  }

  async edit() {
    if (this.state.selectedRowKeys.length == 0) {
      message.error('请先选择数据');
      return
    }
    if (this.state.selectedRowKeys.length > 1) {
      message.error('只能选择一条数据');
      return
    }
    this.props.history.push('card?oper=edit&_id=' + this.state.selectedRowKeys[0])
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

  // async delete() {
  //   if (this.state.selectedRowKeys.length == 0) {
  //     message.error('请先选择数据');
  //     return
  //   }
  //   confirm({
  //     title: '确定要删除这些数据？',
  //     content: '账号上线或等待上线，用户无法删除',
  //     okText: '确定',
  //     okType: 'danger',
  //     cancelText: '取消',
  //     onOk: async() => {
  //       this.setState({loading: true});
  //       await axios.post(`/api/user/unbind`, this.state.selectedRowKeys);
  //       message.success('操作成功');
  //       this.reload()
  //     },
  //     onCancel() {
  //     }
  //   })
  // }

  async delete(r) {
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
      title: '确定要删除这些数据',
      content: '',
      onOkTxt: '确认删除',
      onCancelTxt: '取消',
      onOk: async () => {
        this.setState({ loading: true });
        let result = await axios.post(`/api/consumer/user/unbind`, keys);
        if (result.data.code != 1) {
          message.error(result.data.message);
          this.setState({ loading: false });
          return
        }
        message.success('操作成功');
        this.reload()
      },
      onCancel() {
      }
    })
  }

  async disabled() {
    if (this.state.selectedRowKeys.length == 0) {
      message.error('请先选择数据')
      return
    }
    confirm({
      title: '确定要禁用这些数据？',
      content: '',
      okText: '确定',
      okType: 'danger',
      cancelText: '取消',
      onOk: async () => {
        this.setState({ loading: true });
        let res = await axios.post(`${baseUrl}/disabled`, this.state.selectedRowKeys)
        if (res.data.code != 1) {
          message.error(res.data.message);
          this.setState({ loading: false });
          return
        }
        message.success('操作成功');
        this.reload()
      },
      onCancel() {
      }
    })
  }

  async enable() {
    if (this.state.selectedRowKeys.length == 0) {
      message.error('请先选择数据')
      return
    }
    confirm({
      title: '确定要启用这些数据？',
      content: '',
      okText: '确定',
      okType: 'danger',
      cancelText: '取消',
      onOk: async () => {
        this.setState({ loading: true });
        let res = await axios.post(`${baseUrl}/enable`, this.state.selectedRowKeys)
        if (res.data.code != 1) {
          message.error(res.data.message);
          this.setState({ loading: false });
          return
        }
        message.success('操作成功');
        this.reload()
      },
      onCancel() {
      }
    })
  }

  priceOpen = async (r) => {
    if (r) {
      const res = await axios.post(`${baseUrl}/queryPrice`, { id: r.userID })
      this.setState({ priceVisible: true, data1: res.data.data, code: '' })
    } else {
      if (this.state.selectedRowKeys.length == 0) {
        message.error('请先选择数据');
        return
      }
      if (this.state.selectedRowKeys.length != 1) {
        message.error('请选择一条数据');
        return
      }
      const res = await axios.post(`${baseUrl}/queryPrice`, { id: this.selectedRows[0].userID })
      this.setState({ priceVisible: true, data1: res.data.data, code: '' })
    }
  }

  chargeOpen = async () => {
    if (this.state.selectedRowKeys.length == 0) {
      message.error('请先选择数据');
      return
    }

    this.setState({ chargeVisible: true, chargeValue: null, code: '' })
  };

  chargeSendEmailOpen = async () => {
    if (this.state.selectedRowKeys.length == 0) {
      message.error('请先选择数据');
      return
    }

    this.setState({ chargeSendEmailVisible: true, chargeValue: null, code: '' })
  }

  deductOpen = async () => {
    if (this.state.selectedRowKeys.length == 0) {
      message.error('请先选择数据');
      return
    }

    this.setState({ deductVisible: true, chargeValue: null, code: '' })
  };

  charge = async () => {
    if (this.state.selectedRowKeys.length == 0) {
      message.error('请先选择数据');
      return
    }

    this.setState({ loading: true });
    let res = await axios.post(`${baseUrl}/charge`, { ids: this.state.selectedRowKeys, chargeValue: this.state.chargeValue }, {
      headers: {
        'auth-code': this.state.code,
      }
    });
    if (res.data.code != 1) {
      message.error(res.data.message);
      this.setState({ loading: false });
      return
    } else {
      message.success('操作成功');
      this.setState({ chargeVisible: false });
      this.reload()
      this.props.dispatch({ type: 'user/stat' });
    }
    this.setState({ chargeValue: null, code: '' });
  };

  chargeSendEmail = async () => {
    if (this.state.selectedRowKeys.length == 0) {
      message.error('请先选择数据');
      return
    }

    this.setState({ loading: true });
    let res = await axios.post(`${baseUrl}/chargeSendEmail`, { ids: this.state.selectedRowKeys, chargeValue: this.state.chargeValue }, {
      headers: {
        'auth-code': this.state.code,
      }
    });
    if (res.data.code != 1) {
      message.error(res.data.message);
      this.setState({ loading: false });
      return
    } else {
      message.success('操作成功');
      this.setState({ chargeSendEmailVisible: false });
      this.reload()
    }
    this.setState({ chargeValue: null, code: '' });
  };

  deduct = async () => {
    if (this.state.selectedRowKeys.length == 0) {
      message.error('请先选择数据');
      return
    }

    this.setState({ loading: true });
    let res = await axios.post(`${baseUrl}/deduct`, { ids: this.state.selectedRowKeys, chargeValue: this.state.deductValue }, {
      headers: {
        'auth-code': this.state.code,
      }
    });
    if (res.data.code != 1) {
      message.error(res.data.message);
      this.setState({ loading: false });
      return
    } else {
      message.success('操作成功');
      this.setState({ deductVisible: false });
      this.reload()
      this.props.dispatch({ type: 'user/stat' });
    }
    this.setState({ deductValue: null, code: '' });
  };

  showEditPwd = async () => {
    if (this.state.selectedRowKeys.length == 0) {
      message.error('请先选择数据');
      return
    }
    this.setState({ editPwdVisible: true });
  };

  editPwd = async () => {
    if (this.state.selectedRowKeys.length == 0) {
      message.error('请先选择数据');
      return
    }

    this.setState({ loading: true });
    let result = await axios.post(`${baseUrl}/editPassword`, { ids: this.state.selectedRowKeys, password: this.state.editPwdValue });
    if (result.data.code != 1) {
      message.error(result.data.message);
      this.setState({ loading: false });
      return
    }
    message.success('操作成功');
    this.setState({ editPwdVisible: false });
    this.reload()
  };

  forbidden = async () => {
    if (this.state.selectedRowKeys.length == 0) {
      message.error('请先选择数据');
      return
    }

    this.setState({ loading: true });
    let res = await axios.post(`${baseUrl}/disabled`, { ids: this.state.selectedRowKeys });
    if (res.data.code != 1) {
      message.error(res.data.message);
      this.setState({ loading: false });
      return
    } else {
      message.success('操作成功');
      this.reload()
    }
  };

  unforbidden = async () => {
    if (this.state.selectedRowKeys.length == 0) {
      message.error('请先选择数据');
      return
    }

    this.setState({ loading: true });
    let res = await axios.post(`${baseUrl}/enable`, { ids: this.state.selectedRowKeys });
    if (res.data.code != 1) {
      message.error(res.data.message);
      this.setState({ loading: false });
      return
    } else {
      message.success('操作成功');
      this.reload()
    }
  };

  setVipLevel = async () => {
    if (this.state.selectedRowKeys.length == 0) {
      message.error('请先选择数据');
      return
    }

    this.setState({ vipLevelVisible: true });
  }

  vipLevel = async () => {
    if (this.state.selectedRowKeys.length == 0) {
      message.error('请先选择数据');
      return
    }

    DialogApi.warning({
      title: '手动修改后，该用户无法根据累计筛活消费金额自动提升会员等级，确定要修改等级么？',
      content: '',
      onOkTxt: '确定',
      onCancelTxt: '取消',
      onOk: async () => {
        this.setState({ loading: true });
        let result = await axios.post(`${baseUrl}/setVipLevel`, { ids: this.state.selectedRowKeys, vipLevel: this.state.vipLevel });
        if (result.data.code == 1) {
          message.success('操作成功');
          this.setState({ vipLevelVisible: false, vipLevel: 0 });
          this.reload()
        } else {
          message.error(result.data.message);
          this.setState({ loading: false });
          return
        }
      },
      onCancel() {
      }
    })
  }

  loginUser = async (r) => {
    if (!r) {
      if (this.state.selectedRowKeys.length == 0) {
        message.error('请先选择数据');
        return
      }

      if (this.selectedRows[0].status != 'enable') {
        console.log(this.selectedRows);
        message.error('账号状态不可用');
        return
      }
    } else {
      if (r.status != 'enable') {
        console.log(r);
        message.error('账号状态不可用');
        return
      }
    }

    this.setState({ loading: true });
    let result = await axios.post(`${baseUrl}/esoityiuahei4`, { id: r._id });
    this.setState({ loading: false });
    if (result.data.code === 1) {
      window.location.href = '/'
    } else {
      message.error(result.data.message);
    }
  };

  setUseSocks5 = async (userID, e) => {
    this.setState({ loading: true });
    let result = await axios.post(`${baseUrl}/setUseSocks5`, { userID, socks5Use: e });
    this.setState({ loading: false });

    if (result.data.code === 1) {
      this.reload();
    } else {
      message.error(result.data.message);
    }
  };

  async setUserFlag(record, type, enable) {
    this.setState({ loading: true });
    let result = await axios.post(`${baseUrl}/set/${type}/${record.userID}`, { enable });
    this.setState({ loading: false });

    if (result.data.code === 1) {
      this.reload();
    } else {
      message.error(result.data.message);
    }
  }

  packUnbindAccount = async (filters) => {
    if (!this.selectDates || this.selectDates.length < 2) {
      message.error(`请先选择导出时间范围`);
      return
    }
    let data = {
      filters,
      packBeginDate: this.selectDates[0].toISOString(),
      packEndDate: this.selectDates[1].toISOString(),
    };
    let result = { data: {} };
    try {
      await this.setState({ loading: true });
      result = await axios.post(`/api/consumer/admin/unbindAccountPack/pack`, data);
    } catch (e) {
      console.log(e);
      throw e
    } finally {
      let state = { loading: false };
      if (result.data.code) {
        message.success('操作成功');
        if (result.data.fileName) window.open(`/api/consumer/res/${result.data.fileName}`);
      } else {
        message.error(`操作失败,${result.data.message || ''}`);
      }
      await this.setState(state);
    }
  };

  async downloadUserSocks5(filters) {
    download('/api/consumer/socks5/download', { filters: JSON.stringify(filters) })
  }


  refSelectedCount = (ref) => {
    this.state.selectedCountRef = ref
    this.setState({ selectedCountRef: ref })
    this.handleResize()
  }

  openAdd() {
    this.setState({ addVisible: true, userID: '', password: '', name: '', roleId: undefined, editUser: null })
  }

  closeRecharge() {
    if (this.state.selectedRowKeys.length == 0) {
      message.error('请先选择数据');
      return
    }
    let code = ''
    DialogApi.info({
      title: '输入验证码',
      content: <div>
        验证码：
        <Input onChange={(e) => {
          code = e.target.value
        }}
          type="text" />
      </div>,
      onOk: () => {
        axios.post(`${baseUrl}/closeRecharge`, { ids: this.state.selectedRowKeys }, {
          headers: {
            'auth-code': code,
          }
        }).then(res => {
          if (res.data.code == 1) {
            message.success('修改成功');
            this.reload()
          } else {
            message.error(res.data.message)
          }
        })
      }
    })
  }

  referOpen = async (r) => {
    if (r) {
      const res = await axios.post(`${baseUrl}/queryRefer`, { id: r.userID })
      this.setState({ referVisible: true, data2: res.data.data })
    }
  }

  openRecharge() {
    if (this.state.selectedRowKeys.length == 0) {
      message.error('请先选择数据');
      return
    }
    let code = ''
    DialogApi.info({
      title: '输入验证码',
      content: <div>
        验证码：
        <Input onChange={(e) => {
          code = e.target.value
        }}
          type="text" />
      </div>,
      onOk: () => {
        axios.post(`${baseUrl}/openRecharge`, { ids: this.state.selectedRowKeys }, {
          headers: {
            'auth-code': code,
          }
        }).then(res => {
          if (res.data.code == 1) {
            message.success('修改成功');
            this.reload()
          } else {
            message.error(res.data.message)
          }
        })
      }
    })
  }

  // 把columns放到render中，虽然损失部分性能，但是能方便参数中的匿名回调获取实例状态
  render() {
    const columns = [
      {
        title: '登录ID',
        dataIndex: 'userID',
        key: 'userID',
        width: 104,
        ellipsis: true,
        fixed: 'left',
      }, {
        title: '名称',
        dataIndex: 'name',
        key: 'name',
        width: 120,
        ellipsis: true,
      }, {
        title: '角色',
        dataIndex: 'roleName',
        key: 'roleName',
        width: 94,
        ellipsis: true,
      },
      {
        title: '账户余额',
        dataIndex: 'balance',
        key: 'balance',
        width: 120,
        ellipsis: true,
      },
      {
        title: '账户冻结余额',
        dataIndex: 'frozenBalance',
        key: 'frozenBalance',
        width: 120,
        ellipsis: true,
      },
      {
        title: '总充值金额',
        dataIndex: 'totalRechargeBalance',
        key: 'totalRechargeBalance',
        width: 120,
        ellipsis: true,
      },
      {
        title: '总赠送金额',
        dataIndex: 'totalSendBalance',
        key: 'totalSendBalance',
        width: 120,
        ellipsis: true,
      },
      {
        title: '状态',
        dataIndex: 'status',
        key: 'status',
        width: 82,
        ellipsis: true,
        render: (v, r) => {
          return <Tag color={v == 'enable' ? 'green' : 'red'}>{v == 'enable' ? '正常' : '冻结'}</Tag>
        }
      }, {
        title: '创建者',
        dataIndex: 'createUserID',
        key: 'createUserID',
        width: 100,
        ellipsis: true,
      }, {
        title: '创建时间',
        dataIndex: 'createTime',
        key: 'createTime',
        width: 192,
        ellipsis: true,
        render: formatDate
      }, {
        title: '上次登录时间',
        dataIndex: 'lastLoginTime',
        key: 'lastLoginTime',
        width: 191,
        ellipsis: true,
        render: formatDate
      }, {
        title: '操作',
        dataIndex: 'op',
        key: 'op',
        width: 250,
        ellipsis: true,
        fixed: 'right',
        render: (v, r) => {
          return (<div>
            {this.props.userID == 'admin' ? <Button type="link" onClick={() => { this.loginUser(r) }}>切用户</Button> : ''}
            <Button type="link" onClick={async () => {
              if (this.props.userID != r.createUserID) {
                message.error('不能修改不是本账号创建的用户');
                return
              }
              this.setState({ editUser: r, addVisible: true, onlyRead: false, name: r.name, userID: r.userID, password: r.password, newPassword: r.newPassword, roleId: r.role })
            }}>编辑</Button>
            {this.props.userID == 'admin' ? <Button type="link" onClick={() => { this.delete(r) }}>删除</Button> : ''}
          </div>)
        }
      },
    ];

    const columns1 = [
      {
        title: '平台',
        dataIndex: 'platformName',
        key: 'platformName',
        width: 150,
        ellipsis: true,
      },
      {
        title: '默认价格',
        dataIndex: 'defaultPrice',
        key: 'defaultPrice',
        width: 150,
        ellipsis: true,
      },
    ]

    const columns2 = [
      {
        title: '登录ID',
        dataIndex: 'userID',
        key: 'userID',
        width: 150,
        ellipsis: true,
      },
      {
        title: '名称',
        dataIndex: 'userName',
        key: 'userName',
        width: 150,
        ellipsis: true,
      },
      {
        title: '代理层级',
        dataIndex: 'level',
        key: 'level',
        width: 150,
        ellipsis: true,
        render: (v) => {
          return "下" + v + "级"
        }
      },
      {
        title: '贡献佣金',
        dataIndex: 'amount',
        key: 'amount',
        width: 180,
        ellipsis: true,
      },
      {
        title: '创建时间',
        dataIndex: 'createTime',
        key: 'createTime',
        width: 180,
        ellipsis: true,
        render: formatDate
      }
    ]

    let tableSelectData = { selectedRowKeys: this.state.selectedRowKeys, filters: this.filters, };


    return (<MyTranslate><ExternalAccountFilter>
      <Breadcrumb>
        <Breadcrumb.BreadcrumbItem>系统设置</Breadcrumb.BreadcrumbItem>
        <Breadcrumb.BreadcrumbItem>用户列表</Breadcrumb.BreadcrumbItem>
      </Breadcrumb>


      <div className="search-box">
        <div className='search-item' style={{ minWidth: 280 }}>
          <div className="search-item-label">登录ID</div>
          <div className="search-item-right">
            <Input
              allowClear
              style={{ width: 200 }}
              placeholder="请输入内容"
              value={this.state.searchUserID}
              // 补充事件绑定 ↓
              onChange={e => {
                this.setState({ searchUserID: e.target.value })
                this.filters['userID'] = e.target.value
              }
              }
              onPressEnter={e => {
                this.setState({ searchUserID: e.target.value })
                this.filters['userID'] = e.target.value
                this.reload()
              }
              }

            />
          </div>
        </div>
        <div className='search-item'>
          <div className="search-item-label">状态</div>
          <div className="search-item-right">
            <Select value={this.state.status} style={{ width: 200 }} onChange={v => {
              this.setState({ status: v })
              this.filters['status'] = v
              this.reload()
            }}>
              <Option value="all">全部</Option>
              <Option value="enable">正常</Option>
              <Option value="disabled">冻结</Option>
              {/*{accountStatus.map(ws => {*/}
              {/*  return <Option value={ws.value}>{ws.label}</Option>*/}
              {/*})}*/}
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
          <div>
            <div className={"search-query-btn"} onClick={() => {
              this.openAdd()
            }}>新增
            </div>
            {/*<div className={this.state.selectedRowKeys && this.state.selectedRowKeys.length > 0 ? "search-query-btn" : "search-reset-btn"} onClick={this.showEditPwd.bind(this)}>修改密码</div>*/}
            <div
              className={this.state.selectedRowKeys && this.state.selectedRowKeys.length > 0 ? "search-query-btn" : "search-reset-btn"}
              onClick={this.chargeOpen.bind(this)}>充值
            </div>

            <div
              className={this.state.selectedRowKeys && this.state.selectedRowKeys.length > 0 ? "search-query-btn" : "search-reset-btn"}
              onClick={this.deductOpen.bind(this)}>扣款
            </div>

            <div
              className={this.state.selectedRowKeys && this.state.selectedRowKeys.length > 0 ? "search-query-btn" : "search-reset-btn"}
              onClick={this.forbidden.bind(this)}>冻结
            </div>
            <div
              className={this.state.selectedRowKeys && this.state.selectedRowKeys.length > 0 ? "search-query-btn" : "search-reset-btn"}
              onClick={this.unforbidden.bind(this)}>解冻
            </div>

            {this.props.userID == 'admin' ? <div
              className={this.state.selectedRowKeys && this.state.selectedRowKeys.length > 0 ? "search-delete-btn" : "search-reset-btn"}
              onClick={() => {
                this.delete()
              }}>批量删除</div> : ''}


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

      <Dialog
        header={this.state.editUser ? "编辑" : "新增"}
        width={854}
        height={566}
        visible={this.state.addVisible}
        onConfirm={async () => {
          if (!this.state.userID) {
            message.error('请输入登录ID')
            return
          }
          if (!this.state.editUser) {
            if (!this.state.password && !this.state.newPassword) {
              message.error('请输入密码')
              return
            }
          }
          if (!this.state.name) {
            message.error('请输入名称')
            return
          }
          if (!this.state.roleId) {
            message.error('请选择角色')
            return
          }
          let form = {
            ...this.state.editUser,
            userID: this.state.userID,
            password: this.state.password,
            name: this.state.name,
            role: this.state.roleId,
            newPassword: this.state.newPassword
          };
          this.setState({ loading: true });
          // 过滤掉null
          let res = await axios.post(`${baseUrl}/card/save`, form)
          if (res.data.code == 1) {
            message.success('操作成功')
            this.setState({ addVisible: false, newPassword: null })
            this.reload()
          } else {
            message.error(res.data.message)
            this.setState({ loading: false })
          }
        }} confirmLoading={this.state.loading}
        onCancel={() => {
          this.setState({ addVisible: false, newPassword: null })
        }}
        onClose={() => {
          this.setState({ addVisible: false, newPassword: null })
        }}
      >
        <div style={{ marginLeft: 127, marginTop: 26, color: 'rgba(0, 0, 0, 0.90)' }}>
          <div style={{ display: 'flex', marginBottom: 24 }}>
            <div
              style={{ width: 50, textAlign: 'right', marginRight: 16, lineHeight: '30px' }}>{this.state.editUser ? '' :
                <span style={{ color: '#D54941' }}>*</span>}登录ID
            </div>
            <div>
              <Input value={this.state.userID} disabled={!!this.state.editUser}
                onChange={(e) => this.setState({ userID: e.target.value })} style={{ width: 400 }}
                placeholder='请输入' />
            </div>
          </div>

          <div style={{ display: 'flex', marginBottom: 24 }}>
            <div
              style={{ width: 50, textAlign: 'right', marginRight: 16, lineHeight: '30px' }}>{this.state.editUser ? '' :
                <span style={{ color: '#D54941' }}>*</span>}密码
            </div>
            {this.state.editUser ? <div>
              <Input value={this.state.newPassword}
                onChange={(e) => this.setState({ newPassword: e.target.value })} style={{ width: 400 }}
                placeholder='请输入新密码' />
            </div> : <div>
              <Input value={this.state.password}
                onChange={(e) => this.setState({ password: e.target.value })} style={{ width: 400 }}
                placeholder='请输入密码' />
            </div>}
          </div>

          <div style={{ display: 'flex', marginBottom: 24 }}>
            <div style={{ width: 50, textAlign: 'right', marginRight: 16, lineHeight: '30px' }}><span
              style={{ color: '#D54941' }}>*</span>名称
            </div>
            <div>
              <Input value={this.state.name} onChange={(e) => this.setState({ name: e.target.value })}
                style={{ width: 400 }} placeholder='请输入' />
            </div>
          </div>

          <div style={{ display: 'flex', marginBottom: 24 }}>
            <div style={{ width: 50, textAlign: 'right', marginRight: 16, lineHeight: '30px' }}><span
              style={{ color: '#D54941' }}>*</span>角色
            </div>
            <div>
              <Select value={this.state.roleId} style={{ width: 400 }} placeholder="请选择"
                onChange={v => this.setState({ roleId: v })}>
                {this.state.allRole.map(ws => {
                  return <Option value={ws._id}>{ws.name}</Option>
                })}
              </Select>
            </div>
          </div>
        </div>
      </Dialog>

      <Dialog
        header="输入新密码"
        visible={this.state.editPwdVisible}
        onConfirm={this.editPwd.bind(this)} confirmLoading={this.state.loading}
        onCancel={() => {
          this.setState({ editPwdVisible: false })
        }}
        onClose={() => {
          this.setState({ editPwdVisible: false })
        }}
      >
        <Input onChange={(e) => {
          this.setState({ editPwdValue: e.target.value })
        }} value={this.state.editPwdValue}
          type="text" />
      </Dialog>

      <Dialog
        header="输入充值金额"
        visible={this.state.chargeVisible}
        onConfirm={this.charge} confirmLoading={this.state.loading}
        onCancel={() => {
          this.setState({ chargeVisible: false, chargeValue: null, code: '' })
        }}
        onClose={() => {
          this.setState({ chargeVisible: false, chargeValue: null, code: '' })
        }}
      >
        <div>
          金额：
          <Input onChange={(e) => {
            this.setState({ chargeValue: e.target.value })
          }} value={this.state.chargeValue}
            type="text" />
        </div>
        <div>
          验证码：
          <Input onChange={(e) => {
            this.setState({ code: e.target.value })
          }} value={this.state.code}
            type="text" />
        </div>
      </Dialog>

      <Dialog
        header="输入充值群发额度"
        visible={this.state.chargeSendEmailVisible}
        onConfirm={this.chargeSendEmail} confirmLoading={this.state.loading}
        onCancel={() => {
          this.setState({ chargeSendEmailVisible: false, chargeValue: null, code: '' })
        }}
        onClose={() => {
          this.setState({ chargeSendEmailVisible: false, chargeValue: null, code: '' })
        }}
      >
        <div>
          金额：
          <Input onChange={(e) => {
            this.setState({ chargeValue: e.target.value })
          }} value={this.state.chargeValue}
            type="text" />
        </div>
        <div>
          验证码：
          <Input onChange={(e) => {
            this.setState({ code: e.target.value })
          }} value={this.state.code}
            type="text" />
        </div>
      </Dialog>

      <Dialog
        header="输入扣减金额"
        visible={this.state.deductVisible}
        onConfirm={this.deduct} confirmLoading={this.state.loading}
        onCancel={() => {
          this.setState({ deductVisible: false, deductValue: null, code: '' })
        }}
        onClose={() => {
          this.setState({ deductVisible: false, deductValue: null, code: '' })
        }}
      >
        <div>
          金额：
          <Input onChange={(e) => {
            this.setState({ deductValue: e.target.value })
          }} value={this.state.deductValue}
            type="text" />
        </div>
        <div>
          验证码：
          <Input onChange={(e) => {
            this.setState({ code: e.target.value })
          }} value={this.state.code}
            type="text" />
        </div>
      </Dialog>


      <Dialog
        header={"编辑价格"}
        width={800}
        // height={700}
        visible={this.state.priceVisible}
        placement='center'
        onConfirm={() => {
          let code = ''
          DialogApi.info({
            title: '输入验证码',
            content: <div>
              验证码：
              <Input onChange={(e) => {
                code = e.target.value
              }}
                type="text" />
            </div>,
            onOk: () => {
              this.state.data1.forEach(item => {
                if (item.price && !isNaN(Number(item.price))) {
                  item.price = Number(item.price)
                } else {
                  item.price = null
                }
              })

              axios.post(`${baseUrl}/updatePrice`, this.state.data1, {
                headers: {
                  'auth-code': code,
                }
              }).then(res => {
                if (res.data.code == 1) {
                  message.success('修改成功');
                  this.setState({ priceVisible: false, code: '' })
                } else {
                  message.error(res.data.message)
                }
              })
            }
          })

        }}
        onCancel={() => {
          this.setState({ priceVisible: false })
        }}
        onClose={() => {
          this.setState({ priceVisible: false })
        }}
      >
        <div style={{ margin: 30, color: 'rgba(0, 0, 0, 0.90)' }}>
          <div className="tableContent accountTableContent" style={{ marginTop: 20, height: 'auto' }}>
            <Table
              size="middle"
              tableLayout="fixed"
              columns={columns1}
              dataSource={this.state.data1}
              loading={this.state.loading}
              pagination={false}
            // onChange={this.handleTableChange1.bind(this)}
            />
          </div>
        </div>
      </Dialog>

      <Dialog
        header={"查询推荐"}
        width={1200}
        visible={this.state.referVisible}
        placement='center'
        onConfirm={null}
        onCancel={() => {
          this.setState({ referVisible: false })
        }}
        onClose={() => {
          this.setState({ referVisible: false })
        }}
      >
        <div style={{ margin: 30, color: 'rgba(0, 0, 0, 0.90)' }}>
          <div className="tableContent accountTableContent" style={{ marginTop: 20, height: 'auto' }}>
            <Table
              size="middle"
              tableLayout="fixed"
              columns={columns2}
              dataSource={this.state.data2}
              loading={this.state.loading}
              pagination={false}
            // onChange={this.handleTableChange1.bind(this)}
            />
          </div>
        </div>
      </Dialog>

    </ExternalAccountFilter></MyTranslate>)
  }
}

export default connect(({ user }) => ({
  userID: user.info.userID,
  name: user.info.name,
}))(injectIntl(MyComponent))
