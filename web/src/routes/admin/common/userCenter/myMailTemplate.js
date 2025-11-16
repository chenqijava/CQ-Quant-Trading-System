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
import DialogApi from '../dialog/DialogApi.js'
import ReactQuill from 'react-quill';
import { download } from "components/postDownloadUtils"

const { RangePicker } = DatePicker;
const { Panel } = Collapse;
const { TextArea } = Input;
const { Option } = Select;
const Search = Input.Search;
const confirm = Modal.confirm;
const { TabPane } = Tabs;
const { Item: FormItem } = Form;
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
// import '../../../mock'

// 针对当前页面的基础url
const baseUrl = '/api/mailTemplate';

const searchItems = [
];

const nowDate = new Date().Format("yyyy-MM-dd");

const getUploadImageProps = (regExpStr = 'txt') => {
  let regExp = new RegExp(`^.+\\.(${regExpStr})$`)
  const uploadProps = {
    name: 'file',
    multiple: false,
    action: `/api/consumer/res/uploadTxt/importMailTemp`,
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
      pagination: {
        pageSize: 10,
        current: 1,
        showTotal: (total, range) => `共 ${total} 条`,
        showSizeChanger: true,
        pageSizeOptions: ['10', '20', '30', '40', '50', '100', '200', '300', '400', '500']
      },
      groupPagination: {
        pageSize: 10,
        current: 1,
        showTotal: (total, range) => `共 ${total} 条`,
        showSizeChanger: true,
        pageSizeOptions: ['10', '20', '30', '40', '50', '100', '200', '300', '400', '500']
      },
      selectedRowKeys: [],
      groupSelectedRowKeys: [],

      tableContent: null,
      mainContent: null,
      scrollY: 0,
      groupScrollY: 0,
      mainContentHeight: 0,
      searchRef: null,
      tableContentHeight: 0,
      groupTableContentHeight: 0,


      allGroups: [],
      onlineStatus: '',
      createTimeRange: ['', ''],
      addVisible: false,
      files: [],
      detailVisible: false,
      detail: {},
      addType: '0',
      addName: '',
      addGroupId: '',
      addTitle: '',
      addContent: '',
      groupVisible: false,
      groupAddVisible: false,
      groupEditVisible: false,
      editDetail: false,
      groupAddName: ''
    };
    // 选中行的数据保存在selectedRows变量中
    // 之所以不放入state，因为这些变量是单向流动的，即Table控件产生这些数据，通过callback更新到变量this.selectedRows
    this.selectedRows = []
    this.groupSelectedRows = []
    // filters的写法保持与mongo filter的写法一致，避免与服务器、数据库直接的数据阻抗失衡
    // 例如： filters={username: 'foo'}
    // 因为json协议无法传输regex，所以字符类型的值，都会在服务器端转成regex，以提高匹配度，损失部分查询性能
    this.filters = {
    }

    this.sorter = {
      createTime: -1,
    }
  }

  // 首次加载数据
  async componentWillMount() {
    await this.reload();
    await this.getAllGroup()
  }

  async getAllGroup() {
    let res = await axios.post(`/api/mailTemplate/group/10000/1`, { filters: {type: 1}, sorter: { createTime: -1 } });
    if (res.data.code === 1) {
      this.setState({
        allGroups: res.data.data.data,
        groupSelectedRows: []
      })
    }
  }

  handleResize = () => {
    let height = document.body.getBoundingClientRect().height;
    if (this.state.tableContent) {
      this.setState({ scrollY: this.state.tableContent.getBoundingClientRect().height - 80, })
    }
    if (this.state.groupTableContent) {
      this.setState({ groupScrollY: this.state.tableContent.getBoundingClientRect().height - 80, })
    }
    if (this.state.mainContent) {
      this.setState({ mainContentHeight: this.state.mainContent.getBoundingClientRect().top })
    }
    if (this.state.selectedCountRef) {
      setTimeout(() => {
        if (this.state.selectedCountRef) {
          this.setState({ selectedCountRef: this.state.selectedCountRef, tableContentHeight: height - this.state.selectedCountRef.getBoundingClientRect().top - 84, scrollY: height - this.state.selectedCountRef.getBoundingClientRect().top - 84 - 80 })
        }
      }, 100)
    }
    if (this.state.groupSelectedCountRef) {
      setTimeout(() => {
        if (this.state.groupSelectedCountRef) {
          this.setState({ groupSelectedCountRef: this.state.groupSelectedCountRef, groupTableContentHeight: height - this.state.groupSelectedCountRef.getBoundingClientRect().top - 84, groupScrollY: height - this.state.groupSelectedCountRef.getBoundingClientRect().top - 84 - 80 })
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
    this.setState({ filters: {}, pagination: { ...this.state.pagination }, name: '', groupID: '' })
    this.load(this.state.pagination, this.filters, this.sorter)
  }

  async load(pagination, filters, sorter) {
    if (filters.status === '') {
      delete filters.status
    }
    if (filters.name === '') {
      delete filters.name
    }
    filters.type = 1
    this.setState({ loading: true })
    let res = await axios.post(`${baseUrl}/${pagination.pageSize}/${pagination.current}`, {
      filters: { ...filters }, sorter
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

  async onGroupRowSelectionChange(selectedRowKeys, selectedRows) {
    // 选中状态的数据，因为无需受控，就不记录在state里了，提高效率
    this.setState({ groupSelectedRowKeys: selectedRowKeys });
    this.groupSelectedRows = selectedRows
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

  async handleGroupTableChange(pagination, filters, sorter) {

    // 暂时不用Table的filter，不太好用
    await this.getAllGroup()
  }

  refSelectedCount = (ref) => {
    this.state.selectedCountRef = ref
    this.setState({ selectedCountRef: ref })
    this.handleResize()
  }

  groupRefSelectedCount = (ref) => {
    this.state.groupSelectedCountRef = ref
    this.setState({ groupSelectedCountRef: ref })
    this.handleResize()
  }

  openAdd () {
    this.setState({
      addVisible: true,
      addName: '',
      addTitle: '',
      addContent: '',
      addGroupId: '',
      addType: '0',
      files: []
    })
  }

  // 把columns放到render中，虽然损失部分性能，但是能方便参数中的匿名回调获取实例状态
  render() {
    const { intl } = this.props
    const columns = [
      {
        title: '模板名称',
        dataIndex: 'name',
        key: 'name',
        width: 191,
        ellipsis: true,
      },
      {
        title: '分组名称',
        dataIndex: 'groupName',
        key: 'groupName',
        width: 80,
        ellipsis: true,
      },
      {
        title: '使用次数',
        dataIndex: 'useCount',
        key: 'useCount',
        width: 80,
        ellipsis: true
      },
      {
        title: '当前状态',
        dataIndex: 'status',
        key: 'status',
        width: 80,
        ellipsis: true,
        render: (v, r) => {
          return v === 0 ? '可用' : '冻结'
        }
      },
      {
        title: '创建时间',
        dataIndex: 'createTime',
        key: 'createTime',
        render: formatDate,
        width: 160,
        sorter: true,
        ellipsis: true,
      },
      {
        title: '最后一次修改时间',
        dataIndex: 'lastModifiedDate',
        key: 'lastModifiedDate',
        render: formatDate,
        width: 160,
        sorter: true,
        ellipsis: true,
      },
      {
        title: '操作',
        dataIndex: 'oper',
        key: 'oper',
        width: 150,
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
                onClick={() => {
                  this.setState({
                    editDetail: false,
                    detailVisible: true,
                    detail: r
                  })
                }}
              >查看
              </Button>
              <Button
                type="link"
                style={{
                  padding: 0,
                  minWidth: 'auto',
                }}
                onClick={() => {
                  this.setState({
                    editDetail: true,
                    detailVisible: true,
                    detail: r
                  })
                }}
              >修改
              </Button>
            </div>
          )
        }
      },
    ];

    const groupColumns = [
      {
        title: '名称',
        dataIndex: 'groupName',
        key: 'groupName',
        width: 360,
        ellipsis: true,
      }
    ];

    const customRowStyle = {
      padding: '50px' // 调整此值以改变行高
    };

    return (
    <MyTranslate><ExternalAccountFilter>
      <Breadcrumb>
        <Breadcrumb.BreadcrumbItem>个人中心</Breadcrumb.BreadcrumbItem>
        <Breadcrumb.BreadcrumbItem>个人模板库</Breadcrumb.BreadcrumbItem>
      </Breadcrumb>
      <Dialog
        header={this.state.editDetail ? "修改模板": "查看模板"}
        width={854}
        height={566}
        visible={this.state.detailVisible}
        placement='center'
        onConfirm={async () => {
          if (this.state.editDetail) {
            let form = {}
            if (!this.state.detail?.name) {
              message.error('请输入模板名称')
              return
            }
            if (!this.state.detail?.title) {
              message.error('请输入模板标题')
              return
            }
            if (!this.state.detail?.content) {
              message.error('请输入模板内容')
              return
            }
            form = {
              _id: this.state.detail._id,
              name: this.state.detail.name,
              title: this.state.detail.title,
              content: this.state.detail.content,
              groupId: this.state.detail.groupId,
              type: "0",
              templateType: 1
            };
            this.setState({ loading: true });
            let res = await axios.post(`${baseUrl}/save`, form)
            if (res.data.code == 1) {
              message.success('操作成功')
              this.setState({ detailVisible: false })
              this.reload()
            } else {
              message.error(res.data.message)
              this.setState({ loading: false })
            }
          }
        }} confirmLoading={this.state.loading}
        onCancel={() => {
          this.setState({ detailVisible: false })
        }}
        onClose={() => {
          this.setState({ detailVisible: false })
        }}
      >
        <div style={{ marginTop: 26, color: 'rgba(0, 0, 0, 0.90)' }}>
                  <div style={{ display: 'flex', marginBottom: 24 }}>
                    <div style={{ width: 100, textAlign: 'right', marginRight: 16, lineHeight: '30px' }}>
                      <span style={{ color: '#D54941' }}>*</span>模板名称
                    </div>
                    <div>
                      <Input value={this.state.detail.name} disabled={!this.state.editDetail} onChange={(e) => {
                        let data = this.state.detail
                        data['name'] = e.target.value
                        this.setState({ detail: data })
                      }}
                        placeholder='请输入' />
                    </div>
                  </div>

                  <div style={{ display: 'flex', marginBottom: 24 }}>
                    <div style={{ width: 100, textAlign: 'right', marginRight: 16, lineHeight: '30px' }}>
                      <span style={{ color: '#D54941' }}>*</span>标题
                    </div>
                    <div>
                      <Input value={this.state.detail.title} disabled={!this.state.editDetail} onChange={(e) => {
                        let data = this.state.detail
                        data['title'] = e.target.value
                        this.setState({ detail: data })
                      }}
                        placeholder='请输入' />
                    </div>
                  </div>

                  <div style={{ display: 'flex', marginBottom: 24 }}>
                    <div style={{ width: 100, textAlign: 'right', marginRight: 16, lineHeight: '30px' }}>
                      <span style={{ color: '#D54941' }}>*</span>文案
                    </div>
                    <div style={{ }}>
                                    <ReactQuill
                                      value={this.state.detail.content}
                                      modules={modules}
                                      readOnly={!this.state.editDetail}
                                      onChange={(val) => {
                                        let data = this.state.detail
                                        data['content'] = val
                                        this.setState({ detail: data })
                                      }}
                                      theme="snow"
                                      placeholder="请输入内容"
                                      style={{ height: 300 }}
                                    />
                    </div>
                  </div>
                </div>
      </Dialog>
      <Dialog
        header={"导入模板"}
        width={854}
        height={566}
        visible={this.state.addVisible}
        placement='center'
        onConfirm={async () => {

                                      let form = {}
                                      if (this.state.addType === "1") {
                                        if (!this.state.files || this.state.files.length <= 0) {
                                          message.error('请上传文件')
                                          return
                                        }
                                        form = {
                                          filepath: this.state.files.length > 0 ? this.state.files[0].response.data.filepath : '',
                                          groupId: this.state.addGroupId,
                                          type: "1",
                                          templateType: 1
                                        };
                                      } else {
                                        if (!this.state.addName) {
                                          message.error('请输入模板名称')
                                          return
                                        }
                                        if (!this.state.addTitle) {
                                          message.error('请输入标题')
                                          return
                                        }
                                        if (!this.state.addContent) {
                                          message.error('请输入内容')
                                          return
                                        }
                                        form = {
                                          name: this.state.addName,
                                          title: this.state.addTitle,
                                          content: this.state.addContent,
                                          groupId: this.state.addGroupId,
                                          type: "0",
                                          templateType: 1
                                        };
                                      }
                                        this.setState({ loading: true });
                                        let res = await axios.post(`${baseUrl}/import`, form)
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
        <div style={{marginTop: 26, color: 'rgba(0, 0, 0, 0.90)' }}>
                  <div style={{ display: this.state.addType === "0" ? 'flex': 'none', marginBottom: 24 }}>
                    <div style={{ width: 100, textAlign: 'right', marginRight: 16, lineHeight: '30px' }}>
                      <span style={{ color: '#D54941' }}>*</span>模板名称
                    </div>
                    <div>
                      <Input value={this.state.addName}
                        onChange={(e) => {
                          this.setState({ addName: e.target.value })
                        }}
                        style={{ width: 400 }}
                        placeholder='请输入' />
                    </div>
                  </div>

                  <div style={{ display: 'flex', marginBottom: 24 }}>
                    <div style={{ width: 100, textAlign: 'right', marginRight: 16, lineHeight: '30px' }}>
                      <span style={{ color: '#D54941' }}>*</span>邮件录入方式
                    </div>
                    <div>
                      <Radio.Group value={this.state.addType} onChange={(e) => {
                                      this.setState({ addType: e.target.value })
                                    }}>
                                      <Radio value={"0"}>页面录入</Radio>
                                      <Radio value={"1"}>本地上传</Radio>
                                    </Radio.Group>
                    </div>
                  </div>

                  <div style={{ display: 'flex', marginBottom: 24 }}>
                              <div style={{ width: 100, textAlign: 'right', marginRight: 16, lineHeight: '30px' }}>模板分组</div>
                              <div style={{ color: 'rgba(0, 0, 0, 0.40)' }}>
                                <Select value={this.state.addGroupId} style={{ width: 200 }} placeholder="请选择内容" onChange={async v => {
                                  this.setState({ addGroupId: v })
                                }}>
                                  {this.state.allGroups.map(r => <Option key={r._id} value={r._id}>{r.groupName}</Option>)}
                                </Select>
                              </div>
                            </div>

                  <div style={{ display: this.state.addType === "0" ? 'flex': 'none', marginBottom: 24 }}>
                    <div style={{ width: 100, textAlign: 'right', marginRight: 16, lineHeight: '30px' }}>
                      <span style={{ color: '#D54941' }}>*</span>标题
                    </div>
                    <div>
                      <Input value={this.state.addTitle}
                        onChange={(e) => {
                          this.setState({ addTitle: e.target.value })
                        }}
                        placeholder='请输入' />
                    </div>
                  </div>

                  <div style={{ display: this.state.addType === "0" ? 'flex': 'none', marginBottom: 24 }}>
                    <div style={{ width: 100, textAlign: 'right', marginRight: 16, lineHeight: '30px' }}>
                      <span style={{ color: '#D54941' }}>*</span>内容
                    </div>
                    <div style={{ }}>
                                    <ReactQuill
                                      value={this.state.addContent}
                                      onChange={(val) => this.setState({ addContent: val })}
                                      modules={modules}
                                      readOnly={false}
                                      theme="snow"
                                      placeholder="请输入内容"
                                      style={{ height: 300 }}
                                    />
                    </div>
                  </div>

                  <div style={{ display: this.state.addType === "1" ? 'flex': 'none', marginBottom: 24, flexDirection: "column" }}>
                                        <TUpload
                                          style = {{marginLeft: 116}}
                                          {...getUploadImageProps("txt|csv")}
                                          showUploadProgress={false}
                                          files={this.state.files}
                                          onChange={(info) => {
                                                                            if (info.length > 0) {
                                                                              this.setState({ files: info });
                                                                              return
                                                                            }
                                                                          }}
                                          onRemove={() => this.setState({ files: [] })}
                                        >
                                        </TUpload>
                                        <div style={{ color: 'rgba(0, 0, 0, 0.40)', fontSize: 12, marginLeft: 116}}>
                                        <p>- txt格式：第一行是模版名称、第二行是标题、之后到末尾是文案；文件中若有多个模版用==NEWONE==隔开</p>
                                        <p>- csv格式：一个模版一行，第一列是模版名称，第二列是标题，第三列是文案</p>
                                        </div>
                                    </div>
                </div>
      </Dialog>
      <Dialog
              header={"分组列表"}
              width={450}
              height={566}
              visible={this.state.groupVisible}
              placement='center'
              onConfirm={() => {
                this.setState({ groupVisible: false })
              }}
              onCancel={() => {
                this.setState({ groupVisible: false })
              }}
              onClose={() => {
                this.setState({ groupVisible: false })
              }}
            >
              <div style={{ marginTop: 26, color: 'rgba(0, 0, 0, 0.90)' }}>
                <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                            <div
                              className={"search-reset-btn"}
                              onClick={() => this.setState({groupAddVisible: true, groupVisible: false, groupAddName: ''})}>新增分组
                            </div>
                            <div
                              className={this.state.groupSelectedRowKeys && this.state.groupSelectedRowKeys.length > 0 ? "search-query-btn" : "search-reset-btn"}
                              onClick={() =>
                              {
                                if (this.state.groupSelectedRowKeys && this.state.groupSelectedRowKeys.length === 1) {
                                  this.setState({groupEditVisible: true, groupVisible: false, groupAddName: this.groupSelectedRows[0]['groupName']})
                                } else {
                                  message.error("请选择一个分组")
                                }
                              }}>修改分组名
                            </div>
                            <div
                              className={this.state.groupSelectedRowKeys && this.state.groupSelectedRowKeys.length > 0 ? "search-query-btn" : "search-reset-btn"}
                              onClick={async () => {
                                if (this.state.groupSelectedRowKeys && this.state.groupSelectedRowKeys.length > 0) {
                                                let form = {
                                                  ids: this.state.groupSelectedRowKeys
                                                }
                                                this.setState({ loading: true });
                                                let res = await axios.post(`${baseUrl}/group/delete`, form)
                                                if (res.data.code == 1) {
                                                  message.success('操作成功')
                                                  this.setState({ groupAddVisible: false, groupVisible: true, loading: false })
                                                  this.getAllGroup()
                                                } else {
                                                  message.error(res.data.message)
                                                  this.setState({ loading: false })
                                                }
                                                } else {
                                  message.error("请选择分组")
                                                }
                              }}>删除分组名
                            </div>
                </div>
                <div className="tableSelectedCount"
                            ref={this.groupRefSelectedCount}>{`已选${this.state.groupSelectedRowKeys.length}项`}</div>
                          <div className="tableContent accountTableContent" style={{ height: this.state.groupTableContentHeight }}>
                            <div>
                              <Table
                                size="middle"
                                showHeader={false}
                                tableLayout="fixed"
                                scroll={{
                                  y: this.state.groupScrollY,
                                  x: groupColumns.filter(e => e.width).map(e => e.width).reduce((a, b) => a + b)
                                }}
                                pagination={this.state.groupPagination} rowSelection={{
                                  selectedRowKeys: this.state.groupSelectedRowKeys,
                                  onChange: this.onGroupRowSelectionChange.bind(this)
                                }} columns={groupColumns}
                                rowKey='_id'
                                dataSource={this.state.allGroups}
                                loading={this.state.loading}
                                onChange={this.handleGroupTableChange.bind(this)}
                              />
                            </div>
                          </div>
                          <Pagination
                            showJumper
                            total={this.state.groupPagination.total}
                            current={this.state.groupPagination.current}
                            pageSize={this.state.groupPagination.pageSize}
                            onChange={this.handleGroupTableChange.bind(this)}
                            components={{
                              body: {
                                row: (props) => (
                                  <tr {...props} style={{ ...props.style, ...customRowStyle }} />
                                ),
                              },
                            }}
                          />
              </div>
            </Dialog>
      <Dialog
              header={"新增分组"}
              width={854}
              height={566}
              visible={this.state.groupAddVisible}
              placement='center'
              onConfirm={async () => {
                let form = {
                  groupName: this.state.groupAddName,
                  type: 1
                }
                this.setState({ loading: true });
                let res = await axios.post(`${baseUrl}/group/add`, form)
                if (res.data.code == 1) {
                  message.success('操作成功')
                  this.setState({ groupAddVisible: false, groupVisible: true, loading: false })
                  this.getAllGroup()
                } else {
                  message.error(res.data.message)
                  this.setState({ loading: false })
                }
              }} confirmLoading={this.state.loading}
              onCancel={() => {
                this.setState({ groupAddVisible: false })
              }}
              onClose={() => {
                this.setState({ groupAddVisible: false })
              }}
            >
              <div style={{ marginLeft: 127, marginTop: 26, color: 'rgba(0, 0, 0, 0.90)' }}>
                        <div style={{ display: 'flex', marginBottom: 24 }}>
                          <div style={{ width: 100, textAlign: 'right', marginRight: 16, lineHeight: '30px' }}>
                            <span style={{ color: '#D54941' }}>*</span>分组名称
                          </div>
                          <div>
                            <Input value={this.state.groupAddName}
                            onChange={(e) => {
                            console.log(e.target.value)
                                                      this.setState({ groupAddName: e.target.value })
                                                    }}
                              placeholder='请输入' />
                          </div>
                        </div>
             </div>
            </Dialog>
      <Dialog
              header={"修改分组名"}
              width={854}
              height={566}
              visible={this.state.groupEditVisible}
              placement='center'
              onConfirm={async () => {
                              if (this.state.groupSelectedRowKeys && this.state.groupSelectedRowKeys.length === 1) {
                                let form = {
                                  _id: this.state.groupSelectedRowKeys[0],
                                  groupName: this.state.groupAddName,
                                  type: 1
                                }
                                this.setState({ loading: true });
                                let res = await axios.post(`${baseUrl}/group/updateName`, form)
                                if (res.data.code == 1) {
                                  message.success('操作成功')
                                  this.setState({ groupEditVisible: false, groupVisible: true, loading: false })
                                  this.getAllGroup()
                                } else {
                                  message.error(res.data.message)
                                  this.setState({ loading: false })
                                }
                              } else {
                                  message.error("请选择一个分组")
                              }
                            }} confirmLoading={this.state.loading}
              onCancel={() => {
                this.setState({ groupEditVisible: false })
              }}
              onClose={() => {
                this.setState({ groupEditVisible: false })
              }}
            >
              <div style={{ marginLeft: 127, marginTop: 26, color: 'rgba(0, 0, 0, 0.90)' }}>
                        <div style={{ display: 'flex', marginBottom: 24 }}>
                          <div style={{ width: 100, textAlign: 'right', marginRight: 16, lineHeight: '30px' }}>
                            <span style={{ color: '#D54941' }}>*</span>新名称
                          </div>
                          <div>
                            <Input value={this.state.groupAddName}
                                                        onChange={(e) => {
                                                                                  this.setState({ groupAddName: e.target.value })
                                                                                }}
                                                          placeholder='请输入' />
                          </div>
                        </div>
                      </div>
            </Dialog>
      <div className="account-search-box">
        <div className='account-search-item' style={{ minWidth: 280 }}>
          <div className="account-search-item-label">模板名称</div>
          <div className="account-search-item-right">
            <Input
              allowClear
              style={{ width: 200 }}
              placeholder="请输入"
              value={this.state.name}
              onChange={e => {
                this.setState({ name: e.target.value })
                this.filters['name'] = e.target.value
              }
              }
              onPressEnter={e => {
                this.filters['name'] = e.target.value
                this.reload()
              }
              }
            />
          </div>
        </div>

        <div className='account-search-item'>
          <div className="account-search-item-label">分组</div>
          <div className="account-search-item-right">
            <Select value={this.state.groupID} style={{ width: 200 }} placeholder="请选择内容" onChange={v => {
              this.setState({ groupID: v })
              this.filters['groupID'] = v
              this.reload()
            }}>
              {
                this.state.allGroups.map(e => <Option value={e._id}>{e.groupName}</Option>)
              }
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

      <div className="account-main-content">
        <div className="account-main-content-right">
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
            <div>
              <div className="search-query-btn" onClick={() => {
                this.setState({
                  addVisible: true,
                  addName: '',
                  addTitle: '',
                  addContent: '',
                  addGroupId: '',
                  addType: '0',
                  files: []
                })
              }}>导入模板</div>

              <div
                className={"search-query-btn"}
                onClick={() => this.setState({groupVisible: true})}>分组管理
              </div>
              <div
                className={this.state.selectedRowKeys && this.state.selectedRowKeys.length > 0 ? "search-query-btn" : "search-reset-btn"}
                onClick={async () => {
                  let keys = [];
                  for (const row of this.selectedRows) {
                    keys.push(row._id);
                  }
                  if (keys.length == 0) return;
                  let groupID = ''
                  let count = 0
                  DialogApi.info({
                    title: '请选择分组',
                    content: <><div style={{ display: 'flex', alignItems: 'center', gap: 10 }}>
                      <div className="account-search-item-label" style={{ width: 100, textAlign: 'right' }}>分组</div>
                      <div className="account-search-item-right">
                        <Select style={{ width: 200 }} placeholder="请选择内容" onChange={v => {
                          groupID = v
                        }}>
                          {
                            this.state.allGroups.map(e => <Option value={e._id}>{e.groupName}</Option>)
                          }
                        </Select>
                      </div>
                    </div>
                    </>,
                    onOkTxt: '确认',
                    onCancelTxt: '取消',
                    onOk: async () => {
                      this.setState({ loading: true });
                      let params = {
                        ids: keys,
                        groupId: groupID
                      }
                      let result = await axios.post(`${baseUrl}/setGroup`, params);
                      if (result.data.code == 1) {
                        message.success('操作成功');
                        this.getAllGroup()
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
                                                      }}>设置分组
              </div>
              <div
                className={this.state.selectedRowKeys && this.state.selectedRowKeys.length > 0 ? "search-delete-btn" : "search-reset-btn"}
                onClick={async () => {
                                                        if (this.state.selectedRowKeys && this.state.selectedRowKeys.length > 0) {
                                                                        let form = {
                                                                          ids: this.state.selectedRowKeys
                                                                        }
                                                                        this.setState({ loading: true });
                                                                        let res = await axios.post(`${baseUrl}/delete`, form)
                                                                        if (res.data.code == 1) {
                                                                          message.success('操作成功')
                                                                          this.setState({ loading: false })
                                                                          this.reload()
                                                                        } else {
                                                                          message.error(res.data.message)
                                                                          this.setState({ loading: false })
                                                                        }
                                                                        } else {
                                                          message.error("请选择模板")
                                                                        }
                                                      }}>删除模板
              </div>
              <div
                className={this.state.selectedRowKeys && this.state.selectedRowKeys.length > 0 ? "search-query-btn" : "search-reset-btn"}
                onClick={() => download(`${baseUrl}/export`, {
                  ids:this.state.selectedRowKeys && this.state.selectedRowKeys.length > 0? this.state.selectedRowKeys.join(",") : "" ,type:1
                })}>导出模板
              </div>

              <div
                className={this.state.selectedRowKeys && this.state.selectedRowKeys.length > 0 ? "search-query-btn" : "search-reset-btn"}
                onClick={async () => {
                                                                        if (this.state.selectedRowKeys && this.state.selectedRowKeys.length > 0) {
                                                                                        let form = {
                                                                                          ids: this.state.selectedRowKeys,
                                                                                          status: 1
                                                                                        }
                                                                                        this.setState({ loading: true });
                                                                                        let res = await axios.post(`${baseUrl}/updateStatus`, form)
                                                                                        if (res.data.code == 1) {
                                                                                          message.success('操作成功')
                                                                                          this.setState({ loading: false })
                                                                                          this.reload()
                                                                                        } else {
                                                                                          message.error(res.data.message)
                                                                                          this.setState({ loading: false })
                                                                                        }
                                                                                        } else {
                                                                          message.error("请选择模板")
                                                                                        }
                                                                      }}>冻结
              </div>

              <div
                className={this.state.selectedRowKeys && this.state.selectedRowKeys.length > 0 ? "search-query-btn" : "search-reset-btn"}
                onClick={async () => {
                                                                                        if (this.state.selectedRowKeys && this.state.selectedRowKeys.length > 0) {
                                                                                                        let form = {
                                                                                                          ids: this.state.selectedRowKeys,
                                                                                                          status: 0
                                                                                                        }
                                                                                                        this.setState({ loading: true });
                                                                                                        let res = await axios.post(`${baseUrl}/updateStatus`, form)
                                                                                                        if (res.data.code == 1) {
                                                                                                          message.success('操作成功')
                                                                                                          this.setState({ loading: false })
                                                                                                          this.reload()
                                                                                                        } else {
                                                                                                          message.error(res.data.message)
                                                                                                          this.setState({ loading: false })
                                                                                                        }
                                                                                                        } else {
                                                                                          message.error("请选择模板")
                                                                                                        }
                                                                                      }}>解冻
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
