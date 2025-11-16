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
  Modal
} from 'antd'
import axios from 'axios'
import {formatDate} from 'components/DateFormat'
import ExternalAccountFilter from "components/common/ExternalAccountFilter";
import MyTranslate from "components/common/MyTranslate";
import { Breadcrumb, Dialog} from 'tdesign-react';
import {connect} from 'dva'

const Search = Input.Search
const confirm = Modal.confirm

const pageType = 'apiToken';
// 针对当前页面的基础url
const baseUrl = `/api/consumer/user/${pageType}`;
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
        pageSizeOptions: ['10', '20', '30', '40', '50', '100', '200', '300', '400', '500'],
      },
      config: {},
      selectedRowKeys: [],
      ip: '',//设置ip
      payoutCallbackUrl: '',
      depositCallbackUrl: '',
      previewVisible: false,
      code: '',
      addVisible: false,
      receiveCallbackUrl: '',
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
    this.reloadConfig();
    this.reload()
  }

  async reload() {
    // 重新加载，一般是页面第一次加载的时候来一下
    this.load(this.state.pagination, this.filters, this.sorter)
  }

  async load(pagination, filters, sorter) {
    // 这里的参数是格式好的，无需修改，无阻抗失衡，直接发给服务器
    this.setState({loading: true})
    let res = await axios.post(`/api/apiKey/list/${pagination.pageSize}/${pagination.current}`, {filters, sorter})
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
    if (this.state.selectedRowKeys.length == 0) {
      message.error('请先选择数据');
      return
    }
    data = {ids: this.state.selectedRowKeys}
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
            message.success('处理成功');
            this.reload();
        }
      }
    })
  }

  async setIp(r) {
    let ips = r.whiteIp ? r.whiteIp.split(',').join('\n') : ''
    let receiveCallbackUrl = r.receiveCallbackUrl;
    this.setState({ip: ips, receiveCallbackUrl})

    let data = {id: r._id}
    let that = this
    Modal.info({
      maskClosable: true,
      title: '请输入IP,*代表不限制,一行一个',
      icon: null,
      content: <div><Input.TextArea defaultValue={ips} rows={4} onChange={(e) => {
        that.setState({ip: e.target.value})
      }}/>
      {/* <div>
        接收消息回调地址：
        <Input defaultValue={receiveCallbackUrl} onChange={(e) => {
          that.setState({receiveCallbackUrl: e.target.value})
        }}/>
      </div> */}
      </div>,
      width: 800,
      okText: '保存',
      onOk: async () => {
        let ips = this.state.ip.split('\n').filter(v => v.trim().length > 0).map(v => v.trim());
        data.whiteIp = [...new Set(ips)].join(',');
        data.receiveCallbackUrl = this.state.receiveCallbackUrl;
        let res = await axios.post(`/api/apiKey/update`, data);
        if (res.data.code == 1) {
          this.setState({ip: ''});
          this.reload();
        }
      },
      onCancel: () => {
      }
    });

  }

  async delete () {
    if (this.state.selectedRowKeys.length == 0) {
      message.error('请先选择数据');
      return
    }
    if (this.props.setSecretKey) {
      this.setState({previewVisible: true})
    } else {
      this._delete()
    }
  }

  async _delete () {
    let data = {ids: this.state.selectedRowKeys}
    confirm({
      zIndex: 9999,
      title: `确定要删除这些数据？`,
      content: '',
      okText: '确定',
      okType: 'danger',
      cancelText: '取消',
      onOk: async () => {
        this.setState({loading: true});
        let res = await axios.post(`/api/apiKey/delete`, data, {
          headers: {
            'auth-code': this.state.code, 
          }
        });
        if (res.data.code == 1) {
          message.success('处理成功');
          this.setState({previewVisible: false})
          this.reload();
        } else {
          this.setState({loading: false})
          message.error(res.data.message);
        }
        
      }
    })
  }

  async add () {
    let ips = ''
    this.setState({ip: ips, depositCallbackUrl: '', payoutCallbackUrl: '', addVisible: true})
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

  save = async () => {
    let data = {}
    let ips = this.state.ip.split('\n').filter(v => v.trim().length > 0).map(v => v.trim());
    data.whiteIp = [...new Set(ips)].join(',');
    data.receiveCallbackUrl = this.state.receiveCallbackUrl;
    let res = await axios.post(`/api/apiKey/add`, data, {
      headers: {
        'auth-code': this.state.code, 
      }
    });
    if (res.data.code == 1) {
      Modal.info({
        maskClosable: true,
        title: '新增成功,请复制以下信息',
        icon: null,
        content: <div>
          <p>账号：{res.data.data.userID}</p>
          <p>APIKEY：{res.data.data.apiKey}<Button style={{marginLeft: '20px'}} type="primary" onClick={async () => {
                                  try {
                                      await navigator.clipboard.writeText(res.data.data.apiKey);
                                      // Modal.success({title: '复制成功'});
                                      message.success('复制成功');
                                  } catch (err) {
                                      console.error('复制失败: ', err);
                                      message.error('复制失败');
                                  }
                              }
          
                              }>复制</Button></p>
          <p>APISecret: {res.data.data.apiSecret}<Button style={{marginLeft: '20px'}} type="primary" onClick={async () => {
                                  try {
                                      await navigator.clipboard.writeText(res.data.data.apiSecret);
                                      // Modal.success({title: '复制成功'});
                                      message.success('复制成功');
                                  } catch (err) {
                                      console.error('复制失败: ', err);
                                      message.error('复制失败');
                                  }
                              }
          
                              }>复制</Button></p>
          <p>授权访问IP：{res.data.data.whiteIp}</p>
        </div>,
        width: 800,
        okText: '确认已保存',
        onOk: async () => {
        },
        onCancel: () => {
        }
      });
      this.setState({ip: '', addVisible: false, previewVisible: false});
      this.reload();
    } else {
      message.error(res.data.message);
    }
  }

  // 把columns放到render中，虽然损失部分性能，但是能方便参数中的匿名回调获取实例状态
  render() {
    const columns = [
      {
        title: '账号',
        dataIndex: 'userID',
        key: 'userID',
      },
      {
        title: 'APIKEY',
        dataIndex: 'apiKey',
        key: 'apiKey',
      },
      {
        title: '授权访问IP',
        dataIndex: 'whiteIp',
        key: 'whiteIp',
      },
      {
        title: '操作',
        dataIndex: 'oper',
        key: 'oper',
        render: (v, r) => {
          return <Button type="primary" onClick={this.setIp.bind(this, r)}>设置IP</Button>
        }
      },

    ];

    return (<MyTranslate><ExternalAccountFilter>
              <Breadcrumb>
                <Breadcrumb.BreadcrumbItem>系统设置</Breadcrumb.BreadcrumbItem>
                <Breadcrumb.BreadcrumbItem>API管理</Breadcrumb.BreadcrumbItem>
              </Breadcrumb>

      <Dialog
        header="请输入IP,不填代表不限制,一行一个"
        visible={this.state.addVisible}
        onConfirm={async () => {this.props.setSecretKey ? this.setState({previewVisible: true}) : this.save()}} confirmLoading={this.state.loading}
        onCancel={()=>{this.setState({addVisible:false})}}
        onClose={()=>{this.setState({addVisible:false})}}>
          <div><Input.TextArea value={this.state.ip} rows={4} onChange={(e) => {
            this.setState({ip: e.target.value})
          }}/>
          </div>
          {/* <div>
          接收消息回调地址：
          <Input value={this.state.receiveCallbackUrl} onChange={(e) => {
            this.setState({receiveCallbackUrl: e.target.value})
          }}/>
        </div> */}
        </Dialog>

      <Dialog header="2FA身份验证" visible={this.state.previewVisible} footer={null}
              onCancel={() => {
                this.setState({previewVisible: false})
              }}
              onClose={() => {
                this.setState({previewVisible: false})
              }}
              >
        <div style={{width: 300, margin: '50px auto', textAlign: 'center'}}>
        <div style={{color:"#333"}}>请输入验证器生成的6位验证码</div>
        <Input onChange={(e) => this.setState({code: e.target.value})} style={{margin: "30px auto"}} placeholder="请输入验证器生成的6位验证码" />
        <Button style={{margin: "30px auto",width: 200}} type="primary" loading={this.state.loading} onClick={() => { this.state.addVisible ? this.save() : this._delete()}} >提交</Button>
        </div>
      </Dialog>

      <div className="table-operations">
        {
          [
            // {placeholder: '输入批次号', dataIndex: 'batchid',}
          ].map(v =>
            <Search {...formItemLayout} {...v} onSearch={(value) => {
              this.filters[v.dataIndex] = value;
              this.reload()
            }}/>
          )
        }
      </div>
      <Row type='flex' align='middle'>
        {params.map(param =>
          <Col {...colLayou}>
            {param.desc}:
            <Input type='number' placeholder={Number(this.state.config[param.code]) / (param.unit || 1)} {...param}
                   onPressEnter={(e) => {
                     this.onParamsSetChange(param.code, Number(e.target.value) * (param.unit || 1), param.type)
                   }} style={{width: 150}}/>
          </Col>
        )}
      </Row>
      <div className="table-operations">
        <div className={"search-query-btn"} onClick={this.reload.bind(this)}>刷新</div>
        <div className={"search-query-btn"} onClick={this.add.bind(this)}>新增</div>
        <div className={"search-query-btn"} onClick={this.delete.bind(this)}>删除</div>
      </div>
      <Table pagination={this.state.pagination} rowSelection={{
        selectedRowKeys: this.state.selectedRowKeys,
        onChange: this.onRowSelectionChange.bind(this)
      }} columns={columns} rowKey='_id' dataSource={this.state.data}
             loading={this.state.loading} onChange={this.handleTableChange.bind(this)}/>
    </ExternalAccountFilter></MyTranslate>)
  }
}

export default connect(({user}) => ({
  setSecretKey: user.info.setSecretKey,
}))(MyComponent)
