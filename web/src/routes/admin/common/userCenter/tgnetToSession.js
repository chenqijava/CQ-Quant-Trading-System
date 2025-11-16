import React, { Component } from 'react'
import { BrowserRouter as Router, Route, Link } from 'react-router-dom'
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
  Row,
  Col,
  Radio
} from 'antd'
import { Pagination, Breadcrumb, Dialog, Tag, Input as TInput, Upload as TUpload, Tooltip as TTooltip, Switch, DateRangePicker, Alert,Checkbox, Textarea } from 'tdesign-react';
import {ChevronDownIcon} from 'tdesign-icons-react'
import axios from 'axios'
import { formatDate } from 'components/DateFormat'
import { download } from "components/postDownloadUtils"
import { connect } from 'dva'
import { injectIntl } from 'react-intl'
import DialogApi from '../dialog/DialogApi';
import uuid from 'uuid/v4'

const { Option } = Select;
const Search = Input.Search;
const confirm = Modal.confirm;

// 针对当前页面的基础url
const baseUrl = '/api/consumer/platform';
const params = []

const getUploadImageProps = (regExpStr = 'txt|csv') => {
  let regExp = new RegExp(`^.+\\.(${regExpStr})$`)
  const uploadProps = {
    name: 'file',
    multiple: false,
    action: `/api/consumer/res/uploadZip/tgNetToSession`,
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
      data2: [],
      loading: false,
      pagination: {
        total: 0,
        pageSize: 10,
        current: 1,
        showTotal: (total, range) => `共 ${total} 条`,
        showSizeChanger: true,
        pageSizeOptions: ['10', '20', '30', '40', '50', '100', '200', '300', '400', '500']
      },
      pagination2: {
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
      status: '',
      desc: '',

      progressVisible: false,
      editData: {},
      detailData: null,

      taskDesc: '',
      addMethod: '1',
      addData: '',
      files: [],
      sendNum: 10,
      progressTaskId: ''
    };
    this.timer = null
    // 选中行的数据保存在selectedRows变量中
    // 之所以不放入state，因为这些变量是单向流动的，即Table控件产生这些数据，通过callback更新到变量this.selectedRows
    this.selectedRows = [];
    // filters的写法保持与mongo filter的写法一致，避免与服务器、数据库直接的数据阻抗失衡
    // 例如： filters={username: 'foo'}
    // 因为json协议无法传输regex，所以字符类型的值，都会在服务器端转成regex，以提高匹配度，损失部分查询性能
    this.filters = {
    };
    // sorter的写法保持与mongo sort的写法一致，避免与服务器、数据库直接的数据阻抗失衡
    // 例如：sorter={createTime: -1}
    // 注意：Table控件仅支持单列排序，不支持多列同时排序
    this.sorter = {
      createTime: -1,
      sortNo: -1,
    }
    this.sorter2 = {}

    this.timer2 = null
    // 提前写在constructor则Table首次加载时生效
    // 但是因为没有放到state中，所以除sorter外，都不建议提前设置，保持不受控状态是最好的
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
    if (this.timer) {
      clearInterval(this.timer)
      this.timer = null
    }
  }

  // 首次加载数据
  async componentWillMount() {
    this.reload()
  }

  async reset() {
    this.filters = {}
    this.state.desc = ''
    this.state.status = ''
    this.state.pagination.current = 1
    this.setState({ filters: {}, desc: '', pagination: this.state.pagination, status: '', })
    this.reload()
  }

  async reload() {
    // 重新加载，一般是页面第一次加载的时候来一下
    this.load(this.state.pagination, this.filters, this.sorter)
  }

  async load(pagination, filters, sorter) {
    // 这里的参数是格式好的，无需修改，无阻抗失衡，直接发给服务器
    this.setState({ loading: true });
    let res = await axios.post(`/api/tgNetToSession/${pagination.pageSize}/${pagination.current}`, { filters: { ...filters, userID: this.props.userID }, sorter });
    pagination.total = res.data.data.total;
    this.setState({ loading: false, data: res.data.data.data, pagination, selectedRowKeys: [] });
    this.selectedRows = [];
    this.filters = filters;
    this.sorter = sorter
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
      sort[sorter.field] = sorter.order == 'descend'
        ? -1
        : 1
    }
    // 暂时不用Table的filter，不太好用
    this.load(pagination, this.filters, sort)
  }

  refSelectedCount = (ref) => {
    this.state.selectedCountRef = ref
    this.setState({ selectedCountRef: ref })
    this.handleResize()
  }

  async copyTxt(txt, msg) {
    if (txt) {
      await navigator.clipboard.writeText(txt);
      message.success(msg || '复制成功')
    }
  }

  delete(r) {
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
      title: '确定要删除这些任务吗？',
      onOkTxt: '确认删除',
      onCancelTxt: '取消',
      onOk: async () => {
        this.setState({ loading: true });
        let result = await axios.post(`/api/tgNetToSession/delete`, { ids: keys });
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

  // 把columns放到render中，虽然损失部分性能，但是能方便参数中的匿名回调获取实例状态
  render() {
    const columns = [
      {
        title: '任务描述',
        dataIndex: 'desc',
        key: 'desc',
        width: 100,
        ellipsis: true,
      },{
        title: '总数',
        dataIndex: 'total',
        key: 'total',
        width: 80,
        ellipsis: true,
      }, {
        title: '成功',
        dataIndex: 'success',
        key: 'success',
        width: 80,
        ellipsis: true,
      }, {
        title: '失败',
        dataIndex: 'failed',
        key: 'failed',
        width: 80,
        ellipsis: true,
      }, {
        title: '状态',
        dataIndex: 'status',
        key: 'status',
        width: 80,
        ellipsis: true,
        render: (v) => {
          if (v === 'success') {
            return '完成'
          }
          if (v === 'init') {
            return '执行中'
          }
          if (v === 'processing') {
            return '待执行'
          }
          if (v === 'pause') {
            return '已暂停'
          }
          return '-'
        }
      }, {
        title: '创建时间',
        dataIndex: 'createTime',
        key: 'createTime',
        render: formatDate,
        width: 150,
        ellipsis: true,
      }, {
        title: '完成时间',
        dataIndex: 'finishTime',
        key: 'finishTime',
        render: formatDate,
        width: 150,
        ellipsis: true,
      }, {
        title: '操作',
        dataIndex: 'op',
        key: 'op',
        width: 180,
        ellipsis: true,
        render: (t, record, index) => {
          return (<div>
            <Button type="link" onClick={async () => {
              this.setState({
                editUser: record,
                addVisible: true,
                taskDesc: record.params.taskDesc,
                files: record.params.filepath ? [{
                  uid: uuid() + '',
                  name: 'tgnet.zip',
                  status: 'done',
                  response: '{"status": "success"}',
                  url: '/api/consumer/res/download/' + record.params.filepath
                }] : [],
              })
            }}>查看</Button>
            
            {record.status !== 'success' ? <Button type="link" onClick={async () => {
              DialogApi.warning({
                title: '确认结束任务吗？',
                onOk: async () => {
                  let res = await axios.post(`/api/tgNetToSession/stop`, { ids: [record._id] })
                  if (res.data.code === 1) {
                    message.success('结束成功')
                    this.reload()
                  } else {
                    message.error('结束失败,' + res.data.message)
                  }
                },
                onCancel: () => {
                }
              })
            }}>结束任务</Button> : ''}

            <Button type="link" onClick={async () => {
              // download(`/api/linkCheck/export`, {
              //   groupTaskId: record._id,
              // })
              if (record.result && record.result.out) {
                const response = await fetch('/api/consumer/res/download/' + record.result.out);
                if (!response.ok) throw new Error("下载失败");

                const blob = await response.blob();
                const link = document.createElement("a");
                link.href = URL.createObjectURL(blob);
                link.download = record.result.out.split('\\').pop().split('/').pop();
                document.body.appendChild(link);
                link.click();
                link.remove();
              } else {
                message.error('报告文件生成中，稍后再试')
              }
            }}>下载TXT</Button>

            <Button type="link" onClick={async () => {
              // download(`/api/linkCheck/export`, {
              //   groupTaskId: record._id,
              // })
              if (record.result && record.result.out2) {
                const response = await fetch('/api/consumer/res/download/' + record.result.out2);
                if (!response.ok) throw new Error("下载失败");

                const blob = await response.blob();
                const link = document.createElement("a");
                link.href = URL.createObjectURL(blob);
                link.download = record.result.out2.split('\\').pop().split('/').pop();
                document.body.appendChild(link);
                link.click();
                link.remove();
              } else {
                message.error('报告文件生成中，稍后再试')
              }
            }}>下载ZIP</Button>
          </div>)
        }
      }
    ];

    return (<div>
      <Breadcrumb>
        <Breadcrumb.BreadcrumbItem>个人中心</Breadcrumb.BreadcrumbItem>
        <Breadcrumb.BreadcrumbItem>TGNET转换</Breadcrumb.BreadcrumbItem>
      </Breadcrumb>

      <Dialog
        header={this.state.editUser ? "查看" : "创建任务"}
        width={854}
        height={566}
        visible={this.state.addVisible}
        onConfirm={async () => {
          if (this.state.editUser) {
            this.setState({ addVisible: false })
            return
          }
          if (!this.state.taskDesc) {
            message.error('请输入任务描述')
            return
          }
          if (!this.state.files.length) {
            message.error('请上传文件')
            return
          }
          let form = {
            desc: this.state.taskDesc,
            filepath: this.state.files[0].response.data.filepath,
          };
          this.setState({ loading: true });
          // 过滤掉null
          let res = await axios.post(`/api/tgNetToSession/save`, form)
          if (res.data.code == 1) {
            message.success('操作成功')
            this.setState({ addVisible: false })
            this.reload()
          } else {
            message.error(res.data.message)
            this.setState({ loading: false })
          }
        }} confirmLoading={this.state.loading}
        onCancel={() => {
          this.setState({ addVisible: false, })
        }}
        onClose={() => {
          this.setState({ addVisible: false, })
        }}
      >
        <div style={{ marginLeft: 127, marginTop: 26, color: 'rgba(0, 0, 0, 0.90)' }}>
          <div style={{ display: 'flex', marginBottom: 24 }}>
            <div
              style={{ width: 120, textAlign: 'right', marginRight: 16, lineHeight: '30px' }}>{this.state.editUser ? '' :
                <span style={{ color: '#D54941' }}>*</span>}任务描述
            </div>
            <div>
              <Input value={this.state.taskDesc} disabled={!!this.state.editUser}
                onChange={(e) => this.setState({ taskDesc: e.target.value })} style={{ width: 400 }}
                placeholder='请输入' />
            </div>
          </div>

          <div style={{ display: 'flex', marginBottom: 24 }}>
            <div
              style={{ width: 120, textAlign: 'right', marginRight: 16, lineHeight: '30px' }}>{this.state.editUser ? '' :
                <span style={{ color: '#D54941' }}>*</span>}上传文件
            </div>
            <div style={{ width: 400 }}>
              <TUpload
                disabled={!!this.state.editUser}
                {...getUploadImageProps('zip')}
                files={this.state.files}
                onChange={(info) => {
                  if (info.length > 0) {
                    this.setState({ files: info });
                    return
                  }
                }}
                onRemove={() => this.setState({ files: [] })}
              />
              <div style={{ fontSize: 12, color: 'rgba(0, 0, 0, 0.60)' }}>支持zip文件</div>
            </div>
          </div>
        </div>
      </Dialog>

      <div className="account-search-box">
        <div className='account-search-item'>
          <div className="account-search-item-label">任务名称</div>
          <div className="account-search-item-right">
            <Input
              allowClear
              style={{ width: 200 }}
              placeholder="请输入"
              value={this.state.desc}
              onChange={e => {
                this.setState({ desc: e.target.value })
                this.filters['desc'] = e.target.value
              }
              }
              onPressEnter={e => {
                this.setState({ orderDetailNo: e.target.value })
                this.filters['desc'] = e.target.value
                this.reload()
              }
              }
            />
          </div>
        </div>


        <div className='account-search-item'>
          <div className="account-search-item-label">任务状态</div>
          <div className="account-search-item-right">
            <Select value={this.state.status} style={{ width: 200 }} onChange={v => {
              this.setState({ status: v })
              if (v) {
                this.filters['status'] = v
              } else {
                delete this.filters['status']
              }

              this.reload()
            }}>
              <Option value="">全部</Option>
              <Option value="success">完成</Option>
              <Option value="processing">待执行</Option>
              <Option value="init">执行中</Option>
            </Select>
          </div>
        </div>

        <div className='account-btn-no-expand' style={{ width: '136px' }}>
          <div style={{ display: 'flex', justifyContent: 'right', alignItems: 'center' }}>
            <div className="search-query-btn" onClick={() => this.reload()}>查询</div>
            <div className="search-reset-btn" onClick={() => this.reset()}>重置</div>
          </div>
        </div>
        <div style={{ clear: 'both' }}></div>
      </div>

      <div style={{ paddingTop: 40 }}>
        <div style={{ overflow: 'hidden' }}>
          <div style={{ display: 'flex' }}>
            <div className={"search-query-btn"} onClick={() => {
              this.setState({
                addVisible: true,
                taskDesc: '',
                addMethod: '1',
                addData: '',
                files: [],
                sendNum: 10,
                editUser: null
              })
            }}>新增
            </div>
            <div className={this.state.selectedRowKeys.length > 0 ? "search-delete-btn" : "search-reset-btn"} onClick={() => {
              this.delete()
            }}>批量删除
            </div>
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
    </div>)
  }
}

export default connect(({ user }) => ({
  openKeys: user.openKeys,
  userID: user.info.userID
}))(injectIntl(MyComponent))
