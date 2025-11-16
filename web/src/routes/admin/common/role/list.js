import React, {Component} from 'react'
import {BrowserRouter as Router, Route, Link} from 'react-router-dom'
import {
  Icon,
  Table,
  Divider,
  Upload,
  message,
  Input,
  Row,
  Col,
  Switch,
  Button,
  Modal,
  Tree
} from 'antd'
import axios from 'axios'
import {formatDate} from 'components/DateFormat'
import {FormattedMessage, useIntl, injectIntl} from 'react-intl'
import ExternalAccountFilter from "components/common/ExternalAccountFilter";
import MyTranslate from "components/common/MyTranslate";
import { Pagination, Breadcrumb, Dialog, Tag, Input as TInput, Upload as TUpload, Tooltip as TTooltip,} from 'tdesign-react';
import DialogApi from '../dialog/DialogApi.js'

const Search = Input.Search
const confirm = Modal.confirm
const { TreeNode } = Tree;

const pageType = 'role';
// 针对当前页面的基础url
const baseUrl = `/api/consumer/${pageType}`;
const uploadProps = {
  name: 'file',
  multiple: false,
  showUploadList: false,
  action: `${baseUrl}/upload`,
  beforeUpload(file, fileList) {
    let valid = /^.+\.(csv|txt|json)$/.test(file.name.toLowerCase());
    if (!valid) {
      message.error('文件名不合法')
    }
    return valid
  }
}

const formItemLayout = {
  style: {width: 250}
};

const colLayou = {
  span: 4,
  style: {padding: '5px'}
};

const params = [];

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
        pageSize: 10,
        current: 1,
        showTotal: (total, range) => `共 ${total} 条`,
        showSizeChanger: true,
        pageSizeOptions: ['10', '20', '30', '40', '50', '100', '200', '300', '400', '500']
      },
      config: {},
      selectedRowKeys: [],
      tableContentHeight: 0,
      scrollY: 0,
      selectedCountRef: null,
      addVisible: false,
      menus: [],
      checkedKeys: [], 
      permissions: [ 
      ],
      editVisible: false,
      onlyRead: false,
      editRole: null,
    }
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
    this.sorter = {}
    // 提前写在constructor则Table首次加载时生效
    // 但是因为没有放到state中，所以除sorter外，都不建议提前设置，保持不受控状态是最好的
  }

  // 首次加载数据
  async componentWillMount() {
    // this.reloadConfig();
    this.reload()
    this.loadPermissions()
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

  async reload() {
    // 重新加载，一般是页面第一次加载的时候来一下
    this.state.pagination.current = 1
    this.setState({pagination: this.state.pagination})
    this.load(this.state.pagination, this.filters, this.sorter)
  }

  async load(pagination, filters, sorter) {
    // 这里的参数是格式好的，无需修改，无阻抗失衡，直接发给服务器
    this.setState({loading: true})
    let res = await axios.post(`${baseUrl}/${pagination.pageSize}/${pagination.current}`, {filters, sorter})
    pagination.total = res.data.data.total
    this.setState({loading: false, data: res.data.data.data, pagination, selectedRowKeys: []})
    this.selectedRows = []
    this.filters = filters
    this.sorter = sorter
  }

  async reloadConfig() {
    let config = {};
    for (let i in params) {
      let param = params[i];
      let res = await axios.get(`/api/consumer/${param.type || pageType}/get/${param.code}`);
      if (res.data.code) {
        config[param.code] = res.data.value;
      }
    }
    this.setState({config});
  }

  async onRowSelectionChange(selectedRowKeys, selectedRows) {
    // 选中状态的数据，因为无需受控，就不记录在state里了，提高效率
    this.setState({selectedRowKeys})
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
        oldFilters[key] = {$in: filter};
      } else {
        delete oldFilters[key];
      }
    }
    // 暂时不用Table的filter，不太好用
    this.load(pagination, this.filters, sort)
  }

  // 比较通用的回调
  /**
   * 卡片操作界面
   * @param oper
   * @returns {Promise<void>}
   */
  async action(oper) {
    let params = [['oper', oper]];
    if (oper != 'create') {
      if (this.state.selectedRowKeys.length == 0) {
        message.error('请先选择数据');
        return
      }
      if (this.state.selectedRowKeys.length > 1) {
        message.error('只能选择一条数据');
        return
      }
      params.push(['_id', this.state.selectedRowKeys[0]]);
    }
    this.props.history.push(`card?${params.map(v => v.join('=')).join('&')}`)
  }

  /**
   * 有弹窗提示的操作
   * @param action
   * @returns {Promise<void>}
   */
  async dangerAction(action) {
    let data = null;
    if (action.oper.indexOf('All') < 0) {
      if (this.state.selectedRowKeys.length == 0) {
        message.error('请先选择数据');
        return
      }
      data = this.state.selectedRowKeys;
    } else {
      data = {filters: this.filters};
    }
    confirm({
      title: `确定要${action.name}这些数据？`,
      content: '',
      okText: '确定',
      okType: 'danger',
      cancelText: '取消',
      onOk: async () => {
        switch (action.oper) {
          default:
            this.setState({loading: true});
            await axios.post(`${baseUrl}/${action.oper}`, data);
            message.success('操作成功');
            this.reload()
        }
      },
      onCancel() {
      }
    })
  }

  /**
   * 修改参数
   **/
  async onParamsSetChange(type, value, key) {
    this.setState({loading: true});
    let res = await axios.post(`/api/consumer/${key || pageType}/set/${type}`, {value});
    if (res.data.code == 1) {
      // 修改配置后重新加载所有配置
      await this.reloadConfig();
      message.success('操作成功');
    } else {
      Modal.error({title: res.data.message});
    }
    this.setState({loading: false})
  }

  refSelectedCount = (ref) => {
    this.state.selectedCountRef = ref
    this.setState({selectedCountRef: ref})
    this.handleResize()
  }

  openAdd () {
    this.setState({addVisible: true, name: '', checkedKeys: [], permissions: [],editRole:null})
  }

  _renderTreeNodes = data =>
    data.sort((a, b) => a.index - b.index).map(item => {
        return (
          <TreeNode title={item.name} key={item._id} dataRef={item}>
            {item.submenus && item.submenus.length ? this._renderTreeNodes(item.submenus) : ''}
          </TreeNode>
        );
    });

  async loadPermissions() {
    let menus = [];
    try {
      let result = await axios.get('/api/common/user/loadPermissions');
      if (result.data.code) {
        this.submenusMap = {};     // 用来记录各菜单的子菜单
        result.data.data.forEach(pm => {
          if (!pm.parent) {       // 没有parent的是一级菜单
            menus.push(pm);
          } else {                // 将菜单放到父菜单的数组中
            let id = pm.parent;
            if (!this.submenusMap[id]) {
              this.submenusMap[id] = [];
            }
            this.submenusMap[id].push(pm)
          }
          let id = pm._id;
          if (!this.submenusMap[id]) {
            this.submenusMap[id] = [];
          }
          pm.submenus = this.submenusMap[id];
        })
      }
    } finally {
      this.setState({menus});
    }
  }


  onCheck = (checkedKeys, info) => {
    this.setState({checkedKeys,permissions:info.halfCheckedKeys.concat(checkedKeys)});
  };

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
      onOk: async() => {
        this.setState({loading: true});
        let result = await axios.post(`${baseUrl}/delete`, keys);
        console.log(result);
        if (result.data.data.failedType && result.data.data.failedType.length) {
          DialogApi.error({
            title: '无法删除',
            content: result.data.data.failedType.map(type=>(
                <div>
                  <p>{type}：</p>
                  <p>角色名：{result.data.data.failed[type].join(',')}</p>
                </div>
            )),
            onOkText: "关闭",
            onOk: async()=> {
              this.reload()
            }
          });
        } else {
          message.success('操作成功');
          this.reload()
        }
      },
      onCancel() {
      }
    })
  }

  // 把columns放到render中，虽然损失部分性能，但是能方便参数中的匿名回调获取实例状态
  render() {
    const columns = [
      {
        title: '角色名称',
        dataIndex: 'name',
        key: 'name',
        width: '35%',
        ellipsis: true
      },
      {
        title: '创建时间',
        dataIndex: 'createTime',
        key: 'createTime',
        width: '35%',
        ellipsis: true,
        render: formatDate
      },
      {
        title: '操作',
        dataIndex: 'op',
        key: 'op',
        width: '30%',
        ellipsis: true,
        render: (v, r) => {
          return <div>
            <Button type="link" onClick={async () => {
              let res = await axios.get(`${baseUrl}/card/${r._id}`)
              let domain = res.data.data
              if (domain) {
                const permissions = domain.permissions;
                const checkedKeys = permissions.filter(p => !this.submenusMap[p] || this.submenusMap[p].length === 0);
                this.setState({editRole: r, addVisible: true, onlyRead: false, name: r.name, permissions, checkedKeys})
              } else {
                DialogApi.error({title: '数据不存在'})
              }
            }}>编辑</Button>
            <Button type="link" onClick={async () => {
              let res = await axios.get(`${baseUrl}/card/${r._id}`)
              let domain = res.data.data
              if (domain) {
                const permissions = domain.permissions;
                const checkedKeys = permissions.filter(p => !this.submenusMap[p] || this.submenusMap[p].length === 0);
                this.setState({editRole: r, editVisible: true, onlyRead: true, name: r.name, permissions, checkedKeys})
              } else {
                DialogApi.error({title: '数据不存在'})
              }
            }}>查看</Button>
            <Button type="link" style={{paddingRight: 0}} onClick={() => {this.delete(r)}}>删除</Button>
          </div>
        }
      },
    ];

    return (<MyTranslate><ExternalAccountFilter>
      <Breadcrumb>
        <Breadcrumb.BreadcrumbItem>系统设置</Breadcrumb.BreadcrumbItem>
        <Breadcrumb.BreadcrumbItem>角色管理</Breadcrumb.BreadcrumbItem>
      </Breadcrumb>
      
      <div className="account-main-content">
      <div style={{overflow: 'hidden'}}>
        <div>
        <div className={"search-query-btn"} onClick={() => {this.openAdd()}}>新建</div>
        <div className={"search-delete-btn"} onClick={() => {this.delete()}}>删除</div>
        </div>
        <div className="tableSelectedCount" ref={this.refSelectedCount}>{`已选${this.state.selectedRowKeys.length}项`}</div>
        <div className="tableContent" style={{height: this.state.tableContentHeight}}>
          <div>
            <Table 
              tableLayout="fixed"
              scroll={{y: this.state.scrollY, x: columns.filter(e => e.width).map(e => e.width).reduce((a, b) => a + b)}}
              pagination={this.state.pagination} rowSelection={{
              selectedRowKeys: this.state.selectedRowKeys,
              onChange: this.onRowSelectionChange.bind(this)
            }} columns={columns} rowKey='_id' dataSource={this.state.data} loading={this.state.loading} onChange={this.handleTableChange.bind(this)}/>
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
        header={this.state.editRole ? "修改" : "新增"}
        top={20}
        width={854}
        visible={this.state.addVisible}
        onConfirm={async () => {
          if (!this.state.name) {
            message.error('请输入角色名称') 
            return
          }
          let form = {...this.state.editRole, name: this.state.name};
          this.setState({loading: true});
          form.permissions = this.state.permissions;
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
        onCancel={()=>{this.setState({addVisible:false})}}
        onClose={()=>{this.setState({addVisible:false})}}
      >
          <div style={{marginLeft: 141, marginTop: 26, color: 'rgba(0, 0, 0, 0.90)'}}>
            <div style={{display: 'flex',marginBottom: 24}}>
              <div style={{width: 36, textAlign: 'right', marginRight: 16, lineHeight: '30px'}}><span style={{color:'#D54941'}}>*</span>名称</div>
              <div>
                <Input value={this.state.name} onChange={(e) => this.setState({name: e.target.value})} style={{width: 400}} placeholder='请输入'/>
              </div>
            </div>

            <div style={{display: 'flex'}}>
              <div style={{width: 36, textAlign: 'right', marginRight: 16, lineHeight: '30px'}}><span style={{color:'#D54941'}}>*</span>权限</div>
              <div style={{height: 450, overflowY: 'scroll', width: 400}}>
                <Tree checkable multiple defaultExpandAll onCheck={this.onCheck} checkedKeys={this.state.checkedKeys}
                                     selectedKeys={this.state.permissions}>
                {this._renderTreeNodes(this.state.menus)}
                </Tree>
              </div>
            </div>
          </div>
      </Dialog>


      <Dialog
        header="查看"
        top={20}
        width={854}
        visible={this.state.editVisible}
        onConfirm={() => {
          this.setState({editVisible:false})
        }} confirmLoading={this.state.loading}
        onCancel={()=>{this.setState({editVisible:false})}}
        onClose={()=>{this.setState({editVisible:false})}}
      >
          <div style={{marginLeft: 192, marginTop: 26, color: 'rgba(0, 0, 0, 0.90)'}}>
            <div style={{display: 'flex',marginBottom: 24}}>
              <div style={{width: 56, textAlign: 'right', marginRight: 16, lineHeight: '22px', color:'rgba(0, 0, 0, 0.40)'}}>名称</div>
              <div>
                {this.state.name}
              </div>
            </div>

            <div style={{display: 'flex',marginBottom: 12}}>
              <div style={{width: 56, textAlign: 'right', marginRight: 16, lineHeight: '22px', color:'rgba(0, 0, 0, 0.40)'}}>创建时间</div>
              <div>
                {this.state.editRole ? formatDate(this.state.editRole.createTime) : ''}
              </div>
            </div>

            <div style={{display: 'flex'}}>
              <div style={{width: 56, textAlign: 'right', marginRight: 16, lineHeight: '30px', color:'rgba(0, 0, 0, 0.40)'}}>权限</div>
              <div style={{height: 450, overflowY: 'scroll', width: 320}}>
                <Tree checkable multiple defaultExpandAll onCheck={this.onCheck} checkedKeys={this.state.checkedKeys}
                                     selectedKeys={this.state.permissions} disabled={this.state.onlyRead}>
                {this._renderTreeNodes(this.state.menus)}
                </Tree>
              </div>
            </div>
          </div>
      </Dialog>

            {/* <Dialog
              header="编辑"
              width={921}
              visible={this.state.editAccountVisible}
              onConfirm={() => {}} confirmLoading={this.state.loading}
              onCancel={()=>{this.setState({editAccountVisible:false})}}
              onClose={()=>{this.setState({editAccountVisible:false})}}
            >
               
            </Dialog> */}
    </ExternalAccountFilter></MyTranslate>)
  }
}

export default injectIntl(MyComponent)
