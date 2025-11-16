import React, {Component} from 'react'
import {BrowserRouter as Router, Route, Link} from 'react-router-dom'
import {
  Icon,
  Table,
  Divider,
  Upload,
  message,
  Input,
  Button,
  Modal,
  Select,
  Switch,
  Row,
  Col,
} from 'antd'
import { Pagination, Breadcrumb, Dialog, Tag, Input as TInput, Upload as TUpload, Tooltip as TTooltip,} from 'tdesign-react';
import axios from 'axios'
import {formatDate} from 'components/DateFormat'
import {download} from "components/postDownloadUtils"

const {Option} = Select;
const Search = Input.Search;
const confirm = Modal.confirm;

// 针对当前页面的基础url
const baseUrl = '/api/googleStudio';
const params = []

const colLayou = {
  span: 6,
  style: {padding: '5px'}
};

const getUploadImageProps = (regExpStr = 'txt', type = 'Chatgpt') => {
  let regExp = new RegExp(`^.+\\.(${regExpStr})$`)
  const uploadProps = {
    name: 'file',
    multiple: false,
    action: `/api/googleStudio/import/${type}`,
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
        showSizeChanger: true,
        pageSizeOptions: ['10', '20', '30', '40', '50', '100', '200', '300', '400', '500']
      },
      selectedRowKeys: [],

      config: {},   // 用户参数存储
      selectedCountRef: null,
      tableContentHeight: 0,
      scrollY: 0,
      addVisible: false,
      editUser: null,
      email: '',
      apiKey: '',
      email2: '',
      files: [],
      status: '',
      totalData: {},
      type: 'Chatgpt'
    };
    // 选中行的数据保存在selectedRows变量中
    // 之所以不放入state，因为这些变量是单向流动的，即Table控件产生这些数据，通过callback更新到变量this.selectedRows
    this.selectedRows = [];
    // filters的写法保持与mongo filter的写法一致，避免与服务器、数据库直接的数据阻抗失衡
    // 例如： filters={username: 'foo'}
    // 因为json协议无法传输regex，所以字符类型的值，都会在服务器端转成regex，以提高匹配度，损失部分查询性能
    this.filters = {};
    // sorter的写法保持与mongo sort的写法一致，避免与服务器、数据库直接的数据阻抗失衡
    // 例如：sorter={createTime: -1}
    // 注意：Table控件仅支持单列排序，不支持多列同时排序
    this.sorter = {
      createTime: -1,
      sortNo: -1,
    }
    // 提前写在constructor则Table首次加载时生效
    // 但是因为没有放到state中，所以除sorter外，都不建议提前设置，保持不受控状态是最好的
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

  // 首次加载数据
  async componentWillMount() {
    this.reload()
    // this.loadData()
  }

  async loadData () {
    let res = await axios.post(`/api/googleStudio/canUseCount`, {});
    this.setState({
      totalData: res.data.data
    })
  }

  async reset () {
    this.filters = {}
    this.state.email = ''
    this.state.status = ''
    this.state.type = 'Chatgpt'
    this.state.pagination.current = 1
    this.setState({ filters: {}, email: '', pagination: this.state.pagination, status: '', type: 'Chatgpt' })
    this.reload()
  }

  async reload() {
    // 重新加载，一般是页面第一次加载的时候来一下
    this.load(this.state.pagination, this.filters, this.sorter)
  }

  async load(pagination, filters, sorter) {
    // 这里的参数是格式好的，无需修改，无阻抗失衡，直接发给服务器
    this.setState({loading: true});
    let f = {...filters}
    f.type = 'Chatgpt'
    let res = await axios.post(`${baseUrl}/${pagination.pageSize}/${pagination.current}`, {filters: f, sorter});
    pagination.total = res.data.data.total;
    this.setState({loading: false, data: res.data.data.data, pagination, selectedRowKeys: []});
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

  async delete(r) {
    if (!r) {
      if (this.state.selectedRowKeys.length == 0) {
        message.error('请先选择数据');
        return
      }
    }
    
    confirm({
      title: '确定要删除这些数据？',
      content: '',
      okText: '确定',
      okType: 'danger',
      cancelText: '取消',
      onOk: async() => {
        this.setState({loading: true});
        await axios.post(`${baseUrl}/delete`, r ? [r._id] : this.state.selectedRowKeys);
        message.success('操作成功');
        this.reload()
      },
      onCancel() {
      }
    })
  }

  async test(r) {
    if (!r) {
      if (this.state.selectedRowKeys.length == 0) {
        message.error('请先选择数据');
        return
      }
    }
    
    confirm({
      title: '确定要检测这些数据？',
      content: '',
      okText: '确定',
      okType: 'danger',
      cancelText: '取消',
      onOk: async() => {
        this.setState({loading: true});
        let res = await axios.post(`${baseUrl}/test`, r ? {id: r._id} : {});
        if (res.data.code === 1) {
          message.success('操作成功');
        } else {
          message.error(res.data.message);
        }
        
        this.reload()
      },
      onCancel() {
      }
    })
  }

  refSelectedCount = (ref) => {
    this.state.selectedCountRef = ref
    this.setState({selectedCountRef: ref})
    this.handleResize()
  }

  openAdd () {
    this.setState({
      addVisible: true,
      editUser: null,
      email2: '',
      apiKey: '',
    })
  }

  // 把columns放到render中，虽然损失部分性能，但是能方便参数中的匿名回调获取实例状态
  render() {
    const columns = [
      {
        title: '邮箱',
        dataIndex: 'email',
        key: 'email',
        width: 180,
        ellipsis: true,
      },{
        title: 'AI类型',
        dataIndex: 'type',
        key: 'type',
        width: 120,
        ellipsis: true,
      },{
        title: 'APIKEY',
        dataIndex: 'apiKey',
        key: 'apiKey',
        width: 200,
        ellipsis: true,
      },{
        title: '今天成功次数',
        dataIndex: 'usedSuccessByDay',
        key: 'usedSuccessByDay',
        width: 120,
        ellipsis: true,
        sorter: true,
      }, {
        title: '总成功次数',
        dataIndex: 'usedSuccess',
        key: 'usedSuccess',
        width: 100,
        ellipsis: true,
        sorter: true,
      }, {
        title: '每天使用限制',
        dataIndex: 'limitByDay',
        key: 'limitByDay',
        width: 120,
        ellipsis: true,
        render: (v,r) => {
          return `${r.useByDay}/${v}`
        }
      }, {
        title: '状态',
        dataIndex: 'status',
        key: 'status',
        width: 70,
        sorter: true,
        ellipsis: true,
      },{
        title: '每分钟使用限制',
        dataIndex: 'limitByMinute',
        key: 'limitByMinute',
        width: 120,
        ellipsis: true,
        render: (v,r) => {
          return `${r.useByMinute}/${v}`
        }
      },{
        title: '下次可用时间',
        dataIndex: 'nextUseTime',
        key: 'nextUseTime',
        render: formatDate,
        width: 150,
        sorter: true,
        ellipsis: true,
      },{
        title: '创建时间',
        dataIndex: 'createTime',
        key: 'createTime',
        render: formatDate,
        width: 150,
        ellipsis: true,
      }, {
        title: '操作',
        dataIndex: 'op',
        key: 'op',
        width: 120,
        ellipsis: true,
        render: (t, record, index) => {
          return (<div>
            <Button type="link" onClick={async () => {
              this.test(record)
            }}>检测</Button>
            <Button type="link" onClick={async () => {
              this.delete(record)
            }}>删除</Button>
          </div>)
        }
      }
    ];

    return (<div>
      <Breadcrumb>
        <Breadcrumb.BreadcrumbItem>资源管理</Breadcrumb.BreadcrumbItem>
        <Breadcrumb.BreadcrumbItem>Chatgpt AI管理</Breadcrumb.BreadcrumbItem>
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
            type: 'Chatgpt',
          };
          if (this.state.editUser) {
            form._id = this.state.editUser._id
          }
          this.setState({loading: true});
          // 过滤掉null
          let res = await axios.post(`${baseUrl}/save`, form)
          if (res.data.code == 1) {
            message.success('操作成功')
            this.setState({addVisible: false})
            this.reload()
          } else {
            message.error(res.data.message)
            this.setState({loading: false})
          }
        }} confirmLoading={this.state.loading}
        onCancel={() => {
          this.setState({addVisible: false})
        }}
        onClose={() => {
          this.setState({addVisible: false})
        }}
      >
        <div style={{marginLeft: 127, marginTop: 26, color: 'rgba(0, 0, 0, 0.90)'}}>
          <div style={{display: 'flex', marginBottom: 24}}>
            <div style={{width: 100, textAlign: 'right', marginRight: 16, lineHeight: '30px'}}>
            <span style={{color: '#D54941'}}>*</span>邮箱
            </div>
            <div>
              <Input value={this.state.email2}
                     onChange={(e) => this.setState({email2: e.target.value})} style={{width: 400}}
                     placeholder='请输入'/>
            </div>
          </div>

          <div style={{display: 'flex', marginBottom: 24}}>
            <div style={{width: 100, textAlign: 'right', marginRight: 16, lineHeight: '30px'}}>
            <span style={{color: '#D54941'}}>*</span>Api Key
            </div>
            <div>
              <Input value={this.state.apiKey}
                     onChange={(e) => this.setState({apiKey: e.target.value})} style={{width: 400}}
                     placeholder='请输入'/>
            </div>
          </div>
        </div>
      </Dialog>

      <div className="search-box">
        {/* <div className='search-item' style={{minWidth: 280}}>
          <div className="search-item-label">AI类型</div>
          <div className="search-item-right">
            <Select value={this.state.type} style={{ width: 200 }} placeholder="请选择内容" onChange={v => {
              this.setState({ type: v })
              this.filters['type'] = v
              this.reload()
            }}>
              <Option value={'GoogleStudio'}>GoogleStudio</Option>
              <Option value={'Chatgpt'}>Chatgpt</Option>
            </Select>
          </div>
        </div> */}

        <div className='search-item' style={{minWidth: 280}}>
          <div className="search-item-label">邮箱</div>
          <div className="search-item-right">
            <Input
              allowClear
              style={{width: 200}}
              placeholder="请输入"
              // 补充事件绑定 ↓
              value={this.state.email}
              onChange={e => {
                this.filters.email = e.target.value;
                this.setState({
                  email: e.target.value
                })
              }
              }
              onPressEnter={e => {
                this.reload()
              }
              }
            />
          </div>
        </div>

        <div className='search-item' style={{minWidth: 280}}>
          <div className="search-item-label">状态</div>
          <div className="search-item-right">
            <Select value={this.state.status} style={{ width: 200 }} placeholder="请选择内容" onChange={v => {
                this.setState({ status: v })
                this.filters['status'] = v
                this.reload()
              }}>
                <Option value={'enable'}>enable</Option>
                <Option value={'disable'}>disable</Option>
              </Select>
          </div>
        </div>

        <div className='accountGroup-btn'>
          <div className="search-query-btn" onClick={() => this.reload()}>查询</div>
          <div className="search-reset-btn" onClick={() => this.reset()}>重置</div>
        </div>
      </div>

      <div style={{paddingTop: 40}}>
        <div style={{overflow: 'hidden'}}>
          {/* <div style={{display: 'flex'}}>最近10分钟内成功使用key数量：{this.state.totalData.keyCount} 今天剩余次数(按一个账号1000次)：{this.state.totalData.restCount} 今天消耗次数：{this.state.totalData.usedCount} 今天成功次数：{this.state.totalData.successCount}</div> */}
          <div style={{display: 'flex'}}>

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
            

            <div className={"search-query-btn"} onClick={() => {
              download(`${baseUrl}/export`, { ids: this.state.selectedRowKeys })
            }}>导出
            </div>

            <div className={"search-delete-btn"} onClick={() => {
              this.delete()
            }}>批量删除
            </div>
          </div>
          <div className="tableSelectedCount"
              ref={this.refSelectedCount}>{`已选${this.state.selectedRowKeys.length}项`}</div>
          <div className="tableContent" style={{height: this.state.tableContentHeight}}>
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
                onChange={this.handleTableChange.bind(this)}/>
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
    </div>)
  }
}

export default MyComponent
