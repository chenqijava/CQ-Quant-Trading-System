import React, {Component,useEffect, useRef} from 'react'
import {BrowserRouter as Router, Route, Link} from 'react-router-dom'
import {
  Alert,
  Icon,
  Table,
  Divider,
  Upload,
  message,
  Input,
  InputNumber,
  Button,
  Switch,
  Modal,
  Radio,
  Badge,
  Avatar,
  Tabs,
  Tag,
  Col,
  Row,
} from 'antd'
import axios from 'axios'
import {connect} from 'dva';
import {FormattedMessage, useIntl, injectIntl} from 'react-intl'
import {formatDate} from 'components/DateFormat'
import accountStatus from 'components/accountStatus'
import Style from './index.css'
import ExternalAccountFilter from "../../../components/common/ExternalAccountFilter";
import MyTranslate from "../../../components/common/MyTranslate";
import { Pagination, Breadcrumb, Dialog,Space} from 'tdesign-react';
import DialogApi from "./dialog/DialogApi";

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
      runStatus: '',
      deadStatus: '',
      bindStatus: '',
      dataTimeList:[],
      runStatusCount: {},
      deadStatusCount: {},
      bindStatusCount: {},
      runCount:0,
      deadCount:0,
      bindCount:0,
      
      startMachine: 1,
      endMachine: 1,

      buyVisible: false,
      buyMonthValue: 1,
      buyMachineValue: 0,

      chargeVisible: false,
      chargeMonthValue: 1,

      unitPrice: 0,
      balance: this.props.balance
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
  }

  // 首次加载数据
  async componentWillMount() {
     this.deadReload();
     this.reload();
     //this.searchMmachineScopeVpsCount(this.state.startMachine, this.state.endMachine);

  }

  async deadReload() {
    // 重新加载，一般是页面第一次加载的时候来一下
    this.deadLoad(this.state.deadPagination, this.deadFilters, this.deadSorter)
  }

  async reset() {
    await this.setState({vpsID:'',runStatus: '',deadStatus: '', bindStatus: '',desc:''});
    this.filters = {};
    await this.reload()
  }

  async deadLoad(deadPagination, deadFilters, deadSorter) {
    // 这里的参数是格式好的，无需修改，无阻抗失衡，直接发给服务器
    this.setState({deadLoading: true});
    let res = await axios.post(`/api/vps/deadTime/query`, {
      userAdmin: false,
      filters: deadFilters,
      sorter: deadSorter
    });
   // deadPagination.total = res.data.total;
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
     // this.dataTimeList.push(row.deadTime);
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
    console.log(filters);
    let res = await axios.post(`${baseUrl}/${pagination.pageSize}/${pagination.current}`, {
      filters,
      sorter
    });

    pagination.total = res.data.total;
    this.setState({
      loading: false,
      data: res.data.data,
      pagination,
      selectedRowKeys: [],
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
        // 用于有无的过滤处理
        if (['transferTime', 'unlockTime', 'payload.needAddFriend', 'payload.changeIP'].indexOf(key) > -1) {
          oldFilters[key] = {$exists: filter[0] == 'true'};
        } else if (key == 'payload') {
          filter.map(k => {
            oldFilters[`${key}.${k}`] = {$exists: true}
          });
        } else {
          oldFilters[key] = {$in: filter};
        }
      } else {
        delete oldFilters[key];
      }
    }
    // 暂时不用Table的filter，不太好用
    this.load(pagination, this.filters, sort)
  }

  async _onInputDescriptionEnter(vpsID, description) {
    let data = new FormData();
    data.append('vpsId', vpsID);
    data.append('description', description);
    await axios.post(`${baseUrl}/updateDescription`, data);
    message.success('备注成功')
  }

  async changeFilter(e) {

    console.log("过期时间");
    if (e.target.name == 'all') {
      this.setState({all: 'all', runStatus: '', deadStatus: '', bindStatus: ''});
      delete this.filters.runStatus;
      delete this.filters.deadStatus;
      delete this.filters.bindStatus;
    } else {
      let f = {all: ''};
      f[e.target.name] = e.target.value;
      this.setState(f);
      this.filters[e.target.name] = e.target.value;
    }
    console.log("过期日志");
    this.reload()
  }

  buyMachine = async () => {

    this.setState({buyVisible: true})
    this.setState({deadLoading: true});
    let res = await axios.get(`/api/consumer/userParams/vps/get/vpsUnitPrice`);
    await this.setState({
      unitPrice:res.data.data
    });
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
          let keys = [...this.props.openKeys, 'balanceMenu']
          this.props.dispatch({type: 'user/openKeys', openKeys: keys});
          this.props.history.push('/cloud/user/payment');
          //window.open("/cloud/user/payment", '_blank', 'noopener,noreferrer');// 跳转到指定 URL
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
        本次购买{this.state.buyMachineValue}台设备，每台设备{this.state.buyMonthValue}个月有效期，共计${cost}，剩余余额${this.state.balance - cost}
      </div>,
      onOk: async () => {
        this.setState({loading: true});
        let result = await axios.post(`${baseUrl}/buy`, {
          monthCount: this.state.buyMonthValue,
          amount: this.state.buyMachineValue
        });
        if (result.data.code == 1) {
          message.success('操作成功');
        } else {
          message.error(result.data.message)
          return
        }
        
        this.setState({
          buyVisible: false,
          balance:result.data.data
        });
        this.deadReload();
        this.setState({loading: false});
        //this.searchMmachineScopeVpsCount(this.state.startMachine, this.state.endMachine);
        this.props.dispatch({type: 'user/stat'});
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
    let res = await axios.get(`/api/consumer/userParams/vps/get/vpsUnitPrice`);
    await this.setState({
      unitPrice:res.data.data
    });
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
    if (this.state.chargeMonthValue == 0) {
      message.error('请填写设备的续费时长');
      return
    }
    let cost = this.state.chargeMonthValue * this.state.choosenMachineNum * this.state.unitPrice;
    if (cost > this.state.balance) {
      DialogApi.warning({
        title: this.props.intl.formatMessage({id: '是否确认续费'}),
        content: <div>
            <span>
            账户余额不足,请先充值
            </span>
        </div>,
        onOk: async () => {
          this.setState({editGroupVisible: true})
        },
        onOkTxt: '立即续费',
        onCancel: () => {
        }
      })
      return
    }
    DialogApi.warning({
      title: this.props.intl.formatMessage({id: '是否确认续费'}),
      content: <div>
        本次为{this.state.choosenMachineNum}台设备续费，每台设备续费{this.state.chargeMonthValue}个月有效期，共计${cost}，剩余余额${this.state.balance - cost}
      </div>,
      onOk: async () => {
        this.setState({loading: true});
        let data = {chargeMonthValue: this.state.chargeMonthValue,
          monthCount: this.state.chargeMonthValue,
        };
        if (this.state.choosenTab === 'deadTimeList') {
         // data.deadTimes = this.state.deadSelectedRowKeys;
        } else if (this.state.choosenTab === 'list') {       //deadTimeList / list / machineScope
          data.ids = this.state.selectedRowKeys;
        } else if (this.state.choosenTab === 'machineScope') {
          data.startMachine = this.state.startMachine;
          data.endMachine = this.state.endMachine;
        }
        data.batchIds = this.deadSelectedRows.map(e=>e.batchId);

        let result = await axios.post(`${baseUrl}/batch/renew`, data);
        message.success('操作成功');
        this.setState({
          chargeVisible: false,
          balance:result.data.data
        });
        this.deadReload();
        this.reload();
        //this.searchMmachineScopeVpsCount(this.state.startMachine, this.state.endMachine);
        this.props.dispatch({type: 'user/stat'});
        this.setState({loading: false});
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
      this.setState({showTable: true, scrollY: ref.getBoundingClientRect().height - 70, tableContent: ref})
    }
  }

  async searchMmachineScopeVpsCount(startMachine, endMachine) {
    let result = await axios.post(`${baseUrl}/machineScopeVpsCount`, {startMachine, endMachine});
    this.setState({choosenMachineNum: result.data.count});
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
          return v == '1' ? '运行中' : '掉线'
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
        render: (v, r) => {
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
        <Breadcrumb.BreadcrumbItem>用户中心</Breadcrumb.BreadcrumbItem>
        <Breadcrumb.BreadcrumbItem>设备管理</Breadcrumb.BreadcrumbItem>
      </Breadcrumb>
      <Alert
        message={
          <div>
            提示
            <br />
            账号必须运行在设备上，如需绑定新帐号请购买设备
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
          <div className="main-content" style={{ marginTop: '5px' }}>
              {/*<Button className={Style.buy} onClick={this.buyMachine.bind(this)}>购买设备</Button>*/}
            <div className="search-query-btn" onClick={this.buyMachine.bind(this)}>购买设备</div>
            <div className="search-reset-btn" onClick={this.chargeMachine.bind(this)}>批量续费</div>
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
          <div className="search-box">
            <div className='search-item' style={{ minWidth: 280 }}>
              <div className="search-item-label">设备编号</div>
              <div className="search-item-right">
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
            <div className='search-item' style={{ minWidth: 280 }}>
              <div className="search-item-label">运行状态</div>
              <div className="search-item-right">
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
            <div className='search-item' style={{ minWidth: 300 }}>
              <div className="search-item-label">过期状态</div>
              <div className="search-item-right" style={{minWidth:280}}>
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
            <div className='search-item' style={{ minWidth: 280 }}>
              <div className="search-item-label">分配状态</div>
              <div className="search-item-right">
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
            <div className='accountGroup-btn'>
              <div className="search-query-btn" onClick={() => this.reload()}>查询</div>
              <div className="search-reset-btn" onClick={() => this.reset()}>重置</div>
            </div>
          </div>
          <div className="main-content" style={{ marginTop: '5px' }}>
            <div className="search-query-btn" onClick={this.buyMachine.bind(this)}>购买设备</div>
            <div className="tableSelectedCount">{`已选${this.state.selectedRowKeys.length}项`}</div>
            <div className="tableContent" style={{height: `${this.state.scrollY}px`}} ref={this.refTableContent}>
            {/*<div className="tableContent"  ref={this.refTableContent}>*/}
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
        {/*<TabPane tab="设备范围" key="machineScope">*/}
        {/*  起始设备ID：<InputNumber min={1} style={{width: '120px'}} value={this.state.startMachine}*/}
        {/*                      onChange={this.changeStartMachine.bind(this)}/>，*/}
        {/*  截止设备ID：<InputNumber min={1} style={{width: '120px'}} value={this.state.endMachine}*/}
        {/*                      onChange={this.changeEndMachine.bind(this)}/>，*/}
        {/*  实有设备数：{this.state.choosenMachineNum}*/}
        {/*</TabPane>*/}
      </Tabs>
      {/*<Modal*/}
      {/*  title="设备批量续费"*/}
      {/*  visible={this.state.chargeVisible}*/}
      {/*  onOk={this.charge} confirmLoading={this.state.loading}*/}
      {/*  onCancel={() => {*/}
      {/*    this.setState({chargeVisible: false})*/}
      {/*  }}*/}
      {/*>*/}

      {/*  <Row className={Style.row}>*/}
      {/*    <Col span={12} className={Style.text_left}>账户余额 :&nbsp;&nbsp;</Col>*/}
      {/*    <Col span={12} className={Style.text_right}>&nbsp;&nbsp;￥{this.state.balance}</Col>*/}
      {/*  </Row>*/}
      {/*  <Row className={Style.row}>*/}
      {/*    <Col span={12} className={Style.text_left}>选中设备数 :&nbsp;&nbsp;</Col>*/}
      {/*    <Col span={12} className={Style.text_right}>&nbsp;&nbsp;{this.state.choosenMachineNum}</Col>*/}
      {/*  </Row>*/}
      {/*  <Row className={Style.row}>*/}
      {/*    <Col span={12} className={Style.text_left}>单个设备月单价 :&nbsp;&nbsp;</Col>*/}
      {/*    <Col span={12} className={Style.text_right}>&nbsp;&nbsp;￥{this.state.unitPrice}.0</Col>*/}
      {/*  </Row>*/}
      {/*  <Row className={Style.row}>*/}
      {/*    <Col span={12} className={Style.input_left}>续费时长(月) :&nbsp;&nbsp;</Col>*/}
      {/*    <Col span={12}>&nbsp;&nbsp;<InputNumber onChange={(e) => {*/}
      {/*      this.setState({chargeMonthValue: e})*/}
      {/*    }} min={0} value={this.state.chargeMonthValue}/></Col>*/}
      {/*  </Row>*/}
      {/*  <Row className={Style.row}>*/}
      {/*    <Col span={12} className={Style.text_left}>预计花费 :&nbsp;&nbsp;</Col>*/}
      {/*    <Col span={12}*/}
      {/*         className={Style.text_right_red}>&nbsp;&nbsp;￥{this.state.chargeMonthValue * this.state.choosenMachineNum * this.state.unitPrice}</Col>*/}
      {/*  </Row>*/}

      {/*</Modal>*/}


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
          <div style={{width: 154, textAlign: 'right', marginRight: 16, color:'#000000E5',fontFamily: "PingFang SC",fontSize: '14px',fontStyle: 'normal',fontWeight: '400',lineHeight: '22px'}}>设备单价 :</div>
          <div style={{display: 'flex',height: '22px',justifyContent: 'center'}}>
           <div style={{color: '#D54941',textOverflow: 'ellipsis',whiteSpace: 'nowrap',fontFamily: "PingFang SC",fontSize: '20px',fontStyle: 'normal',fontWeight: 600}}> $ {this.state.unitPrice} </div>
            <div style={{fontFamily: "PingFang SC",fontSize: '14px',fontStyle: 'normal',fontWeight: 400,lineHeight: '22px',color: 'var(--text-icon-font-gy-340-placeholder, rgba(0, 0, 0, 0.40))'}}>/台/月</div>
            </div>
        </div>
        <div style={{display: 'flex',marginBottom: 24}}>
          <div style={{width: 154, textAlign: 'right', marginRight: 16, color:'#000000E5',fontFamily: "PingFang SC",fontSize: '14px',fontStyle: 'normal',fontWeight: '400',lineHeight: '22px'}}>已选设备数量(台):</div>
          <div style={{overflow: 'hidden', color: 'var(--text-icon-font-gy-340-placeholder, rgba(0, 0, 0, 0.40))',textOverflow: 'ellipsis',whiteSpace: 'nowrap',fontFamily: "PingFang SC",
          fontSize: '20px',fontStyle: 'normal',fontWeight: '600'}}> {this.state.choosenMachineNum}</div>
        </div>

        <div style={{display: 'flex',marginBottom: 24}}>
          <div style={{width: 154, textAlign: 'right', marginRight: 16, color:'#000000E5',fontFamily: "PingFang SC",fontSize: '14px',fontStyle: 'normal',fontWeight: '400',lineHeight: '22px'}}>续费时长(月) :</div>
          <div style={{textAlign: 'right', marginRight: 16, lineHeight: '30px'}}>
            <InputNumber onChange={(e) => {
              this.setState({chargeMonthValue: e})
            }} min={0} value={this.state.chargeMonthValue}/>
          </div>
        </div>

        <div style={{display: 'flex',marginBottom: 24}}>
          <div style={{width: 154, textAlign: 'right', marginRight: 16, color:'#000000E5',fontFamily: "PingFang SC",fontSize: '14px',fontStyle: 'normal',fontWeight: '400',lineHeight: '22px'}}>账户余额 :</div>
          <div style={{textAlign: 'right', marginRight: 16, lineHeight: '30px',color:'#000000'}}>
          <span style={{  overflow: 'hidden',color: 'var(--text-icon-font-gy-190-primary, rgba(0, 0, 0, 0.90))',textOverflow: 'ellipsis', whiteSpace: 'nowrap',fontFamily: "PingFang SC",
            fontSize: '20px',fontStyle: 'normal',fontWeight: '600',}}>$ {this.state.balance}</span>
            <img src="/icons/refresh.png" onClick={() => this.reload()} style={{width: 16, height: 16,marginLeft:8,cursor: 'pointer'}}/>
            <a href="/cloud/user/payment"  rel="noopener noreferrer" style={{marginLeft:10}}>
              立即充值
            </a>
          </div>
        </div>

        <div style={{display: 'flex',marginBottom: 24}}>
          <div style={{width: 154, textAlign: 'right', marginRight: 16, color:'#000000E5',fontFamily: "PingFang SC",fontSize: '14px',fontStyle: 'normal',fontWeight: '400',lineHeight: '22px'}}>总计 :</div>
          <div style={{overflow: 'hidden',color: 'var(--Error-Error6-Normal, #D54941)',textOverflow: 'ellipsis',whiteSpace: 'nowrap',
          fontFamily: "PingFang SC",fontSize: '20px',fontStyle: 'normal',fontWeight: '600'}}>
            {(() => {
              const chargeMonthValue = Number(this.state.chargeMonthValue) || 0;
              const choosenMachineNum = Number(this.state.choosenMachineNum) || 0;
              const unitPrice = Number(this.state.unitPrice) || 0;
              const total = chargeMonthValue * choosenMachineNum * unitPrice;
              return total > 0 ? `$ ${total}` : '$ 0';
            })()}
          </div>
        </div>
      </Dialog>

      <Dialog
        header="购买设备"
        visible={this.state.buyVisible}
        onCancel={() => {
          this.setState({buyVisible: false})
        }}
        onClose={() => {
          this.setState({buyVisible: false})
        }}
        onConfirm={this.buy}
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
          <div style={{width: 154, textAlign: 'right', marginRight: 16, color:'#000000E5',fontFamily: "PingFang SC",fontSize: '14px',fontStyle: 'normal',fontWeight: '400',lineHeight: '22px'}}>设备单价 </div>
          <div style={{display: 'flex',height: '22px',justifyContent: 'center'}}>
           <div style={{color: '#D54941',textOverflow: 'ellipsis',whiteSpace: 'nowrap',fontFamily: "PingFang SC",fontSize: '20px',fontStyle: 'normal',fontWeight: 600}}> $ {this.state.unitPrice} </div>
            <div style={{fontFamily: "PingFang SC",fontSize: '14px',fontStyle: 'normal',fontWeight: 400,lineHeight: '22px',color: 'var(--text-icon-font-gy-340-placeholder, rgba(0, 0, 0, 0.40))'}}>/台/月</div>
            </div>
        </div>
        <div style={{display: 'flex',marginBottom: 24}}>
          <div style={{width: 154, textAlign: 'right', marginRight: 16, color:'#000000E5',fontFamily: "PingFang SC",fontSize: '14px',fontStyle: 'normal',fontWeight: '400',lineHeight: '22px'}}>设备数量(台) </div>
          <div style={{textAlign: 'right', marginRight: 16, lineHeight: '30px'}}>
            <InputNumber onChange={(e) => {
            this.setState({buyMachineValue: e})
          }} min={0} value={this.state.buyMachineValue} precision={0}/>
          </div>
        </div>
        <div style={{display: 'flex',marginBottom: 24}}>
          <div style={{width: 154, textAlign: 'right', marginRight: 16, color:'#000000E5',fontFamily: "PingFang SC",fontSize: '14px',fontStyle: 'normal',fontWeight: '400',lineHeight: '22px'}}>购买时长(月) </div>
          <div style={{textAlign: 'right', marginRight: 16, lineHeight: '30px'}}>
            <InputNumber onChange={(e) => {
              this.setState({buyMonthValue: e})
            }} min={0} value={this.state.buyMonthValue} precision={0}/>
          </div>
        </div>
        <div style={{display: 'flex',marginBottom: 24}}>
          <div style={{width: 154, textAlign: 'right', marginRight: 16, color:'#000000E5',fontFamily: "PingFang SC",fontSize: '14px',fontStyle: 'normal',fontWeight: '400',lineHeight: '22px'}}>账户余额 </div>
          <div style={{textAlign: 'right', marginRight: 16, color:'#000000'}}>
            <span style={{  overflow: 'hidden',color: 'var(--text-icon-font-gy-190-primary, rgba(0, 0, 0, 0.90))',textOverflow: 'ellipsis', whiteSpace: 'nowrap',fontFamily: "PingFang SC",
            fontSize: '20px',fontStyle: 'normal',fontWeight: '600',}}>$ {this.state.balance}</span>
            <img src="/icons/refresh.png" onClick={() => this.reload()} style={{width: 16, height: 16,marginLeft:8,cursor: 'pointer'}}/>
            <a href="/cloud/user/payment"  rel="noopener noreferrer" style={{marginLeft:10}}>
            立即充值
          </a>
          </div>
        </div>
        <div style={{display: 'flex',marginBottom: 24}}>
          <div style={{width: 154, textAlign: 'right', marginRight: 16, color:'#000000E5',fontFamily: "PingFang SC",fontSize: '14px',fontStyle: 'normal',fontWeight: '400',lineHeight: '22px'}}>总计 </div>
          <div style={{overflow: 'hidden',color: 'var(--Error-Error6-Normal, #D54941)',textOverflow: 'ellipsis',whiteSpace: 'nowrap',
          fontFamily: "PingFang SC",fontSize: '20px',fontStyle: 'normal',fontWeight: '600'}}>{(() => {
            const buyMachineValue = Number(this.state.buyMachineValue) || 0;
            const unitPrice = Number(this.state.unitPrice) || 0;
            const buyMonthValue = Number(this.state.buyMonthValue) || 0;
            const total = buyMachineValue * unitPrice * buyMonthValue;
            return total > 0 ? `$ ${total}` : '$ 0';
          })()}</div>
        </div>
      </Dialog>
    </div></ExternalAccountFilter></MyTranslate>)
  }
}

// 先使用 connect 函数连接 Redux 状态
const ConnectedComponent = connect(({ user }) => ({
  userID: user.info.userID,
  balance:user.info.balance,
  openKeys: user.openKeys
}))(MyComponent);

// 再使用 injectIntl 函数注入国际化功能
const ConnectedAndIntlComponent = injectIntl(ConnectedComponent);
export default ConnectedAndIntlComponent;


