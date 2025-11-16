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
  // Switch,
  Modal,
  // Radio,
  Badge,
  Form,
  Row,
  Col,
} from 'antd'
import axios from 'axios'
import { FormattedMessage, useIntl, injectIntl } from 'react-intl'
import ExternalAccountFilter from "components/common/ExternalAccountFilter";
import MyTranslate from "components/common/MyTranslate";
import { Pagination, Breadcrumb, Dialog, Tag, Input as TInput, Upload as TUpload, Tooltip as TTooltip, Radio, Switch, Image, Space } from 'tdesign-react';
import DialogApi from '../../common/dialog/DialogApi.js'
import { connect } from 'dva'
import uuid from 'uuid/v4'
import { CloseIcon } from 'tdesign-icons-react';
import BasicImageViewer from 'components/common/BasicImageViewer'
import indexImage from '../../../../images/index.jpg'
import SysPermissionButton from '../../../../components/common/SysPermissionButton.js'
import { status } from 'nprogress'

const Search = Input.Search;
const confirm = Modal.confirm;
const { Item: FormItem } = Form;

const pageType = 'params';

// 通用全局参数设置
const params = [
  // {
  //   type: 'index',
  //   code: 'placard',
  //   itemType: 'image',
  //   desc: '首页海报',
  // },
  // {
  //   type: 'vps',
  //   code: 'vpsUnitPrice',
  //   desc: 'vps单价',
  // },
  {
    type: 'account',
    code: 'checkAccountInterval',
    desc: '邮件接码配置：检查邮箱可用时间间隔（秒）',
  },
  {
    type: 'webConfig',
    code: 'baseUrl',
    desc: '邮件接码配置：网站域名',
  },
  {
    type: 'account',
    code: 'onceImageNum',
    desc: 'AI图片识别配置：一次请求处理的图片数量',
  },
  {
    type: 'account',
    code: 'tooManyWaitSecond',
    desc: 'AI图片识别配置：图片识别429等待秒数',
  },

  {
    type: 'account',
    code: 'sendEmailIntervalInSecond',
    desc: '邮件群发配置：同账号发送间隔(秒)',
  },
  {
    type: 'account',
    code: 'sendEmailMaxNumByDay',
    desc: '邮件群发配置：同账号单日发送上限',
  },
  {
    type: 'account',
    code: 'sendEmailAiModel',
    desc: '邮件群发配置：AI模型(chatgpt/google)',
  },
  {
    type: 'account',
    code: 'tuiRetry',
    desc: '邮件群发配置：退信重试次数',
  },
  {
    type: 'account',
    code: 'whiteIp',
    desc: '请求429IP白名单',
  },
  {
    type: 'account',
    code: 'whiteIp2',
    desc: '请求429IP白名单2(并发更大)',
  },
  // {
  //   type: 'account',
  //   code: 'useThirdGmailCheck',
  //   desc: '邮箱检测配置：使用第三方检测Gmail',
  // },
  // {
  //   type: 'account',
  //   code: 'useThreadNumGmailCheck',
  //   desc: '邮箱检测配置：启动检测线程数',
  // },
  // {
  //   type: 'account',
  //   code: 'onceCheckTimeout',
  //   desc: '邮箱检测配置：单次检测超时时间（秒）',
  // },
  {
    type: "account",
    code: "severnumber",
    desc: "DNS域名访问的服务器编号",
  },
  {
    type: 'account',
    code: 'linkCheckTemplate',
    desc: '链接检测配置：检测邮件内容模版',
    itemType: 'file'
  },
  {
    type: 'account',
    code: 'linkCheckSubjectTemplate',
    desc: '链接检测配置：检测邮件主题模版',
    itemType: 'file'
  },
  {
    type: 'task',
    code: 'openSieveActiveTask',
    itemType: 'switch',
    desc: '筛开通配置：是否执行筛开通任务',
  },
  {
    type: 'server',
    code: 'cloudMasterURL',
    desc: '筛开通配置：筛开通总控服务器地址',
  },
  {
    type: "server",
    code: "cloudMasterCookie",
    desc: "筛开通配置：筛开通总控服务器Cookie",
  }
];

const rechargeMethodParam = {
  type: 'account',
  code: 'rechargeMethod',
  desc: '充值方案',
}

const proxyMethodParam = {
  type: 'account',
  code: 'proxyMethod',
  desc: '代理返佣方案',
}

const getUploadProps = (regExpStr = 'txt') => {
  let regExp = new RegExp(`^.+\\.(${regExpStr})$`)
  const uploadProps = {
    name: 'file',
    multiple: false,
    action: `/api/consumer/res/uploadTxt/params`,
    beforeUpload(file, fileList) {
      if (!regExp.test(file.name.toLowerCase())) {
        message.error(`文件名不合法,文件后缀为( ${regExpStr} )`)
        return false
      }
      return true
    }
  };
  return uploadProps
}

const getUploadImageProps = (regExpStr = 'jpg|png|jpeg') => {
  let regExp = new RegExp(`^.+\\.(${regExpStr})$`)
  const uploadProps = {
    name: 'file',
    multiple: false,
    action: `/api/consumer/res/upload/params`,
    beforeUpload(file, fileList) {
      // const isLt2M = file.size / 1024 / 1024 < 3;
      // if (!isLt2M) {
      //   message.error('文件必须小于3MB!');
      //   return false
      // }
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
      // 通用全局参数设置
      config: {},

      loading: false,
      taskServerCount: 0,
      globalShowTimeLimit: 0,
      isAdmin: this.props.userID == 'admin',
      height: 0,
      indexplacard: [],
      image: null,
      rechargeMethod: [],
      proxyMethod: [],
      addRechargeMethod: false,
      editRechargeMethod: false,
      value: '',
      send: '',
      status: 'enable',
      oldValue: '',

      addProxyMethod: false,
      editProxyMethod: false,
      value: '',
      index: '',
    };
  }

  // 首次加载数据
  async componentWillMount() {
    await this.reloadConfig();
  }

  handleResize = () => {
    let height = document.body.getBoundingClientRect().height;
    this.setState({ height });
  }

  async componentDidMount() {
    window.addEventListener("resize", this.handleResize);
    this.handleResize()
  }

  componentWillUnmount() {
    window.removeEventListener("resize", this.handleResize);
  }

  async reloadConfig() {
    let config = {};
    let ps = []
    for (let param of params) {
      ps.push(new Promise(async (resolve, reject) => {
        let api = param.userID ? 'userParams' : 'params';
        let res = await axios.get(`/api/consumer/${api}/${param.type || pageType}/get/${param.code}`);
        if (res.data.code) {
          config[`${param.type}-${param.code}`] = (param.unit ? (res.data.value / param.unit) : res.data.value);
        }
        resolve();
      }))
      await new Promise(resolve => setTimeout(resolve, 200));
    }
    // let ps = params.map();
    let res = await axios.get(`/api/consumer/params/${rechargeMethodParam.type || pageType}/get/${rechargeMethodParam.code}`);
    if (res.data.code) {
      this.setState({ rechargeMethod: (typeof res.data.value === 'string' ? JSON.parse(res.data.value) : res.data.value).map((e, index) => ({ ...e, index: index + 1 })) });
    }
    let res2 = await axios.get(`/api/consumer/params/${proxyMethodParam.type || pageType}/get/${proxyMethodParam.code}`);
    if (res2.data.code) {
      this.setState({ proxyMethod: (typeof res2.data.value === 'string' ? JSON.parse(res2.data.value) : res2.data.value).map((e, index) => ({ ...e, index: index + 1 })) });
    }
    await Promise.all(ps);
    this.setState({ config });
    if (config[`index-placard`]) {
      this.setState({ indexplacard: config[`index-placard`] })
    }
  }

  /**
   * 修改参数
   **/
  // async onParamsSetChange(type, value, key) {
  async onParamsSetChange(param, value) {
    this.setState({ loading: true });
    let data;
    let indexplacards = [];

    if (this.state.indexplacard) {
      for (let index of this.state.indexplacard) {
        indexplacards.push(index);
      }
    }

    if (param.code == 'placard' && value) {
      indexplacards.push(value);
      value = indexplacards;
    }

    switch (typeof value) {
      case 'boolean':
        data = { value };
        break;
      default:
        data = { value: param.unit ? (Number(value) * param.unit) : value }
    }
    data.userID = param.userID;
    let res = await axios.post(`/api/consumer/params/${param.type || pageType}/set/${param.code}`, data);
    if (res.data.code == 1) {
      // 修改配置后重新加载所有配置
      await this.reloadConfig();
      message.success('操作成功');
    } else {
      Modal.error({ title: res.data.message });
    }
    this.setState({ loading: false })
  }

  async deleteImage(param, url) {
    let data;
    let indexplacards = [];

    if (this.state.indexplacard) {
      for (let index of this.state.indexplacard) {
        if (url != index) {
          indexplacards.push(index);
        }
      }
    }

    data = { value: indexplacards }
    DialogApi.warning({
      title: '确定要删除这张图片？',
      content: '',
      onOkText: '确定',
      onCancelText: '取消',
      onOk: async () => {
        data.userID = param.userID;
        let res = await axios.post(`/api/consumer/params/${param.type || pageType}/set/${param.code}`, data);
        if (res.data.code == 1) {
          // 修改配置后重新加载所有配置
          await this.reloadConfig();
          message.success('删除成功');
        } else {
          message.error(res.data.message);
        }
      },
      onCancel() {
      }
    })
  }

  render() {
    const columns = [{
      title: '序号',
      dataIndex: 'index',
      key: 'index',
      width: 50,
    }, {
      title: '充值金额',
      dataIndex: 'value',
      key: 'value',
      width: 120,
    }, {
      title: '赠送金额',
      dataIndex: 'send',
      key: 'send',
      width: 120,
    }, {
      title: '启用',
      dataIndex: 'status',
      key: 'status',
      width: 80,
      render: (text, record) => {
        return <Switch size="medium" value={text === 'enable'} disabled={!this.state.isAdmin} onChange={(checked) => {
          record.status = checked ? 'enable' : 'disable';
          this.onParamsSetChange(rechargeMethodParam, JSON.stringify(this.state.rechargeMethod))
        }} />
      }
    }, {
      title: '操作',
      dataIndex: 'op',
      key: 'op',
      width: 100,
      render: (text, record) => {
        return <div>
          <Button type="link" onClick={() => {
            this.setState({
              editRechargeMethod: true,
              value: record.value,
              send: record.send,
              status: record.status,
              oldValue: record.value,
            })
          }}>编辑</Button>
          <Button type="link" onClick={() => {
            this.state.rechargeMethod = this.state.rechargeMethod.filter(e => e.value !== record.value)
            this.onParamsSetChange(rechargeMethodParam, JSON.stringify(this.state.rechargeMethod))
          }}>删除</Button>
        </div>
      }
    },]

    const columns2 = [{
      title: '代理层级',
      dataIndex: 'index',
      key: 'index',
      width: 50,
      render: (v) => {
        return v + "级"
      }
    }, {
      title: '返佣比例',
      dataIndex: 'value',
      key: 'value',
      width: 120,
    }, {
      title: '启用',
      dataIndex: 'status',
      key: 'status',
      width: 80,
      render: (text, record) => {
        return <Switch size="medium" value={text === 'enable'} disabled={!this.state.isAdmin} onChange={(checked) => {
          record.status = checked ? 'enable' : 'disable';
          this.onParamsSetChange(proxyMethodParam, JSON.stringify(this.state.proxyMethod))
        }} />
      }
    }, {
      title: '操作',
      dataIndex: 'op',
      key: 'op',
      width: 100,
      render: (text, record) => {
        return <div>
          <Button type="link" onClick={() => {
            this.setState({
              editProxyMethod: true,
              value: record.value,
              status: record.status,
              index: record.index
            })
          }}>编辑</Button>
          <Button type="link" onClick={() => {
            this.state.proxyMethod = this.state.proxyMethod.filter(e => e.index !== record.index)
            this.onParamsSetChange(proxyMethodParam, JSON.stringify(this.state.proxyMethod))
          }}>删除</Button>
        </div>
      }
    },]

    return (
      <MyTranslate><ExternalAccountFilter>
        <Breadcrumb>
          <Breadcrumb.BreadcrumbItem>系统设置</Breadcrumb.BreadcrumbItem>
          <Breadcrumb.BreadcrumbItem>全局参数设置</Breadcrumb.BreadcrumbItem>
        </Breadcrumb>


        <div style={{ marginLeft: 50, marginTop: 50, color: 'rgba(0, 0, 0, 0.90)', height: this.state.height - 160, overflow: 'auto' }}>
          {params.map(param =>
            <div style={{ display: 'flex', marginBottom: 24 }}>
              <div style={{ width: 300, textAlign: 'right', marginRight: 16, lineHeight: '30px' }}>{param.desc}</div>
              <div>
                {
                  (() => {
                    switch (param.itemType) {
                      case 'switch':
                        return <Switch size="medium" value={this.state.config[`${param.type}-${param.code}`]} disabled={!this.state.isAdmin} onChange={(checked) => {
                          this.onParamsSetChange(param, checked)
                        }} />;
                      case 'file':
                        return <TUpload
                          {...getUploadProps('txt')}
                          disabled={!this.state.isAdmin}
                          files={this.state.config[`${param.type}-${param.code}`] ? [{
                            uid: uuid() + '',
                            name: `${param.type}-${param.code}.txt`,
                            status: 'done',
                            response: '{"status": "success"}',
                            url: '/api/consumer/res/download/' + this.state.config[`${param.type}-${param.code}`]
                          }] : []}
                          onChange={
                            (info) => {
                              if (info.length > 0) {
                                this.setState({ config: { ...this.state.config, [`${param.type}-${param.code}`]: info[0].response.data.filepath } });
                                this.onParamsSetChange(param, info[0].response.data.filepath)
                                return
                              }
                            }
                          }
                          onRemove={() => this.setState({ config: { ...this.state.config, [`${param.type}-${param.code}`]: null } })}
                          autoUpload
                          showUploadProgress
                          theme="file"
                          useMockProgress
                        />
                      case 'image':
                        return (<div style={{ display: 'flex', paddingTop: 15 }}>
                          <Space breakLine>
                            {this.state.indexplacard.length == 0 && (
                              <div style={{ display: 'flex', width: '130px' }}>
                                {BasicImageViewer(indexImage, 110, 110)}
                              </div>
                            )}
                            {this.state.indexplacard.map((ws) => {
                              return (
                                <div style={{ display: 'flex', width: '130px' }}>
                                  {BasicImageViewer('/api/consumer/res/download/' + ws, 110, 110)}
                                  <Button
                                    onClick={() => this.deleteImage(param, ws)}
                                    style={{
                                      height: '24px',
                                      borderRadius: '50%',
                                      backgroundColor: '#f3f3f3',
                                      color: 'red',
                                      cursor: 'pointer',
                                      display: 'flex',
                                      alignItems: 'center',
                                      justifyContent: 'center',
                                      transform: 'translate(-45%, -55%)',
                                      border: 'none'
                                    }}
                                  >
                                    <CloseIcon />
                                  </Button>
                                </div>
                              )
                            })}
                          </Space>
                          <TUpload
                            {...getUploadImageProps()}
                            disabled={!this.state.isAdmin}
                            files={this.state.image ? [this.state.image] : []}
                            onChange={
                              (info) => {
                                console.log('====CQ', info)
                                if (info.length > 0) {
                                  for (let file of info) {
                                    if (file.response) {
                                      this.setState({ config: { ...this.state.config, [`${param.type}-${param.code}`]: info[0].response.data.filepath } });
                                      this.onParamsSetChange(param, info[0].response.data.filepath)
                                      return
                                    }
                                  }
                                }
                              }
                            }
                            theme="image"
                            style={{ marginLeft: 20 }}
                            onRemove={() => this.setState({ image: null })}
                          />
                        </div>)
                      default:
                        return <Input disabled={!this.state.isAdmin} type='number' value={this.state.config[`${param.type}-${param.code}`]} {...param}
                          style={{ width: 400 }}
                          onChange={(e) => {
                            this.setState({ config: { ...this.state.config, [`${param.type}-${param.code}`]: e.target.value } })
                          }}
                          onPressEnter={(e) => {
                            this.onParamsSetChange(param, e.target.value)
                          }} />
                    }
                  })()
                }
              </div>
            </div>
          )}
          <SysPermissionButton permission="buttonTest">测试按钮</SysPermissionButton>
          <div>
            <div>充值阶梯价格设置&nbsp;<Button type="link" onClick={() => {
              this.setState({ addRechargeMethod: true })
            }}>新增</Button></div>
            <div className="tableContent accountTableContent" style={{ marginTop: 20, height: 'auto' }}>
              <Table
                size="middle"
                tableLayout="fixed"
                columns={columns}
                dataSource={this.state.rechargeMethod}
                loading={this.state.loading}
              />
            </div>
          </div>

          {/* <div>
        <div>返佣比例设置&nbsp;<Button type="link" onClick={() => {
              this.setState({addProxyMethod: true})
            }}>新增</Button></div>
        <div className="tableContent accountTableContent" style={{ marginTop: 20, height: 'auto' }}>
          <Table
            size="middle"
            tableLayout="fixed"
            columns={columns2}
            dataSource={this.state.proxyMethod}
            loading={this.state.loading}
          />
        </div>
      </div> */}


          <Dialog
            header={this.state.editRechargeMethod ? "编辑" : "新增"}
            visible={this.state.editRechargeMethod || this.state.addRechargeMethod}
            onConfirm={() => {
              if (this.state.value === '') {
                message.error('充值金额不能为空')
                return
              }
              if (this.state.send === '') {
                message.error('赠送金额不能为空')
                return
              }
              if (this.state.addRechargeMethod) {
                console.log(this.state.rechargeMethod, this.state.value)
                if (this.state.rechargeMethod.filter(e => e.value === Number(this.state.value)).length > 0) {
                  message.error('充值金额不能重复')
                  return
                }
              }
              if (this.state.editRechargeMethod) {
                if (this.state.rechargeMethod.filter(e => e.value === Number(this.state.value) && e.value !== Number(this.state.oldValue)).length > 0) {
                  message.error('充值金额不能重复')
                  return
                }
                this.state.rechargeMethod = this.state.rechargeMethod.filter(e => e.value !== Number(this.state.oldValue))
              }
              this.state.rechargeMethod = this.state.rechargeMethod.filter(e => e.value !== Number(this.state.value))
              this.state.rechargeMethod.push({
                value: Number(this.state.value),
                send: Number(this.state.send),
                status: this.state.status,
              })
              // 按value排序
              this.state.rechargeMethod.sort((a, b) => {
                return a.value - b.value
              })
              this.onParamsSetChange(rechargeMethodParam, JSON.stringify(this.state.rechargeMethod))
              this.setState({ editRechargeMethod: false, addRechargeMethod: false, value: '', send: '', status: 'enable' })
            }} confirmLoading={this.state.loading}
            onCancel={() => {
              this.setState({ editRechargeMethod: false, addRechargeMethod: false, value: '', send: '', status: 'enable' })
            }}
            onClose={() => {
              this.setState({ editRechargeMethod: false, addRechargeMethod: false, value: '', send: '', status: 'enable' })
            }}
          >
            <div>
              充值金额
              <Input onChange={(e) => {
                this.setState({ value: e.target.value })
              }} value={this.state.value}
                type="text" />
            </div>
            <div>
              赠送金额
              <Input onChange={(e) => {
                this.setState({ send: e.target.value })
              }} value={this.state.send}
                type="text" />
            </div>
            <div>
              是否启用
              <Switch
                value={this.state.status === 'enable'}
                onChange={(checked) => {
                  this.setState({ status: checked ? 'enable' : 'disable' })
                }}
              />
            </div>
          </Dialog>


          <Dialog
            header={this.state.editProxyMethod ? "编辑" : "新增"}
            visible={this.state.editProxyMethod || this.state.addProxyMethod}
            onConfirm={() => {
              if (this.state.value === '') {
                message.error('返佣比例不能为空')
                return
              }
              if (this.state.addProxyMethod) {
                this.state.proxyMethod.push({
                  value: Number(this.state.value),
                  status: this.state.status,
                  index: this.state.proxyMethod.length + 1
                })
              }
              if (this.state.editProxyMethod) {
                this.state.proxyMethod[this.state.index - 1].value = Number(this.state.value)
                this.state.proxyMethod[this.state.index - 1].status = this.state.status
              }
              // 按value排序
              this.state.proxyMethod.sort((a, b) => {
                return a.index - b.index
              })
              this.onParamsSetChange(proxyMethodParam, JSON.stringify(this.state.proxyMethod))
              this.setState({ editProxyMethod: false, addProxyMethod: false, value: '', index: '', status: 'enable' })
            }} confirmLoading={this.state.loading}
            onCancel={() => {
              this.setState({ editProxyMethod: false, addProxyMethod: false, value: '', index: '', status: 'enable' })
            }}
            onClose={() => {
              this.setState({ editProxyMethod: false, addProxyMethod: false, value: '', index: '', status: 'enable' })
            }}
          >
            <div>
              返佣比例
              <Input onChange={(e) => {
                this.setState({ value: e.target.value })
              }} value={this.state.value}
                type="text" />
            </div>
            <div>
              是否启用
              <Switch
                value={this.state.status === 'enable'}
                onChange={(checked) => {
                  this.setState({ status: checked ? 'enable' : 'disable' })
                }}
              />
            </div>
          </Dialog>
        </div>
      </ExternalAccountFilter></MyTranslate>)
  }
}

export default connect(({ user }) => ({
  userID: user.info.userID,
  name: user.info.name,
}))(injectIntl(MyComponent))
