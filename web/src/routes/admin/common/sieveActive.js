import React, { Component } from 'react'
import { BrowserRouter as Router, Route, Link } from 'react-router-dom'
import {
  Icon,
  Table,
  Divider,
  Upload,
  message,
  Input,
  InputNumber,
  Button,
  Modal,
  Cascader,
  Select,
  Switch,
  Form,
  Tabs,
  Row,
  Col,
  Radio,
  Avatar,
  Tooltip,
  Alert,
  // Checkbox,
  Collapse,
  DatePicker,
} from 'antd'
import axios from 'axios'
import { FormattedMessage, useIntl, injectIntl } from 'react-intl'
import { formatDate } from 'components/DateFormat'
import accountStatus from 'components/accountStatus'
import ExternalAccountFilter from "components/common/ExternalAccountFilter";
import MyTranslate from "components/common/MyTranslate";
import { Pagination, Breadcrumb, Dialog, Tag, Input as TInput, Upload as TUpload, Tooltip as TTooltip, DateRangePicker, Button as TButton, loading, Checkbox } from 'tdesign-react';
import { SearchIcon } from 'tdesign-icons-react'
import DialogApi from './dialog/DialogApi.js'
import { download } from "components/postDownloadUtils"
import { status } from 'nprogress'
import projectTypes from 'components/projectTypes'
import SieveActiveTaskDetail from './sieveActiveDetail.js'

const { RangePicker } = DatePicker;
const { Panel } = Collapse;
const { TextArea } = Input;
const { Option } = Select;
const Search = Input.Search;
const confirm = Modal.confirm;
const { TabPane } = Tabs;
const { Item: FormItem } = Form;

// import '../../../mock'

// 针对当前页面的基础url
const baseUrl = '/api/account';
const consumer = '/api/consumer/account';

const searchItems = [
  // {desc: '昵称', key: 'nickname'},
  // {desc: '用户名', key: 'username'},
  // {desc: '手机号', key: 'phone'},
  // {desc: 'accID', key: 'accID'},
  // {desc: '被封提示', key: 'errorMessage'},
  // {desc: '群ID', key: 'chatroomID'},
  // {desc: '新设备', key: 'loginedNewDevice'},
];

const nowDate = new Date().Format("yyyy-MM-dd");

const getUploadImageProps = (regExpStr = 'txt') => {
  let regExp = new RegExp(`^.+\\.(${regExpStr})$`)
  const uploadProps = {
    name: 'file',
    multiple: false,
    action: `/api/consumer/res/uploadTxt/sieveActive`,
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
      loading: false,
      pagination: {
        pageSize: 10,
        current: 1,
        showTotal: (total, range) => `共 ${total} 条`,
        showSizeChanger: true,
        pageSizeOptions: ['10', '20', '30', '40', '50', '100', '200', '300', '400', '500']
      },
      selectedRowKeys: [],

      tableContent: null,
      mainContent: null,
      scrollY: 0,
      mainContentHeight: 0,
      searchRef: null,
      tableContentHeight: 0,

      createTimeRange: ['', ''],
      finishTimeRange: ['', ''],
      addVisible: false,
      files: [],
      editUser: null,
      desc: '',
      desc2: '',
      orderId: '',
      status: '',
      orderVisible: false,
      orders: [],
      project: projectTypes[0].value,
    },
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
      createTime: -1,
    }
    // 提前写在constructor则Table首次加载时生效
    // 但是因为没有放到state中，所以除sorter外，都不建议提前设置，保持不受控状态是最好的
  }

  // 首次加载数据
  async componentWillMount() {
    await this.reload();
  }

  refSearch = (ref) => {
    this.state.searchRef = ref
    this.setState({ searchRef: ref })
    this.handleResize()
  }

  handleResize = () => {
    let height = document.body.getBoundingClientRect().height;
    if (this.state.tableContent) {
      this.setState({ scrollY: this.state.tableContent.getBoundingClientRect().height - 80, })
    }
    if (this.state.mainContent) {
      this.setState({ mainContentHeight: this.state.mainContent.getBoundingClientRect().top })
    }
    if (this.state.searchRef) {
      setTimeout(() => {
        if (this.state.searchRef) {
          this.setState({ searchRef: this.state.searchRef })
        }
      }, 100)
    }
    if (this.state.selectedCountRef) {
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
    // 重新加载，一般是页面第一次加载的时候来一下
    this.state.pagination.current = 1
    this.setState({ pagination: this.state.pagination })
    this.load(this.state.pagination, this.filters, this.sorter)
  }

  async reset() {
    this.filters = {}
    this.state.pagination.current = 1
    this.setState({ filters: {}, pagination: { ...this.state.pagination }, createTimeRange: ['', ''], finishTimeRange: ['', ''], orderId: '', desc2: '', status: '' })
    this.load(this.state.pagination, this.filters, this.sorter)
  }

  async load(pagination, filters, sorter) {
    if (filters._id === '') {
      delete filters._id
    }
    if (filters.desc === '') {
      delete filters.desc
    }
    if (filters.status === '') {
      delete filters.status
    }
    // 这里的参数是格式好的，无需修改，无阻抗失衡，直接发给服务器
    this.setState({ loading: true })
    let res = await axios.post(`/api/task/${pagination.pageSize}/${pagination.current}`, {
      filters: { ...filters, type: 'SieveActive' }, sorter
    });
    pagination.total = res.data.data.total;
    this.setState({
      loading: false,
      data: res.data.data.data,
      pagination,
      selectedRowKeys: []
    });
    this.selectedRows = [];
    this.filters = filters;
    this.sorter = sorter;
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
      sort[sorter.field] = sorter.order == 'descend' ? -1 : 1
    } else {
      sort = {};
      sort['createTime'] = -1
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

  refSelectedCount = (ref) => {
    this.state.selectedCountRef = ref
    this.setState({ selectedCountRef: ref })
    this.handleResize()
  }

  openAdd() {
    this.setState({
      addVisible: true,
      editUser: null,
      desc: '',
      files: [],
    })
  }

  // 复制文本
  async copyTxt(txt, msg) {
    if (txt) {
      await navigator.clipboard.writeText(txt);
      message.success(msg || '复制成功')
    }
  }

  onUploadAccountAvatarChange = async (info) => {
    if (info.length > 0) {
      this.setState({ files: info });
      return
    }
  }

  cancelOrder(forceStop=false) {
    if (this.selectedRows.length === 0) {
      message.error('请选择订单')
      return
    }

    let ids = this.selectedRows.map(item => item._id)

    DialogApi.warning({
      title: `确定要${forceStop? "取消": "停止"}这些订单吗？`,
      onOkTxt: '确认',
      onCancelTxt: '取消',
      onOk: async () => {
        this.setState({ loading: true });
        let result = await axios.post(`/api/sieveActive/stop`, { ids, forceStop });
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
    const { intl } = this.props
    const columns = [
      {
        title: '订单ID',
        dataIndex: '_id',
        key: '_id',
        width: 191,
        ellipsis: true,
        render: (v, r) => {
          return v ? <div onClick={() => this.copyTxt(v, '已复制到剪切板')}>{v}</div> : '-'
        }
      },
      {
        title: '任务描述',
        dataIndex: 'desc',
        key: 'desc',
        width: 100,
        ellipsis: true,
      },
      {
        title: '项目',
        dataIndex: 'params.project',
        key: 'params.project',
        width: 80,
        ellipsis: true,
        render: (v, r) => {
          let pt = projectTypes.find(e => e.value === v)
          return pt ? pt.label : v;
        }
      },
      {
        title: '总数',
        dataIndex: 'total',
        key: 'total',
        width: 80,
        ellipsis: true,
        render: (v, r) => {
          return <div><div>{v}</div><div style={{color: "blue"}}>{((r.success + r.failed) / r.total * 100).toFixed(2)}%</div></div>
        }
      },
      {
        title: '完成/有效/失败/未筛',
        dataIndex: 'total',
        key: 'total',
        width: 200,
        ellipsis: true,
        render: (v, r) => {
          return r.params ? `${r.success || 0}/${r.params.validDataCount || 0}/${r.failed || 0}/${r.params.unexecuteCount || 0}` : ''
        }
      },
      {
        title: '订单状态',
        dataIndex: 'status',
        key: 'status',
        width: 80,
        ellipsis: true,
        render: (v, r) => {
          if (v === 'processing') {
            return <Tag>待执行</Tag>
          }
          if (v === 'success') {
            return <Tag color="green">已完成</Tag>
          }
          if (v === 'init') {
            return <Tag color="orange">执行中</Tag>
          }
          if (v === 'failed') {
            return <><Tag color="red">失败</Tag><div>{r.result? r.result.msg: ''}</div></>
          }
        }
      },
      // {
      //   title: '完成数',
      //   dataIndex: 'success',
      //   key: 'success',
      //   width: 140,
      //   ellipsis: true,
      //   render: (v, r) => {
      //     return <div><div>{r.success + r.failed}</div><div>{((r.success + r.failed) / r.total * 100).toFixed(2)}%</div></div>
      //   }
      // },
      {
        title: '创建时间',
        dataIndex: 'createTime',
        key: 'createTime',
        render: formatDate,
        width: 160,
        ellipsis: true,
      },
      {
        title: '完成时间',
        dataIndex: 'finishTime',
        key: 'finishTime',
        width: 160,
        ellipsis: true,
        render: (v, r) => {
          return v ? formatDate(v) : '--'
        }
      },
      {
        title: '操作',
        dataIndex: 'oper',
        key: 'oper',
        width: 200,
        // fixed: 'right',
        render: (v, r) => {
          return (
            <div style={{
              display: 'flex',
              alignItems: 'center',
              gap: '12px', // 按钮间距
              padding: '8px 0 8px 8px', // 上下内边距8px，左侧内边距16px
              marginLeft: '8px' // 增加左侧外边距
            }}>
              <Button
                type="link"
                style={{
                  padding: 0,
                  minWidth: 'auto',
                }}
                onClick={async () => {
                  Modal.info({
                    title:"任务详情",
                    width: 1200,
                    maskClosable: true,
                    content:<SieveActiveTaskDetail groupTaskId={r._id}/>
                  })
                }}
              >详情</Button>

              <Button
                type="link"
                style={{
                  padding: 0,
                  minWidth: 'auto',
                }}
                onClick={async () => {
                  const response = await fetch('/api/consumer/res/download/' + r.params.dataFilePath);
                  if (!response.ok) throw new Error("下载失败");

                  const blob = await response.blob();
                  const link = document.createElement("a");
                  link.href = URL.createObjectURL(blob);
                  link.download = 'emails.txt';
                  document.body.appendChild(link);
                  link.click();
                  link.remove();
                }}
              >下载原数据
              </Button>

              {r.status === 'success' ? <Button
                type="link"
                style={{
                  padding: 0,
                  minWidth: 'auto',
                }}
                onClick={() => {
                  this.setState({
                    orderVisible: true,
                    orders: r.params.results? r.params.results.map(result => ({name: result})).filter(e => e.name): []
                  })
                }}
              >订单报表
              </Button> : ''}
            </div>
          )
        }
      },
    ];

    const customRowStyle = {
      padding: '50px' // 调整此值以改变行高
    };

    return (<MyTranslate><ExternalAccountFilter>
      <Breadcrumb>
        <Breadcrumb.BreadcrumbItem>资源管理</Breadcrumb.BreadcrumbItem>
        <Breadcrumb.BreadcrumbItem>筛开通管理</Breadcrumb.BreadcrumbItem>
      </Breadcrumb>


      <Dialog
        header={"创建任务"}
        width={674}
        height={566}
        visible={this.state.addVisible}
        placement='center'
        onConfirm={async () => {
          if (!this.state.desc) {
            message.error('请输入任务描述')
            return
          }
          if (!this.state.project) {
            message.error('请选择项目')
            return
          }
          if (!this.state.files || this.state.files.length <= 0) {
            message.error('请上传文件')
            return
          }
          let form = {
            desc: this.state.desc,
            project: this.state.project,
            filepath: this.state.files[0].response.data.filepath,
          };
          this.setState({ loading: true });
          // 过滤掉null
          let res = await axios.post(`/api/sieveActive/save`, form)
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
          this.setState({ addVisible: false })
        }}
        onClose={() => {
          this.setState({ addVisible: false })
        }}
      >
        <div style={{ marginLeft: 75, marginTop: 26, color: 'rgba(0, 0, 0, 0.90)' }}>
          <div style={{ display: 'flex', marginBottom: 24 }}>
            <div style={{ width: 100, textAlign: 'right', marginRight: 16, lineHeight: '30px' }}>
              <span style={{ color: '#D54941' }}>*</span>任务描述
            </div>
            <div>
              <Input value={this.state.desc}
                onChange={(e) => this.setState({ desc: e.target.value })} style={{ width: 300 }}
                placeholder='请输入' />
            </div>
          </div>

          <div style={{ display: 'flex', marginBottom: 24 }}>
            <div style={{ width: 100, textAlign: 'right', marginRight: 16, lineHeight: '30px' }}>
              <span style={{ color: '#D54941' }}>*</span>项目
            </div>
            <div>
              <Select value={this.state.project} placeholder="请选择项目"
                onChange={(e) => this.setState({ project: e })} style={{ width: 300 }}
              >
                {projectTypes.map(item => <Option value={item.value} key={item.value}>{item.label}</Option>)}
              </Select>
            </div>
          </div>

          <div style={{ display: 'flex', marginBottom: 24 }}>
            <div style={{ width: 100, textAlign: 'right', marginRight: 16, lineHeight: '30px' }}>
              <span style={{ color: '#D54941' }}>*</span>上传文件
            </div>
            <div style={{ color: 'rgba(0, 0, 0, 0.40)' }}>
              <TUpload
                {...getUploadImageProps()}
                files={this.state.files}
                onChange={this.onUploadAccountAvatarChange.bind(this)}
                onRemove={() => this.setState({ files: [] })}
              />
              <div style={{ fontSize: 12 }}>请选择txt格式的文件上传，文件小于30M</div>
            </div>
          </div>
        </div>
      </Dialog>


      <Dialog
        header={"订单报表"}
        width={874}
        height={566}
        visible={this.state.orderVisible}
        placement='center'
        onCancel={() => {
          this.setState({ orderVisible: false })
        }}
        onClose={() => {
          this.setState({ orderVisible: false })
        }}
        onConfirm={() => {
          this.setState({ orderVisible: false })
        }}
      >
        <div style={{ marginTop: 2, color: 'rgba(0, 0, 0, 0.90)' }}>
          <Table
            size="middle"
            tableLayout="fixed"
            pagination={false} columns={[
              {
                title: '文件名',
                dataIndex: 'name',
                key: 'name',
                width: 200,
                ellipsis: true,
                render: (v, r) => {
                  return v.substring(v.lastIndexOf('/') + 1);
                }
              }, {
                title: '操作',
                dataIndex: 'oper',
                key: 'oper',
                width: 80,
                // fixed: 'right',
                render: (v, r) => {
                  return (
                    <div style={{
                      display: 'flex',
                      alignItems: 'center',
                      gap: '12px', // 按钮间距
                      padding: '8px 0 8px 8px', // 上下内边距8px，左侧内边距16px
                      marginLeft: '8px' // 增加左侧外边距
                    }}>
                      <Button
                        type="link"
                        style={{
                          padding: 0,
                          minWidth: 'auto',
                        }}
                        onClick={async () => {
                          const response = await fetch('/api/consumer/res/download/' + r.name);
                          if (!response.ok) throw new Error("下载失败");

                          const blob = await response.blob();
                          const link = document.createElement("a");
                          link.href = URL.createObjectURL(blob);
                          // r.name 最后一个/ 后面的字符串
                          link.download = r.name.split('/').pop();
                          document.body.appendChild(link);
                          link.click();
                          link.remove();
                        }}
                      >导出邮箱
                      </Button>
                    </div>
                  )
                }
              },
            ]}
            dataSource={this.state.orders}
            loading={this.state.loading}
          />
        </div>
      </Dialog>

      <div className="account-search-box">
        <div className='account-search-item' style={{ minWidth: 280 }}>
          <div className="account-search-item-label">订单ID</div>
          <div className="account-search-item-right">
            <Input
              allowClear
              style={{ width: 200 }}
              placeholder="请输入"
              value={this.state.orderId}
              onChange={e => {
                this.setState({ orderId: e.target.value })
                this.filters['_id'] = e.target.value
              }
              }
              onPressEnter={e => {
                this.setState({ orderId: e.target.value })
                this.filters['_id'] = e.target.value
                this.reload()
              }
              }
            />
          </div>
        </div>

        <div className='account-search-item' style={{ minWidth: 280 }}>
          <div className="account-search-item-label">任务描述</div>
          <div className="account-search-item-right">
            <Input
              allowClear
              style={{ width: 200 }}
              placeholder="请输入"
              value={this.state.desc2}
              onChange={e => {
                this.setState({ desc2: e.target.value })
                this.filters['desc'] = e.target.value
              }
              }
              onPressEnter={e => {
                this.setState({ desc2: e.target.value })
                this.filters['desc'] = e.target.value
                this.reload()
              }
              }
            />
          </div>
        </div>

        <div className='account-search-item'>
          <div className="account-search-item-label">订单状态</div>
          <div className="account-search-item-right">
            <Select value={this.state.status} style={{ width: 200 }} onChange={v => {
              this.setState({ status: v })
              this.filters['status'] = v
              this.reload()
            }}>
              <Option value="">全部</Option>
              <Option value="processing">待执行</Option>
              <Option value="init">执行中</Option>
              <Option value="success">已完成</Option>
            </Select>
          </div>
        </div>

        <div className='account-search-item'>
          <div className="account-search-item-label">创建时间</div>
          <div className="account-search-item-right" style={{ width: 500 }}>
            <DateRangePicker
              mode="date"
              presetsPlacement="bottom"
              enableTimePicker
              value={this.state.createTimeRange}
              onChange={(e) => {
                this.setState({
                  createTimeRange: e
                })
                this.filters['createTime'] = {
                  $gte: e[0],
                  $lte: e[1],
                }
                this.reload()
              }}
            />
          </div>
        </div>

        <div className='account-search-item'>
          <div className="account-search-item-label">完成时间</div>
          <div className="account-search-item-right" style={{ width: 500 }}>
            <DateRangePicker
              mode="date"
              presetsPlacement="bottom"
              enableTimePicker
              value={this.state.finishTimeRange}
              onChange={(e) => {
                this.setState({
                  finishTimeRange: e
                })
                this.filters['finishTime'] = {
                  $gte: e[0],
                  $lte: e[1],
                }
                this.reload()
              }}
            />
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

      <div className="account-main-content">
        <div className="account-main-content-right">
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
            <div style={{ display: 'flex' }}>
              <div className={"search-query-btn"} onClick={() => {
                this.openAdd()
              }}>新增
              </div>

              <div
                className={this.state.selectedRowKeys && this.state.selectedRowKeys.length > 0 ? "search-delete-btn" : "search-reset-btn"}
                onClick={() => this.cancelOrder(false)}>停止订单任务

              </div>

              <div
                className={this.state.selectedRowKeys && this.state.selectedRowKeys.length > 0 ? "search-delete-btn" : "search-reset-btn"}
                onClick={() => this.cancelOrder(true)}>取消订单

              </div>
            </div>
          </div>
          <div className="tableSelectedCount"
            ref={this.refSelectedCount}>{`已选${this.state.selectedRowKeys.length}项`}</div>
          <div className="tableContent accountTableContent" style={{ height: this.state.tableContentHeight }}>
            <div>
              <Table
                size="middle"
                tableLayout="fixed"
                scroll={{
                  y: this.state.scrollY,
                  x: columns.filter(e => e.width).map(e => e.width).reduce((a, b) => a + b)
                }}
                pagination={this.state.pagination} rowSelection={{
                  selectedRowKeys: this.state.selectedRowKeys,
                  onChange: this.onRowSelectionChange.bind(this)
                }} columns={columns}
                rowKey='_id'
                dataSource={this.state.data}
                loading={this.state.loading}
                onChange={this.handleTableChange.bind(this)}
              />
            </div>
          </div>
          <Pagination
            showJumper
            total={this.state.pagination.total}
            current={this.state.pagination.current}
            pageSize={this.state.pagination.pageSize}
            onChange={this.handleTableChange.bind(this)}
            components={{
              body: {
                row: (props) => (
                  <tr {...props} style={{ ...props.style, ...customRowStyle }} />
                ),
              },
            }}
          />
        </div>
      </div>
    </ExternalAccountFilter></MyTranslate>
    )
  }
}

export default injectIntl(MyComponent)
