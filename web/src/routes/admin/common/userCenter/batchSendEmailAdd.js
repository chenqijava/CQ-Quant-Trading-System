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
  Radio,
  DatePicker
} from 'antd'
import { Pagination, Breadcrumb, Dialog, Tag, Input as TInput, Upload as TUpload, Tooltip as TTooltip, Switch, DateRangePicker, Steps, Checkbox, Textarea, Slider } from 'tdesign-react';
import axios from 'axios'
import { formatDate } from 'components/DateFormat'
import { download } from "components/postDownloadUtils"
import { connect } from 'dva'
import { injectIntl } from 'react-intl'
import ReactQuill from 'react-quill';
import DialogApi from '../dialog/DialogApi';
import { version } from 'nprogress';
const { StepItem } = Steps;
import uuid from 'uuid/v4'
import moment from 'moment';
import { FileCopyIcon } from 'tdesign-icons-react';

const { Option } = Select;

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
    action: `/api/consumer/res/uploadTxt/batchSendEmail`,
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
      loading: false,
      currentStep: 0,

      taskName: '',
      emailAddType: 'page',
      title: '',
      content: '',
      useAiOptimize: 'enable',
      addMethod: '1',
      filepath: '',
      addData: '',
      taskId: '',
      action: 'open',
      estimateSendNum: 0,
      sendMethod: '1',
      sendTime: new Date(),
      loopSend: 'no',
      loopType: '',
      monitorOpen: 'no',
      monitorClick: 'no',
      addUnsubscribe: 'no',
      testAB: 'no',
      titleB: '',
      contentB: '',
      percent: 20,
      testTimeLengthHour: 4,
      factor: 'reply',
      testNum: 10,
      testEmailTaskId: '',
      files: [],
      taskList: [],

      testData: [],

      maxCurrentStep: 0,
      type: 'direct',

      contentParams: '',
      contentParamsFiles: [],
      titleParams: '',
      titleParamsFiles: [],
      contactParams: '',
      contactParamsFiles: [],

      systemEmailCount: 10,
      otherEmails: '',
      oneAccountSendLimit: 20,
      failRatePauseTask: 20,
      availableAccountPauseTask: 20,
      spamRatePauseTask: 20,

      closeFailRatePauseTask: 'disable',
      closeAvailableAccountPauseTask: 'disable',
      closeSpamRatePauseTask: 'disable',
    };

    this.timer = null;
    this.state.type = this.query.type
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
    }
    // 提前写在constructor则Table首次加载时生效
    // 但是因为没有放到state中，所以除sorter外，都不建议提前设置，保持不受控状态是最好的
  }

  // 首次加载数据
  async componentWillMount() {
    if (this.query.id) {
      let data = await axios.post(`/api/batchSendEmail/detailTest/${this.query.id}`, {})
      if (data.data.code === 1) {
        if (data.data.data.params.reqDto.addMethod === '3') {
          this.loadTaskList()
        }
        if (data.data.data.params.reqDto.testEmailTaskId) {
          this.state.testEmailTaskId = data.data.data.params.reqDto.testEmailTaskId
          this.loadTestData(true)
        }
        this.setState({
          maxCurrentStep: 5,
          ...data.data.data.params.reqDto,
          files: data.data.data.params.reqDto.filepath ? [{
            uid: uuid() + '',
            name: 'addData.txt',
            status: 'done',
            response: '{"status": "success"}',
            url: '/api/consumer/res/download/' + data.data.data.params.reqDto.filepath
          }] : [],
          contentParamsFiles: data.data.data.params.reqDto.contentParamsFilepath ? [{
            uid: uuid() + '',
            name: 'contentParams.txt',
            status: 'done',
            response: '{"status": "success"}',
            url: '/api/consumer/res/download/' + data.data.data.params.reqDto.contentParamsFilepath
          }] : [],
          contactParamsFiles: data.data.data.params.reqDto.contactParamsFilepath ? [{
            uid: uuid() + '',
            name: 'contactParams.txt',
            status: 'done',
            response: '{"status": "success"}',
            url: '/api/consumer/res/download/' + data.data.data.params.reqDto.contactParamsFilepath
          }] : [],
          titleParamsFiles: data.data.data.params.reqDto.titleParamsFilepath ? [{
            uid: uuid() + '',
            name: 'titleParams.txt',
            status: 'done',
            response: '{"status": "success"}',
            url: '/api/consumer/res/download/' + data.data.data.params.reqDto.titleParamsFilepath
          }] : []
        })
      }
    } else {
        const savedData = localStorage.getItem("batchSendEmailAddStep1Temp");
        if (savedData) {
              try {
                // 解析JSON（需处理解析失败的情况）
                const parsedData = JSON.parse(savedData);
                this.setState({
                          taskName: parsedData.taskName,
                          title: parsedData.title,
                          content: parsedData.content,
                          titleParams: parsedData.titleParams,
                          contactParams: parsedData.contactParams,
                          contentParams: parsedData.contentParams,
                          titleParamsFiles: parsedData.titleParamsFiles,
                          contactParamsFiles: parsedData.contactParamsFiles,
                          contentParamsFiles: parsedData.contentParamsFiles,
                });
                console.log('已恢复临时数据');
              } catch (error) {
                console.error('临时数据解析失败，清除无效数据', error);
                localStorage.removeItem("batchSendEmailAddStep1Temp");
              }
         }
    }
  }

  async componentWillUnmount() {
    if (this.timer) {
      clearInterval(this.timer)
      this.timer = null
    }
  }

  async loadTaskList() {
    let data = await axios.post(`/api/batchSendEmail/secondSend`, {})
    this.setState({
      taskList: data.data.data
    })
  }

  async onDateTimeChange(value, dateString) {
    console.log('Selected Time: ', value);
    console.log('Formatted Selected Time: ', dateString);
    this.setState({ sendTime: value });
  }

  async sendTestEmail() {
    if (this.state.contentB && this.state.contentB.length > 5 * 1024 * 1024) {
      message.error('测试邮件内容不能超过5MB')
      return
    }
    if (this.state.content && this.state.content.length > 5 * 1024 * 1024) {
      message.error('邮件内容不能超过5MB')
      return
    }
    if (this.state.loading) {
      return
    }
    this.state.loading = true
    this.setState({ loading: true })
    try {
      let data = await axios.post(`/api/batchSendEmail/test`, {
        taskName: this.state.taskName,
        emailAddType: this.state.emailAddType,
        title: this.state.title,
        content: this.state.content,
        useAiOptimize: this.state.useAiOptimize,
        addMethod: this.state.addMethod,
        filepath: this.state.files.length > 0 ? this.state.files[0].response.data.filepath : '',
        titleParams: this.state.titleParams,
        contentParams: this.state.contentParams,
        contactParams: this.state.contactParams,
        titleParamsFilepath: this.state.titleParamsFiles.length > 0 ? this.state.titleParamsFiles[0].response.data.filepath : '',
        contentParamsFilepath: this.state.contentParamsFiles.length > 0 ? this.state.contentParamsFiles[0].response.data.filepath : '',
        contactParamsFilepath: this.state.contactParamsFiles.length > 0 ? this.state.contactParamsFiles[0].response.data.filepath : '',
        systemEmailCount: this.state.systemEmailCount,
        otherEmails: this.state.otherEmails,
        oneAccountSendLimit: this.state.oneAccountSendLimit,
        failRatePauseTask: this.state.failRatePauseTask,
        availableAccountPauseTask: this.state.availableAccountPauseTask,
        spamRatePauseTask: this.state.spamRatePauseTask,
        closeFailRatePauseTask: this.state.closeFailRatePauseTask,
        closeAvailableAccountPauseTask: this.state.closeAvailableAccountPauseTask,
        closeSpamRatePauseTask: this.state.closeSpamRatePauseTask,
        addData: this.state.addData,
        taskId: this.state.taskId,
        action: this.state.action,
        estimateSendNum: this.state.estimateSendNum,
        sendMethod: this.state.sendMethod,
        sendTime: new Date(this.state.sendTime).toISOString(),
        loopSend: this.state.loopSend,
        loopType: this.state.loopType,
        monitorOpen: this.state.monitorOpen,
        monitorClick: this.state.monitorClick,
        addUnsubscribe: this.state.addUnsubscribe,
        testAB: this.state.testAB,
        titleB: this.state.titleB,
        contentB: this.state.contentB,
        percent: this.state.percent,
        testTimeLengthHour: this.state.testTimeLengthHour,
        factor: this.state.factor,
        count: this.state.testNum,
      })
      if (data.data.data) {
        this.setState({
          testEmailTaskId: data.data.data
        })
        this.loadTestData()
        this.timer = setInterval(() => {
          this.loadTestData()
        }, 1000 * 5)
      } else {
        message.error(data.data.message)
      }
    } finally {
      this.state.loading = false
      this.setState({ loading: false })
    }
  }

  async sendEmail() {
    if (this.state.contentB && this.state.contentB.length > 5 * 1024 * 1024) {
      message.error('测试邮件内容不能超过5MB，可能图片太大')
      return
    }
    if (this.state.content && this.state.content.length > 5 * 1024 * 1024) {
      message.error('邮件内容不能超过5MB，可能图片太大')
      return
    }
    if (this.state.loading) {
      return
    }
    this.state.loading = true
    this.setState({ loading: true })
    try {
      let data = await axios.post(`/api/batchSendEmail/save`, {
        taskName: this.state.taskName,
        emailAddType: this.state.emailAddType,
        title: this.state.title,
        content: this.state.content,
        useAiOptimize: this.state.useAiOptimize,
        addMethod: this.state.addMethod,
        filepath: this.state.files.length > 0 ? this.state.files[0].response.data.filepath : '',
        titleParams: this.state.titleParams,
        contentParams: this.state.contentParams,
        contactParams: this.state.contactParams,
        titleParamsFilepath: this.state.titleParamsFiles.length > 0 ? this.state.titleParamsFiles[0].response.data.filepath : '',
        contentParamsFilepath: this.state.contentParamsFiles.length > 0 ? this.state.contentParamsFiles[0].response.data.filepath : '',
        contactParamsFilepath: this.state.contactParamsFiles.length > 0 ? this.state.contactParamsFiles[0].response.data.filepath : '',
        systemEmailCount: this.state.systemEmailCount,
        otherEmails: this.state.otherEmails,
        oneAccountSendLimit: this.state.oneAccountSendLimit,
        failRatePauseTask: this.state.failRatePauseTask,
        availableAccountPauseTask: this.state.availableAccountPauseTask,
        spamRatePauseTask: this.state.spamRatePauseTask,
        closeFailRatePauseTask: this.state.closeFailRatePauseTask,
        closeAvailableAccountPauseTask: this.state.closeAvailableAccountPauseTask,
        closeSpamRatePauseTask: this.state.closeSpamRatePauseTask,
        addData: this.state.addData,
        taskId: this.state.taskId,
        action: this.state.action,
        estimateSendNum: this.state.estimateSendNum,
        sendMethod: this.state.sendMethod,
        sendTime: new Date(this.state.sendTime).toISOString(),
        loopSend: this.state.loopSend,
        loopType: this.state.loopType,
        monitorOpen: this.state.monitorOpen,
        monitorClick: this.state.monitorClick,
        addUnsubscribe: this.state.addUnsubscribe,
        testAB: this.state.testAB,
        titleB: this.state.titleB,
        contentB: this.state.contentB,
        percent: this.state.percent,
        testTimeLengthHour: this.state.testTimeLengthHour,
        factor: this.state.factor,
        testEmailTaskId: this.state.testEmailTaskId,
        count: this.state.testNum,
        type: this.state.type
      })
      if (data.data.code === 1) {
        message.success('提交成功')
        this.props.history.push('/cloud/user/batchSendEmail')
      } else {
        message.error(data.data.message)
      }
    } finally {
      this.state.loading = false
      this.setState({ loading: false })
    }
  }

  async loadTestData(hideMsg) {
    try {
      let data = await axios.post(`/api/batchSendEmail/detailTest/${this.state.testEmailTaskId}`, {})
      let result = data.data.data.result || {}
      if ((result.successA || 0) + (result.failedA || 0) > 0) {
        if (!hideMsg) {
          message.success('测试完成')
          if (this.timer) {
            clearInterval(this.timer)
            this.timer = null
          }
        }
      }
      this.setState({
        testData: data.data.data.params.reqDto.testAB === 'yes' ? [
          { version: 'A组', total: (result.successA || 0) + (result.failedA || 0), success: result.successA || 0, failed: result.failedA || 0 },
          { version: 'B组', total: (result.successB || 0) + (result.failedB || 0), success: result.successB || 0, failed: result.failedB || 0 }] :
          [{ version: 'A组', total: (result.successA || 0) + (result.failedA || 0), success: result.successA || 0, failed: result.failedA || 0 }]
      })
    } catch (e) {
      message.error(e.message)
    }
  }

  async copyTxt(txt, msg) {
    if (txt) {
      await navigator.clipboard.writeText(txt);
      message.success(msg || '复制成功')
    }
  }

  // 把columns放到render中，虽然损失部分性能，但是能方便参数中的匿名回调获取实例状态
  render() {
    const columns2 = [
      {
        title: '发送内容',
        dataIndex: 'version',
        key: 'version',
        width: 80,
      },
      {
        title: '发送次数',
        dataIndex: 'total',
        key: 'total',
        width: 100
      },
      {
        title: '正常数量',
        dataIndex: 'success',
        key: 'success',
        width: 100
      },
      {
        title: '失败数量',
        dataIndex: 'failed',
        key: 'failed',
        width: 100
      },
    ]


    return (<div>
      <Breadcrumb>
        <Breadcrumb.BreadcrumbItem>个人中心</Breadcrumb.BreadcrumbItem>
        <Breadcrumb.BreadcrumbItem>{this.query.id ? '查看群发' : '新建群发'}</Breadcrumb.BreadcrumbItem>
      </Breadcrumb>

      <div>
        <div style={{ display: 'flex', width: '1100px', height: '32px', margin: '25px 0 44px 131px' }}>

          <Steps
            current={this.state.currentStep}
            layout="horizontal"
            separator="line"
            sequence="positive"
            theme="default"
            onChange={(v) => {
              if (this.query.id || this.state.maxCurrentStep >= v) this.setState({ currentStep: v })
            }}
          >
            <StepItem
              title="编辑邮件内容"
            />
            <StepItem
              title="选择收件人"
            />
            <StepItem
              title="设置发送方式"
            />
            <StepItem
              title="A/B测试"
            />
            <StepItem
              title="邮件测试"
            />
            <StepItem
              title="确认并发送"
            />
          </Steps>
        </div>
      </div>

      {
        this.state.currentStep === 0 ? <div>
          <div style={{ marginLeft: 180, fontSize: 18, fontWeight: 600, color: '#000' }}>步骤1：编辑邮件内容</div>
          <div style={{ display: 'flex', marginLeft: 206, marginTop: 10 }}>
            <div style={{ color: 'rgba(0, 0, 0, 0.90)', width: 75, lineHeight: '32px', textAlign: 'right' }}><span style={{ color: '#D54941' }}>*</span>任务名称</div>
            <div style={{ width: 400, marginLeft: 16 }}>
              <TInput
                disabled={this.query.id}
                placeholder="请输入任务名称"
                clearable
                value={this.state.taskName}
                onChange={(value) => {
                  this.setState({ taskName: value })
                }}
              />
            </div>
          </div>

          <div style={{ display: 'flex', marginLeft: 184, marginTop: 35 }}>
            <div style={{ color: 'rgba(0, 0, 0, 0.90)', width: 100, lineHeight: '24px', textAlign: 'right' }}>邮件录入方式</div>
            <div style={{ width: 400, marginLeft: 16 }}>
              <Radio.Group disabled={this.query.id} value={this.state.emailAddType} onChange={(e) => {
                this.setState({ emailAddType: e.target.value })
              }}>
                <Radio value={"page"}>页面录入</Radio>
                {/* <Radio value={"repo"}>素材库选择</Radio> */}
              </Radio.Group>
            </div>
          </div>

          <div style={{ display: 'flex', marginLeft: 228, marginTop: 30 }}>
            <div style={{ color: 'rgba(0, 0, 0, 0.90)', width: 56, lineHeight: '32px', textAlign: 'right' }}><span style={{ color: '#D54941' }}>*</span>标题</div>
            <div style={{ width: 400, marginLeft: 16 }}>
              <TInput
                disabled={this.query.id}
                placeholder="请输入标题"
                clearable
                value={this.state.title}
                onChange={(value) => {
                  this.setState({ title: value })
                }}
              />
            </div>
          </div>

          <div style={{ display: 'flex', marginLeft: 228, marginTop: 30 }}>
            <div style={{ color: 'rgba(0, 0, 0, 0.90)', width: 56, lineHeight: '32px', textAlign: 'right' }}><span style={{ color: '#D54941' }}>*</span>内容</div>
            <div>
              <div style={{ width: 1060, marginLeft: 16 }}>
                <ReactQuill
                  value={this.state.content}
                  modules={modules}
                  readOnly={this.query.id}
                  onChange={(val) => this.setState({ content: val })}
                  theme="snow"
                  placeholder="请输入内容"
                  style={{ height: 300 }}
                />
              </div>
              <div style={{ color: '#000', marginLeft: 20, marginTop: 50 }}>
                通配符：在需要的地方插入{'{{var1 | 默认值}}'}、{'{{var2 | 默认值}}'}、{'{{var3 | 默认值}}'}，默认值是指如果字段为空 → 自动使用备用值填充。请在收件人处配置。<br />
                标题通配符/接粉通配符/文案通配符：在需要的地方插入{'{{title}}'}、{'{{contact}}'}、{'{{content}}'}，系统将轮询替换文案中的通配符。
              </div>


              <div style={{ color: '#000', marginLeft: 20, marginTop: 28 }}>
                <Checkbox checked={this.state.useAiOptimize == 'enable'} disabled={this.query.id} onChange={(val) => this.setState({ useAiOptimize: val ? 'enable' : 'disable' })}>启用AI智能优化邮件内容</Checkbox>
                <div style={{ color: 'rgba(0, 0, 0, 0.40)', fontSize: 12, }}>系统会微调邮件内容和图片，降低风控</div>
              </div>
            </div>
          </div>


          <div style={{ display: 'flex', marginLeft: 214, marginTop: 30 }}>
            <div style={{ color: 'rgba(0, 0, 0, 0.90)', width: 70, lineHeight: '32px', textAlign: 'right' }}><span style={{ color: '#D54941' }}></span>标题通配符</div>
            <div style={{ width: 400, marginLeft: 16 }}>
              <Textarea
                disabled={this.query.id}
                placeholder="一行为一条数据，使用回车键(Enter)换行。"
                value={this.state.titleParams}
                rows={4}
                onChange={(value) => {
                  this.setState({ titleParams: value })
                }}
              />
              <div>标题通配符：{'{{title}}'} <FileCopyIcon style={{ cursor: 'pointer' }} onClick={() => {
                this.copyTxt('{{title}}')
              }} fillColor='transparent' strokeColor='currentColor' strokeWidth={2} /></div>
            </div>

            <div style={{ color: 'rgba(0, 0, 0, 0.90)', width: 70, lineHeight: '32px', textAlign: 'right' }}><span style={{ color: '#D54941' }}></span>文件上传</div>
            <div style={{ width: 400, marginLeft: 16 }}>
              <TUpload
                disabled={this.query.id}
                {...getUploadImageProps('txt')}
                files={this.state.titleParamsFiles}
                onChange={(info) => {
                  if (info.length > 0) {
                    this.setState({ titleParamsFiles: info });
                    return
                  }
                }}
                onRemove={() => this.setState({ titleParamsFiles: [] })}
              />
              <div style={{ fontSize: 12 }}>请选择txt格式的文件上传</div>
            </div>
          </div>

          <div style={{ display: 'flex', marginLeft: 214, marginTop: 30 }}>
            <div style={{ color: 'rgba(0, 0, 0, 0.90)', width: 70, lineHeight: '32px', textAlign: 'right' }}><span style={{ color: '#D54941' }}></span>接粉通配符</div>
            <div style={{ width: 400, marginLeft: 16 }}>
              <Textarea
                disabled={this.query.id}
                placeholder="一行为一条数据，使用回车键(Enter)换行。"
                value={this.state.contactParams}
                rows={4}
                onChange={(value) => {
                  this.setState({ contactParams: value })
                }}
              />
              <div>接粉通配符：{'{{contact}}'} <FileCopyIcon style={{ cursor: 'pointer' }} onClick={() => {
                this.copyTxt('{{contact}}')
              }} fillColor='transparent' strokeColor='currentColor' strokeWidth={2} /></div>
            </div>

            <div style={{ color: 'rgba(0, 0, 0, 0.90)', width: 70, lineHeight: '32px', textAlign: 'right' }}><span style={{ color: '#D54941' }}></span>文件上传</div>
            <div style={{ width: 400, marginLeft: 16 }}>
              <TUpload
                disabled={this.query.id}
                {...getUploadImageProps('txt')}
                files={this.state.contactParamsFiles}
                onChange={(info) => {
                  if (info.length > 0) {
                    this.setState({ contactParamsFiles: info });
                    return
                  }
                }}
                onRemove={() => this.setState({ contactParamsFiles: [] })}
              />
              <div style={{ fontSize: 12 }}>请选择txt格式的文件上传</div>
            </div>
          </div>

          <div style={{ display: 'flex', marginLeft: 214, marginTop: 30 }}>
            <div style={{ color: 'rgba(0, 0, 0, 0.90)', width: 70, lineHeight: '32px', textAlign: 'right' }}><span style={{ color: '#D54941' }}></span>文案通配符</div>
            <div style={{ width: 400, marginLeft: 16 }}>
              <Textarea
                disabled={this.query.id}
                placeholder="两个文案间用==NEWLINE==间隔"
                value={this.state.contentParams}
                rows={4}
                onChange={(value) => {
                  this.setState({ contentParams: value })
                }}
              />
              <div>文案通配符：{'{{content}}'} <FileCopyIcon style={{ cursor: 'pointer' }} onClick={() => {
                this.copyTxt('{{content}}')
              }} fillColor='transparent' strokeColor='currentColor' strokeWidth={2} /></div>
            </div>

            <div style={{ color: 'rgba(0, 0, 0, 0.90)', width: 70, lineHeight: '32px', textAlign: 'right' }}><span style={{ color: '#D54941' }}></span>文件上传</div>
            <div style={{ width: 400, marginLeft: 16 }}>
              <TUpload
                disabled={this.query.id}
                {...getUploadImageProps('txt')}
                files={this.state.contentParamsFiles}
                onChange={(info) => {
                  if (info.length > 0) {
                    this.setState({ contentParamsFiles: info });
                    return
                  }
                }}
                onRemove={() => this.setState({ contentParamsFiles: [] })}
              />
              <div style={{ fontSize: 12 }}>请选择txt格式的文件上传</div>
            </div>
          </div>

          <div style={{ marginLeft: 308, marginTop: 38 }}>
            <div className="search-reset-btn" onClick={() => this.props.history.goBack()}>返回</div>
            <div className="search-query-btn" onClick={() => {
              if (!this.state.taskName) {
                message.error('请输入任务名称')
                return
              }
              if (!this.state.title) {
                message.error('请输入标题')
                return
              }
              if (!this.state.content) {
                message.error('请输入内容')
                return
              }
              if (this.state.titleParams && this.state.titleParamsFiles.length > 0) {
                message.error('标题通配符不能同时上传文件和输入文本，请选择一种方式')
                return
              }
              if (this.state.contactParams && this.state.contactParamsFiles.length > 0) {
                message.error('接粉通配符不能同时上传文件和输入文本，请选择一种方式')
                return
              }
              if (this.state.contentParams && this.state.contentParamsFiles.length > 0) {
                message.error('文案通配符不能同时上传文件和输入文本，请选择一种方式')
                return
              }
              this.setState({ currentStep: 1, maxCurrentStep: 1 })
            }}>下一步</div>
            <div className="search-query-btn" onClick={() => {
                          if (!this.state.taskName) {
                            message.error('请输入任务名称')
                            return
                          }
                          if (!this.state.title) {
                            message.error('请输入标题')
                            return
                          }
                          if (!this.state.content) {
                            message.error('请输入内容')
                            return
                          }
                          if (this.state.titleParams && this.state.titleParamsFiles.length > 0) {
                            message.error('标题通配符不能同时上传文件和输入文本，请选择一种方式')
                            return
                          }
                          if (this.state.contactParams && this.state.contactParamsFiles.length > 0) {
                            message.error('接粉通配符不能同时上传文件和输入文本，请选择一种方式')
                            return
                          }
                          if (this.state.contentParams && this.state.contentParamsFiles.length > 0) {
                            message.error('文案通配符不能同时上传文件和输入文本，请选择一种方式')
                            return
                          }
                          const newData = {
                          taskName: this.state.taskName,
                          title: this.state.title,
                          content: this.state.content,
                          titleParams: this.state.titleParams,
                          contactParams: this.state.contactParams,
                          contentParams: this.state.contentParams,
                          titleParamsFiles: this.state.titleParamsFiles,
                          contactParamsFiles: this.state.contactParamsFiles,
                          contentParamsFiles: this.state.contentParamsFiles,
                          };
                          localStorage.setItem("batchSendEmailAddStep1Temp", JSON.stringify(newData));
                        }}>暂存</div>
          </div>
        </div> : ''
      }

      {
        this.state.currentStep === 1 ? <div>
          <div style={{ marginLeft: 180, fontSize: 18, fontWeight: 600, color: '#000' }}>步骤2：选择收件人</div>

          <div style={{ display: 'flex', marginLeft: 184, marginTop: 35 }}>
            <div style={{ color: 'rgba(0, 0, 0, 0.90)', width: 100, lineHeight: '24px', textAlign: 'right' }}>收件人录入方式</div>
            <div style={{ width: 400, marginLeft: 16 }}>
              <Radio.Group disabled={this.query.id} value={this.state.addMethod} onChange={(e) => {
                if (e.target.value === '3') {
                  this.loadTaskList()
                }
                this.setState({ addMethod: e.target.value })
              }}>
                <Radio value={"1"}>页面录入</Radio>
                <Radio value={"2"}>上传文件</Radio>
                <Radio value={"3"}>二次营销</Radio>
              </Radio.Group>
            </div>
          </div>

          {
            this.state.addMethod == "1" ? <div style={{ display: 'flex', marginLeft: 228, marginTop: 30 }}>
              <div style={{ color: 'rgba(0, 0, 0, 0.90)', width: 56, lineHeight: '24px', textAlign: 'right' }}><span style={{ color: '#D54941' }}>*</span>收件人</div>
              <div style={{ width: 400, marginLeft: 16 }}>
                <Textarea
                  disabled={this.query.id}
                  placeholder="一行为一条数据，使用回车键(Enter)换行。通配符用----分隔；如test@example.com----var1----var2"
                  value={this.state.addData}
                  rows={4}
                  onChange={(value) => {
                    this.setState({ addData: value })
                  }}
                />
              </div>
            </div> : ''
          }

          {
            this.state.addMethod == "2" ? <div style={{ display: 'flex', marginLeft: 228, marginTop: 30 }}>
              <div style={{ color: 'rgba(0, 0, 0, 0.90)', width: 56, lineHeight: '24px', textAlign: 'right' }}><span style={{ color: '#D54941' }}>*</span>收件人</div>
              <div style={{ width: 400, marginLeft: 16 }}>
                <TUpload
                  disabled={this.query.id}
                  {...getUploadImageProps()}
                  files={this.state.files}
                  onChange={(info) => {
                    if (info.length > 0) {
                      this.setState({ files: info });
                      return
                    }
                  }}
                  onRemove={() => this.setState({ files: [] })}
                />
                {/* <InputImageMsg value={this.state.images} onUploadChange={this.onUploadChange.bind(this)} hidePlus={false} /> */}
                <div style={{ fontSize: 12 }}>支持txt、csv文件，录入格式为：邮箱---var1---var2，不需要标题列</div>
              </div>
            </div> : ''
          }

          {
            this.state.addMethod == "3" ? <div style={{ display: 'flex', marginLeft: 205, marginTop: 30 }}>
              <div style={{ color: 'rgba(0, 0, 0, 0.90)', width: 80, lineHeight: '32px', textAlign: 'right' }}><span style={{ color: '#D54941' }}>*</span>请选择任务</div>
              <div style={{ width: 400, marginLeft: 16 }}>
                <Select disabled={this.query.id} value={this.state.taskId} style={{ width: 400 }} onChange={(e) => {
                  this.setState({ taskId: e });
                }}>
                  {
                    this.state.taskList.map(ws => {
                      return <Option value={ws.id}>{`${ws.name}(${ws.id})`}</Option>
                    })
                  }
                </Select>
              </div>
            </div> : ''
          }

          {
            this.state.addMethod == '3' ? <div style={{ display: 'flex', marginLeft: 184, marginTop: 35 }}>
              <div style={{ color: 'rgba(0, 0, 0, 0.90)', width: 100, lineHeight: '24px', textAlign: 'right' }}>行为</div>
              <div style={{ width: 600, marginLeft: 16 }}>
                <Radio.Group disabled={this.query.id} value={this.state.action} onChange={(e) => {
                  this.setState({ action: e.target.value })
                }}>
                  <Radio value={"reply"}>已回复</Radio>
                  <Radio value={"noReply"}>未回复</Radio>
                  <Radio value={"open"}>已打开</Radio>
                  <Radio value={"noOpen"}>未打开</Radio>
                  <Radio value={"click"}>已点击</Radio>
                  <Radio value={"noClick"}>未点击</Radio>
                </Radio.Group>
              </div>
            </div> : ''
          }

          {
            this.state.addMethod == '3' ? <div style={{ display: 'flex', marginLeft: 184, marginTop: 35 }}>
              <div style={{ color: 'rgba(0, 0, 0, 0.90)', width: 100, lineHeight: '32px', textAlign: 'right' }}>预计发送数量</div>
              <div style={{ width: 600, marginLeft: 16, lineHeight: '32px', color: '#000' }}>
                {this.state.taskList.filter(e => e.id == this.state.taskId).length > 0 ? this.state.taskList.filter(e => e.id == this.state.taskId)[0][this.state.action] : 0}
              </div>
            </div> : ''
          }

          {/* <div style={{ display: 'flex', marginLeft: 184, marginTop: 35 }}>
            <div style={{ color: 'rgba(0, 0, 0, 0.90)', width: 100, lineHeight: '32px', textAlign: 'right' }}><span style={{ color: '#D54941' }}>*</span>系统种子邮箱</div>
            <div style={{ width: 600, marginLeft: 16, lineHeight: '32px', color: '#000' }}>
              <Select disabled={this.query.id} value={this.state.emailGroupId} style={{ width: 400 }} onChange={(e) => {
                  this.setState({ emailGroupId: e });
                }}>
                  {
                    this.state.emailGroupList.map(ws => {
                      return <Option value={ws._id}>{`${ws.groupName}`}</Option>
                    })
                  }
                </Select>
            </div>
          </div> */}

          <div style={{ display: 'flex', marginLeft: 164, marginTop: 35 }}>
            <div style={{ color: 'rgba(0, 0, 0, 0.90)', width: 120, lineHeight: '32px', textAlign: 'right' }}><span style={{ color: '#D54941' }}>*</span>系统种子邮箱数量</div>
            <div style={{ width: 400, marginLeft: 16 }}>
              <TInput
                disabled={this.query.id}
                placeholder="请输入数量"
                clearable
                value={this.state.systemEmailCount}
                onChange={(value) => {
                  this.setState({ systemEmailCount: value })
                }}
              />
            </div>
          </div>

          <div style={{ display: 'flex', marginLeft: 184, marginTop: 35 }}>
            <div style={{ color: 'rgba(0, 0, 0, 0.90)', width: 100, lineHeight: '32px', textAlign: 'right' }}><span style={{ color: '#D54941' }}></span>外部种子邮箱</div>
            <div style={{ width: 400, marginLeft: 16 }}>
              <Textarea
                disabled={this.query.id}
                placeholder="如123@example.com，一行一个，系统自动分布"
                value={this.state.otherEmails}
                rows={4}
                onChange={(value) => {
                  this.setState({ otherEmails: value })
                }}
              />
            </div>
          </div>

          <div style={{ marginLeft: 298, marginTop: 38 }}>
            {/* <div className="search-reset-btn" onClick={() => this.props.history.goBack()}>返回</div> */}
            <div className="search-query-btn" onClick={() => {
              this.setState({ currentStep: this.state.currentStep - 1 })
            }}>上一步</div>
            <div className="search-query-btn" onClick={() => {
              if (this.state.addMethod == '1' && !this.state.addData) {
                message.error('请输入收件人')
                return
              }
              if (this.state.addMethod == '2' && (!this.state.files || this.state.files.length < 1)) {
                message.error('请输入收件人')
                return
              }
              if (this.state.addMethod == '3' && (!this.state.taskId)) {
                message.error('请选择任务')
                return
              }
              if (!this.state.systemEmailCount) {
                message.error('请输入系统种子邮箱数量')
                return
              }
              if (isNaN(this.state.systemEmailCount)) {
                message.error('请输入合法的系统种子邮箱数量')
                return
              }
              this.setState({ currentStep: this.state.currentStep + 1, maxCurrentStep: 2 })
            }}>下一步</div>
          </div>
        </div> : ''
      }

      {
        this.state.currentStep === 2 ? <div>
          <div style={{ marginLeft: 180, fontSize: 18, fontWeight: 600, color: '#000' }}>步骤3：设置发送方式</div>

          <div style={{ display: 'flex', marginLeft: 184, marginTop: 35 }}>
            <div style={{ color: 'rgba(0, 0, 0, 0.90)', width: 100, lineHeight: '24px', textAlign: 'right' }}>发送时间</div>
            <div style={{ width: 400, marginLeft: 16 }}>
              <Radio.Group disabled={this.query.id} value={this.state.sendMethod} onChange={(e) => {
                this.setState({ sendMethod: e.target.value })
              }}>
                <Radio value={"1"}>立即发送</Radio>
                <Radio value={"2"}>定时发送</Radio>
              </Radio.Group>
            </div>
          </div>

          {this.state.sendMethod === '2' ? <div style={{ display: 'flex', marginLeft: 184, marginTop: 35 }}>
            <div style={{ color: 'rgba(0, 0, 0, 0.90)', width: 100, lineHeight: '24px', textAlign: 'right' }}></div>
            <div style={{ width: 600, marginLeft: 16 }}>
              <DatePicker value={moment(new Date(this.state.sendTime))} disabled={this.query.id} showTime onChange={this.onDateTimeChange.bind(this)} />
            </div>
          </div> : ''}

          <div style={{ display: 'flex', marginLeft: 184, marginTop: 35 }}>
            <div style={{ color: 'rgba(0, 0, 0, 0.90)', width: 100, lineHeight: '24px', textAlign: 'right' }}>是否追踪打开</div>
            <div style={{ width: 400, marginLeft: 16 }}>
              <Radio.Group disabled={this.query.id} value={this.state.monitorOpen} onChange={(e) => {
                this.setState({ monitorOpen: e.target.value })
              }}>
                <Radio value={"no"}>否</Radio>
                <Radio value={"yes"}>是<span style={{ color: 'rgba(0, 0, 0, 0.40)', fontSize: 12, marginLeft: 10}}>开启后会增加邮件垃圾率！</span></Radio>
              </Radio.Group>
            </div>
          </div>

          <div style={{ display: 'flex', marginLeft: 184, marginTop: 35 }}>
            <div style={{ color: 'rgba(0, 0, 0, 0.90)', width: 100, lineHeight: '24px', textAlign: 'right' }}>是否追踪点击</div>
            <div style={{ width: 400, marginLeft: 16 }}>
              <Radio.Group disabled={this.query.id} value={this.state.monitorClick} onChange={(e) => {
                this.setState({ monitorClick: e.target.value })
              }}>
                <Radio value={"no"}>否</Radio>
                <Radio value={"yes"}>是<span style={{ color: 'rgba(0, 0, 0, 0.40)', fontSize: 12, marginLeft: 10}}>开启后会增加邮件垃圾率！</span></Radio>
              </Radio.Group>
            </div>
          </div>

          {/* <div style={{ display: 'flex', marginLeft: 184, marginTop: 35 }}>
            <div style={{ color: 'rgba(0, 0, 0, 0.90)', width: 100, lineHeight: '24px', textAlign: 'right' }}>添加退订链接</div>
            <div style={{ width: 400, marginLeft: 16 }}>
              <Radio.Group disabled={this.query.id} value={this.state.addUnsubscribe} onChange={(e) => {
                this.setState({ addUnsubscribe: e.target.value })
              }}>
                <Radio value={"no"}>否</Radio>
                <Radio value={"yes"}>是</Radio>
              </Radio.Group>
            </div>
          </div> */}

          <div style={{ display: 'flex', marginLeft: 164, marginTop: 35 }}>
            <div style={{ color: 'rgba(0, 0, 0, 0.90)', width: 120, lineHeight: '32px', textAlign: 'right' }}><span style={{ color: '#D54941' }}>*</span>单账号发送上限</div>
            <div style={{ width: 400, marginLeft: 16 }}>
              <TInput
                disabled={this.query.id}
                placeholder="请输入单账号发送上限"
                clearable
                value={this.state.oneAccountSendLimit}
                onChange={(value) => {
                  this.setState({ oneAccountSendLimit: value })
                }}
              />
            </div>
          </div>

          <div style={{ display: 'flex', marginLeft: 64, marginTop: 35 }}>
            <div style={{ color: 'rgba(0, 0, 0, 0.90)', width: 220, lineHeight: '32px', textAlign: 'right' }}><span style={{ color: '#D54941' }}>*</span>发送失败超过多少比例暂停任务</div>
            <div style={{ width: 400, marginLeft: 16 }}>
              <TInput
                disabled={this.query.id || this.state.closeFailRatePauseTask == 'enable'}
                placeholder="请输入"
                clearable
                value={this.state.failRatePauseTask}
                onChange={(value) => {
                  this.setState({ failRatePauseTask: value })
                }}
              />
            </div>
            <div style={{ marginLeft: 16, marginTop: 5 }}>
              <Checkbox checked={this.state.closeFailRatePauseTask == 'enable'} disabled={this.query.id} onChange={(val) => this.setState({ closeFailRatePauseTask: val ? 'enable' : 'disable' })}>不开启</Checkbox>
            </div>
          </div>

          <div style={{ display: 'flex', marginLeft: 64, marginTop: 35 }}>
            <div style={{ color: 'rgba(0, 0, 0, 0.90)', width: 220, lineHeight: '32px', textAlign: 'right' }}><span style={{ color: '#D54941' }}>*</span>可用账号低于多少暂停任务</div>
            <div style={{ width: 400, marginLeft: 16 }}>
              <TInput
                disabled={this.query.id || this.state.closeAvailableAccountPauseTask == 'enable'}
                placeholder="请输入"
                clearable
                value={this.state.availableAccountPauseTask}
                onChange={(value) => {
                  this.setState({ availableAccountPauseTask: value })
                }}
              />
            </div>
            <div style={{ marginLeft: 16, marginTop: 5 }}>
              <Checkbox checked={this.state.closeAvailableAccountPauseTask == 'enable'} disabled={this.query.id} onChange={(val) => this.setState({ closeAvailableAccountPauseTask: val ? 'enable' : 'disable' })}>不开启</Checkbox>
            </div>
          </div>

          <div style={{ display: 'flex', marginLeft: 64, marginTop: 35 }}>
            <div style={{ color: 'rgba(0, 0, 0, 0.90)', width: 220, lineHeight: '32px', textAlign: 'right' }}><span style={{ color: '#D54941' }}>*</span>种子邮箱垃圾率高于多少暂停任务</div>
            <div style={{ width: 400, marginLeft: 16 }}>
              <TInput
                disabled={this.query.id || this.state.closeSpamRatePauseTask == 'enable'}
                placeholder="请输入"
                clearable
                value={this.state.spamRatePauseTask}
                onChange={(value) => {
                  this.setState({ spamRatePauseTask: value })
                }}
              />
            </div>
            <div style={{ marginLeft: 16, marginTop: 5 }}>
              <Checkbox checked={this.state.closeSpamRatePauseTask == 'enable'} disabled={this.query.id} onChange={(val) => this.setState({ closeSpamRatePauseTask: val ? 'enable' : 'disable' })}>不开启</Checkbox>
            </div>
          </div>


          <div style={{ marginLeft: 298, marginTop: 38 }}>
            {/* <div className="search-reset-btn" onClick={() => this.props.history.goBack()}>返回</div> */}
            <div className="search-query-btn" onClick={() => {
              this.setState({ currentStep: this.state.currentStep - 1 })
            }}>上一步</div>
            <div className="search-query-btn" onClick={() => {
              if (this.state.sendMethod === '2' && !this.state.sendTime) {
                message.error('请输入发送时间')
                return
              }
              if (!this.state.oneAccountSendLimit) {
                message.error('请输入单账号发送上限')
                return
              }
              if (isNaN(this.state.oneAccountSendLimit)) {
                message.error('请输入合法的单账号发送上限')
                return
              }
              if (this.state.closeFailRatePauseTask != 'enable' && !this.state.failRatePauseTask) {
                message.error('请输入发送失败超过多少比例暂停任务')
                return
              }
              if (this.state.closeFailRatePauseTask != 'enable' && isNaN(this.state.failRatePauseTask)) {
                message.error('请输入合法的发送失败超过多少比例暂停任务')
                return
              }
              if (this.state.closeAvailableAccountPauseTask != 'enable' && !this.state.availableAccountPauseTask) {
                message.error('请输入可用账号低于多少暂停任务')
                return
              }
              if (this.state.closeAvailableAccountPauseTask != 'enable' && isNaN(this.state.availableAccountPauseTask)) {
                message.error('请输入合法的可用账号低于多少暂停任务')
                return
              }

              if (this.state.closeSpamRatePauseTask != 'enable' && !this.state.spamRatePauseTask) {
                message.error('请输入种子邮箱垃圾率高于多少暂停任务')
                return
              }
              if (this.state.closeSpamRatePauseTask != 'enable' && isNaN(this.state.spamRatePauseTask)) {
                message.error('请输入合法的种子邮箱垃圾率高于多少暂停任务')
                return
              }
              this.setState({ currentStep: this.state.currentStep + 1, maxCurrentStep: 3 })
            }}>下一步</div>
          </div>
        </div> : ''
      }

      {
        this.state.currentStep === 3 ? <div>
          <div style={{ marginLeft: 180, fontSize: 18, fontWeight: 600, color: '#000' }}>步骤4：A/B测试(可选)</div>

          <div style={{ display: 'flex', marginLeft: 184, marginTop: 35 }}>
            <div style={{ color: 'rgba(0, 0, 0, 0.90)', width: 100, lineHeight: '24px', textAlign: 'right' }}>是否启用A/B测</div>
            <div style={{ width: 400, marginLeft: 16 }}>
              <Radio.Group disabled={this.query.id} value={this.state.testAB} onChange={(e) => {
                this.setState({ testAB: e.target.value })
              }}>
                <Radio value={"no"}>否</Radio>
                <Radio value={"yes"}>是</Radio>
              </Radio.Group>
            </div>
          </div>

          {
            this.state.testAB === 'yes' ? <div style={{ display: 'flex', marginLeft: 184, marginTop: 30 }}>
              <div style={{ color: 'rgba(0, 0, 0, 0.90)', width: 100, lineHeight: '32px', textAlign: 'right' }}><span style={{ color: '#D54941' }}>*</span>版本B标题</div>
              <div style={{ width: 400, marginLeft: 16, marginRight: 16 }}>
                <TInput
                  disabled={this.query.id}
                  placeholder="请输入标题"
                  clearable
                  value={this.state.titleB}
                  onChange={(value) => {
                    this.setState({ titleB: value })
                  }}
                />
              </div>
              <Button type='link' onClick={() => this.setState({ titleB: this.state.title, contentB: this.state.content })} >同步版本A</Button>
            </div> : ''
          }

          {
            this.state.testAB === 'yes' ? <div style={{ display: 'flex', marginLeft: 184, marginTop: 30 }}>
              <div style={{ color: 'rgba(0, 0, 0, 0.90)', width: 100, lineHeight: '32px', textAlign: 'right' }}><span style={{ color: '#D54941' }}>*</span>版本B内容</div>
              <div>
                <div style={{ width: 1060, marginLeft: 16 }}>
                  <ReactQuill
                    readOnly={this.query.id}
                    value={this.state.contentB}
                    modules={modules}
                    onChange={(val) => this.setState({ contentB: val })}
                    theme="snow"
                    placeholder="请输入内容"
                    style={{ height: 300 }}
                  />
                </div>
              </div>
            </div> : ''
          }

          {
            this.state.testAB === 'yes' ? <div style={{ display: 'flex', marginLeft: 184, marginTop: 80 }}>
              <div style={{ color: 'rgba(0, 0, 0, 0.90)', width: 100, lineHeight: '32px', textAlign: 'right' }}><span style={{ color: '#D54941' }}>*</span>测试组比例</div>
              <div style={{ width: 400, marginLeft: 16, marginRight: 16, display: 'flex' }}>
                <Slider
                  disabled={this.query.id}
                  label
                  value={this.state.percent}
                  onChange={(value) => {
                    this.setState({ percent: value })
                  }}
                  layout="horizontal"
                  max={90}
                  min={10}
                />
                <div style={{ width: 50, marginLeft: 16 }}>
                  <TInput
                    value={this.state.percent}
                    disabled
                    onChange={(value) => {
                      this.setState({ percent: value })
                    }}
                  />
                </div>
              </div>
            </div> : ''
          }

          {
            this.state.testAB === 'yes' ? <div style={{ display: 'flex', marginLeft: 184, marginTop: 30 }}>
              <div style={{ color: 'rgba(0, 0, 0, 0.90)', width: 100, lineHeight: '32px', textAlign: 'right' }}><span style={{ color: '#D54941' }}>*</span>测试时长(h)</div>
              <div style={{ width: 400, marginLeft: 16, }}>
                <TInput
                  disabled={this.query.id}
                  placeholder="请输入测试时长"
                  clearable
                  value={this.state.testTimeLengthHour}
                  onChange={(value) => {
                    this.setState({ testTimeLengthHour: value })
                  }}
                />
              </div>
            </div> : ''
          }

          {
            this.state.testAB === 'yes' ? <div style={{ display: 'flex', marginLeft: 184, marginTop: 35 }}>
              <div style={{ color: 'rgba(0, 0, 0, 0.90)', width: 100, lineHeight: '24px', textAlign: 'right' }}><span style={{ color: '#D54941' }}>*</span>决定因素</div>
              <div style={{ width: 400, marginLeft: 16 }}>
                <Radio.Group disabled={this.query.id} value={this.state.factor} onChange={(e) => {
                  this.setState({ factor: e.target.value })
                }}>
                  <Radio value={"reply"}>回复率</Radio>
                  <Radio value={"open"}>打开率</Radio>
                  <Radio value={"click"}>点击率</Radio>
                  <Radio value={"other"}>去进度页手动选择</Radio>
                </Radio.Group>
              </div>
            </div> : ''
          }

          <div style={{ marginLeft: 298, marginTop: 38 }}>
            {/* <div className="search-reset-btn" onClick={() => this.props.history.goBack()}>返回</div> */}
            <div className="search-query-btn" onClick={() => {
              this.setState({ currentStep: this.state.currentStep - 1 })
            }}>上一步</div>
            <div className="search-query-btn" onClick={() => {
              if (this.state.testAB === 'yes' && !this.state.titleB) {
                message.error('请输入版本B标题')
                return
              }
              if (this.state.testAB === 'yes' && !this.state.contentB) {
                message.error('请输入版本B内容')
                return
              }
              if (this.state.testAB === 'yes' && !this.state.factor) {
                message.error('请输入决定因素')
                return
              }
              if (this.state.testAB === 'yes' && (!this.state.testTimeLengthHour || isNaN(this.state.testTimeLengthHour))) {
                message.error('请输入测试时长')
                return
              }
              this.setState({ currentStep: this.state.currentStep + 1, maxCurrentStep: 4 })
            }}>下一步</div>
          </div>
        </div> : ''
      }

      {
        this.state.currentStep === 4 ? <div>
          <div style={{ marginLeft: 180, fontSize: 18, fontWeight: 600, color: '#000' }}>步骤5：邮件测试</div>


          <div style={{ color: '#000', marginTop: 18, marginLeft: 188 }}>输入需要发送测试的数量，点击测试按钮，我们将发送邮件至系统内部种子邮箱，判断邮件内容是否进垃圾箱，测试邮件将扣除次数</div>

          <div style={{ display: 'flex', marginLeft: 184, marginTop: 30 }}>
            <div style={{ color: 'rgba(0, 0, 0, 0.90)', width: 100, lineHeight: '32px', textAlign: 'right' }}><span style={{ color: '#D54941' }}>*</span>测试数量</div>
            <div style={{ width: 200, marginLeft: 16, }}>
              <TInput
                disabled={this.query.id}
                placeholder="请输入测试数量"
                clearable
                value={this.state.testNum}
                onChange={(value) => {
                  this.setState({ testNum: value })
                }}
              />
            </div>

            {this.query.id ? '' : <div className='search-query-btn' style={{ marginLeft: 16 }} onClick={() => {
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
                title: `确认发送${this.state.testAB === 'yes' ? this.state.testNum * 2 : this.state.testNum}封测试邮件吗？`,
                onOk: () => {
                  this.sendTestEmail()
                },
                onCancel: () => {
                  message.info('测试邮件发送已取消')
                }
              })
            }}>发送测试</div>
            }
          </div>

          {
            this.state.testEmailTaskId ? <div style={{ marginLeft: 188, marginTop: 38 }}>
              <div className='search-reset-btn' onClick={() => this.loadTestData(true)}>刷新</div>
              <Table
                size="middle"
                tableLayout="fixed"
                pagination={false} columns={columns2}
                dataSource={this.state.testData}
                loading={this.state.loading}
              />
            </div> : ''
          }

          <div style={{ marginLeft: 188, marginTop: 38 }}>
            {/* <div className="search-reset-btn" onClick={() => this.props.history.goBack()}>返回</div> */}
            <div className="search-query-btn" onClick={() => {
              if (this.timer) {
                clearInterval(this.timer)
                this.timer = null
              }
              this.setState({ currentStep: this.state.currentStep - 1 })
            }}>上一步</div>
            <div className="search-query-btn" onClick={() => {
              if (this.timer) {
                clearInterval(this.timer)
                this.timer = null
              }
              this.setState({ currentStep: this.state.currentStep + 1, maxCurrentStep: 5 })
            }}>下一步</div>
          </div>
        </div> : ''
      }

      {
        this.state.currentStep === 5 ? <div>
          <div style={{ marginLeft: 180, fontSize: 18, fontWeight: 600, color: '#000' }}>步骤6：确认并发送</div>

          <div style={{ color: '#000', marginTop: 21, marginLeft: 180 }}>请确认你的任务配置，确认无误后，即可发送或调度邮件任务</div>

          <div style={{ marginTop: 21, marginLeft: 180, width: 720, padding: 24, borderRadius: 8, background: '#F5F5F5' }}>
            <div><span>邮件内容：</span>【{this.state.title}】{this.state.content.replace(/<[^>]*>/g, '').length > 10 ? this.state.content.replace(/<[^>]*>/g, '').slice(0, 10) : this.state.content.replace(/<[^>]*>/g, '')}</div>
            <div><span>收件人：</span>{this.state.addMethod === '1' ? '页面录入' : this.state.addMethod === '2' ? '上传文件' : '二次营销'}</div>
            <div><span>发送方式：</span>{this.state.sendMethod === '1' ? '立即发送' : '定时发送'}</div>
            <div><span>A/B测：</span>{this.state.testAB === 'yes' ? '启用' : '未启用'}</div>
            <div><span>邮件测试：</span>{this.state.testEmailTaskId ? '已测试' : '未测试'}</div>
          </div>

          <div style={{ marginLeft: 180, marginTop: 55 }}>
            {/* <div className="search-reset-btn" onClick={() => this.props.history.goBack()}>返回</div> */}
            <div className="search-query-btn" onClick={() => {
              this.setState({ currentStep: this.state.currentStep - 1 })
            }}>上一步</div>
            {this.query.id ? <div className="search-reset-btn" onClick={() => this.props.history.goBack()}>返回</div> : <div className="search-query-btn" onClick={() => {
              this.sendEmail()
            }}>确认发送</div>}
          </div>
        </div> : ''
      }

    </div>)
  }
}

export default connect(({ user }) => ({
  openKeys: user.openKeys,
  userID: user.info.userID
}))(injectIntl(MyComponent))
