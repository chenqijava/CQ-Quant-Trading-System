import React, {Component} from 'react'
import {BrowserRouter as Router, Route, Link} from 'react-router-dom'
import {
  Icon,
  Table,
  Divider,
  message,
  Row,
  Col,
  Button,
  Radio,
  Alert,
  Select,
  Card,
  Avatar,
  Upload,
  Input,
  Tabs,
  DatePicker,
  Modal,
  Tag
} from 'antd'
import axios from 'axios'
import moment from 'moment';
import {formatDate} from 'components/DateFormat'
const {RangePicker} = DatePicker;
import accountStatus from 'components/accountStatus'
import InputFinderFeed from "components/msg/inputFinderFeed";
import InputImageMsg from "components/msg/inputImageMsg";

const {Option, OptGroup} = Select;
const {TabPane} = Tabs;
const confirm = Modal.confirm
const {TextArea} = Input;

// 针对当前页面的基础url
const baseUrl = '/api/chatroom';
const consumer = '/api/consumer/chatroom';
const uploadProps = {
  name: 'file',
  multiple: false,
  action: '/api/consumer/res/wxHeadUpload',
  beforeUpload(file, fileList) {
    const isLt2M = file.size / 1024 / 1024 < 3;
    if (!isLt2M) {
      message.error('文件必须小于3M!');
      return false
    }
    let jpg = /^.+\.(jpg|png)$/.test(file.name.toLowerCase());
    if (!jpg) {
      message.error('文件名不合法')
    }
    return jpg
  }
};

function getBase64(file) {
  return new Promise((resolve, reject) => {
    const reader = new FileReader();
    reader.readAsDataURL(file);
    reader.onload = () => resolve(reader.result);
    reader.onerror = error => reject(error);
  });
}

const gridStyle = {
  width: '50%',
  padding: "10px"
};

const gridStyle2 = {
  width: '100%',
  padding: "10px"
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
        position: 'both',
        showSizeChanger: true,
        pageSizeOptions: ['10', '20', '30', '40', '50', '100', '200', '300', '400', '500']
      },
      selectedRowKeys: [],

      choosenPagination: {
        total: 0,
        pageSize: 10,
        current: 1,
        showTotal: (total, range) => `共 ${total} 条`,
        position: 'both'
      },

      nicknamesMap: {},

      groupRadioOptions: [],   //分组信息 [{},{}]

      groupLoading: false,
      roomGroups: [],         //群分组

      ownerAccID: '',
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
  }

  // 首次加载数据
  async componentWillMount() {
    this.selectAllRoomGroup();
    this.getGroups();
  }

  async componentDidUpdate(prevProps) {
    let oldPageType = prevProps.pageType;
    let newPageType = this.props.pageType;
    if (oldPageType !== newPageType || newPageType !== this.state.pageType)
      this.reloadByPageAddMethod(newPageType);
  }

  async reloadByPageAddMethod(pageType) {
    let state = {
      pageType,
    };
    switch (pageType) {
      case 'sendScene':
        this.inTask = false;
        break;
      case 'sendFinderFeed':
      case 'sendMsg':
      default:
        delete this.inTask;
    }
    await this.setState(state);
    this.reload();
  }

  async reload() {
    // 重新加载，一般是页面第一次加载的时候来一下
    this.load(this.state.pagination, this.filters, this.sorter)
  }

  async load(pagination, filters, sorter) {
    // 这里的参数是格式好的，无需修改，无阻抗失衡，直接发给服务器
    this.setState({loading: true});
    let removeDuplicateAccID = this.props.removeDuplicateAccID;

    let res = await axios.post(`${consumer}/simpleList/${pagination.pageSize}/${pagination.current}`, {
      filters, sorter,
      unBanned: true,
      removeDuplicateAccID,
      inTask: this.inTask,
    });
    pagination.total = res.data.total;
    this.state.choosenPagination.total = 0;
    let state = {
      loading: false, data: res.data.data, pagination, selectedRowKeys: [],
      choosenPagination: this.state.choosenPagination
    };
    let nicknamesMap = {};
    if (removeDuplicateAccID) {
      state.data.forEach(cr => {
        let nickname = cr.nickname;
        if (nicknamesMap[nickname]) nicknamesMap[nickname]++;
        else nicknamesMap[nickname] = 1;
      });
    }
    state.nicknamesMap = nicknamesMap;
    this.setState(state);
    this.selectedRows = [];
    this.filters = filters;
    this.sorter = sorter
  }

  async onRowSelectionChange(selectedRowKeys, selectedRows) {
    // 选中状态的数据，因为无需受控，就不记录在state里了，提高效率
    this.state.choosenPagination.pageSize = this.state.pagination.pageSize;
    this.state.choosenPagination.total = selectedRowKeys.length;
    this.setState({selectedRowKeys, choosenPagination: this.state.choosenPagination});
    this.selectedRows = selectedRows
    if (typeof this.props.onChange == 'function') {
      this.props.onChange({selectedRowKeys, selectedRows})
    }
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

  async selectAllRoomGroup() {
    // 这里的参数是格式好的，无需修改，无阻抗失衡，直接发给服务器
    this.setState({groupLoading: true});
    let res = await axios.get(`${consumer}/group/groupInfo`);

    this.setState({
      groupLoading: false,
      roomGroups: res.data.group
    });
  }

  async getGroups() {
    let res = await axios.get(`/api/consumer/accountGroup/accountGroupInfo`);
    let arr = [{
      value: "全部",
      label: '全部'
    }];
    let groupJson = {};
    for (const g of res.data.group) {
      if (g.groupName === '默认分组') {
        this.setState({groupID: g._id});
      }
      arr.push({
        value: g._id,
        label: g.groupName
      });
      groupJson[g._id] = g.groupName
    }
    this.setState({
      group: res.data.group,
      groupId2Name: groupJson,
      groupRadioOptions: arr
    });
  }

  //所属账号的分组筛选
  async changeOwnerGroupIDFilter(v) {
    if (v == '全部') {
      delete this.filters.ownerGroupID;
    } else {
      this.filters['ownerGroupID'] = v;
    }
    this.reload()
  }

  //群组筛选
  async changeGroupFilter(v) {
    if (v == '-1') {
      delete this.filters.roomGroupID;
    } else {
      this.filters['roomGroupID'] = v;
    }
    this.reload()
  }

  async changeFilters(type, value) {
    if (!value) {
      delete this.filters[type];
    } else {
      this.filters[type] = value;
    }
    this.reload()
  }

  // 把columns放到render中，虽然损失部分性能，但是能方便参数中的匿名回调获取实例状态
  render() {

    const columns = [
      {
        title: '群名称',
        dataIndex: 'nickname',
        key: 'nickname',
        sorter: true,
        render: (v, r) => {
          let style = {};
          let isDuplicateName = this.state.nicknamesMap[v] > 1;
          if (isDuplicateName) style.backgroundColor = 'red';

          let members = r.membersCache || [];
          let hasSuperAdmin = !!members.find(m => m.permission == 'superadmin');      //  群主
          let hasAdmin = !!members.find(m => m.permission == 'admin');                //  管理员
          return (<div style={style}>
            <span>{v}{isDuplicateName ? `(${r._id})` : ''}</span>
            <span style={{float: 'right'}}>
              <span style={{margin: '0 10px 0 10px'}}>{hasSuperAdmin ? '有群主' : ''}</span>
              <span>{hasAdmin ? '' : '没有管理员'}</span>
            </span>
          </div>)
        }
      }, {
        title: '所属账号',
        dataIndex: 'ownerAccid.nickname',
        key: 'ownerAccid.nickname',
        render: (v, r)=> {
          let status = accountStatus.find(ws => ws.value === r.ownerAccid.onlineStatus);
          if (status) {
            return (<div>{v} <Tag color={status.color}>{status.label}</Tag></div>)
          } else {
            return <div>{v}</div>
          }
        }
      }
    ];

    return (
        <Card title={this.props.title || "第一步：选择群"}>
          <Card.Grid style={gridStyle}>
            <Row>
              <span>群</span>
              {[
                {key: 'nickname', placeholder: '群名称查询'},
                {key: 'ownerAccID', placeholder: '所属账号的accID查询'},
              ].map(({key, placeholder}) =>
                  <Input style={{width: 150, float: 'right'}} placeholder={placeholder}
                         size='small' onPressEnter={(e) => this.changeFilters(key, e.target.value)}
                         value={this.state[key]} onChange={(e) => {
                    this.setState({[key]: e.target.value})
                  }}/>
              )}
              <Select size='small' placeholder="群组查询" style={{width: 130, float: 'right', marginRight: '10px'}}
                      onChange={this.changeOwnerGroupIDFilter.bind(this)} loading={this.state.groupLoading}>
                {
                  this.state.groupRadioOptions.map(item => (
                      <Select.Option key={item.value} value={item.value}>
                        {item.label}
                      </Select.Option>
                  ))
                }
              </Select>
              {/*<Select size='small' placeholder="群组查询" style={{width: 130, float: 'right', marginRight: '10px'}}*/}
              {/*        onChange={this.changeGroupFilter.bind(this)} loading={this.state.groupLoading}>*/}
              {/*  {<Select.Option key='-1' value='-1'>*/}
              {/*    全部群组*/}
              {/*  </Select.Option>}*/}
              {/*  {*/}
              {/*    this.state.roomGroups.map(item => (*/}
              {/*        <Select.Option key={item._id} value={item._id}>*/}
              {/*          {item.groupName}*/}
              {/*        </Select.Option>*/}
              {/*    ))*/}
              {/*  }*/}
              {/*</Select>*/}
              <div style={{float: 'right'}}>
                创建时间：
                <RangePicker size='small' allowClear={false} showTime format="YYYY-MM-DD HH:mm:ss"
                             placeholder={['开始时间', '结束时间']} onOk={value => {
                  this.filters.createTime = {
                    $gt: value[0].toISOString(),
                    $lt: value[1].toISOString()
                  };
                  this.reload()
                }}/>
              </div>
              {this.state.pageType == 'sendScene' ?
                  <Select size='small' placeholder="任务状态查询" style={{width: 130, float: 'right', marginRight: '10px'}}
                          onChange={(v) => {
                            if (v == 'all') {
                              delete this.inTask;
                            } else {
                              this.inTask = v;
                            }
                            this.reload()
                          }} loading={this.state.groupLoading} defaultValue={false}>
                    <Select.Option key='all' value='all'>
                      全部
                    </Select.Option>
                    <Select.Option key='true' value={true}>
                      任务中
                    </Select.Option>
                    <Select.Option key='false' value={false}>
                      非任务中
                    </Select.Option>
                  </Select> : ''}
            </Row>
            <Table pagination={this.state.pagination} size="small" rowSelection={{
              selectedRowKeys: this.state.selectedRowKeys,
              onChange: this.onRowSelectionChange.bind(this)
            }} columns={columns} rowKey='_id' dataSource={this.state.data} loading={this.state.loading}
                   scroll={{y: 490}} onChange={this.handleTableChange.bind(this)}/>
          </Card.Grid>
          <Card.Grid style={gridStyle}>
            <span>已选群 (已选群数:{this.state.choosenPagination.total})</span>
            <Table pagination={this.state.choosenPagination} size="small"
                   columns={columns} rowKey='_id' scroll={{y: 490}} style={{marginTop: '3px'}}
                   dataSource={this.state.selectedRowKeys.length ? this.selectedRows : []} loading={this.state.loading}/>
          </Card.Grid>
        </Card>
    )
  }
}

export default MyComponent
