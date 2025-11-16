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
import accountStatus from '../../components/accountStatus'
import MyTranslate from "../common/MyTranslate";
import {formatDate} from 'components/DateFormat'
import { Pagination, Dialog} from 'tdesign-react';

const {Option, OptGroup} = Select;
const {TextArea} = Input;
const Search = Input.Search;
const {TabPane} = Tabs;

const gridStyle = {
  width: '60%',
  padding: "0px",
};

const gridAccountStyle = {
  width: '40%',
  padding: "0px",
};

const rowMargin = {
  marginTop: '10px'
};

class SelectCard extends Component {

  constructor(props) {
    super(props);

    this.state = {
      choosenTab: this.props.title,      //accountGroup/account

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
      choosenPagination: {
        total: 0,
        pageSize: 10,
        current: 1,
        showTotal: (total, range) => `共 ${total} 条`,
        position: 'both',
        showSizeChanger: true,
        pageSizeOptions: ['10', '20', '30', '40', '50', '100', '200', '300', '400', '500']
      },
      selectedRowKeys: [],
      onlineStatus: {},       //账号在线状态 {accID: onlineStatus}
      groups: [],
      selectAccountGroup: [],
      accountGroupData: [],
      accountGroupLoading: false,
      accountGroupPagination: {
        total: 0,
        pageSize: 100,
        current: 1,
        showTotal: (total, range) => `共 ${total} 条`,
        position: 'both',
        // showSizeChanger: true,
        // pageSizeOptions: ['10', '20', '30', '40', '50', '100', '200', '300', '400', '500']
      },
      accountGroupSelectedRowKeys: [],
      accountGroupSelectedRowKeys2: [],
      accountGroupChoosenPagination: {
        total: 0,
        accountCount: 0,
        pageSize: 10000,
        current: 1,
        showTotal: (total, range) => `共 ${total} 条`,
        position: 'both'
      },
      accountGroupCount: {},
      showTable: false,
      scrollY: 0,
      tableContent: null,
      groupSearch: 'all', //全部分组
    }
    // 选中行的数据保存在selectedRows变量中
    // 之所以不放入state，因为这些变量是单向流动的，即Table控件产生这些数据，通过callback更新到变量this.selectedRows
    this.selectedRows = [];
    this.accountGroupSelectedRows = [];
    // filters的写法保持与mongo filter的写法一致，避免与服务器、数据库直接的数据阻抗失衡
    // 例如： filters={username: 'foo'}
    // 因为json协议无法传输regex，所以字符类型的值，都会在服务器端转成regex，以提高匹配度，损失部分查询性能
    this.filters = props.filter || {};
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
    this.reload();
    this.accountGroupReload();
  }
  

  handleResize = () => {
    if (this.state.tableContent) {
      this.setState({scrollY: this.state.tableContent.getBoundingClientRect().height - 100,})
    }
  }

  async componentDidMount() {
    window.addEventListener("resize", this.handleResize);
  }

  componentWillUnmount() {
    window.removeEventListener("resize", this.handleResize);
  }

  async accountGroupReload() {
    this.accountGroupLoad(this.state.accountGroupPagination, this.accountGroupFilters, this.accountGroupSorter);
  }

  async accountGroupLoad(accountGroupPagination, accountGroupFilters, accountGroupSorter) {
    // 这里的参数是格式好的，无需修改，无阻抗失衡，直接发给服务器
    this.onChange({accountGroupLoading: true});
    let res = await axios.post(`/api/consumer/accountGroup/${accountGroupPagination.pageSize}/${accountGroupPagination.current}`, {
      unBanned: true
    });
    accountGroupPagination.total = res.data.data.total;
    this.state.accountGroupChoosenPagination.total = 0;
    this.state.accountGroupChoosenPagination.accountCount = 0;

    this.state.selectAccountGroup = [{
      value: "all",
      label: '全部'
    }];
   for (const g of res.data.data.data) {
    this.state.selectAccountGroup.push({
        value: g._id,
        label: g.groupName
      });
    }
    this.onChange({
      accountGroupLoading: false,
      accountGroupData: res.data.data.data,
      accountGroupCount: res.data.data.total,
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
          //accountCount = this.state.accountGroupCount[row._id].accountCount;
          break
        }
      } else {
        keys.push(row._id);
        rows.push(row);
        if (this.state.accountGroupCount[row._id]) {
          //accountCount += this.state.accountGroupCount[row._id].accountCount
        }
      }
    }
    this.state.accountGroupChoosenPagination.accountCount = accountCount;
    this.state.accountGroupChoosenPagination.pageSize = this.state.accountGroupPagination.pageSize;
    this.state.accountGroupChoosenPagination.total = keys.length;
    // 选中状态的数据，因为无需受控，就不记录在state里了，提高效率
    this.onChange({
      rows,
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
  }

  /*
  async accountTotalCountSearch(pagination, filters, sorter) {
    let res = await axios.post(`/api/consumer/account/accountTotalCount`, {
      filters, sorter,
      unBanned: true
    });
    pagination.total = res.data.total;
    this.onChange({pagination});
  }*/

  async load(pagination,filters, sorter) {
    // 这里的参数是格式好的，无需修改，无阻抗失衡，直接发给服务器
    this.onChange({loading: true});
    filters['onlineStatus'] = "1";
    let res = await axios.post(`/api/consumer/account/${pagination.pageSize}/${pagination.current}`, {
      filters, sorter});
    let onlineStatus = {};
    for (const acc of res.data.data.data) {
     if (!onlineStatus[acc.accID]) {
        onlineStatus[acc.accID] = acc.onlineStatus;
      }
    }
    pagination.total = res.data.data.total;

    this.state.choosenPagination.total = 0;
    this.onChange({
      loading: false,
      data: res.data.data.data,
      selectedRowKeys: [],
      pagination
    });
    this.selectedRows = [];
    this.filters = filters;
    this.sorter = sorter
  }

  async onRowSelectionChange(selectedRowKeys, selectedRows) {
    if (selectedRows.filter(row => this.state.onlineStatus[row.accID] === '0').length > 0) {
      message.error('请不要选择离线的账号');
    } else if (selectedRows.filter(row => this.state.onlineStatus[row.accID] === '-1').length > 0) {
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

  async handleGroupTableChange(pagination, filters, sorter) {
    // 此处是table控件的回调，其中的参数格式不是很合理，调整一下后，传递给load
    let sort = this.sorter;
    if (sorter && sorter.field) {
      sort = {};
      sort[sorter.field] = sorter.order == 'descend'
        ? -1
        : 1
    }
    this.accountGroupLoad(pagination, this.filters, sort)
  }


  //分组筛选
  async changeGroupFilter(e) {
    e = { target: { value: e } };
    this.setState({ groupSearch: e.target.value });
    if (e.target.value == '全部') {
      delete this.filters.groupID;
    } else {
      this.filters['groupID'] = e.target.value;
    }
  }

  async changeTab(key) {
    this.onChange({choosenTab: key});
  }

  async onChange(data) {
    let onChangeData = {};
    this.setState(data)
    for (let key in data) {
      if (['choosenTab', 'selectedRowKeys', 'rows', 'accountGroupSelectedRowKeys'].indexOf(key) != -1) {
        onChangeData[key] = data[key];
      }
    }
    this.props.onChange(onChangeData);
  }

  async reset() {
    this.filters = {}
    this.setState({remark: '', phone: '', groupSearch: 'all' })
    this.load(this.state.pagination,this.filters, this.sorter)
  }

  refTableContent = (ref) => {
    if (ref && ref.getBoundingClientRect) {
      this.setState({showTable: true, scrollY: ref.getBoundingClientRect().height - 100, tableContent: ref})
    }
  }

  render() {
    const groupColumns = [
      {
        title: '分组名称',
        dataIndex: 'groupName',
        key: 'groupName',
        translateRender: true,
        render: (v, r) => {
          //  只翻译全部分组
          return <div translate={false}>{v}</div>
        }
      }, {
        title: '账号数量',
        dataIndex: 'accountOnlineNum',
        key: 'accountOnlineNum'
      }
      , {
        title: '创建时间',
        dataIndex: 'createTime',
        key: 'createTime',
        render: formatDate,
        width: 200,
        ellipsis: true,
      }
    ];

    const sGroupColumns = [
      {
        title: '分组名称',
        dataIndex: 'groupName',
        key: 'groupName',
        translateRender: true,
        render: (v, r) => {
          //  只翻译全部分组
          return <div translate={false}>{v}</div>
        }
      }, {
        title: '账号数量',
        dataIndex: 'accountOnlineNum',
        key: 'accountOnlineNum'
      }
    ];


    const columns = [
      {
        title: '手机号',
        dataIndex: 'phone',
        key: 'phone',
      },
      {
        title: '备注',
        dataIndex: 'remark',
        key: 'remark'
      },
      ,
      {
        title: '归属分组',
        dataIndex: 'groupName',
        key: 'groupName'
      },
    ];

    const sAccountColumns = [
      {
        title: '手机号',
        dataIndex: 'phone',
        key: 'phone',
      },
      {
        title: '备注',
        dataIndex: 'remark',
        key: 'remark'
      }
    ];


    return (<MyTranslate>
      <Card>
        <Tabs defaultActiveKey={this.state.choosenTab} onChange={this.changeTab.bind(this)}>
        {this.props.title == 'accountGroup' ?
          <TabPane key="accountGroup">
            <Card.Grid style={gridStyle}>
              <div class="main-content">
                <div>
                  <div>分组列表</div>
                </div>
                <div className="tableContent" ref={this.refTableContent}>
                  <div>
                    {this.state.showTable ? <Table
                      tableLayout="fixed"
                      scroll={{ y: this.state.scrollY, x: 700 }}
                      pagination={this.state.accountGroupPagination} rowSelection={{
                        selectedRowKeys: this.state.accountGroupSelectedRowKeys,
                        onChange: this.accountGroupOnRowSelectionChange.bind(this)
                      }} columns={groupColumns} rowKey='_id' dataSource={this.state.accountGroupData} loading={this.state.loading} /> : ''}
                  </div>
                </div>
                <Pagination
                  showJumper
                  total={this.state.accountGroupPagination.total}
                  current={this.state.accountGroupPagination.current}
                  pageSize={this.state.accountGroupPagination.pageSize}
                  onChange={this.handleGroupTableChange.bind(this)}
                />
              </div>
            </Card.Grid>
            <Card.Grid style={gridAccountStyle}>
              <div class="main-content">
                <div>
                  <div>{`已选分组(${this.state.accountGroupChoosenPagination.accountCount}`})</div>
                </div>
                <div className="tableContent" ref={this.refTableContent}>
                  <div>
                    {this.state.showTable ? <Table
                      tableLayout="fixed"
                      scroll={{ y: this.state.scrollY, x: 300 }}
                      pagination={this.state.accountGroupChoosenPagination}
                      rowSelection={{
                        selectedRowKeys: this.state.accountGroupSelectedRowKeys,
                        onChange: this.accountGroupOnRowSelectionChange.bind(this)
                      }}
                      columns={sGroupColumns} rowKey='_id'
                      dataSource={this.state.accountGroupSelectedRowKeys.length ? this.accountGroupSelectedRows : []}
                      loading={this.state.accountGroupLoading} /> : ''}
                  </div>
                </div>
                <Pagination
                  showJumper
                  total={this.state.accountGroupChoosenPagination.total}
                  current={this.state.accountGroupChoosenPagination.current}
                  pageSize={this.state.accountGroupChoosenPagination.pageSize}
                />
              </div>
            </Card.Grid>
          </TabPane>
           : ''
          }
          {this.props.title == 'account' ?
          <TabPane key="account">
            <div className="search-box">

              <div className='search-item'>
                <div className="search-item-label">手机号</div>
                <div className="search-item-right">
                  <Input style={{
                    width: 200
                  }} placeholder={'请输入'} value={this.state.phone} onChange={(value) => {
                    value = value.target.value;
                    if (value) {
                      this.filters.phone = value;
                    }
                    else {
                      delete this.filters.phone;
                    }

                    this.setState({ phone: value });
                    // this.reload()
                  }} />
                </div>
              </div>

              
              <div className='search-item'>
                <div className="search-item-label">备注</div>
                <div className="search-item-right">
                  <Input style={{
                    width: 200
                  }} placeholder={'请输入'} value={this.state.remark} onChange={(value) => {
                    value = value.target.value;
                    if (value) {
                      this.filters.remark = value;
                    }
                    else {
                      delete this.filters.remark;
                    }

                    this.setState({ remark: value });
                    // this.reload()
                  }} />
                </div>
              </div>


              <div className='search-item'>
                <div className="search-item-label">分组</div>
                <div className="search-item-right">
                  <Select value={this.state.groupSearch} style={{ width: 200 }} onChange={this.changeGroupFilter.bind(this)}>
                    {this.state.selectAccountGroup.map(ws => {
                      return <Option value={ws.value}>{ws.label}</Option>
                    })}
                  </Select>
                </div>
              </div>
              <div className='accountGroup-btn'>
                <div className="search-query-btn" onClick={() => this.reload()}>查询</div>
                <div className="search-reset-btn" onClick={() => this.reset()}>重置</div>
              </div>
            </div>
            <Card.Grid style={gridStyle}>
              <div class="main-content" style={{marginTop: '0px'}}>
              {<div>
                  <div className="tableSelectedCount">{`已选${this.state.choosenPagination.total}项`}</div>
                </div> }
                <div className="tableContent" ref={this.refTableContent} style={{height: 'calc(100vh - 492px)'}}>
                  <div>
                    {this.state.showTable ? <Table
                      tableLayout="fixed"
                      scroll={{ y: this.state.scrollY, x: 700 }}
                      pagination={this.state.pagination} rowSelection={{
                        selectedRowKeys: this.state.selectedRowKeys,
                        onChange: this.onRowSelectionChange.bind(this)
                      }} columns={columns} rowKey='_id' dataSource={this.state.data} loading={this.state.loading} /> : ''}
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
            </Card.Grid>
            <Card.Grid style={gridAccountStyle}>
              <div class="main-content" style={{marginTop: '0px'}}>
              {<div>
                  <div className="tableSelectedCount">{`已选账号(${this.state.choosenPagination.total})`}</div>
                </div> }
                <div className="tableContent" ref={this.refTableContent} style={{height: 'calc(100vh - 492px)'}}>
                  <div>
                    {this.state.showTable ? <Table
                      tableLayout="fixed"
                      scroll={{ y: this.state.scrollY, x: 300 }}
                      pagination={this.state.choosenPagination}
                      columns={sAccountColumns} rowKey='_id'
                      dataSource={this.state.selectedRowKeys.length ? this.selectedRows : []}
                      loading={this.state.loading} /> : ''}
                  </div>
                </div>
                <Pagination
                  showJumper
                  total={this.state.choosenPagination.total}
                  current={this.state.choosenPagination.current}
                  pageSize={this.state.choosenPagination.pageSize}
                />
              </div>
            </Card.Grid>
          </TabPane>
           : ''
          }
        </Tabs>
      </Card>
    </MyTranslate>)
  }
}

export default SelectCard;
