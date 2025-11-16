import React, {Component} from 'react'
import {Button, Checkbox, Col, DatePicker, Input, message, Modal, Radio, Row, Select, Switch, Table, Tabs,} from 'antd'
import axios from 'axios'
import {formatDate} from 'components/DateFormat'
import EditGroupSocks5Area from "./groupSetButtons/EditGroupSocks5Area";
import EditLabels from "./setButtons/EditLabels";
import moment from 'moment';
import ExternalAccountFilter from "components/common/ExternalAccountFilter";
import MyTranslate from "components/common/MyTranslate";
import {injectIntl} from 'react-intl'
import {Breadcrumb, Dialog, Pagination, Tag, Tooltip as TTooltip} from 'tdesign-react';
import DialogApi from './dialog/DialogApi';

const {TextArea} = Input;
const {Option} = Select;
const Search = Input.Search;
const confirm = Modal.confirm;
const {TabPane} = Tabs;

// 针对当前页面的基础url
const baseUrl = '/api/accountGroup';
const consumer = '/api/consumer/accountGroup';
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
      washVisible: false,
      washSelected: ['1', '2'],
      pagination: {
        pageSize: 10,
        current: 1,
        showTotal: (total, range) => `共 ${total} 条`,
        showSizeChanger: true,
        pageSizeOptions: ['10', '20', '30', '40', '50', '100', '200', '300', '400', '500']
      },
      selectedRowKeys: [],
      groupCount: {},        //各分组下账号数量 {group._id: {accountCount: 0, verifyFriendCount: 0}}

      onlineStatus: '1',

      addVisible: false,          //创建分组
      addValue: '',

      editVisible: false,         //修改分组名称
      editValue: '',

      showTable: false,
      scrollY: 0,
      tableContent: null,
      groupNameSearch: '',
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
    this.reload();
  }

  handleResize = () => {
    if (this.state.tableContent) {
      this.setState({scrollY: this.state.tableContent.getBoundingClientRect().height - 80,})
    }
  }

  async componentDidMount() {
    window.addEventListener("resize", this.handleResize);
  }

  componentWillUnmount() {
    window.removeEventListener("resize", this.handleResize);
  }

  async componentDidUpdate(prevProps) {
    let splits = this.props.match.url.split('/');
    let pageType = splits[splits.length - 1];
    if (pageType != this.state.pageType) {
      this.setState({pageType});
    }
  }

  async reload() {
    // 重新加载，一般是页面第一次加载的时候来一下
    this.state.pagination.current = 1;
    this.setState({pagination: this.state.pagination})
    this.load(this.state.pagination, this.filters, this.sorter)
  }

  async load(pagination, filters, sorter) {
    // 这里的参数是格式好的，无需修改，无阻抗失衡，直接发给服务器
    this.setState({loading: true});
    let onlineStatus = null;
    // if (this.state.onlineStatus != '全部') onlineStatus = this.state.onlineStatus
    let res = await axios.post(`${consumer}/${pagination.pageSize}/${pagination.current}`, {
      userAdmin: true,
      filters,
      sorter,
      onlineStatus,
      includeAccountNum: true
    });
    pagination.total = res.data.data.total;
    this.setState({
      washVisible: false,
      loading: false,
      data: res.data.data.data,
      pagination,
      selectedRowKeys: [],
      groupCount: res.data.groupCount
    });
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

  async delete(r) {
    // console.log('====CQ',r)
    let keys = [];
    if (r) {
      if (r.groupName === '默认分组') {
        message.error('默认分组不可操作');
        return
      } else {
        keys.push(r._id);
      }
    } else {
      if (this.state.selectedRowKeys.length == 0) {
        message.error('请先选择数据');
        return
      }

      for (const row of this.selectedRows) {
        if (row.groupName === '默认分组') {
          message.error('默认分组不可操作');
          return
        } else {
          keys.push(row._id);
        }
      }
    }
    if (keys.length == 0) return;
    DialogApi.warning({
      title: '确定要删除这些数据？',
      content: '只能删除账号数量为0的分组',
      onOkTxt: '确定删除',
      onCancelTxt: '取消',
      onOk: async() => {
        this.setState({loading: true});
        let result = await axios.post(`${consumer}/delete`, keys);
        if (result.data.code === 0) {
          DialogApi.error({
            title: '无法删除',
            content: result.data.message,
            onOkTxt: "关闭",
            onOk: async()=> {
              this.reload()
            }
          })
        } else {
          message.success('操作成功');
        }
        this.reload()
      },
      onCancel() {
      }
    })
    // confirm({
    //   title: '确定要删除这些数据？',
    //   content: '只能删除账号数量为0的分组',
    //   okText: '确定',
    //   okType: 'danger',
    //   cancelText: '取消',
    //   onOk: async() => {
    //     this.setState({loading: true});
    //     let result = await axios.post(`${consumer}/delete`, keys);
    //     if (result.data.failedType && result.data.failedType.length) {
    //       Modal.error({
    //         title: '失败信息展示',
    //         content: result.data.failedType.map(type=>(
    //           <div>
    //             <p>{type}：</p>
    //             <p>组名：{result.data.failed[type].join(',')}</p>
    //           </div>
    //         )),
    //         okText: "关闭",
    //         onOk: async()=> {
    //           this.reload()
    //         }
    //       });
    //     } else {
    //       message.success('操作成功');
    //     }
    //     this.reload()
    //   },
    //   onCancel() {
    //   }
    // })
  }

  createOpen = async() => {
    this.setState({addVisible: true, addValue: ""})
  };

  async setGroupName(e) {
    if (e.target.value !== '默认分组') {
      this.setState({addValue: e.target.value})
    } else {
      message.error('不可使用 默认分组 作为分组名称');
      this.setState({addValue: ""})
    }
  }

  create = async() => {
    if (!this.state.addValue) {
      message.error('请填写分组名称');
      return
    }
    this.setState({loading: true});
    let res =  await axios.post(`${consumer}/save`, {groupName: this.state.addValue});
    if (res.data.code === 0) {
      this.setState({addVisible: false, loading: false});
      message.error(res.data.message);
    }else {
      message.success('添加分组成功');
      this.setState({addVisible: false});
      this.reload()
    }
  };

  editOpen = async(r) => {
    if (!r) {
      if (this.state.selectedRowKeys.length === 0) {
        message.error('请先选择数据');
        return
      }
      if (this.state.selectedRowKeys.length > 1) {
        message.error('只能选择一条数据');
        return
      }
      r = this.selectedRows[0]
    } else {
      this.selectedRows = [r]
    }

    if (r.groupName === '默认分组') {
      message.error('默认分组不可操作');
      return
    }

    this.setState({editVisible: true, editValue: r.groupName})
  };

  edit = async() => {
    let _id = this.selectedRows[0]._id
    if (!this.state.editValue) {
      message.error('请填写分组名称');
      return
    }

    this.setState({loading: true});
    await axios.post(`${consumer}/save`, {
      _id,
      __v: this.selectedRows[0].__v,
      groupName: this.state.editValue
    });
    message.success('操作成功');
    this.setState({editVisible: false});
    this.reload()
  };

  async reset() {
    this.state.pagination.current = 1;
    this.setState({groupNameSearch: '', pagination: this.state.pagination});
    this.filters = {}
    await this.reload()
  }


  refTableContent = (ref) => {
    if (ref && ref.getBoundingClientRect) {
      this.setState({showTable: true, scrollY: ref.getBoundingClientRect().height - 80, tableContent: ref})
    }
  }

  // 把columns放到render中，虽然损失部分性能，但是能方便参数中的匿名回调获取实例状态
  render() {
    const columns = [
      {
        title: '分组名称',
        dataIndex: 'groupName',
        key: 'groupName',
        width: 343,
        ellipsis: true,
      },
      {
        title: '账号数量',
        dataIndex: 'accountNum',
        key: 'accountNum',
        width: 200,
        ellipsis: true,
      },
      {
        title: '在线账号数量',
        dataIndex: 'accountOnlineNum',
        key: 'accountOnlineNum',
        width: 200,
        ellipsis: true,
      },
      {
        title: '创建时间',
        dataIndex: 'createTime',
        key: 'createTime',
        render: formatDate,
        width: 400,
      },{
        title: '操作',
        dataIndex: 'op',
        key: 'op',
        width: 130,
        ellipsis: true,
        render: (v, r) => {
          return r.groupName != '默认分组' ? (<div>
            <Button type="link" onClick={()=>{this.editOpen(r)}}>修改</Button>
            <Button type="link" onClick={()=>{this.delete(r)}}>删除</Button>
          </div>) : ''
        }
      }
    ]

    let tableSelectData = {selectedRowKeys: this.state.selectedRowKeys, filters: this.filters,};

    let options = this.state.socks5Areas || [];

    return (<MyTranslate><ExternalAccountFilter>
      <Breadcrumb>
        <Breadcrumb.BreadcrumbItem>资源管理</Breadcrumb.BreadcrumbItem>
        <Breadcrumb.BreadcrumbItem>邮箱分组</Breadcrumb.BreadcrumbItem>
      </Breadcrumb>

      <div className="search-box">

        <div className='search-item'>
          <div className="search-item-label">分组名称</div>
          <div className="search-item-right">
            <Input style={{
              width: 200
            }} placeholder={'请输入'} value={this.state.groupNameSearch}
                   onChange={(value) => {
                     value = value.target.value;
                     if (value) {
                       this.filters.groupName = value;
                     } else {
                       delete this.filters.groupName;
                     }

                     this.setState({groupNameSearch: value});
                     // this.reload()
                   }}
                   onPressEnter={(value) => {
                     value = value.target.value;
                     if (value) {
                       this.filters.groupName = value;
                     } else {
                       delete this.filters.groupName;
                     }

                     this.setState({groupNameSearch: value});
                     this.reload()
                   }}



            />
          </div>
        </div>

        <div className='accountGroup-btn'>
          <div className="search-query-btn" onClick={() => this.reload()}>查询</div>
          <div className="search-reset-btn" onClick={() => this.reset()}>重置</div>
        </div>
      </div>

      <div className="main-content">
        <div>
          <div className="search-query-btn" onClick={this.createOpen.bind(this)}>新增</div>
          <div className={this.state.selectedRowKeys && this.state.selectedRowKeys.length > 0 ? "search-delete-btn" : "search-reset-btn"} onClick={() => this.delete()}>批量删除</div>
        </div>

        <div className="tableSelectedCount">{`已选${this.state.selectedRowKeys.length}项`}</div>
        <div className="tableContent" ref={this.refTableContent}>
          <div>
            { this.state.showTable ? <Table
              tableLayout="fixed"
              scroll={{y: this.state.scrollY, x: columns.filter(e => e.width).map(e => e.width).reduce((a, b) => a + b)}}
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

      <Dialog
        header="新增"
        visible={this.state.addVisible}
        onConfirm={this.create} confirmLoading={this.state.loading}
        onClose={()=>{this.setState({addVisible:false})}}
        onCancel={()=>{this.setState({addVisible:false})}}
      >
        <div className="dialog_item">
          <span className='required_label'></span>
          <span>分组名称</span>
          <div className='dialog_item_input'>
            <Input placeholder='请输入' onChange={this.setGroupName.bind(this)} value={this.state.addValue} type="text"/>
          </div>
        </div>
      </Dialog>

      <Dialog
        header="编辑"
        visible={this.state.editVisible}
        onConfirm={this.edit} confirmLoading={this.state.loading}
        onClose={()=>{this.setState({editVisible:false})}}
        onCancel={()=>{this.setState({editVisible:false})}}
      >
        <div className="dialog_item">
          <span className='required_label'></span><span>分组名称</span>
          <div className='dialog_item_input'>
            <Input placeholder='请输入' onChange={(e)=>{this.setState({editValue:e.target.value})}} value={this.state.editValue} type="text"/>
          </div>
        </div>
      </Dialog>
    </ExternalAccountFilter></MyTranslate>)
  }
}

export default injectIntl(MyComponent)
