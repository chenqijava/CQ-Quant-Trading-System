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
} from 'antd'
import { Pagination, Breadcrumb, Dialog, Tag, Input as TInput, Upload as TUpload, Tooltip as TTooltip, Switch, DateRangePicker, Alert,Checkbox } from 'tdesign-react';
import {ChevronDownIcon} from 'tdesign-icons-react'
import axios from 'axios'
import { formatDate } from 'components/DateFormat'
import { download } from "components/postDownloadUtils"
import { connect } from 'dva'
import { injectIntl } from 'react-intl'
import DialogApi from '../dialog/DialogApi';

const { Option } = Select;
const Search = Input.Search;
const confirm = Modal.confirm;

// 针对当前页面的基础url
const baseUrl = '/api/consumer/platform';
const params = []

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
      data3: [],
      data4: [],
      data5: [],
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
      pagination3: {
        total: 0,
        pageSize: 10,
        current: 1,
        showTotal: (total, range) => `共 ${total} 条`,
        showSizeChanger: true,
        pageSizeOptions: ['10', '20', '30', '40', '50', '100', '200', '300', '400', '500']
      },
      pagination4: {
        total: 0,
        pageSize: 10,
        current: 1,
        showTotal: (total, range) => `共 ${total} 条`,
        showSizeChanger: true,
        pageSizeOptions: ['10', '20', '30', '40', '50', '100', '200', '300', '400', '500']
      },
      pagination5: {
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
      recordVisible: false,
      testNum: '',

      progressVisible: false,
      editData: {},
      detailData: null,

      showAdd: false,
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
    this.filters2 = {
    };
    this.filters3 = {
    };
    // sorter的写法保持与mongo sort的写法一致，避免与服务器、数据库直接的数据阻抗失衡
    // 例如：sorter={createTime: -1}
    // 注意：Table控件仅支持单列排序，不支持多列同时排序
    this.sorter = {
      createTime: -1,
      sortNo: -1,
    }

    this.sorter2 = {
      createTime: -1,
    }
    this.sorter3 = {
      createTime: -1,
    }

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
    let res = await axios.post(`/api/batchSendEmail/${pagination.pageSize}/${pagination.current}`, { filters: { ...filters, userID: this.props.userID }, sorter });
    pagination.total = res.data.data.total;
    this.setState({ loading: false, data: res.data.data.data, pagination, selectedRowKeys: [] });
    this.selectedRows = [];
    this.filters = filters;
    this.sorter = sorter
  }

  async load3(pagination, filters, sorter) {
    // 这里的参数是格式好的，无需修改，无阻抗失衡，直接发给服务器
    this.setState({ loading: true });
    let res = await axios.post(`/api/batchSendEmail/detailList/${this.state.editData._id}/${pagination.pageSize}/${pagination.current}`, { filters: { ...filters, userID: this.props.userID }, sorter });
    pagination.total = res.data.data.total;
    this.setState({ loading: false, data3: res.data.data.data, pagination3: pagination });
    this.filters3 = filters;
    this.sorter3 = sorter
  }

  async load4(pagination) {
    // 这里的参数是格式好的，无需修改，无阻抗失衡，直接发给服务器
    this.setState({ loading: true });
    let res = await axios.post(`/api/batchSendEmail/systemEmailDetail/${this.state.editData._id}/${pagination.pageSize}/${pagination.current}`, { });
    pagination.total = res.data.data.total;
    this.setState({ loading: false, data4: res.data.data.data, pagination4: pagination });
  }

  async load5(pagination) {
    // 这里的参数是格式好的，无需修改，无阻抗失衡，直接发给服务器
    this.setState({ loading: true });
    let res = await axios.post(`/api/batchSendEmail/otherEmailDetail/${this.state.editData._id}/${pagination.pageSize}/${pagination.current}`, { });
    pagination.total = res.data.data.total;
    this.setState({ loading: false, data5: res.data.data.data, pagination5: pagination });
  }

  async getDetail() {
    this.setState({ loading: true });
    let res = await axios.post(`/api/batchSendEmail/detail/${this.state.editData._id}`, {});
    this.setState({ loading: false, detailData: res.data.data });
  }

  async loadTestData(pagination, filters, sorter) {
    // 这里的参数是格式好的，无需修改，无阻抗失衡，直接发给服务器
    this.setState({ loading: true });
    let res = await axios.post(`/api/batchSendEmail/test/${this.selectedRows[0]._id}/${pagination.pageSize}/${pagination.current}`, { filters: { ...filters, userID: this.props.userID }, sorter });
    pagination.total = res.data.data.total;
    this.setState({ loading: false, data2: res.data.data.data, pagination2: pagination, });
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
      onOk: async () => {
        this.setState({ loading: true });
        await axios.post(`${baseUrl}/delete`, r ? [r._id] : this.state.selectedRowKeys);
        message.success('操作成功');
        this.reload()
      },
      onCancel() {
      }
    })
  }

  async enableBtnClick(record, enable) {
    this.setState({ loading: true });
    await axios.post(`${baseUrl}/changeEnable/${record._id}`, { enable });
    message.success('操作成功');
    this.reload()
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
        let result = await axios.post(`/api/batchSendEmail/delete`, { ids: keys });
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

  async handleTableChange2(pagination, filters, sorter) {
    // 暂时不用Table的filter，不太好用
    this.loadTestData(pagination, this.filters2, this.sorter2)
  }

  async handleTableChange3(pagination, filters, sorter) {
    // 暂时不用Table的filter，不太好用
    let sort = this.sorter3;
    if (sorter && sorter.field) {
      sort = {};
      sort[sorter.field] = sorter.order == 'descend'
        ? -1
        : 1
    }
    this.load3(pagination, this.filters3, sort)
  }

  async handleTableChange4(pagination, filters, sorter) {
    this.load4(pagination)
  }

  async handleTableChange5(pagination, filters, sorter) {
    this.load5(pagination)
  }

  async sendTestEmail(r) {
    if (this.state.loading) {
      return
    }
    this.state.loading = true
    this.setState({ loading: true })
    let reqDto = r.params.reqDto
    try {
      let data = await axios.post(`/api/batchSendEmail/test`, {
        taskName: reqDto.taskName,
        emailAddType: reqDto.emailAddType,
        title: reqDto.title,
        content: reqDto.content,
        useAiOptimize: reqDto.useAiOptimize,
        addMethod: reqDto.addMethod,
        filepath: reqDto.filepath,
        addData: reqDto.addData,
        taskId: reqDto.taskId,
        action: reqDto.action,
        estimateSendNum: reqDto.estimateSendNum,
        sendMethod: reqDto.sendMethod,
        sendTime: new Date(reqDto.sendTime).toISOString(),
        loopSend: reqDto.loopSend,
        loopType: reqDto.loopType,
        monitorOpen: reqDto.monitorOpen,
        monitorClick: reqDto.monitorClick,
        testAB: reqDto.testAB,
        titleB: reqDto.titleB,
        contentB: reqDto.contentB,
        percent: reqDto.percent,
        testTimeLengthHour: reqDto.testTimeLengthHour,
        factor: reqDto.factor,
        count: this.state.testNum,
        groupTaskId: r._id,
      })
      if (data.data.data) {
        message.success('测试任务已创建')
        this.loadTestData(this.state.pagination2, this.filters2, this.sorter2)
        this.timer = setInterval(() => {
          this.loadTestData(this.state.pagination2, this.filters2, this.sorter2)
        }, 1000 * 5)
      } else {
        message.error(data.data.message)
      }
    } finally {
      this.state.loading = false
      this.setState({ loading: false })
    }
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
        title: '发送渠道',
        dataIndex: 'params.reqDto.type',
        key: 'params.reqDto.type',
        width: 100,
        ellipsis: true,
        render: (v) => {
          if (v === 'api') {
            return 'API'
          }
          return '系统'
        }
      }, {
        title: '总数',
        dataIndex: 'total',
        key: 'total',
        width: 80,
        ellipsis: true,
      },{
        title: '总投递次数',
        dataIndex: 'params.totalSendEmailCount',
        key: 'params.totalSendEmailCount',
        width: 100,
        ellipsis: true,
        render: (v) => {
          return v || '0'
        }
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
        width: 400,
        ellipsis: true,
        render: (t, record, index) => {
          return (<div>
            <Button type="link" onClick={async () => {
              this.props.history.push('/cloud/user/batchSendEmailAdd?id=' + record._id + '&type=' + record.params.reqDto.type)

            }}>查看</Button>
            <Button type="link" onClick={async () => {
              this.state.editData = record
              let flag = false
              let p = new Promise(async (r) => {
                while (!flag) {
                  this.setState({loading: true})
                  await new Promise((r) => {
                    setTimeout(r, 50)
                  })
                }
                this.setState({loading: false})
                r()
              })
              await Promise.all([this.load3(this.state.pagination3, this.filters3, this.sorter3), this.getDetail()])
              flag = true
              await p
              this.setState({
                progressVisible: true,
                editData: record
              })
            }}>进度</Button>

            {record.status !== 'pause' && record.status !== 'success' ? <Button type="link" onClick={async () => {
              DialogApi.warning({
                title: '确认暂停任务吗？',
                onOk: async () => {
                  let res = await axios.post(`/api/batchSendEmail/pause`, { ids: [record._id] })
                  if (res.data.code === 1) {
                    message.success('暂停成功')
                    this.reload()
                  } else {
                    message.error('暂停失败,' + res.data.message)
                  }
                },
                onCancel: () => {}
              })
            }}>暂停</Button> : ''}

            {record.status === 'pause' ? <Button type="link" onClick={async () => {
              DialogApi.warning({
                title: '确认继续任务吗？',
                onOk: async () => {
                  let res = await axios.post(`/api/batchSendEmail/start`, { ids: [record._id] })
                  if (res.data.code === 1) {
                    message.success('继续成功')
                    this.reload()
                  } else {
                    message.error('继续失败,' + res.data.message)
                  }
                },
                onCancel: () => {}
              })
            }}>继续</Button> : ''}

            {record.status !== 'success' ? <Button type="link" onClick={async () => {
              DialogApi.warning({
                title: '确认结束任务吗？',
                onOk: async () => {
                  let res = await axios.post(`/api/batchSendEmail/stop`, { ids: [record._id] })
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
              let a1 = '0'
              let a2 = '0'
              let a3 = '0'
              let a4 = '0'
              DialogApi.info({
                width: 600,
                title: '下载报告',
                content: <>
                  <div style={{ display: 'flex', alignItems: 'center' }}>
                    <div style={{ color: 'rgba(0, 0, 0, 0.90)', width: 100, lineHeight: '32px', textAlign: 'right' }}><span style={{ color: '#D54941' }}>*</span>自定义导出项</div>
                    <Checkbox style={{marginLeft: 10}} defaultChecked={false} onChange={(val) => a1 = val ? '1' : '0'}>发送方</Checkbox>
                    <Checkbox style={{marginLeft: 10}} defaultChecked={false} onChange={(val) => a2 = val ? '2' : '0'}>是否打开</Checkbox>
                    <Checkbox style={{marginLeft: 10}} defaultChecked={false} onChange={(val) => a3 = val ? '3' : '0'}>是否点击</Checkbox>
                    <Checkbox style={{marginLeft: 10}} defaultChecked={false} onChange={(val) => a4 = val ? '4' : '0'}>是否回复</Checkbox>
                  </div>
                </>,
                onOk: async () => {
                  download(`/api/batchSendEmail/export`, {
                    groupTaskId: record._id,
                    fields: a1 + ',' + a2 + ',' + a3 + ',' + a4
                  })
                },
                onCancel: () => {}
              })
              // download(`/api/batchSendEmail/export`, {
              //   groupTaskId: record._id,
              // })
            }}>下载明细</Button>

            <Button type="link" onClick={async () => {
              download(`/api/batchSendEmail/exportFail`, {
                groupTaskId: record._id,
              })
            }}>失败账号导出</Button>

            <Button type="link" onClick={async () => {
              this.state.editData = record
              let flag = false
              let p = new Promise(async (r) => {
                while (!flag) {
                  this.setState({loading: true})
                  await new Promise((r) => {
                    setTimeout(r, 50)
                  })
                }
                this.setState({loading: false})
                r()
              })
              await Promise.all([this.load4(this.state.pagination4), this.load5(this.state.pagination5)])
              flag = true
              await p
              this.setState({
                maidianVisible: true,
                editData: record
              })
            }}>埋点情况</Button>

            {/* {record.status === 'Waiting' ? <Button type="link" onClick={async () => {
              let res = await axios.post(`/api/buyEmailOrder/release`, { id: record._id })
              if (res.data.code === 1) {
                message.success('释放成功')
                this.reload()
              } else {
                message.error('释放失败,' + res.data.message)
              }
            }}>释放</Button> : ''}

            {record.status === 'Waiting' || record.status === 'CodeReceived' ? <Button type="link" onClick={async () => {
              window.open(`/api/latest/code?id=${record.subTaskId}&aid=${record.accid}`, '_blank')
            }}>链接</Button> : ''} */}
          </div>)
        }
      }
    ];

    const columns2 = [
      {
        title: '发送账号',
        dataIndex: 'params.sendEmail',
        key: 'params.sendEmail',
        width: 150,
        ellipsis: true,
      },
      {
        title: '接收账号',
        dataIndex: 'params.email',
        key: 'params.email',
        width: 150,
        ellipsis: true,
      },
      {
        title: '发送状态',
        dataIndex: 'status',
        key: 'status',
        width: 100,
        ellipsis: true,
        render: (v) => {
          if (v === 'success') {
            return '成功'
          }
          if (v === 'failed') {
            return '失败'
          }
          return '发送中'
        }
      },
      {
        title: '结果',
        dataIndex: 'result',
        key: 'result',
        width: 100,
        ellipsis: true,
        render: (v) => {
          if (v && v.msg) {
            if (v.msg === '收到邮件') {
              return '正常'
            } else {
              return v.msg
            }
          }

          return '--'
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
      },
    ]


    const columns3 = [
      {
        title: '发送账号',
        dataIndex: 'params.sendEmail',
        key: 'params.sendEmail',
        width: 150,
        ellipsis: true,
      },
      {
        title: '接收账号',
        dataIndex: 'params.email',
        key: 'params.email',
        width: 150,
        ellipsis: true,
      },
      {
        title: '发送状态',
        dataIndex: 'status',
        key: 'status',
        width: 100,
        ellipsis: true,
        sorter: true,
        render: (v, r) => {
          if (v === 'success') {
            return '成功'
          }
          if (v === 'failed') {
            return '失败'
          }
          if (r.params.tuiRetry) {
            return "重试中"
          }
          return '发送中'
        }
      },
      {
        title: '是否打开',
        dataIndex: 'result.open',
        key: 'result.open',
        width: 100,
        ellipsis: true,
        sorter: true,
        render: (v,r) => {
          if (r.result && r.result.open) {
            return '是'
          }
          return '否'
        }
      },
      {
        title: '是否点击',
        dataIndex: 'result.click',
        key: 'result.click',
        width: 100,
        ellipsis: true,
        sorter: true,
        render: (v,r) => {
          if (r.result && r.result.click) {
            return '是'
          }
          return '否'
        }
      },
      {
        title: '是否回复',
        dataIndex: 'result.reply',
        key: 'result.reply',
        width: 100,
        ellipsis: true,
        sorter: true,
        render: (v, r) => {
          if (r.result && r.result.reply) {
            if (r.result.reply === '1') {
              return '是'
            } else {
              return '退信'
            }
          }
          return '否'
        }
      },
      {
        title: '结果',
        dataIndex: 'result',
        key: 'result',
        width: 100,
        ellipsis: true,
        render: (v) => {
          if (v && v.msg) {
            if (v.msg === '收到邮件') {
              return '正常'
            } else {
              return v.msg
            }
          }

          return '--'
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
      },
    ]

    const columns4 = [{
      title: '版本',
      dataIndex: 'version',
      key: 'version',
      width: 100,
      ellipsis: true,
    }, {
      title: '发送内容',
      dataIndex: 'content',
      key: 'content',
      width: 100,
      ellipsis: true,
      render: (v) => {
        return <Button type='link' onClick={() => {
          DialogApi.info({
            width: 1000,
            title: '内容',
            content: <div style={{maxHeight: 500, overflow: 'auto'}}><div dangerouslySetInnerHTML={{ __html: v || '' }}></div></div>,
            onCancel: () => {}
          })
        }}>查看</Button>
      }
    },{
      title: '总数',
      dataIndex: 'total',
      key: 'total',
      width: 100,
      ellipsis: true,
    },{
      title: '成功',
      dataIndex: 'success',
      key: 'success',
      width: 100,
      ellipsis: true,
    },{
      title: '失败',
      dataIndex: 'failed',
      key: 'failed',
      width: 100,
      ellipsis: true,
    },{
      title: '打开率',
      dataIndex: 'openRate',
      key: 'openRate',
      width: 100,
      ellipsis: true,
      render: (v, r) => {
        return <div>{r.openNum}({(r.openNum / (r.total||1) * 100).toFixed(2)}%)</div>
      }
    },{
      title: '点击率',
      dataIndex: 'clickRate',
      key: 'clickRate',
      width: 100,
      ellipsis: true,
      render: (v, r) => {
        return <div>{r.clickNum}({(r.clickNum / (r.total||1) * 100).toFixed(2)}%)</div>
      }
    },{
      title: '回复率',
      dataIndex: 'replyRate',
      key: 'replyRate',
      width: 100,
      ellipsis: true,
      render: (v, r) => {
        return <div>{r.replyNum}({(r.replyNum / (r.total||1) * 100).toFixed(2)}%)</div>
      }
    },{
      title: '操作',
      dataIndex: 'op',
      key: 'op',
      width: 150,
      ellipsis: true,
      render: (v,r) => {
        return this.state.detailData.groupTask.params.testABStep !== '2' && this.state.detailData.groupTask.publishStatus === 'success' && this.state.detailData.groupTask.params.reqDto.factor === 'other' ? <Button type='link' onClick={() => {
          DialogApi.warning({
            title: '确认选择此版本发送',
            onOk: async () => {
              let res =await axios.post(`/api/batchSendEmail/executeAB/${this.state.detailData.groupTask._id}/${r.version==='版本A' ? 'A':'B'}`)
              if (res.data.code === 1) {
                message.success('操作成功')
                this.getDetail()
              } else {
                message.error('操作失败，' + res.data.message)
              }
            },
            onCancel: () => {}
          })
        }}>选择此版本发送</Button> : ''
      }
    },]

    const columns5 = [
      {
        title: '发送账号',
        dataIndex: 'params.sendEmail',
        key: 'params.sendEmail',
        width: 150,
        ellipsis: true,
      },
      {
        title: '接收账号',
        dataIndex: 'params.email',
        key: 'params.email',
        width: 150,
        ellipsis: true,
      },
      {
        title: '标签',
        dataIndex: 'result.labels',
        key: 'result.labels',
        width: 150,
        ellipsis: true,
        render: (v, r) => {
          if ((v && v.split(',').indexOf('^s') < 0) && r.status == 'success') {
            return '正常'
          }
          if (v && v.split(',').indexOf('^s') >= 0) {
            return '垃圾'
          }
          return '--'
        }
      },
      {
        title: '发送状态',
        dataIndex: 'status',
        key: 'status',
        width: 100,
        ellipsis: true,
        sorter: true,
        render: (v, r) => {
          if (v === 'success') {
            return '成功'
          }
          if (v === 'failed') {
            return '失败'
          }
          if (r.params.tuiRetry) {
            return "重试中"
          }
          return '发送中'
        }
      },
      {
        title: '结果',
        dataIndex: 'result',
        key: 'result',
        width: 100,
        ellipsis: true,
        render: (v) => {
          if (v && v.msg) {
            if (v.msg === '收到邮件') {
              return '正常'
            } else {
              return v.msg
            }
          }

          return '--'
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
      },
    ]

    
    const columns6 = [
      {
        title: '发送账号',
        dataIndex: 'params.sendEmail',
        key: 'params.sendEmail',
        width: 150,
        ellipsis: true,
      },
      {
        title: '接收账号',
        dataIndex: 'params.email',
        key: 'params.email',
        width: 150,
        ellipsis: true,
      },
      {
        title: '发送状态',
        dataIndex: 'status',
        key: 'status',
        width: 100,
        ellipsis: true,
        // sorter: true,
        render: (v, r) => {
          if (v === 'success') {
            return '成功'
          }
          if (v === 'failed') {
            return '失败'
          }
          if (r.params.tuiRetry) {
            return "重试中"
          }
          return '发送中'
        }
      },
      {
        title: '是否打开',
        dataIndex: 'result.open',
        key: 'result.open',
        width: 100,
        ellipsis: true,
        // sorter: true,
        render: (v,r) => {
          if (r.result && r.result.open) {
            return '是'
          }
          return '否'
        }
      },
      {
        title: '是否点击',
        dataIndex: 'result.click',
        key: 'result.click',
        width: 100,
        ellipsis: true,
        // sorter: true,
        render: (v,r) => {
          if (r.result && r.result.click) {
            return '是'
          }
          return '否'
        }
      },
      {
        title: '是否回复',
        dataIndex: 'result.reply',
        key: 'result.reply',
        width: 100,
        ellipsis: true,
        // sorter: true,
        render: (v, r) => {
          if (r.result && r.result.reply) {
            if (r.result.reply === '1') {
              return '是'
            } else {
              return '退信'
            }
          }
          return '否'
        }
      },
      {
        title: '结果',
        dataIndex: 'result',
        key: 'result',
        width: 100,
        ellipsis: true,
        render: (v) => {
          if (v && v.msg) {
            if (v.msg === '收到邮件') {
              return '正常'
            } else {
              return v.msg
            }
          }

          return '--'
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
      },
    ]
    

    return (<div>
      <Breadcrumb>
        <Breadcrumb.BreadcrumbItem>个人中心</Breadcrumb.BreadcrumbItem>
        <Breadcrumb.BreadcrumbItem>邮件群发</Breadcrumb.BreadcrumbItem>
      </Breadcrumb>


      <Dialog
        header={"任务检测"}
        width={1200}
        // height={700}
        visible={this.state.recordVisible}
        placement='center'
        confirmBtn={null}
        onCancel={() => {
          if (this.timer) {
            clearInterval(this.timer)
            this.timer = null
          }
          this.setState({ recordVisible: false })
        }}
        onClose={() => {
          if (this.timer) {
            clearInterval(this.timer)
            this.timer = null
          }
          this.setState({ recordVisible: false })
        }}
      >
        <div style={{ margin: 30, marginTop: 10, color: 'rgba(0, 0, 0, 0.90)' }}>
          <Alert
            title="提示"
            message={(<p>
              <ul style={{ listStyle: 'none', marginLeft: '-40px' }}>
                <li>在群发过程中，你可以使用「主动检测」功能，将当前任务的邮件内容模拟发送到系统内置的种子邮箱，我们将自动分析邮件是否进入收件箱、垃圾箱或广告标签页。检测邮件将扣除次数</li>
              </ul>
            </p>)}
          />

          <div style={{ display: 'flex', marginLeft: 0, marginTop: 10 }}>
            <div style={{ color: 'rgba(0, 0, 0, 0.90)', width: 100, lineHeight: '32px', textAlign: 'right' }}><span style={{ color: '#D54941' }}>*</span>测试数量</div>
            <div style={{ width: 200, marginLeft: 16, }}>
              <TInput
                placeholder="请输入测试数量"
                clearable
                value={this.state.testNum}
                onChange={(value) => {
                  this.setState({ testNum: value })
                }}
              />
            </div>

            <div className='search-query-btn' style={{ marginLeft: 16 }} onClick={() => {
              if (!this.state.testNum) {
                message.error('请输入测试数量')
                return
              }

              if (isNaN(this.state.testNum)) {
                message.error('请输入测试数量')
                return
              }

              if (this.state.testNum > 10000) {
                message.error('测试数量不能大于10000')
                return
              }

              if (this.state.testNum <= 0) {
                message.error('测试数量不能小于等于0')
                return
              }

              DialogApi.warning({
                title: `确认发送${this.selectedRows[0].params.reqDto.testAB === 'yes' ? this.state.testNum * 2 : this.state.testNum}封测试邮件吗？`,
                onOk: () => {
                  this.sendTestEmail(this.selectedRows[0])
                },
                onCancel: () => {
                  message.info('测试邮件发送已取消')
                }
              })
            }}>发送测试</div>
          </div>

          <div className="tableContent accountTableContent" style={{ marginTop: 20, height: 'auto' }}>
            <Table
              size="middle"
              tableLayout="fixed"
              pagination={this.state.pagination2} columns={columns2}
              dataSource={this.state.data2}
              loading={this.state.loading}
              onChange={this.handleTableChange2.bind(this)}
            />
          </div>

          <Pagination
            showJumper
            total={this.state.pagination2.total}
            current={this.state.pagination2.current}
            pageSize={this.state.pagination2.pageSize}
            onChange={this.handleTableChange2.bind(this)}
          />
        </div>
      </Dialog>

      <Dialog
        header={"埋点情况"}
        width={1400}
        visible={this.state.maidianVisible}
        placement='center'
        confirmBtn={null}
        onCancel={() => {
          this.setState({ maidianVisible: false })
        }}
        onClose={() => {
          this.setState({ maidianVisible: false })
        }}
        closeOnOverlayClick={false}
      >
        <div style={{ margin: 30, marginTop: 10, color: 'rgba(0, 0, 0, 0.90)' }}>
          <div style={{color:'#000', fontWeight:600,fontSize:16, marginBottom: 19}}>系统内种子邮箱明细</div>
          <div className="tableContent accountTableContent" style={{ marginTop: 20, height: 'auto' }}>
            <Table
              size="middle"
              tableLayout="fixed"
              pagination={this.state.pagination4} columns={columns5}
              dataSource={this.state.data4}
              loading={this.state.loading}
              onChange={this.handleTableChange4.bind(this)}
            />
          </div>

          <Pagination
            showJumper
            total={this.state.pagination4.total}
            current={this.state.pagination4.current}
            pageSize={this.state.pagination4.pageSize}
            onChange={this.handleTableChange4.bind(this)}
          />

          <div style={{color:'#000', fontWeight:600,fontSize:16, marginBottom: 19}}>外部种子邮箱明细</div>
          <div className="tableContent accountTableContent" style={{ marginTop: 20, height: 'auto' }}>
            <Table
              size="middle"
              tableLayout="fixed"
              pagination={this.state.pagination5} columns={columns6}
              dataSource={this.state.data5}
              loading={this.state.loading}
              onChange={this.handleTableChange5.bind(this)}
            />
          </div>

          <Pagination
            showJumper
            total={this.state.pagination5.total}
            current={this.state.pagination5.current}
            pageSize={this.state.pagination5.pageSize}
            onChange={this.handleTableChange5.bind(this)}
          />
        </div>
      </Dialog>


      <Dialog
        header={"进度"}
        width={1400}
        visible={this.state.progressVisible}
        placement='center'
        confirmBtn={null}
        onCancel={() => {
          this.setState({ progressVisible: false })
        }}
        onClose={() => {
          this.setState({ progressVisible: false })
        }}
        closeOnOverlayClick={false}
      >
        { this.state.detailData ? <div style={{ margin: 30, marginTop: 10, color: 'rgba(0, 0, 0, 0.90)' }}>
          <div style={{ display: 'flex' }}>
            <div style={{ padding: 24, width: 220, height: 102, borderRadius: 12, boxShadow: '0px 1px 10px 0px rgba(0, 0, 0, 0.05), 0px 4px 5px 0px rgba(0, 0, 0, 0.08), 0px 2px 4px -1px rgba(0, 0, 0, 0.12)', background: '#FFF' }}>
              <div style={{ color: '#4B4B4B', fontSize: 16 }}>发送成功数</div>
              <div style={{ display: 'flex', marginTop: 10, alignItems: 'baseline' }}>
                <div style={{ color: '#000', fontSize: 24, fontWeight: 600 }}>{this.state.detailData.successNum}</div>
                <div style={{ color: 'rgba(0,0,0,0.6)', fontSize: 14, fontWeight: 600, marginLeft: 8, }}>({(this.state.detailData.successNum/this.state.detailData.groupTask.total * 100).toFixed(2)}%)</div>
              </div>
            </div>

            <div style={{ marginLeft: 50, padding: 24, width: 220, height: 102, borderRadius: 12, boxShadow: '0px 1px 10px 0px rgba(0, 0, 0, 0.05), 0px 4px 5px 0px rgba(0, 0, 0, 0.08), 0px 2px 4px -1px rgba(0, 0, 0, 0.12)', background: '#FFF' }}>
              <div style={{ color: '#4B4B4B', fontSize: 16 }}>打开数</div>
              <div style={{ display: 'flex', marginTop: 10, alignItems: 'baseline' }}>
                <div style={{ color: '#000', fontSize: 24, fontWeight: 600 }}>{this.state.detailData.openNum}</div>
                <div style={{ color: 'rgba(0,0,0,0.6)', fontSize: 14, fontWeight: 600, marginLeft: 8, }}>({(this.state.detailData.openNum/this.state.detailData.groupTask.total * 100).toFixed(2)}%)</div>
              </div>
            </div>

            <div style={{ marginLeft: 50, padding: 24, width: 220, height: 102, borderRadius: 12, boxShadow: '0px 1px 10px 0px rgba(0, 0, 0, 0.05), 0px 4px 5px 0px rgba(0, 0, 0, 0.08), 0px 2px 4px -1px rgba(0, 0, 0, 0.12)', background: '#FFF' }}>
              <div style={{ color: '#4B4B4B', fontSize: 16 }}>回复数</div>
              <div style={{ display: 'flex', marginTop: 10, alignItems: 'baseline' }}>
                <div style={{ color: '#000', fontSize: 24, fontWeight: 600 }}>{this.state.detailData.replyNum}</div>
                <div style={{ color: 'rgba(0,0,0,0.6)', fontSize: 14, fontWeight: 600, marginLeft: 8, }}>({(this.state.detailData.replyNum/this.state.detailData.groupTask.total * 100).toFixed(2)}%)</div>
              </div>
            </div>

            <div style={{ marginLeft: 50, padding: 24, width: 220, height: 102, borderRadius: 12, boxShadow: '0px 1px 10px 0px rgba(0, 0, 0, 0.05), 0px 4px 5px 0px rgba(0, 0, 0, 0.08), 0px 2px 4px -1px rgba(0, 0, 0, 0.12)', background: '#FFF' }}>
              <div style={{ color: '#4B4B4B', fontSize: 16 }}>点击数</div>
              <div style={{ display: 'flex', marginTop: 10, alignItems: 'baseline' }}>
                <div style={{ color: '#000', fontSize: 24, fontWeight: 600 }}>{this.state.detailData.clickNum}</div>
                <div style={{ color: 'rgba(0,0,0,0.6)', fontSize: 14, fontWeight: 600, marginLeft: 8, }}>({(this.state.detailData.clickNum/this.state.detailData.groupTask.total * 100).toFixed(2)}%)</div>
              </div>
            </div>

            <div style={{ marginLeft: 50, padding: 24, width: 220, height: 102, borderRadius: 12, boxShadow: '0px 1px 10px 0px rgba(0, 0, 0, 0.05), 0px 4px 5px 0px rgba(0, 0, 0, 0.08), 0px 2px 4px -1px rgba(0, 0, 0, 0.12)', background: '#FFF' }}>
              <div style={{ color: '#4B4B4B', fontSize: 16 }}>退信数</div>
              <div style={{ display: 'flex', marginTop: 10, alignItems: 'baseline' }}>
                <div style={{ color: '#000', fontSize: 24, fontWeight: 600 }}>{this.state.detailData.callbackNum}</div>
                <div style={{ color: 'rgba(0,0,0,0.6)', fontSize: 14, fontWeight: 600, marginLeft: 8, }}>({(this.state.detailData.callbackNum/this.state.detailData.groupTask.total * 100).toFixed(2)}%)</div>
              </div>
            </div>
          </div>

          { this.state.detailData.groupTask.params.reqDto.testAB == 'yes' ? <div>
            <div style={{color:'#000', fontWeight:600,fontSize:16, marginTop: 39, marginBottom: 19}}>A/B测结果</div>
            <Table
              size="middle"
              tableLayout="fixed"
              pagination={false} columns={columns4}
              dataSource={[{
                version: '版本A', 
                content: this.state.detailData.groupTask.params.reqDto.content, 
                total: this.state.detailData.totalA, 
                success: this.state.detailData.successNumA,
                failed: this.state.detailData.failNumA,
                openNum: this.state.detailData.openNumA,
                replyNum: this.state.detailData.replyNumA,
                clickNum: this.state.detailData.clickNumA,
              },{
                version: '版本B', 
                content: this.state.detailData.groupTask.params.reqDto.contentB, 
                total: this.state.detailData.totalB, 
                success: this.state.detailData.successNumB,
                failed: this.state.detailData.failNumB,
                openNum: this.state.detailData.openNumB,
                replyNum: this.state.detailData.replyNumB,
                clickNum: this.state.detailData.clickNumB,
              }]}
              loading={this.state.loading}
            />
          </div> : '' }

          <div style={{color:'#000', fontWeight:600,fontSize:16, marginTop: 39, marginBottom: 19}}>发送明细</div>

          <div className="tableContent accountTableContent" style={{ marginTop: 20, height: 'auto' }}>
            <Table
              size="middle"
              tableLayout="fixed"
              pagination={this.state.pagination3} columns={columns3}
              dataSource={this.state.data3}
              loading={this.state.loading}
              onChange={this.handleTableChange3.bind(this)}
            />
          </div>

          <Pagination
            showJumper
            total={this.state.pagination3.total}
            current={this.state.pagination3.current}
            pageSize={this.state.pagination3.pageSize}
            onChange={this.handleTableChange3.bind(this)}
          />
        </div> : '' }
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
            <div className={"search-query-btn"} style={{alignItems:'center',display: 'flex',gap: '6px', position: 'relative'}} onMouseEnter={() => {
              if (this.timer2) {
                clearTimeout(this.timer2)
                this.timer2 = null
              }
              this.setState({
                showAdd: true
              })
            }} onMouseLeave={() => {
              if (this.timer2) {
                clearTimeout(this.timer2)
                this.timer2 = null
              }
              this.timer2 = setTimeout(() => {
                this.setState({
                  showAdd: false
                })
              }, 500)
            }} onClick={() => {
              // this.props.history.push('/cloud/user/batchSendEmailAdd')
            }}>
              <span>新增</span>
              <ChevronDownIcon style={{fontSize: 16}} />
              { this.state.showAdd ? <div style={{paddingTop: 10, color:'rgba(0, 0, 0, 0.90)', textAlign: 'center', lineHeight: '36px', zIndex: 999, top: 34, left: 0, position:'absolute', width: 92, height: 92, background: '#fff', borderRadius: 6, border: '0.5px solid #DCDCDC', boxShadow: '0 3px 14px 2px rgba(0, 0, 0, 0.05), 0 8px 10px 1px rgba(0, 0, 0, 0.06), 0 5px 5px -3px rgba(0, 0, 0, 0.10)'}}>

                <div onClick={() => {
                  this.props.history.push('/cloud/user/batchSendEmailAdd?type=direct')
                }}>系统渠道</div>
                <div onClick={() => {
                  this.props.history.push('/cloud/user/batchSendEmailAdd?type=api')
                }}>API渠道</div>
              </div> : '' }
            </div>

            <div className={this.state.selectedRowKeys.length > 0 ? "search-query-btn" : "search-reset-btn"} onClick={() => {
              if (this.state.selectedRowKeys.length <= 0) {
                message.error('请选择任务')
                return
              }
              this.loadTestData(this.state.pagination2, this.filters2, this.sorter2)
              this.setState({
                recordVisible: true
              })
            }}>任务检测
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
