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
import { Pagination, Breadcrumb, Dialog, Tag, Input as TInput, Upload as TUpload, Tooltip as TTooltip, Switch, DateRangePicker, Alert,Checkbox, Textarea, Select as TSelect } from 'tdesign-react';
import {ChevronDownIcon} from 'tdesign-icons-react'
import axios from 'axios'
import { formatDate } from 'components/DateFormat'
import { download } from "components/postDownloadUtils"
import { connect } from 'dva'
import { injectIntl } from 'react-intl'
import DialogApi from '../dialog/DialogApi';
import uuid from 'uuid/v4'
import ReactQuill from 'react-quill';

const { Option } = Select;
const Search = Input.Search;
const confirm = Modal.confirm;
const { Option : TOption } = TSelect;

// 针对当前页面的基础url
const baseUrl = '/api/consumer/platform';
const params = []

const modules = {
  toolbar: [
    // 字体样式
    [{ 'header': '1' }, { 'header': '2' }, { 'font': [] }],

    // 字体颜色和背景颜色
    [{ 'color': [] }, { 'background': [] }],

    // 对齐方式
    [{ 'align': [] }],

    // 字体加粗、斜体、下划线、删除线
    ['bold', 'italic', 'underline', 'strike'],

    // 列表样式
    [{ 'list': 'ordered' }, { 'list': 'bullet' }],

    // 引用、代码块
    ['blockquote', 'code-block'],

    // 链接插入
    ['link'],

    // 图片插入
    ['image'],

    // 清除格式按钮
    ['clean'],
  ],
};

const getUploadImageProps = (regExpStr = 'txt|csv') => {
  let regExp = new RegExp(`^.+\\.(${regExpStr})$`)
  const uploadProps = {
    name: 'file',
    multiple: false,
    action: `/api/consumer/res/uploadTxt/linkCheck`,
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
      selectedRowKeys2: [],

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
      genNum: 3,
      content: '',
      subject: '',
      aiModel: 'Chatgpt',
      sendNum: 10,
      progressTaskId: '',
      source: "0",
      emotion: [],
      target: '默认',
      character: '默认',
      style: '默认',
      other: '默认',
      addGroupId: '',
      addTemplateId: '',
      contentInfo: '',
      prompt: '请帮我写一封......主题的邮件，包含......内容，不要出现......内容，必须出现......内容；\n收件人是........群体，字数在xxx-xxx内，署名是......，分点/分段/......阐述，语言为......。',
      allGroups: [],
      allUserGroups: [],
      allTemplates: [],
      allUserTemplates: []
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
    this.getAllTemplate();
    this.getAllGroup()
  }

  async getAllTemplate() {
    let res = await axios.post(`/api/mailTemplate/10000/1`, { filters: {type: 0, status: 0}, sorter: { createTime: -1 } });
    if (res.data.code === 1) {
      this.setState({
        allTemplates: res.data.data.data,
      })
    }
    res = await axios.post(`/api/mailTemplate/10000/1`, { filters: {type: 1, status: 0}, sorter: { createTime: -1 } });
    if (res.data.code === 1) {
      this.setState({
        allUserTemplates: res.data.data.data,
      })
    }
  }

  async getAllGroup() {
    let res = await axios.post(`/api/mailTemplate/group/10000/1`, { filters: {type: 0}, sorter: { createTime: -1 } });
    if (res.data.code === 1) {
      this.setState({
        allGroups: res.data.data.data,
      })
    }
    res = await axios.post(`/api/mailTemplate/group/10000/1`, { filters: {type: 1}, sorter: { createTime: -1 } });
    if (res.data.code === 1) {
      this.setState({
        allUserGroups: res.data.data.data,
      })
    }
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
    let res = await axios.post(`/api/contentAIGen/${pagination.pageSize}/${pagination.current}`, { filters: { ...filters, userID: this.props.userID }, sorter });
    pagination.total = res.data.data.total;
    this.setState({ loading: false, data: res.data.data.data, pagination, selectedRowKeys: [] });
    this.selectedRows = [];
    this.filters = filters;
    this.sorter = sorter
  }

  async load2(pagination, filters, sorter) {
    // 这里的参数是格式好的，无需修改，无阻抗失衡，直接发给服务器
    this.setState({ loading: true });
    let res = await axios.post(`/api/contentAIGen/detail/${this.state.progressTaskId}/${pagination.pageSize}/${pagination.current}`, sorter);
    pagination.total = res.data.data.total;
    this.setState({ loading: false, data2: res.data.data.data, pagination2: pagination });
  }

  async onRowSelectionChange(selectedRowKeys, selectedRows) {
    // 选中状态的数据，因为无需受控，就不记录在state里了，提高效率
    this.setState({ selectedRowKeys });
    this.selectedRows = selectedRows
  }

  async onRowSelectionChange2(selectedRowKeys, selectedRows) {
    // 选中状态的数据，因为无需受控，就不记录在state里了，提高效率
    this.setState({ selectedRowKeys2: selectedRowKeys });
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

  async handleTableChange2(pagination, filters, sorter) {
    // 此处是table控件的回调，其中的参数格式不是很合理，调整一下后，传递给load
    let sort = this.sorter2;
    if (sorter && sorter.field) {
      sort = {};
      sort[sorter.field] = sorter.order == 'descend'
        ? -1
        : 1
    }
    // 暂时不用Table的filter，不太好用
    this.load2(pagination, {}, sort)
  }

  refSelectedCount = (ref) => {
    this.state.selectedCountRef = ref
    this.setState({ selectedCountRef: ref })
    this.handleResize()
  }

  async copyTxt(txt, msg) {
    if (txt) {
      txt = txt.replaceAll("<br>", "\n")
      let parser = new DOMParser();
      let doc = parser.parseFromString(txt, 'text/html');

      await navigator.clipboard.writeText(doc.body.textContent || txt);
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
        let result = await axios.post(`/api/contentAIGen/delete`, { ids: keys });
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


  downloadTxt(filename, content) {
    const blob = new Blob([content], { type: 'text/plain;charset=utf-8' });
    const url = URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = filename;
    a.click();
    URL.revokeObjectURL(url);
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
                taskDesc: record.params.reqDto.desc,
                subject: record.params.reqDto.subject,
                content: record.params.reqDto.content,
                contentInfo: record.params.reqDto.contentInfo,
                genNum: record.params.reqDto.genNum,
                aiModel: record.params.reqDto.aiModel,
                sendNum: record.params.reqDto.sendNum,
                source: record.params.reqDto.source,
                emotion: record.params.reqDto.emotion,
                target: record.params.reqDto.target,
                character: record.params.reqDto.character,
                style: record.params.reqDto.style,
                other: record.params.reqDto.other,
                addGroupId: record.params.reqDto.groupId,
                addTemplateId: record.params.reqDto.templateId,
                prompt: record.params.reqDto.content
              })
              setTimeout(() => {
                this.setState({
                  content: record.params.reqDto.content,
                })
              }, 100)
            }}>查看</Button>
            <Button type="link" onClick={async () => {
              this.setState({
                progressVisible: true,
                progressTaskId: record._id,
                pagination2: {
                  total: 0,
                  pageSize: 10,
                  current: 1,
                  showTotal: (total, range) => `共 ${total} 条`,
                  showSizeChanger: true,
                  pageSizeOptions: ['10', '20', '30', '40', '50', '100', '200', '300', '400', '500']
                }
              })
              this.state.pagination2 = {
                total: 0,
                pageSize: 10,
                current: 1,
                showTotal: (total, range) => `共 ${total} 条`,
                showSizeChanger: true,
                pageSizeOptions: ['10', '20', '30', '40', '50', '100', '200', '300', '400', '500']
              }
              this.state.progressTaskId = record._id
              this.sorter2 = {}
              this.load2(this.state.pagination2, {}, this.sorter2)
            }}>进度</Button>

            {record.status !== 'success' ? <Button type="link" onClick={async () => {
              DialogApi.warning({
                title: '确认结束任务吗？',
                onOk: async () => {
                  let res = await axios.post(`/api/linkCheck/stop`, { ids: [record._id] })
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

            {record.status !== 'success' ? '':
              <Button type="link" onClick={async () => {
                DialogApi.warning({
                  title: '确认重新生成吗？',
                  onOk: async () => {
                    let form = {
                      ...record.params.reqDto,
                      taskDesc: record.params.reqDto.taskDesc + '（重新生成）',
                    };
                    this.setState({ loading: true });
                    // 过滤掉null
                    let res = await axios.post(`/api/contentAIGen/save`, form)
                    if (res.data.code == 1) {
                      message.success('操作成功')
                      this.setState({ addVisible: false })
                      this.reload()
                    } else {
                      message.error(res.data.message)
                      this.setState({ loading: false })
                    }
                  },
                  onCancel: () => {
                  }
                })
              }}>重新生成</Button> }
          </div>)
        }
      }
    ];

    const columns2 = [
      {
        title: '序号',
        dataIndex: 'no',
        key: 'no',
        width: 150,
        ellipsis: true,
      },{
        title: '文案',
        dataIndex: 'content',
        key: 'content',
        width: 150,
        ellipsis: true,
        render: (v) => {
          return <Button type="link" onClick={() => {
            DialogApi.info({
              width: 1000,
              title: '内容',
              content: <div>
                <div className={"search-query-btn"} onClick={() => {
                  this.copyTxt(v)
                }}>复制</div>
                <div style={{maxHeight: 500, overflow: 'auto'}}><div dangerouslySetInnerHTML={{ __html: v || '' }}></div></div></div>,
              onCancel: () => {}
            })
          }}>查看</Button>
        }
      },{
        title: '垃圾率',
        dataIndex: 'rate',
        key: 'rate',
        width: 80,
        ellipsis: true,
        sorter: true,
        render: (v) => {
          return (v * 100) + '%'
        }
      }, {
        title: '发送数量',
        dataIndex: 'sendNum',
        key: 'sendNum',
        width: 100,
        ellipsis: true,
        render: (v, r) => {
          return (r.junkNum + r.normalNum) || '-'
        }
      },{
        title: '状态',
        dataIndex: 'status',
        key: 'status',
        width: 80,
        ellipsis: true,
        render: (v) => {
          if (v) {
            return '进行中'
          } else {
            return '完成'
          }
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
      }
    ];

    return (<div>
      <Breadcrumb>
        <Breadcrumb.BreadcrumbItem>个人中心</Breadcrumb.BreadcrumbItem>
        <Breadcrumb.BreadcrumbItem>文案生成</Breadcrumb.BreadcrumbItem>
      </Breadcrumb>

      <Dialog
        header={this.state.editUser ? "查看" : "创建任务"}
        width={1200}
        height={566}
        placement='center'
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
          if (!this.state.subject) {
            message.error('请输入标题')
            return
          }
          if (!this.state.content && !this.state.prompt) {
            message.error('请输入内容')
            return
          }
          if (!this.state.genNum || isNaN(this.state.genNum)) {
            message.error('请输入生成数量')
            return
          }
          if (!this.state.aiModel) {
            message.error('请选择AI模型')
            return
          }
          if (!this.state.sendNum || isNaN(this.state.sendNum)) {
            message.error('请输入单链接测试次数')
            return
          }
          let form = {
            desc: this.state.taskDesc,
            sendNum: this.state.sendNum,
            subject: this.state.subject,
            content: this.state.source === "3" ? this.state.prompt : this.state.content,
            contentInfo: this.state.contentInfo,
            genNum: this.state.genNum,
            aiModel: this.state.aiModel,
            source: this.state.source,
            emotion: this.state.emotion,
            target: this.state.target,
            character: this.state.character,
            style: this.state.style,
            other: this.state.other,
            groupId: this.state.addGroupId,
            templateId: this.state.addTemplateId,
          };
          this.setState({ loading: true });
          // 过滤掉null
          let res = await axios.post(`/api/contentAIGen/save`, form)
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
                                    <span style={{ color: '#D54941' }}>*</span>}基础文案来源
                                </div>
                                <div>
                                <Radio.Group value={this.state.source} disabled={!!this.state.editUser} onChange={(e) => {
                                                this.setState({ source: e.target.value, addGroupId: '', addTemplateId: '' })
                                              }}>
                                                <Radio value={"0"}>已有完整文案，降低风控</Radio>
                                                <Radio value={"1"}>没有完整文案，套用系统模板库文案</Radio>
                                                <Radio value={"2"}>没有完整文案，套用个人模板库文案</Radio>
                                                <Radio value={"3"}>没有完整文案，AI直接生成</Radio>
                                              </Radio.Group>
                                </div>
                              </div>
          <div style={{ display: 'flex', marginBottom: 24 }}>
                      <div
                        style={{ width: 120, textAlign: 'right', marginRight: 16, lineHeight: '30px' }}>情感基调
                      </div>
                      <div>
                      <TSelect style={{width: 130}} value={this.state.emotion} disabled={!!this.state.editUser} multiple={true} max={3} onChange={v => {
                                    this.setState({ emotion: v })
                                  }}>
                                    <TOption value="默认">默认</TOption>
                                    <TOption value="友好">友好</TOption>
                                    <TOption value="亲切">亲切</TOption>
                                    <TOption value="温暖">温暖</TOption>
                                    <TOption value="幽默">幽默</TOption>
                                    <TOption value="轻松">轻松</TOption>
                                    <TOption value="热情">热情</TOption>
                                    <TOption value="激励">激励</TOption>
                                    <TOption value="惊喜">惊喜</TOption>
                                    <TOption value="信任感">信任感</TOption>
                                    <TOption value="紧迫感">紧迫感</TOption>
                                    <TOption value="专业">专业</TOption>
                                    <TOption value="简洁">简洁</TOption>
                                  </TSelect>
                      </div>
                    </div>

          <div style={{ display: 'flex', marginBottom: 24 }}>
                      <div
                        style={{ width: 120, textAlign: 'right', marginRight: 16, lineHeight: '30px' }}>功能目标
                      </div>
                      <div>
                      <Select style={{ width: 130}} value={this.state.target} disabled={!!this.state.editUser} onChange={v => {
                                    this.setState({ target: v })
                                  }}>
                                    <Option value="默认">默认</Option>
                                    <Option value="引流">引流</Option>
                                    <Option value="促单">促单</Option>
                                    <Option value="转化">转化</Option>
                                    <Option value="教育">教育</Option>
                                    <Option value="邀请">邀请</Option>
                                    <Option value="感谢">感谢</Option>
                                    <Option value="反馈">反馈</Option>
                                    <Option value="确认">确认</Option>
                                    <Option value="通知">通知</Option>
                                    <Option value="推广">推广</Option>
                                    <Option value="引导">引导</Option>
                                    <Option value="挽留">挽留</Option>
                                    <Option value="祝贺">祝贺</Option>
                                  </Select>
                      </div>
                    </div>
          <div style={{ display: 'flex', marginBottom: 24 }}>
                      <div
                        style={{ width: 120, textAlign: 'right', marginRight: 16, lineHeight: '30px' }}>发件角色
                      </div>
                      <div>
                      <Select style={{ width: 130}} value={this.state.character} disabled={!!this.state.editUser} onChange={v => {
                                    this.setState({ character: v })
                                  }}>
                                    <Option value="默认">默认</Option>
                                    <Option value="导师">导师</Option>
                                    <Option value="专家">专家</Option>
                                    <Option value="朋友">朋友</Option>
                                    <Option value="客服">客服</Option>
                                    <Option value="经理">经理</Option>
                                    <Option value="创始人">创始人</Option>
                                    <Option value="销售">销售</Option>
                                    <Option value="平台">平台</Option>
                                    <Option value="指导者">指导者</Option>
                                    <Option value="支持者">支持者</Option>
                                    <Option value="陪伴者">陪伴者</Option>
                                  </Select>
                      </div>
                    </div>
          <div style={{ display: 'flex', marginBottom: 24 }}>
                      <div
                        style={{ width: 120, textAlign: 'right', marginRight: 16, lineHeight: '30px' }}>语言风格
                      </div>
                      <div>
                      <Select style={{ width: 130}} value={this.state.style} disabled={!!this.state.editUser} onChange={v => {
                                    this.setState({ style: v })
                                  }}>
                                    <Option value="默认">默认</Option>
                                    <Option value="口语化">口语化</Option>
                                    <Option value="书面语">书面语</Option>
                                    <Option value="对话体（Q&A）">对话体（Q&A）</Option>
                                    <Option value="报告式">报告式</Option>
                                    <Option value="清单体">清单体</Option>
                                    <Option value="文章">文章</Option>
                                  </Select>
                      </div>
                    </div>
          <div style={{ display: 'flex', marginBottom: 24 }}>
                      <div
                        style={{ width: 120, textAlign: 'right', marginRight: 16, lineHeight: '30px' }}>其他
                      </div>
                      <div>
                      <Select style={{ width: 130}} value={this.state.other} disabled={!!this.state.editUser} onChange={v => {
                                    this.setState({ other: v })
                                  }}>
                                    <Option value="默认">默认</Option>
                                    <Option value="个性化">个性化</Option>
                                    <Option value="情绪化">情绪化</Option>
                                    <Option value="数据驱动">数据驱动</Option>
                                    <Option value="故事化">故事化</Option>
                                    <Option value="行动导向">行动导向</Option>
                                    <Option value="结果导向">结果导向</Option>
                                    <Option value="用户中心">用户中心</Option>
                                    <Option value="问题解决">问题解决</Option>
                                    <Option value="正向鼓励">正向鼓励</Option>
                                    <Option value="温和建议">温和建议</Option>
                                  </Select>
                      </div>
                    </div>
          <div style={{ display: this.state.source === "1" || this.state.source === "2" ? 'flex': 'none', marginBottom: 24 }}>
                      <div
                        style={{ width: 120, textAlign: 'right', marginRight: 16, lineHeight: '30px' }}>{this.state.editUser ? '' :
                          <span style={{ color: '#D54941' }}>*</span>}模板分组
                      </div>
                      <div>
                      <Select value={this.state.addGroupId} disabled={!!this.state.editUser} onChange={v => {
                                    this.setState({ addGroupId: v, addTemplateId: '' })
                                  }}>
                                  {this.state.source === "1" ? this.state.allGroups.map(r => <Option key={r._id} value={r._id}>{r.groupName}</Option>) :
                                  this.state.allUserGroups.map(r => <Option key={r._id} value={r._id}>{r.groupName}</Option>)}
                                  </Select>
                      </div>
                    </div>
          <div style={{ display: this.state.source === "1" || this.state.source === "2" ? 'flex': 'none', marginBottom: 24 }}>
                      <div
                        style={{ width: 120, textAlign: 'right', marginRight: 16, lineHeight: '30px' }}>{this.state.editUser ? '' :
                          <span style={{ color: '#D54941' }}>*</span>}具体模板
                      </div>
                      <div>
                      <Select value={this.state.addTemplateId} disabled={!!this.state.editUser} onChange={v => {
                                    let template
                                    if (this.state.source === "1") {
                                      let templates = this.state.allTemplates.filter(r => r._id === v)
                                      if (templates) template = templates[0]
                                    }
                                    if (this.state.source === "2") {
                                      let templates = this.state.allUserTemplates.filter(r => r._id === v)
                                      if (templates) template = templates[0]
                                    }
                                    this.setState({ addTemplateId: v, subject: template?.title, content: template?.content })
                                  }}>
                                  {this.state.source === "1" ? this.state.allTemplates.filter(r => r.groupID === this.state.addGroupId).map(r => <Option key={r._id} value={r._id}>{r.name}</Option>) :
                                  this.state.allUserTemplates.filter(r => r.groupID === this.state.addGroupId).map(r => <Option key={r._id} value={r._id}>{r.name}</Option>)}
                                  </Select>
                      </div>
                    </div>
          <div style={{ display: 'flex', marginBottom: 24 }}>
            <div style={{ width: 120, textAlign: 'right', marginRight: 16, lineHeight: '30px' }}>{this.state.editUser ? '' :<span style={{ color: '#D54941' }}>*</span>}标题</div>
            <div style={{ width: 400 }}>
              <TInput
                disabled={!!this.state.editUser}
                placeholder="请输入标题"
                clearable
                value={this.state.subject}
                onChange={(value) => {
                  this.setState({ subject: value })
                }}
              />
            </div>
          </div>

          <div style={{ display: this.state.source === "0" || this.state.source === "1" || this.state.source === "2" ? 'flex': 'none', marginBottom: 80 }}>
            <div style={{ width: 120, textAlign: 'right', marginRight: 16, lineHeight: '30px' }}>{this.state.editUser ? '' : <span style={{ color: '#D54941' }}>*</span> }内容</div>
            <div>
              <div style={{ width: 800 }}>
                <ReactQuill
                  value={this.state.content}
                  modules={modules}
                  readOnly={!!this.state.editUser}
                  onChange={(val) => this.setState({ content: val })}
                  theme="snow"
                  placeholder="请输入内容"
                  style={{ height: 300 }}
                />
              </div>
            </div>
          </div>
          <div style={{ display: this.state.source === "1" || this.state.source === "2" ? 'flex': 'none', marginBottom: 80 }}>
            <div style={{ width: 120, textAlign: 'right', marginRight: 16, lineHeight: '30px' }}>信息</div>
            <div>
              <Textarea
                            disabled={!!this.state.editUser}
                            placeholder="若不在模板中修改，可在此处输入相关信息，如署名，待遇，联系方式等\n若联系方式为链接形式，请在创建完文案后点击【链接优化】，以降低链接对邮件垃圾率的影响\n若此栏有信息，则使用此栏信息套用模板"
                            value={this.state.contentInfo}
                            onChange={(value) => {
                              this.setState({ contentInfo: value })
                            }}
                          />
            </div>
          </div>
          <div style={{ display: this.state.source === "3" ? 'flex': 'none', marginBottom: 24 }}>
            <div style={{ width: 120, textAlign: 'right', marginRight: 16, lineHeight: '30px' }}>{this.state.editUser ? '' :<span style={{ color: '#D54941' }}>*</span>}提示词及邮件主题</div>
            <div style={{ width: 400 }}>
              <Textarea
                disabled={!!this.state.editUser}
                rows={5}
//                placeholder="直接输入提示词及邮件主题，点击生成邮件正文，示例：\n请帮我写一封......主题的邮件，包含......内容，不要出现......内容，必须出现......内容；\n收件人是........群体，字数在xxx-xxx内，署名是......，分点/分段/......阐述，语言为.....。\n*若联系方式为链接形式,请在创建完文案后点击【链接优化】,以降低链接对邮件垃圾率的影响。"
                value={this.state.prompt}
                onChange={(value) => {
                  this.setState({ prompt: value })
                }}
              />
            </div>
          </div>
          <div style={{ display: 'flex', marginBottom: 24 }}>
            <div
              style={{ width: 120, textAlign: 'right', marginRight: 16, lineHeight: '30px' }}>{this.state.editUser ? '' :
                <span style={{ color: '#D54941' }}>*</span>}生成文案数量
            </div>
            <div>
              <Input value={this.state.genNum} disabled={!!this.state.editUser}
                onChange={(e) => this.setState({ genNum: e.target.value })} style={{ width: 400 }}
                placeholder='请输入' />
            </div>
          </div>

          <div style={{ display: 'flex', marginBottom: 24 }}>
            <div
              style={{ width: 120, textAlign: 'right', marginRight: 16, lineHeight: '30px' }}>{this.state.editUser ? '' :
                <span style={{ color: '#D54941' }}>*</span>}AI模型
            </div>
            <div>
              <Select disabled={!!this.state.editUser} value={this.state.aiModel} style={{ width: 400 }} onChange={(e) => {
                this.setState({ aiModel: e });
              }}>
                {
                  [{ id: 'Chatgpt', name: 'Chatgpt' }, { id: 'GoogleStudio', name: 'Google' }].map(ws => {
                    return <Option value={ws.id}>{`${ws.name}`}</Option>
                  })
                }
              </Select>
            </div>
          </div>

          <div style={{ display: 'flex', marginBottom: 24 }}>
            <div
              style={{ width: 120, textAlign: 'right', marginRight: 16, lineHeight: '30px' }}>{this.state.editUser ? '' :
                <span style={{ color: '#D54941' }}>*</span>}单文案测试次数
            </div>
            <div>
              <Input value={this.state.sendNum} disabled={!!this.state.editUser}
                onChange={(e) => this.setState({ sendNum: e.target.value })} style={{ width: 400 }}
                placeholder='请输入' />
            </div>
          </div>
        </div>
      </Dialog>


      <Dialog
        header={"进度"}
        width={1200}
        visible={this.state.progressVisible}
        placement='center'
        confirmBtn={null}
        onCancel={() => {
          this.setState({ progressVisible: false, })
        }}
        onClose={() => {
          this.setState({ progressVisible: false, })
        }}
      >
        <div style={{ marginTop: 26, color: 'rgba(0, 0, 0, 0.90)' }}>
          <div className="search-query-btn" onClick={() => {
            let lines = this.state.data2.filter(e => this.state.selectedRowKeys2.includes(e.no)).map(e => e.content)
            this.downloadTxt('文案.txt', lines.join("\n==NEWLINE=="))
          }}>批量导出文案</div>
          <div className="tableContent">
            <div>
              <Table
                tableLayout="fixed"
                scroll={{
                  x: columns2.filter(e => e.width).map(e => e.width).reduce((a, b) => a + b)
                }}
                rowSelection={{
                  selectedRowKeys: this.state.selectedRowKeys2,
                  onChange: this.onRowSelectionChange2.bind(this)
                }}
                pagination={this.state.pagination2} columns={columns2} rowKey='no' dataSource={this.state.data2} loading={this.state.loading}
                onChange={this.handleTableChange2.bind(this)} />
            </div>
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
                source: "0",
                emotion: [],
                target: '默认',
                character: '默认',
                style: '默认',
                other: '默认',
                subject: '',
                content: '',
                contentInfo: '',
                prompt: '请帮我写一封......主题的邮件，包含......内容，不要出现......内容，必须出现......内容；\n收件人是........群体，字数在xxx-xxx内，署名是......，分点/分段/......阐述，语言为......。',
                genNum: 3,
                aiModel: 'Chatgpt',
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
