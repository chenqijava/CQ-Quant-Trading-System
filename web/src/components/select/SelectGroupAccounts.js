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
  Input,
  InputNumber,
  TimePicker,
  Upload,
  Modal,
  Tag,
  Tabs
} from 'antd'
import moment from 'moment';
import axios from 'axios'
import accountStatus from 'components/accountStatus'

const {Option, OptGroup} = Select;
const {TextArea} = Input;
const Search = Input.Search;
const {TabPane} = Tabs;

const gridStyle = {
  width: '50%',
  padding: "20px"
};

const rowMargin = {
  marginTop: '10px'
};

/**
 * 选择可用(非被封账号)
 */
class SelectCard extends Component {

  constructor(props) {
    super(props);
    this.state = {
      choosenTab: 'accountGroup',      //accountGroup/account

      data: [],
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
      onlineStatus: {},       //账号在线状态 {accID: onlineStatus}
      groups: [],
      selectAccountGroup: [],


      accountGroupData: [],
      accountGroupLoading: false,
      accountGroupPagination: {
        total: 0,
        pageSize: 10000,
        current: 1,
        showTotal: (total, range) => `共 ${total} 条`,
        position: 'both',
        // showSizeChanger: true,
        // pageSizeOptions: ['10', '20', '30', '40', '50', '100', '200', '300', '400', '500']
      },
      accountGroupSelectedRowKeys: [],
      accountGroupChoosenPagination: {
        total: 0,
        accountCount: 0,
        pageSize: 10000,
        current: 1,
        showTotal: (total, range) => `共 ${total} 条`,
        position: 'both'
      },
      accountGroupCount: {},
    }
    // 选中行的数据保存在selectedRows变量中
    // 之所以不放入state，因为这些变量是单向流动的，即Table控件产生这些数据，通过callback更新到变量this.selectedRows
    this.selectedRows = [];
    this.accountGroupSelectedRows = [];
    // filters的写法保持与mongo filter的写法一致，避免与服务器、数据库直接的数据阻抗失衡
    // 例如： filters={username: 'foo'}
    // 因为json协议无法传输regex，所以字符类型的值，都会在服务器端转成regex，以提高匹配度，损失部分查询性能
    this.filters = {};
    this.accountGroupFilters = {};
    // sorter的写法保持与mongo sort的写法一致，避免与服务器、数据库直接的数据阻抗失衡
    // 例如：sorter={createTime: -1}
    // 注意：Table控件仅支持单列排序，不支持多列同时排序
    this.sorter = {
      createTime: -1
    };
    this.accountGroupSorter = {
      createTime: -1
    };
    // 提前写在constructor则Table首次加载时生效
    // 但是因为没有放到state中，所以除sorter外，都不建议提前设置，保持不受控状态是最好的
  }

  async componentWillMount() {
    this.accountGroupReload();
    this.reload();
  }

  async accountGroupReload() {
    this.accountGroupLoad(this.state.accountGroupPagination, this.accountGroupFilters, this.accountGroupSorter);
  }

  async accountGroupLoad(accountGroupPagination, accountGroupFilters, accountGroupSorter) {
    // 这里的参数是格式好的，无需修改，无阻抗失衡，直接发给服务器
    this.onChange({accountGroupLoading: true});
    let res = await axios.post(`/api/consumer/accountGroup/${accountGroupPagination.pageSize}/${accountGroupPagination.current}`, {
      filters: accountGroupFilters, sorter: accountGroupSorter,
      unBanned: false,
      onlineStatus:{$in:['0','1']}
    });
    accountGroupPagination.total = res.data.total;
    this.state.accountGroupChoosenPagination.total = 0;
    this.state.accountGroupChoosenPagination.accountCount = 0;
    this.onChange({
      accountGroupLoading: false,
      accountGroupData: res.data.data,
      accountGroupCount: res.data.groupCount,
      accountGroupSelectedRowKeys: [],
      accountGroupPagination,
      accountGroupChoosenPagination: this.state.accountGroupChoosenPagination
    });
    this.accountGroupSelectedRows = [];
    this.accountGroupFilters = accountGroupFilters;
    this.accountGroupSorter = accountGroupSorter
  }

  async accountGroupOnRowSelectionChange(accountGroupSelectedRowKeys, accountGroupSelectedRows) {
    let keys = [], rows = [];
    let accountCount = 0;
    let deleteAllGroup = false;
    if (this.state.accountGroupSelectedRowKeys.indexOf('allGroup') !== -1 && accountGroupSelectedRowKeys.length > 1) {
      deleteAllGroup = true;
    }
    for (const row of accountGroupSelectedRows) {
      if (row._id === 'allGroup') {
        if (!deleteAllGroup) {
          keys = [row._id];
          rows = [row];
          accountCount = this.state.accountGroupCount[row._id].accountCount;
          break
        }
      } else {
        keys.push(row._id);
        rows.push(row);
        if (this.state.accountGroupCount[row._id]) {
          accountCount += this.state.accountGroupCount[row._id].accountCount
        }
      }
    }
    this.state.accountGroupChoosenPagination.accountCount = accountCount;
    this.state.accountGroupChoosenPagination.pageSize = this.state.accountGroupPagination.pageSize;
    this.state.accountGroupChoosenPagination.total = keys.length;
    // 选中状态的数据，因为无需受控，就不记录在state里了，提高效率
    this.onChange({
      accountCount,
      accountGroupSelectedRowKeys: keys,
      accountGroupChoosenPagination: this.state.accountGroupChoosenPagination
    });
    this.accountGroupSelectedRows = rows
  }

  async accountGroupHandleTableChange(accountGroupPagination, accountGroupFilters, accountGroupSorter) {
    // 此处是table控件的回调，其中的参数格式不是很合理，调整一下后，传递给load
    let sort = this.accountGroupSorter;
    if (accountGroupSorter && accountGroupSorter.field) {
      sort = {};
      sort[accountGroupSorter.field] = accountGroupSorter.order == 'descend'
        ? -1
        : 1
    }
    // 暂时不用Table的filter，不太好用
    this.accountGroupLoad(accountGroupPagination, this.accountGroupFilters, sort)
  }

  async reload() {
    // 重新加载，一般是页面第一次加载的时候来一下
    this.load(this.state.pagination, this.filters, this.sorter);
    this.accountTotalCountSearch(this.state.pagination, this.filters, this.sorter);
  }

  async accountTotalCountSearch(pagination, filters, sorter) {
    filters = {
      ...filters,
      onlineStatus:{$in:['0','1']}
    }
    let res = await axios.post(`/api/consumer/account/accountTotalCount`, {
      filters, sorter,
      unBanned: false,
    });
    pagination.total = res.data.total;
    this.onChange({pagination});
  }

  async load(pagination, filters, sorter) {
    // 这里的参数是格式好的，无需修改，无阻抗失衡，直接发给服务器
    this.onChange({loading: true});
    filters = {
      ...filters,
      onlineStatus:{$in:['0','1']}
    }
    let res = await axios.post(`/api/consumer/account/groupAccountList/${pagination.pageSize}/${pagination.current}`, {
      filters, sorter,
      unBanned: false,
    });
    let selectAccountGroup = [{
      value: "all",
      label: '全部账号'
    }];
    for (const g of res.data.group) {
      selectAccountGroup.push({
        value: g._id,
        label: g.groupName
      });
    }
    let onlineStatus = {};
    for (const acc of res.data.data) {
      if (!onlineStatus[acc.accID]) {
        onlineStatus[acc.accID] = acc.onlineStatus;
      }
    }
    this.state.choosenPagination.total = 0;
    this.onChange({
      loading: false,
      data: res.data.data,
      selectedRowKeys: [],
      groups: res.data.group,
      selectAccountGroup,
      onlineStatus,
      pagination,
      choosenPagination: this.state.choosenPagination
    });
    this.selectedRows = [];
    this.filters = filters;
    this.sorter = sorter
  }

  async onRowSelectionChange(selectedRowKeys, selectedRows) {
    if (selectedRows.filter(row => this.state.onlineStatus[row.accID] === '-1').length > 0) {
      message.error('请不要选择被封的账号');
    }
    // 选中状态的数据，因为无需受控，就不记录在state里了，提高效率
    this.state.choosenPagination.pageSize = this.state.pagination.pageSize;
    this.state.choosenPagination.total = selectedRowKeys.length;
    this.onChange({selectedRowKeys, choosenPagination: this.state.choosenPagination});
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

  //分组筛选
  async changeGroupFilter(v) {
    if (v == 'all') {
      delete this.filters.groupID;
    } else {
      this.filters['groupID'] = v;
    }
    this.reload()
  }

  async changeTab(key) {
    this.onChange({choosenTab: key});
  }

  async onChange(data) {
    let onChangeData = {};
    this.setState(data)
    for (let key in data) {
      if (['choosenTab', 'selectedRowKeys', 'accountCount', 'accountGroupSelectedRowKeys'].indexOf(key) != -1) {
        onChangeData[key] = data[key];
      }
    }
    this.props.onChange(onChangeData);
  }

  render() {
    const groupColumns = [
      {
        title: '组名',
        dataIndex: 'groupName',
        key: 'groupName'
      }, {
        title: '在线账号',
        dataIndex: 'accountCount',
        key: 'accountCount',
        render: (v, r)=> {
          return this.state.accountGroupCount[r._id] ? this.state.accountGroupCount[r._id].accountCount : 0;
        }
      }
    ];

    const columns = [
      {
        title: '昵称',
        dataIndex: 'nickname',
        key: 'nickname',
        render: (v, r)=> {
          let status = accountStatus.find(ws => ws.value === this.state.onlineStatus[r.accID]);
          return (<div>{v}<Tag color={status.color}>{status.label}</Tag></div>);
        }
      }, {
        title: 'accID',
        dataIndex: 'accID',
        key: 'accID'
      }
    ];

    return (
      <Card title={this.props.title || "第一步：选择在线账号"}>
        <Tabs defaultActiveKey={this.state.choosenTab} onChange={this.changeTab.bind(this)}>
          <TabPane tab="账号分组" key="accountGroup">
            <Card.Grid style={gridStyle}>
              <div>
                <span>分组列表</span>
              </div>
              <Table pagination={this.state.accountGroupPagination} size="small" rowSelection={{
                selectedRowKeys: this.state.accountGroupSelectedRowKeys,
                onChange: this.accountGroupOnRowSelectionChange.bind(this)
              }} columns={groupColumns} rowKey='_id' dataSource={this.state.accountGroupData}
                     loading={this.state.accountGroupLoading}
                     scroll={{ y: 490 }} onChange={this.accountGroupHandleTableChange.bind(this)}/>
            </Card.Grid>
            <Card.Grid style={gridStyle}>
              <span>已选账号 ({this.state.accountGroupChoosenPagination.accountCount})</span>
              <Table pagination={this.state.accountGroupChoosenPagination} size="small"
                     columns={groupColumns} rowKey='_id' scroll={{ y: 490 }} style={{marginTop: '3px'}}
                     dataSource={this.state.accountGroupSelectedRowKeys.length ? this.accountGroupSelectedRows : []}
                     loading={this.state.accountGroupLoading}/>
            </Card.Grid>
          </TabPane>
        </Tabs>
      </Card>
    )
  }
}

export default SelectCard;
