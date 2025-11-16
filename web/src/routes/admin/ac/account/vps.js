import React, {Component,useEffect, useRef} from 'react'
import {BrowserRouter as Router, Route, Link} from 'react-router-dom'
import {
  Alert,
  Table,
  message,
  Input,
  InputNumber,
  Modal,
  Radio,
  Badge,
  Avatar,
  Tabs,
  Tag,
  Select,
  DatePicker
} from 'antd'
import axios from 'axios'
import {FormattedMessage, useIntl, injectIntl} from 'react-intl'
import {formatDate} from 'components/DateFormat'
import accountStatus from 'components/accountStatus'
import Style from '../../index.css'
import { Pagination, Breadcrumb, Dialog,Space} from 'tdesign-react';
import DialogApi from "../../common/dialog/DialogApi";
import ExternalAccountFilter from "../../../../components/common/ExternalAccountFilter";
import MyTranslate from "../../../../components/common/MyTranslate";
const { Option, OptGroup } = Select;
import {connect} from 'dva'

const Search = Input.Search;
const confirm = Modal.confirm;
const InputGroup = Input.Group;
const {TabPane} = Tabs;

// 针对当前页面的基础url
const baseUrl = '/api/vps';



class MyComponent extends Component {
  constructor(props) {
    super(props);
    // data由服务器返回表格的数据
    // loading由客户端控制
    // pagination控制表格分页功能，其中数据总数pagination.total由服务器返回
    // 以上变量受控，因为需要传递给Table控件
    this.state = {
      scrollY:"",
      choosenTab: 'deadTimeList',  // deadTimeList / list / machineScope
      choosenMachineNum: 0,   //选中的要充值的设备的总数
      showTable: true,
      deadData: [],
      deadLoading: false,
      deadPagination: {
        total: 0,
        pageSize: 10000,
        current: 1,
        showTotal: (total, range) => `共 ${total} 条`,
        position: 'both',
        showSizeChanger: true,
        pageSizeOptions: ['10', '20', '30', '40', '50', '100', '200', '300', '400', '500', '1000', '2000']
      },
      deadSelectedRowKeys: [],

      data: [],
      loading: false,
      pagination: {
        total: 0,
        pageSize: 10,
        current: 1,
        showTotal: (total, range) => `共 ${total} 条`,
        position: 'both',
        showSizeChanger: true,
        pageSizeOptions: ['10', '20', '30', '40', '50', '100', '200', '300', '400', '500', '1000', '2000']
      },
      selectedRowKeys: [],
      accounts: {},

      all: 'all',
      userBind: '',
      runStatus: '',
      deadStatus: '',
      bindStatus: '',
      runStatusCount: {},
      userBindCount: {},
      deadStatusCount: {},
      bindStatusCount: {},

      addVpsVisible: false,
      addVpsNum: 10,

      startMachine: 1,
      endMachine: 1,

      userId: '全部',
      userID2Name: {},
      userRadioOptions: [],

      buyVisible: false,
      buyMonthValue: 1,
      buyMachineValue: 0,

      chargeVisible: false,
      chargeMonthValue: 1,
      chargeDelayValue: "",

      unitPrice: 0,
      balance: 0
    };
    // this.divRef = React.createRef(); // 创建ref
    // 选中行的数据保存在selectedRowKeys selectedRows变量中
    // 之所以不放入state，因为这些变量是单向流动的，即Table控件产生这些数据，通过callback更新到变量this.selectedRowKeys等
    this.selectedRows = [];
    this.deadSelectedRows = [];
    // filters的写法保持与mongo filter的写法一致，避免与服务器、数据库直接的数据阻抗失衡
    // 例如： filters={username: 'foo'}
    // 因为json协议无法传输regex，所以字符类型的值，都会在服务器端转成regex，以提高匹配度，损失部分查询性能
    this.filters = {};
    this.deadFilters = {};
    // sorter的写法保持与mongo sort的写法一致，避免与服务器、数据库直接的数据阻抗失衡
    // 例如：sorter={createTime: -1}
    // 注意：Table控件仅支持单列排序，不支持多列同时排序
    this.sorter = {
      createTime: -1
    };
    this.deadSorter = {
      deadTime: -1
    };
    // 提前写在constructor则Table首次加载时生效
    // 但是因为没有放到state中，所以除sorter外，都不建议提前设置，保持不受控状态是最好的
    this.isAdmin = this.props.userID == 'admin';
  }

  // 首次加载数据
  async componentWillMount() {
    this.deadReload();
    this.reload();
   this.getUsers();
    //this.searchMmachineScopeVpsCount(this.state.startMachine, this.state.endMachine);
  }

  async deadReload() {
    // 重新加载，一般是页面第一次加载的时候来一下
    this.deadLoad(this.state.deadPagination, this.deadFilters, this.deadSorter)
  }

  async reset() {
    await this.setState({userID:'',vpsID:'',runStatus: '',deadStatus: '', bindStatus: '',desc:'',userId:''});
    this.filters = {};
    this.deadFilters={};
    await this.reload();
    await this.deadReload();
  }

  async deadLoad(deadPagination, deadFilters, deadSorter) {
    // 这里的参数是格式好的，无需修改，无阻抗失衡，直接发给服务器
    console.log(deadFilters);
    this.setState({deadLoading: true});
    let res = await axios.post(`/api/vps/deadTime/query`, {
      userAdmin: true,
      filters: deadFilters,
      sorter: deadSorter
    });

    //deadPagination.total = res.data.total;
    this.setState({
      deadLoading: false,
      deadData: res.data.data,
      deadPagination,
      deadSelectedRowKeys: []
    });
    this.deadSelectedRows = [];
    this.deadFilters = deadFilters;
    this.deadSorter = deadSorter
  }

  async deadOnRowSelectionChange(deadSelectedRowKeys, deadSelectedRows) {
    let choosenMachineNum = 0;
    for (const row of deadSelectedRows) {
      choosenMachineNum += row.vpsCount;
    }
    // 选中状态的数据，因为无需受控，就不记录在state里了，提高效率
    this.setState({deadSelectedRowKeys, choosenMachineNum});
    this.deadSelectedRows = deadSelectedRows
  }

  async deadHandleTableChange(deadPagination, deadFilters, deadSorter) {
    // 此处是table控件的回调，其中的参数格式不是很合理，调整一下后，传递给load
    let sort = this.deadSorter;
    if (deadSorter && deadSorter.field) {
      sort = {};
      sort[deadSorter.field] = deadSorter.order == 'descend' ? -1 : 1
    }
    // 暂时不用Table的filter，不太好用
    this.deadLoad(deadPagination, this.deadFilters, deadSorter)
  }


  async reload() {
    // 重新加载，一般是页面第一次加载的时候来一下
    this.load(this.state.pagination, this.filters, this.sorter)
  }

  async load(pagination, filters, sorter) {
    // 这里的参数是格式好的，无需修改，无阻抗失衡，直接发给服务器
    this.setState({loading: true});
    let res = await axios.post(`${baseUrl}/${pagination.pageSize}/${pagination.current}`, {
      userAdmin: true,
      filters,
      sorter
    });

    pagination.total = res.data.total;

    this.setState({
      loading: false,
      data: res.data.data,
      pagination,
      selectedRowKeys: [],
      userBindCount:res.data.userBindCount,
      runStatusCount: res.data.runStatusCount,
      deadStatusCount: res.data.deadStatusCount,
      bindStatusCount: res.data.bindStatusCount
    });
    this.selectedRows = [];
    this.filters = filters;
    this.sorter = sorter
  }

  async onRowSelectionChange(selectedRowKeys, selectedRows) {
    // 选中状态的数据，因为无需受控，就不记录在state里了，提高效率
    this.setState({selectedRowKeys, choosenMachineNum: selectedRowKeys.length});
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

  async getUsers() {
    let res = await axios.get(`/api/consumer/user/getAllUser`);
    let arr = [{
      value: "全部",
      label: '全部'
    }];
    let obj = {};
    for (const user of res.data.data) {
      arr.push({
        value: user.userID,
        label: user.name
      });
      obj[user.userID] = user.name
    }
    this.setState({
      userID2Name: obj,
      userRadioOptions: arr
    });
  }

  // 处理注册锁定
  async useLockBtnClicked(vps) {
    await axios.post('/api/consumer/vps/vpsUseLock', {
      vpsID: vps.vpsID,
      useLock: !!!vps.useLock
    });
    // this.reload()
  }

  async _onInputDescriptionEnter(vpsID, description) {
    await axios.post(`${baseUrl}/updateDescription`, {
      vpsID,
      description
    });
    message.success('备注成功')
  }



  addVps = async () => {
    console.log("新增设备")
    if (this.state.addVpsNum <= 0) {
      message.error('请填写新增设备数量');
      return
    }
    this.setState({loading: true});
    await axios.post('/api/vps/addVps', {addNum: this.state.addVpsNum});
    message.success('操作成功');
    this.setState({loading: false,addVpsVisible:false});
    this.reload()
  }

  async changeFilter(e) {
    if (e.target.name == 'all') {
      this.setState({all: 'all', runStatus: '', deadStatus: '', bindStatus: ''});
      delete this.filters.runStatus;
      delete this.filters.userBind;
      delete this.filters.deadStatus;
      delete this.filters.bindStatus;
    } else {
      let f = {all: ''};
      f[e.target.name] = e.target.value;
      this.setState(f);
      this.filters[e.target.name] = e.target.value;
    }
    //this.reload()
  }


  async addVpsShow() {
    this.setState({addVpsVisible: true});
  }

  buyMachine = async () => {
    this.setState({buyVisible: true})
  };

  balanceLess = async () => new Promise(resolve => {
    Modal.confirm({
      content: <div>
          <span>
          确定要修改当前过滤条件下所有账号的分组吗?
          </span>
      </div>,
      onOk: () => resolve(true),
      onCancle: () => resolve(false),
    });
  })

  buy = async () => {
    if (this.state.buyMachineValue == 0) {
      message.error('请填写新购买设备的数量');
      return
    }
    if (this.state.buyMonthValue == 0) {
      message.error('请选择新购买设备的有效时长');
      return
    }
    let cost = this.state.buyMonthValue * this.state.buyMachineValue * this.state.unitPrice;
    if (cost > this.state.balance) {
      this.setState({buyVisible:false})
      //message.error("账户余额不足");
      DialogApi.warning({
        title: this.props.intl.formatMessage({id: '账户余额不足,请先充值'}),
        content: <div>
        </div>,
        onOk: async () => {
          this.setState({editGroupVisible: true})
          window.open("https://t.me/Messi0831", '_blank', 'noopener,noreferrer');// 跳转到指定 URL
        },
        onOkTxt: '立即充值',
        onCancel: () => {
        },
      })
      return
    }
    DialogApi.warning({
      title: this.props.intl.formatMessage({id: '是否确认购买'}),
      content: <div>
        本次购买 <span style={{color: 'red'}}>{this.state.buyMachineValue}</span> 台设备，
        每台设备 <span style={{color: 'red'}}>{this.state.buyMonthValue}</span> 个月有效期，
        共计 <span style={{color: 'red'}}>{cost}</span>，余额 <span style={{color: 'red'}}>{this.state.balance - cost}</span>
      </div>,
      onOk: async () => {
        this.setState({loading: true});
        let result = await axios.post(`${baseUrl}/buy`, {
          buyMonthValue: this.state.buyMonthValue,
          buyMachineValue: this.state.buyMachineValue
        });
        message.success('操作成功');
        this.setState({buyVisible: false});
        this.deadReload();
        this.reload();
        this.searchMmachineScopeVpsCount(this.state.startMachine, this.state.endMachine);
      },
      //onOkTxt: '立即续费',
      onCancel: () => {
      }
    })

    // confirm({
    //   title: (<div>
    //     本次购买 <span style={{color: 'red'}}>{this.state.buyMachineValue}</span> 台设备，
    //     每台设备 <span style={{color: 'red'}}>{this.state.buyMonthValue}</span> 个月有效期，
    //     共计 <span style={{color: 'red'}}>{cost}</span>，余额 <span style={{color: 'red'}}>{this.state.balance - cost}</span>
    //   </div>),
    //   content: '',
    //   okText: '确定',
    //   okType: 'danger',
    //   cancelText: '取消',
    //   onOk: async () => {
    //     this.setState({loading: true});
    //     let result = await axios.post(`${baseUrl}/buy`, {
    //       buyMonthValue: this.state.buyMonthValue,
    //       buyMachineValue: this.state.buyMachineValue
    //     });
    //     message.success('操作成功');
    //     this.setState({buyVisible: false});
    //     this.deadReload();
    //     this.reload();
    //     this.searchMmachineScopeVpsCount(this.state.startMachine, this.state.endMachine);
    //   },
    //   onCancel() {
    //   }
    // })
  };

  chargeMachine = async () => {
    if (this.state.choosenTab === 'deadTimeList') {
      if (this.state.deadSelectedRowKeys.length === 0) {
        message.error('请先选择时间批次');
        return
      }
    } else if (this.state.choosenTab === 'list') {       //deadTimeList / list / machineScope
      if (this.state.selectedRowKeys.length === 0) {
        message.error('请先选择设备');
        return
      }
    } else if (this.state.choosenTab === 'machineScope') {
      if (!(this.state.startMachine >= 1 && this.state.endMachine >= this.state.startMachine)) {
        message.error('请正确选择设备范围');
        return
      }
      if (this.state.startMachine > this.state.endMachine) {
        message.error('请正确选择设备范围');
        return
      }
    } else {
      return
    }
    if (this.state.choosenMachineNum === 0) {
      message.error('无设备需要续费');
    }

    this.setState({chargeVisible: true})
  };

  charge = async () => {
    if (this.state.choosenMachineNum === 0) {
      message.error('无设备需要续费');
      return
    }
    if (this.state.chargeDelayValue == "") {
      message.error('请填写设备延期至哪一天');
      return
    }
    // let cost = this.state.chargeMonthValue * this.state.choosenMachineNum * this.state.unitPrice;
    // if (cost > this.state.balance) {
    //   DialogApi.warning({
    //     title: this.props.intl.formatMessage({id: '是否确认续费'}),
    //     content: <div>
    //         <span>
    //         账户余额不足,请先充值
    //         </span>
    //     </div>,
    //     onOk: async () => {
    //       this.setState({editGroupVisible: true})
    //     },
    //     onOkTxt: '立即续费',
    //     onCancel: () => {
    //     }
    //   })
    //   return
    // }
    DialogApi.warning({
      title: this.props.intl.formatMessage({id: '提示'}),
      content: <div>
        本次为 <span style={{color: 'red'}}>{this.state.choosenMachineNum}</span> 台设备延期，
        各设备延期至 <span style={{color: 'red'}}>{this.state.chargeDelayValue} </span>
      </div>,
      onOk: async () => {
        this.setState({loading: true});
        let data = {deadTime: this.state.chargeDelayValue};
        if (this.state.choosenTab === 'deadTimeList') {
          data.deadTimes = this.state.deadSelectedRowKeys;
        } else if (this.state.choosenTab === 'list') {       //deadTimeList / list / machineScope
          data.ids = this.state.selectedRowKeys;
        } else if (this.state.choosenTab === 'machineScope') {
          data.startMachine = this.state.startMachine;
          data.endMachine = this.state.endMachine;
        }
        data.batchIds = this.deadSelectedRows.map(e=>e.batchId);
        let result = await axios.post(`/api/vps/batch/renew`, data);
        message.success('操作成功');
        this.setState({chargeVisible: false});
        this.deadReload();
        this.reload();
      },
      onCancel: () => {
      }
    })

    // confirm({
    //   title: (<div>
    //     本次为 <span style={{color: 'red'}}>{this.state.choosenMachineNum}</span> 台设备续费，
    //     每台设备续费 <span style={{color: 'red'}}>{this.state.chargeMonthValue}</span> 个月有效期，
    //     共计 <span style={{color: 'red'}}>{cost}</span>，余额 <span style={{color: 'red'}}>{this.state.balance - cost}</span>
    //   </div>),
    //   content: '',
    //   okText: '确定',
    //   okType: 'danger',
    //   cancelText: '取消',
    //   onOk: async () => {
    //     this.setState({loading: true});
    //     let data = {chargeMonthValue: this.state.chargeMonthValue};
    //     if (this.state.choosenTab === 'deadTimeList') {
    //       data.deadTimes = this.state.deadSelectedRowKeys;
    //     } else if (this.state.choosenTab === 'list') {       //deadTimeList / list / machineScope
    //       data.ids = this.state.selectedRowKeys;
    //     } else if (this.state.choosenTab === 'machineScope') {
    //       data.startMachine = this.state.startMachine;
    //       data.endMachine = this.state.endMachine;
    //     }
    //     let result = await axios.post(`${baseUrl}/charge`, data);
    //     message.success('操作成功');
    //     this.setState({chargeVisible: false});
    //     this.deadReload();
    //     this.reload();
    //     this.searchMmachineScopeVpsCount(this.state.startMachine, this.state.endMachine);
    //   },
    //   onCancel() {
    //   }
    // })
  };

  async changeTab(key) {
    this.setState({choosenTab: key});
  }

  async changeStartMachine(v) {
    if (v <= this.state.endMachine) {
      this.setState({startMachine: v});
      await this.searchMmachineScopeVpsCount(v, this.state.endMachine)
    }
  }

  async changeEndMachine(v) {
    if (v >= this.state.startMachine) {
      this.setState({endMachine: v});
      await this.searchMmachineScopeVpsCount(this.state.startMachine, v)
    }
  }

  refTableContent = (ref) => {
    if (ref && ref.getBoundingClientRect) {
      console.log("当前窗口高度为：",ref.getBoundingClientRect().height)
     // this.setState({showTable: true, scrollY: ref.getBoundingClientRect().height - 120, tableContent: ref})
    }
  }

  async searchMmachineScopeVpsCount(startMachine, endMachine) {
    let result = await axios.post(`/api/vps2/machineScopeVpsCount`, {startMachine, endMachine});
    this.setState({choosenMachineNum: result.data.count});
  }

  //用户筛选
  async changeUserIDFilter(e) {
    console.log(e);
    this.setState({userID: e});
    if (e == '全部') {
      delete this.filters.userID;
      delete this.deadFilters.userID;
    } else {
      this.filters['userID'] = e;
      this.deadFilters['userID'] = e;
    }
    // this.deadReload();
    // this.reload();
  }

  render() {
    const columns = [
      {
        title: '设备编号',
        dataIndex: 'vpsId',
        key: 'vpsId',
      }, {
        title: '运行状态',
        dataIndex: 'runStatus',
        key: 'runStatus',
        render: (v) => {
          return v == '1' ? '运行中' : v == '0' ? '掉线' : ''
        }
      }, {
        title: '到期时间',
        dataIndex: 'deadTime',
        key: 'deadTime',
        render: (v, r) => {
          if(r.deadTime == null){
            return '';
          }
          return formatDate(new Date()) > formatDate(r.deadTime) ? `${formatDate(r.deadTime)}（已过期）`:`${formatDate(r.deadTime)}`;
        }
      }, {
        title: '分配账号',
        dataIndex: 'bindStatus',
        key: 'bindStatus',
        render: (v,r) => {
          if (v == '1') {
            return  r.accName;
          } else if (v == '0') {
            return '未分配'
          } else {
            return ''
          }
        }
      }, {
        title: '备注(输入备注后回车保存)',
        dataIndex: 'description',
        key: 'description',
        render: (text, record, index) => {
          return (<Input defaultValue={text} onPressEnter={(e) => {
            this._onInputDescriptionEnter(record.vpsId, e.target.value)
          }}/>)
        }
      }
    ];

    const deadColumns = [
      {
        title: '到期时间',
        dataIndex: 'deadTime',
        key: 'deadTime',
        render: (v, r) => {
          if(r.deadTime == null){
            return '';
          }
          return formatDate(new Date()) > formatDate(r.deadTime) ? `${formatDate(r.deadTime)}（已过期）`:`${formatDate(r.deadTime)}`;
        }
      }, {
        title: '设备总数',
        dataIndex: 'vpsCount',
        key: 'vpsCount'
      }, {
        title: '已分配账号',
        dataIndex: 'bindCount',
        key: 'bindCount'
      }, {
        title: '未分配账号',
        dataIndex: 'unBindCount',
        key: 'unBindCount'
      }
    ];

    return (<MyTranslate><ExternalAccountFilter><div className={Style.vps}>
      <Breadcrumb>
        <Breadcrumb.BreadcrumbItem>档案与资源</Breadcrumb.BreadcrumbItem>
        <Breadcrumb.BreadcrumbItem>设备管理</Breadcrumb.BreadcrumbItem>
      </Breadcrumb>
      <Alert
        message={
          <div>
            提示
            <br />
            账号必须运行在手机、平板等设备上，如需绑定新账号请购买设备
          </div>
        }
        type="info"
        showIcon
        style={{
          margin: '20px 0',
          backgroundColor: '#EEF3FF',
          borderColor: '#EEF3FF'
        }}
      />
      <Tabs defaultActiveKey={this.state.choosenTab} onChange={this.changeTab.bind(this)}>
        <TabPane tab="时间批次" key="deadTimeList">
          <div className="search-box">
            <div className='search-item'>
              <div className="search-item-label">用户</div>
              <div className="search-item-right">
                <Select value={this.state.userID} style={{ width: 180 }} onChange={this.changeUserIDFilter.bind(this)}>
                  {this.state.userRadioOptions.map(ws => {
                    return <Option value={ws.value}>{ws.label}</Option>
                  })}
                </Select>
              </div>
            </div>
            <div className='accountGroup-btn'>
              <div className="search-query-btn" onClick={() => this.deadReload()}>查询</div>
              <div className="search-reset-btn" onClick={() => this.reset()}>重置</div>
            </div>
          </div>

          <div className="main-content" style={{ marginTop: '5px' }}>
            {/*<Button className={Style.buy} onClick={this.buyMachine.bind(this)}>购买设备</Button>*/}
            {this.isAdmin ? <div className="search-query-btn" onClick={this.addVpsShow.bind(this)}>新增设备</div> : ''}
            {this.isAdmin ? <div className="search-reset-btn" onClick={this.chargeMachine.bind(this)}>批量续费</div> : ''}
            {/*<Button className={Style.renew} onClick={this.chargeMachine.bind(this)}>批量续费</Button>*/}
            <div className="tableSelectedCount">{`已选${this.state.deadSelectedRowKeys.length}项`}</div>
            <div className="tableContent" style={{height: `${this.state.scrollY}px`}} ref={this.refTableContent}>
              {/*<div className="tableContent"  ref={this.divRef}>*/}
              <div>
                { this.state.showTable ? <Table
                  tableLayout="fixed"
                  scroll={{y: this.state.scrollY-70, x: 1000}}
                  pagination={this.state.deadPagination} rowSelection={{
                  selectedRowKeys: this.state.deadSelectedRowKeys,
                  onChange: this.deadOnRowSelectionChange.bind(this)
                }} columns={deadColumns} rowKey='_id' dataSource={this.state.deadData} loading={this.state.loading}/> : '' }
              </div>
            </div>
            {/*<Pagination*/}
            {/*  showJumper*/}
            {/*  total={this.state.deadPagination.total}*/}
            {/*  current={this.state.deadPagination.current}*/}
            {/*  pageSize={this.state.deadPagination.pageSize}*/}
            {/*  onChange={this.deadHandleTableChange.bind(this)}*/}
            {/*/>*/}
          </div>
        </TabPane>
        <TabPane tab="设备列表" key="list">
          <div className="account-search-box">
            <div className='account-search-item'>
              <div className="account-search-item-label">用户</div>
              <div className="account-search-item-right">
                <Select value={this.state.userID} style={{ width: 180 }} onChange={this.changeUserIDFilter.bind(this)}>
                  {this.state.userRadioOptions.map(ws => {
                    return <Option value={ws.value}>{ws.label}</Option>
                  })}
                </Select>
              </div>
            </div>
            <div className='account-search-item' style={{ minWidth: 280 }}>
              <div className="account-search-item-label">设备编号</div>
              <div className="account-search-item-right">
                <Input
                  allowClear
                  style={{ width: 200 }}
                  placeholder="请输入内容"
                  value={this.state.vpsID}
                  // 补充事件绑定 ↓
                  onChange={e => {
                    this.setState({ vpsID: e.target.value })
                    this.filters['vpsId'] = e.target.value
                  }
                  }
                />
              </div>
            </div>
            <div className='account-search-item' style={{ minWidth: 220 }}>
              <div className="account-search-item-label">运行状态</div>
              <div className="account-search-item-right">
                <Radio.Group name="runStatus" value={this.state.runStatus} buttonStyle="solid"
                             style={{marginRight: '20px'}}
                             onChange={this.changeFilter.bind(this)}>
                  <Radio.Button value="0">
                    <Badge
                      offset={[10, -10]} showZero
                      count={this.state.runStatusCount['0'] ? this.state.runStatusCount['0'] : 0} overflowCount={100000}
                      style={{float: 'right'}}
                    >掉线</Badge>
                  </Radio.Button>
                  <Radio.Button value="1">
                    <Badge
                      offset={[10, -10]} showZero
                      count={this.state.runStatusCount['1'] ? this.state.runStatusCount['1'] : 0} overflowCount={100000}
                      style={{float: 'right', backgroundColor: '#52c41a'}}
                    >运行中</Badge>
                  </Radio.Button>
                </Radio.Group>
              </div>
            </div>

            {this.isAdmin ?
              <div className='account-search-item' style={{ minWidth: 250 }}>
                <div className="account-search-item-label"  style={{width:50}}>用户</div>
                <div className="account-search-item-right" style={{minWidth:200}}>
              <Radio.Group name="userBind" value={this.state.userBind} buttonStyle="solid"
                           style={{marginRight: '40px'}}
                           onChange={this.changeFilter.bind(this)}>
                <Radio.Button value="0">
                  <Badge
                    offset={[10, -10]} showZero
                    count={this.state.userBindCount['0'] ? this.state.userBindCount['0'] : 0} overflowCount={1000000}
                    style={{float: 'right', backgroundColor: '#52c41a'}}
                  >无用户</Badge>
                </Radio.Button>
                <Radio.Button value="1">
                  <Badge
                    offset={[10, -10]} showZero
                    count={this.state.userBindCount['1'] ? this.state.userBindCount['1'] : 0} overflowCount={1000000}
                    style={{float: 'right', backgroundColor: '#108ee9'}}
                  >有用户</Badge>
                </Radio.Button>
              </Radio.Group>
                </div>
              </div>
              : ''}

            <div className='account-search-item' style={{ minWidth: 300 }}>
              <div className="account-search-item-label">过期状态</div>
              <div className="account-search-item-right" style={{minWidth:280}}>
                <Radio.Group name="deadStatus" value={this.state.deadStatus} buttonStyle="solid"
                             style={{marginRight: '20px'}}
                             onChange={this.changeFilter.bind(this)}>
                  <Radio.Button value="0">
                    <Badge
                      offset={[10, -10]} showZero
                      count={this.state.deadStatusCount['0'] ? this.state.deadStatusCount['0'] : 0} overflowCount={100000}
                      style={{float: 'right', backgroundColor: '#52c41a'}}
                    >未过期</Badge>
                  </Radio.Button>
                  <Radio.Button value="1">
                    <Badge
                      offset={[10, -10]} showZero
                      count={this.state.deadStatusCount['1'] ? this.state.deadStatusCount['1'] : 0} overflowCount={100000}
                      style={{float: 'right', backgroundColor: 'orange'}}
                    >即将过期</Badge>
                  </Radio.Button>
                  <Radio.Button value="2">
                    <Badge
                      offset={[10, -10]} showZero
                      count={this.state.deadStatusCount['2'] ? this.state.deadStatusCount['2'] : 0} overflowCount={100000}
                      style={{float: 'right'}}
                    >已过期</Badge>
                  </Radio.Button>
                </Radio.Group>
              </div>
            </div>
            <div className='account-search-item' style={{ minWidth: 280 }}>
              <div className="account-search-item-label">分配状态</div>
              <div className="account-search-item-right">
                <Radio.Group name="bindStatus" value={this.state.bindStatus} buttonStyle="solid"
                             onChange={this.changeFilter.bind(this)}>
                  <Radio.Button value="0">
                    <Badge
                      offset={[10, -10]} showZero
                      count={this.state.bindStatusCount['0'] ? this.state.bindStatusCount['0'] : 0} overflowCount={100000}
                      style={{float: 'right', backgroundColor: '#52c41a'}}
                    >未分配</Badge>
                  </Radio.Button>
                  <Radio.Button value="1">
                    <Badge
                      offset={[10, -10]} showZero
                      count={this.state.bindStatusCount['1'] ? this.state.bindStatusCount['1'] : 0} overflowCount={100000}
                      style={{float: 'right', backgroundColor: '#108ee9'}}
                    >已分配</Badge>
                  </Radio.Button>
                </Radio.Group>
              </div>
            </div>
            {/*<div className='account-btn'>*/}
            {/*  <div style={{display: 'flex', justifyContent: 'right', alignItems: 'center'}}>*/}
            {/*    <div className="search-query-btn" onClick={() => this.reload()}>查询</div>*/}
            {/*    <div className="search-reset-btn" onClick={() => this.reset()}>重置</div>*/}
            {/*  </div>*/}
            {/*</div>*/}
            <div className='account-btn-no-expand'>
              <div style={{display: 'flex', justifyContent: 'right', alignItems: 'center'}}>
                <div className="search-query-btn" onClick={() => this.reload()}>查询</div>
                <div className="search-reset-btn" onClick={() => this.reset()}>重置</div>
                {/* <div style={{color: '#3978F7', display: 'inline-block', marginRight: 10, cursor: 'pointer'}} onClick={() => {this.setState({expandSearch: true});this.handleResize()}}><img src="/icons2/suffix-1.png" style={{width: 16, height: 16, marginTop: -3, marginRight: 8, marginLeft: 8}}/>更多筛选</div> */}
              </div>
            </div>
            <div style={{clear: 'both'}}></div>
          </div>
          <div className="main-content" style={{ marginTop: '5px' }}>
            <div className="search-query-btn" onClick={this.addVpsShow.bind(this)}>新增设备</div>

            <div className="tableSelectedCount">{`已选${this.state.selectedRowKeys.length}项`}</div>
            <div className="tableContent" style={{height: `${this.state.scrollY}px`}} ref={this.refTableContent}>
              <div>
                { this.state.showTable ? <Table
                  tableLayout="fixed"
                  scroll={{y: this.state.scrollY-70, x: 1000}}
                  pagination={this.state.pagination} rowSelection={{
                  selectedRowKeys: this.state.selectedRowKeys,
                  onChange: this.onRowSelectionChange.bind(this)
                }} columns={columns} rowKey='_id' dataSource={this.state.data} loading={this.state.loading}/> : '' }
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
        </TabPane>
      </Tabs>
      <Dialog
        visible={this.state.chargeVisible}
        onCancel={() => {
          this.setState({chargeVisible: false})
        }}
        onClose={() => {
          this.setState({chargeVisible: false})
        }}
        onConfirm={this.charge}
        header="批量续费"
        style={{
          width: '50%',
          maxWidth: '600px',
          position: 'fixed', // 固定定位
          left: '50%',
          top: '50%',
          transform: 'translate(-50%, -50%)', // 中心点定位
          margin: 0 // 清除默认margin
        }}
      >
        <div style={{display: 'flex',marginBottom: 24}}>
          <div style={{width: 154, textAlign: 'right', marginRight: 16, lineHeight: '30px'}}><strong>选中设备数 :</strong></div>
          <div style={{textAlign: 'right', marginRight: 16, lineHeight: '30px'}}> {this.state.choosenMachineNum}</div>
        </div>
        <div style={{display: 'flex',marginBottom: 24}}>
          <div style={{width: 154, textAlign: 'right', marginRight: 16, lineHeight: '30px'}}><strong>延期日期 :</strong></div>
          <DatePicker type="datatime" format="YYYY-MM-DD HH:mm:ss" onChange={(date,dateString)=>{this.setState({chargeDelayValue: dateString})}}/>
        </div>
      </Dialog>

      <Dialog
        header="新增设备"
        visible={this.state.addVpsVisible}
        onCancel={() => {
          this.setState({addVpsVisible: false})
        }}
        onClose={() => {
          this.setState({addVpsVisible: false})
        }}
        onConfirm={this.addVps}
        style={{
          width: '50%',
          maxWidth: '600px',
          position: 'fixed', // 固定定位
          left: '50%',
          top: '50%',
          transform: 'translate(-50%, -50%)', // 中心点定位
          margin: 0 // 清除默认margin
        }}
      >
        <div style={{display: 'flex',marginBottom: 24}}>
          <div style={{width: 154, textAlign: 'right', marginRight: 16, lineHeight: '30px'}}><strong>新增设备数量：</strong></div>
          <div style={{textAlign: 'right', marginRight: 16, lineHeight: '30px'}}>
            <InputNumber onChange={(v)=>{this.setState({addVpsNum:v})}} min={1} max={10000} value={this.state.addVpsNum}/>
          </div>
        </div>
      </Dialog>
    </div></ExternalAccountFilter></MyTranslate>)
  }
}

// 先使用 connect 函数连接 Redux 状态
const ConnectedComponent = connect(({ user }) => ({
  userID: user.info.userID,
  balance:user.info.balance
}))(MyComponent);

// 再使用 injectIntl 函数注入国际化功能
const ConnectedAndIntlComponent = injectIntl(ConnectedComponent);

export default ConnectedAndIntlComponent;
