import React, { Component } from 'react'
import { BrowserRouter as Router, Route, Link } from 'react-router-dom'
import {
  Icon,
  Table,
  Divider,
  message,
  Modal,
  Row,
  Col,
  Radio,
  Button,
  Avatar,
  Select
} from 'antd'
import axios from 'axios'
import { formatDate } from 'components/DateFormat'
import tTypes from 'components/taskTypes'
import taskStatuss from 'components/taskStatuss'
import { Pagination, Breadcrumb, Dialog } from 'tdesign-react';

const { BreadcrumbItem } = Breadcrumb;
const confirm = Modal.confirm;

// 针对当前页面的基础url
const baseUrl = '/api/consumer/task/quartzDetail';

class MyComponent extends Component {
  constructor(props) {
    super(props);
    this.query = this.props.taskQuartzDetail.substring(1).split('&').map((v) => {
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
        total: 0,            //添加次数
        pageSize: 10,
        current: 1,
        showTotal: (total, range) => `共 ${total} 条`,
        showSizeChanger: true,
        pageSizeOptions: ['10', '20', '30', '40', '50', '100', '200', '300', '400', '500']
      },
      selectedRowKeys: [],

      statusName: {
        'all': '全部',
      },
      statuses: [
        { label: '全部', value: 'all' },
      ],
      status: 'all',
      gtc: {},

      sendMsgVisible: false,   //发送消息任务
      params: {},               //任务参数

      imageBigVisible: false,
      imageBigUrl: '',
      showTable: false,
      scrollY: 0,
      tableContent: null,
    };
    Object.keys(taskStatuss).forEach(key => {
      if (key == 'waitPublish') return
      this.state.statusName[key] = taskStatuss[key];
      this.state.statuses.push(
        { label: taskStatuss[key], value: key },
      )
    })
    // 选中行的数据保存在selectedRows变量中
    // 之所以不放入state，因为这些变量是单向流动的，即Table控件产生这些数据，通过callback更新到变量this.selectedRows
    this.selectedRows = [];
    // filters的写法保持与mongo filter的写法一致，避免与服务器、数据库直接的数据阻抗失衡
    // 例如： filters={username: 'foo'}
    // 因为json协议无法传输regex，所以字符类型的值，都会在服务器端转成regex，以提高匹配度，损失部分查询性能
    this.filters = {
      status: this.query.globalMessage ? 'failed' : 'all'
    };
    // sorter的写法保持与mongo sort的写法一致，避免与服务器、数据库直接的数据阻抗失衡
    // 例如：sorter={createTime: -1}
    // 注意：Table控件仅支持单列排序，不支持多列同时排序
    this.sorter = {
      createTime: -1
    };
    // 提前写在constructor则Table首次加载时生效
    // 但是因为没有放到state中，所以除sorter外，都不建议提前设置，保持不受控状态是最好的
    this.type = this.query.type
  }

  // 首次加载数据
  async componentWillMount() {
    this.reload();
  }

  handleResize = () => {
    if (this.state.tableContent) {
      this.setState({ scrollY: this.state.tableContent.getBoundingClientRect().height - 80, })
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
    this.load(this.state.pagination, this.filters, this.sorter)
  }

  async load(pagination, filters, sorter) {
    // 这里的参数是格式好的，无需修改，无阻抗失衡，直接发给服务器
    this.setState({ loading: true });
    let res = await axios.post(`${baseUrl}/${this.query.id}/${pagination.pageSize}/${pagination.current}`, {
      filters,
      sorter,
      type: this.query.type,      //accountSurvivalSearch任务有此参数
      taskType: this.query.taskType || 'sendRoomMsg',
    });
    pagination.total = res.data.total;
    this.setState({
      loading: false,
      data: res.data.data,
      pagination,
      selectedRowKeys: [],
      gtc: res.data.gtc
    });
    this.selectedRows = [];
    this.filters = filters;
    this.sorter = sorter
  }

  async handleTableChange(pagination, filters, sorter) {
    // 此处是table控件的回调，其中的参数格式不是很合理，调整一下后，传递给load
    let sort = this.sorter;
    if (sorter && sorter.field) {
      sort = {}
      sort[sorter.field] = sorter.order == 'descend'
        ? -1
        : 1
    }
    // 暂时不用Table的filter，不太好用
    this.load(pagination, this.filters, sort)
  }

  async changeTaskType(e) {
    e = { target: { value: e } };
    this.filters.status = e.target.value;
    this.reload()
  }

  async backToList() {
    if (this.query.type && this.query.type === 'accountSurvivalSearch') {
      this.props.history.push('/cloud/account/accountSurvivalSearch');
    } else if (this.query.type && this.query.type === 'adminSurvivalSearch') {
      this.props.history.push('/admin/account/adminSurvivalSearch');
    } else {
      let params = [];
      if (this.query.type) {
        params.push(['type', this.query.type].join('='))
      }
      this.props.history.push(`/cloud/account/taskQuartz${params.length > 0 ? `?${params.join('&')}` : ''}`)
    }
  }

  async showDetail(r) {
    this.setState({
      sendMsgVisible: true,
      params: r.params
    })
  }

  async reset() {
    this.setState({status: 'all'})
    delete this.filters.status
    this.reload()
  }
  

  refTableContent = (ref) => {
    if (ref && ref.getBoundingClientRect) {
      this.setState({ showTable: true, scrollY: ref.getBoundingClientRect().height - 80, tableContent: ref })
    }
  }

  // 把columns放到render中，虽然损失部分性能，但是能方便参数中的匿名回调获取实例状态
  render() {
    let columns = [];
    if ([tTypes.sendMsg, tTypes.sendRoomMsg, tTypes.sendTimingMsg, tTypes.batchSendMsg].indexOf(this.query.type) != -1) {
      columns = [
        {
          title: '执行账号',
          dataIndex: 'id',
          key: 'id',
          width: 160,
          ellipsis: true,
          render: (v, r) => {
            if (this.state.gtc[v])
              return `${this.state.gtc[v].nickname}[${this.state.gtc[v].accID}]`;
          }
        }, {
          title: '手机号',
          dataIndex: 'id',
          key: 'phoneNumber',
          width: 130,
          ellipsis: true,
          render: (v, r) => {
            if (this.state.gtc[v]) {
              return this.state.gtc[v].phone;
            }
          }
        }, {
          title: '好友/群 账号',
          dataIndex: 'friendId',
          key: 'friendId',
          width: 160,
          ellipsis: true,
          render: (v, r) => {
            if (this.state.gtc[r.id] && this.state.gtc[r.id][r._id] && this.state.gtc[r.id][r._id].friendWxID)
              return `${this.state.gtc[r.id][r._id].friendNickname}[${this.state.gtc[r.id][r._id].friendWxID}]`;
          }
        }, {
          title: '状态',
          dataIndex: 'status',
          key: 'status',
          width: 100,
          ellipsis: true,
          //sorter: true,
          render: (v, r) => {
            return this.state.statusName[v]
          }
        }, {
          title: '创建时间',
          dataIndex: 'createTime',
          key: 'createTime',
          width: 130,
          ellipsis: true,
          render: formatDate
        }, {
          title: '完成时间',
          dataIndex: 'finishTime',
          key: 'finishTime',
          width: 130,
          ellipsis: true,
          render: formatDate
        }, {
          title: '备注',
          dataIndex: 'result',
          key: 'result',
          width: 250,
          ellipsis: true,
          render: (v, r) => {
            if (v) {
              if (v.msg)
                return v.msg;
              else if (v.message)
                return v.message;
              else return v
            } else {
              let addition = '';
              if (r.status === 'init' && r.checkParams && r.checkParams.nextSendMsgTime) {
                addition = "下次执行时间:" + formatDate(new Date(r.checkParams.nextSendMsgTime));
              }
              return addition
            }
          }
        }
        // , {
        //   title: '操作',
        //   dataIndex: 'oper',
        //   key: 'oper',
        //   render: (v, r) => {
        //     return (<div>
        //       <Button type="link" onClick={() => {
        //         this.showDetail(r)
        //       }}>详情</Button>
        //     </div>)
        //   }
        // }
      ];
    } else if ([tTypes.sendMsgByQR].indexOf(this.query.type) >= 0) {
      columns = [
        {
          title: '账号',
          dataIndex: 'id',
          key: 'id',
          render: (v, r) => {
            if (this.state.gtc[v]) {
              return `${this.state.gtc[v].nickname}[${this.state.gtc[v].accID}]`;
            } else if (r.params && r.params.ownerAccID)
              return `${r.params.ownerNickname}[${r.params.ownerAccID}]`;
            else return v
          }
        }, {
          title: '目标ID',
          dataIndex: 'params.addData',
          key: 'addData',
        }, {
          title: '目标昵称',
          dataIndex: 'params.nickname',
          key: 'params.nickname',
        }, {
          title: '状态',
          dataIndex: 'status',
          key: 'status',
          sorter: true,
          render: (v) => {
            return this.state.statusName[v]
          }
        }, {
          title: '创建时间',
          dataIndex: 'createTime',
          key: 'createTime',
          render: formatDate
        }, {
          title: '完成时间',
          dataIndex: 'finishTime',
          key: 'finishTime',
          render: formatDate
        }, {
          title: '操作',
          dataIndex: 'oper',
          key: 'oper',
          render: (v, r) => {
            return (<div>
              <Button type="link" onClick={() => {
                this.showDetail(r)
              }}>详情</Button>
            </div>)
          }
        }, {
          title: '备注',
          dataIndex: 'result',
          key: 'result',
          render: (v) => {
            if (v) {
              if (v.msg)
                return v.msg;
              else if (v.message)
                return v.message;
              else return v
            }
          }
        }
      ];
    } else if ([tTypes.sendStrangerMsg].indexOf(this.query.type) >= 0) {
      columns = [
        {
          title: '账号',
          dataIndex: 'id',
          key: 'id',
          width: 160,
          ellipsis: true,
          render: (v, r) => {
            if (this.state.gtc[v]) {
              return `${this.state.gtc[v].nickname}[${this.state.gtc[v].accID}]`;
            } else if (r.params && r.params.ownerAccID)
              return `${r.params.ownerNickname}[${r.params.ownerAccID}]`;
            else return v
          }
        }, {
          title: '目标ID',
          dataIndex: 'params.addData',
          key: 'addData',
          width: 160,
          ellipsis: true,
        }, {
          title: '消息类型',
          dataIndex: 'params.msgType',
          key: 'params.msgType',
          width: 100,
          ellipsis: true,
          render: (v, r) => {
            if (r.params['friendAddScene']) {
              return '情景消息'
            }
            let msgTypeMap = {
              1: '文本',
              3: '图片',
              34: '语音',
              43: '视频',
              20: '轻视频',
              37: '好友请求',
              49: '卡片消息',
              50: '联系人',
              51: '转发消息',
              52: '转发消息',
            }
            return msgTypeMap[v] || ''
          }
        }, {
          title: '消息内容',
          dataIndex: 'params.content',
          key: 'params.content',
          width: 250,
          ellipsis: true,
          render: (v, r) => {
            if (r.params.content) {
              return r.params.content
            } else if (r.params.fwd) {
              return JSON.stringify(r.params.fwd)
            } else if (r.params.friendAddScene) {
              return r.params.friendAddScene
            }
          }
        }, {
          title: '状态',
          dataIndex: 'status',
          key: 'status',
          width: 100,
          ellipsis: true,
          //sorter: true,
          render: (v) => {
            return this.state.statusName[v]
          }
        }, {
          title: '创建时间',
          dataIndex: 'createTime',
          key: 'createTime',
          width: 130,
          ellipsis: true,
          render: formatDate
        }, {
          title: '完成时间',
          dataIndex: 'finishTime',
          key: 'finishTime',
          width: 130,
          ellipsis: true,
          render: formatDate
        }, {
          title: '备注',
          dataIndex: 'result',
          key: 'result',
          width: 200,
          ellipsis: true,
          render: (v) => {
            if (v) {
              if (v.msg)
                return v.msg;
              else if (v.message)
                return v.message;
              else return v
            }
          }
        }
        // , {
        //   title: '操作',
        //   dataIndex: 'oper',
        //   key: 'oper',
        //   render: (v, r) => {
        //     return (<div>
        //       <Button type="link" onClick={() => {
        //         this.showDetail(r)
        //       }}>详情</Button>
        //     </div>)
        //   }
        // }
      ];
    } else if ([tTypes.yangGroup].indexOf(this.query.type) >= 0) {
      columns = [
        // {
        //   title: '账号',
        //   dataIndex: 'id',
        //   key: 'id',
        //   render: (v, r) => {
        //     if (this.state.gtc[v]) {
        //       return `${this.state.gtc[v].nickname}[${this.state.gtc[v].accID}]`;
        //     } else if (r.params && r.params.ownerAccID)
        //       return `${r.params.ownerNickname}[${r.params.ownerAccID}]`;
        //     else return v
        //   }
        // },
        {
          title: '群链接',
          dataIndex: 'params.link',
          key: 'params.link',
          width: 160,
          ellipsis: true,
        }, {
          title: '群ID',
          dataIndex: 'params.accID',
          key: 'params.accID',
          width: 160,
          ellipsis: true,
        }, {
          title: '轮次',
          dataIndex: 'params.execTimes',
          key: 'params.execTimes',
          width: 100,
          ellipsis: true,
          render: (v) => {
            return `第${(v || 0) + 1}轮`
          }
        }, {
          title: '状态',
          dataIndex: 'status',
          key: 'status',
          //sorter: true,
          width: 100,
          ellipsis: true,
          render: (v) => {
            return this.state.statusName[v]
          }
        }, {
          title: '创建时间',
          dataIndex: 'createTime',
          key: 'createTime',
          width: 130,
          ellipsis: true,
          render: formatDate
        }, {
          title: '完成时间',
          dataIndex: 'finishTime',
          key: 'finishTime',
          width: 130,
          ellipsis: true,
          render: formatDate
        }, {
          title: '备注',
          dataIndex: 'result',
          key: 'result',
          width: 250,
          ellipsis: true,
          render: (v) => {
            if (v) {
              if (v.msg)
                return v.msg;
              else if (v.message)
                return v.message;
              else return v
            }
          }
        }
        //  ,{
        //     title: '操作',
        //     dataIndex: 'oper',
        //     key: 'oper',
        //     render: (v, r) => {
        //       return (r.status == 'success' ? <div>
        //         <Button type="link" onClick={() => {
        //           this.props.history.push('/cloud/account/taskBatchDetail?id=' + r.params.yangGroupSpeakTaskId);
        //         }}>查看任务</Button>
        //       </div> : '')
        //     }
        //   }
      ];
    } else if ([tTypes.sendStrangerMsg2].indexOf(this.query.type) >= 0) {
      columns = [
        {
          title: '账号',
          dataIndex: 'id',
          key: 'id',
          width: 160,
          ellipsis: true,
          render: (v, r) => {
            if (r.params && r.params.ownerAccID)
              return `${r.params.nickname}`;
            else return v
          }
        }, {
          title: '状态',
          dataIndex: 'status',
          key: 'status',
          //sorter: true,
          width: 100,
          ellipsis: true,
          render: (v) => {
            return this.state.statusName[v]
          }
        }, {
          title: '创建时间',
          dataIndex: 'createTime',
          key: 'createTime',
          width: 130,
          ellipsis: true,
          render: formatDate
        }, {
          title: '完成时间',
          dataIndex: 'finishTime',
          key: 'finishTime',
          width: 130,
          ellipsis: true,
          render: formatDate
        }, {
          title: '备注',
          dataIndex: 'result',
          key: 'result',
          width: 250,
          ellipsis: true,
          render: (v) => {
            if (v) {
              if (v.msg)
                return v.msg;
              else if (v.message)
                return v.message;
              else return v
            }
          }
        }
      ]
    } else {
      columns = [
        {
          title: '账号',
          dataIndex: 'userID',
          key: 'userID',
          width: 160,
          ellipsis: true,
          // render: (v, r) => {
          //   if (this.state.gtc[v]) {
          //     return `${this.state.gtc[v].nickname}[${this.state.gtc[v].accID}]`;
          //   } else if (r.params && r.params.ownerAccID)
          //     return `${r.params.ownerNickname}[${r.params.ownerAccID}]`;
          //   else return v
          // }
        }, {
          title: '状态',
          dataIndex: 'status',
          key: 'status',
          //sorter: true,
          width: 100,
          ellipsis: true,
          render: (v) => {
            return this.state.statusName[v]
          }
        }, {
          title: '创建时间',
          dataIndex: 'createTime',
          key: 'createTime',
          width: 130,
          ellipsis: true,
          render: formatDate
        }, {
          title: '完成时间',
          dataIndex: 'finishTime',
          key: 'finishTime',
          width: 130,
          ellipsis: true,
          render: formatDate
        }, {
          title: '备注',
          dataIndex: 'result',
          key: 'result',
          width: 250,
          ellipsis: true,
          render: (v) => {
            if (v) {
              if (v.msg)
                return v.msg;
              else if (v.message)
                return v.message;
              else return v
            }
          }
        }
        , {
          title: '操作',
          dataIndex: 'oper',
          key: 'oper',
          width: 130,
          render: (v, r) => {
            return (<div>
              <Button type="link" onClick={() => {
                this.showDetail(r)
              }}>详情</Button>
            </div>)
          }
        }
      ];
    }

    return (<div>
      {/* <Breadcrumb>
        <BreadcrumbItem>任务进度</BreadcrumbItem>
        <BreadcrumbItem>{tTypes.tTypeName[this.type]}</BreadcrumbItem>
      </Breadcrumb> */}

      <div className="search-box">
        <div className='search-item'>
          <div className="search-item-label">任务状态</div>
          <div className="search-item-right">
            <Select value={this.state.status} style={{ width: 200 }} onChange={this.changeTaskType.bind(this)}>
              {this.state.statuses.map(ws => {
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

            <div class="main-content">
              <div className="tableContent" ref={this.refTableContent} style={{height: 'calc(100vh - 485px)'}}>
                <div>
                  {this.state.showTable ? <Table
                    tableLayout="fixed"
                    scroll={{ y: this.state.scrollY, x: 1000 }}
                    pagination={this.state.pagination} columns={columns} rowKey='_id' dataSource={this.state.data} loading={this.state.loading} /> : ''}
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
        header="参数详情"
        visible={this.state.sendMsgVisible}
        confirmBtn={null}
        onClose={() => { this.setState({ sendMsgVisible: false })}}
        onCancel={() => {this.setState({ sendMsgVisible: false })}}
      >
        <div>
          {
            this.state.params && this.state.params.rowData ? <p>{this.state.params.account}</p> : ''
          }
        </div>

        <div>
          {
            this.state.params && this.state.params.content ? <p>{this.state.params.content}</p> : ''
          }
        </div>
        <div>
          {
            this.state.params && this.state.params.fileName ?
              <div key={`image-${this.state.params.fileName}`}
                style={{ display: 'inline-block', marginRight: '12px', marginBottom: '12px' }}
                onClick={() => {
                  this.setState({
                    imageBigVisible: true,
                    imageBigUrl: `/api/consumer/res/downloadUploadFileByBase64/${this.state.params.fileName}`
                  })
                }}>
                <Avatar shape="square" size={64}
                  src={`/api/consumer/res/downloadUploadFileByBase64/${this.state.params.fileName}`} />
              </div> : ''
          }
        </div>

        <div>
          {
            this.state.params && this.state.params.messages ?
              this.state.params.messages.map(msg => (
                msg.content ?
                  <p>{msg.content}</p>
                  :
                  msg.fileName ?
                    <div key={`image-${msg.fileName}`}
                      style={{ display: 'inline-block', marginRight: '12px', marginBottom: '12px' }}
                      onClick={() => {
                        this.setState({
                          imageBigVisible: true,
                          imageBigUrl: `/api/consumer/res/downloadUploadFileByBase64/${msg.fileName}`
                        })
                      }}>
                      <Avatar shape="square" size={64}
                        src={`/api/consumer/res/downloadUploadFileByBase64/${msg.fileName}`} />
                    </div> : ''

              )) : ''
          }
        </div>

        <div>
          {
            this.state.params && this.state.params.text ? <p>{this.state.params.text}</p> : ''
          }
        </div>

        <div>
          {
            this.state.params && this.state.params.files ?
              <div key={`image-${this.state.params.files}`}
                style={{ display: 'inline-block', marginRight: '12px', marginBottom: '12px' }}
                onClick={() => {
                  this.setState({
                    imageBigVisible: true,
                    imageBigUrl: `/api/consumer/res/downloadUploadFileByBase64/${this.state.params.files[0].fileName}`
                  })
                }}>
                <Avatar shape="square" size={64}
                  src={`/api/consumer/res/downloadUploadFileByBase64/${this.state.params.files[0].fileName}`} />
              </div> : ''
          }
        </div>

        <div>
          {
            this.state.params && this.state.params.fwd ? <p>{this.state.params.fwd.url}/{this.state.params.fwd.msgIDs}</p> : ''
          }
        </div>

        <div>
          {
            this.state.params && this.state.params.images ?
              this.state.params.images.map(image => {
                let src = `/api/consumer/res/downloadUploadFileByBase64/${image.fileName}`;
                if (image.fileName.endsWith('.jpg')) {
                  src = `/api/consumer/res/${image.fileName}`;
                }
                return (
                  <div key={`image-${this.state.params.fileName}`}
                    style={{ display: 'inline-block', marginRight: '12px', marginBottom: '12px' }}
                    onClick={() => {
                      this.setState({ imageBigVisible: true, imageBigUrl: src })
                    }}>
                    <Avatar shape="square" size={64}
                      src={src} />
                  </div>
                )
              }) : ''
          }
        </div>
      </Dialog>
      <Modal visible={this.state.imageBigVisible} footer={null}
        onCancel={() => {
          this.setState({ imageBigVisible: false })
        }}>
        <img alt="example" style={{ width: '100%' }} src={this.state.imageBigUrl} />
      </Modal>
    </div>)
  }
}

export default MyComponent
