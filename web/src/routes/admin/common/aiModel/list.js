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
  Button,
  Modal,
  Select,
  Radio
} from 'antd'
import axios from 'axios'
import {formatDate} from 'components/DateFormat'
import ExternalAccountFilter from "components/common/ExternalAccountFilter";
import MyTranslate from "components/common/MyTranslate";
import { Breadcrumb, Dialog, Switch} from 'tdesign-react';
import {connect} from 'dva'

const Search = Input.Search
const confirm = Modal.confirm
const { Option } = Select;

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
      aiModels: [],
      searchModel: '',
      editUser: null,
      model: '',
      type: 'by_token',
      countPrice: '',
      inputTokenPrice: '',
      outputTokenPrice: '',
      sortNo: 0,
      status: 'enable',
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
    this.sorter = {sortNo: -1, createTime: -1}
    // 提前写在constructor则Table首次加载时生效
    // 但是因为没有放到state中，所以除sorter外，都不建议提前设置，保持不受控状态是最好的
  }

  // 首次加载数据
  async componentWillMount() {
    this.reload()
    this.loadAiModels()
  }

  async reload() {
    // 重新加载，一般是页面第一次加载的时候来一下
    this.load(this.state.pagination, this.filters, this.sorter)
  }

  async loadAiModels() {
    let res = await axios.get(`/api/aiModelPrice/model/list`)
    this.setState({aiModels: res.data.data})
  }

  async reset() {
    this.filters = {}
    this.setState({ filters: {}, userID: '', searchModel: '' })
    this.load(this.state.pagination, this.filters, this.sorter)
  }

  async load(pagination, filters, sorter) {
    // 这里的参数是格式好的，无需修改，无阻抗失衡，直接发给服务器
    this.setState({loading: true})
    let res = await axios.post(`/api/aiModelPrice/list/${pagination.pageSize}/${pagination.current}`, {filters, sorter})
    pagination.total = res.data.data.total
    this.setState({loading: false, data: res.data.data.data, pagination, selectedRowKeys: []})
    this.selectedRows = []
    this.filters = filters
    this.sorter = sorter
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

  async _delete (id) {
    let data = {ids: id ? [id] : this.state.selectedRowKeys}
    confirm({
      zIndex: 9999,
      title: `确定要删除这些数据？`,
      content: '',
      okText: '确定',
      okType: 'danger',
      cancelText: '取消',
      onOk: async () => {
        this.setState({loading: true});
        let res = await axios.post(`/api/aiModelPrice/delete`, data, {
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
    this.setState({addVisible: true, editUser: null, status: 'enable', type: 'by_token', model: '', inputTokenPrice: '', outputTokenPrice:'',countPrice:'',sortNo:0})
  }

  // 把columns放到render中，虽然损失部分性能，但是能方便参数中的匿名回调获取实例状态
  render() {
    const columns = [
      {
        title: '名称',
        dataIndex: 'model',
        key: 'model',
      },
      {
        title: '价格($)',
        dataIndex: 'type',
        key: 'type',
        render: (v, r) => {
          return v == 'by_count' ? <div>{r.countPrice + '/次'}</div> : <div>
            输入: {r.inputTokenPrice + '/M'} <br/>
            输出: {r.outputTokenPrice + '/M'}
          </div>
        }
      },
      {
        title: '状态',
        dataIndex: 'status',
        key: 'status',
        render: (v, r) => {
          return v == 'enable' ? '上架' : '下架'
        }
      },
      {
        title: '排序',
        dataIndex: 'sortNo',
        key: 'sortNo',
      },
      {
        title: '创建时间',
        dataIndex: 'createTime',
        key: 'createTime',
        render: formatDate
      },
      {
        title: '操作',
        dataIndex: 'oper',
        key: 'oper',
        render: (v, r) => {
          return <div style={{
              display: 'flex',
              alignItems: 'center',
              gap: '12px', // 按钮间距
              padding: '8px 0 8px 8px', // 上下内边距8px，左侧内边距16px
              marginLeft: '8px' // 增加左侧外边距
            }}>
              <Button type="link"
                style={{
                  padding: 0,
                  minWidth: 'auto',
                }} onClick={() => {
                  this.setState({editUser: r, model: r.model, type: r.type, countPrice: r.countPrice, inputTokenPrice: r.inputTokenPrice, 
                    outputTokenPrice: r.outputTokenPrice, sortNo: r.sortNo, status: r.status, addVisible: true})
                }}>编辑</Button>
              <Button type="link"
                style={{
                  padding: 0,
                  minWidth: 'auto',
                }} onClick={() => {
                  this._delete(r._id)
                }}>删除</Button>
            </div>
        }
      },

    ];

    return (<MyTranslate><ExternalAccountFilter>
              <Breadcrumb>
                <Breadcrumb.BreadcrumbItem>系统设置</Breadcrumb.BreadcrumbItem>
                <Breadcrumb.BreadcrumbItem>模型管理</Breadcrumb.BreadcrumbItem>
              </Breadcrumb>

      <Dialog
        header={this.state.editUser ? "编辑" : "新增"}
        width={854}
        height={566}
        visible={this.state.addVisible}
        placement='center'
        onConfirm={async () => {
          if (!this.state.model) {
            message.error('请选择模型')
            return
          }
          if (!this.state.type) {
            message.error('请选择计费方式')
            return
          }
          if (this.state.type === 'by_count') {
            if (!this.state.countPrice || isNaN(this.state.countPrice)) {
              message.error('请输入正确单价')
              return
            }
          } else {
            if (!this.state.inputTokenPrice || isNaN(this.state.inputTokenPrice)) {
              message.error('请输入正确输入价格')
              return
            }
            if (!this.state.outputTokenPrice || isNaN(this.state.outputTokenPrice)) {
              message.error('请输入正确输出价格')
              return
            }
          }
          if (isNaN(this.state.sortNo)) {
            message.error('请输入正确排序')
            return
          }
          let form = {
            model: this.state.model,
            type: this.state.type,
            countPrice: this.state.countPrice,
            inputTokenPrice: this.state.inputTokenPrice,
            outputTokenPrice: this.state.outputTokenPrice,
            sortNo: this.state.sortNo,
            status: this.state.status,
          };
          if (this.state.editUser) {
            form._id = this.state.editUser._id
          }
          this.setState({loading: true});
          // 过滤掉null
          let res = await axios.post(form._id ? `/api/aiModelPrice/update` : `/api/aiModelPrice/add`, form)
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
            <span style={{color: '#D54941'}}>*</span>模型名称
            </div>
            <div>
              <Select disabled={this.state.editUser} value={this.state.model} style={{ width: 200 }} onChange={v => {
                this.setState({ model: v })
              }}>
                {this.state.aiModels.map(ws => {
                return <Option value={ws}>{ws}</Option>
                })}
              </Select>
            </div>
          </div>

          <div style={{display: 'flex', marginBottom: 24}}>
            <div style={{width: 100, textAlign: 'right', marginRight: 16, lineHeight: '30px'}}>
            <span style={{color: '#D54941'}}>*</span>计费方式
            </div>
            <div style={{ width: 400, marginLeft: 16 }}>
              <Radio.Group value={this.state.type} onChange={(e) => {
                this.setState({ type: e.target.value })
              }}>
                <Radio value={"by_token"}>按token</Radio>
                <Radio value={"by_count"}>按次</Radio>
              </Radio.Group>
            </div>
          </div>

          {this.state.type == 'by_token' ? <>
            <div style={{display: 'flex', marginBottom: 24}}>
              <div style={{width: 100, textAlign: 'right', marginRight: 16, lineHeight: '30px'}}>
              <span style={{color: '#D54941'}}>*</span>输入价格
              </div>
              <div>
                <Input value={this.state.inputTokenPrice}
                      onChange={(e) => this.setState({inputTokenPrice: e.target.value})} style={{width: 400}}
                      placeholder='请输入'/> $/M token
              </div>
            </div>

            <div style={{display: 'flex', marginBottom: 24}}>
              <div style={{width: 100, textAlign: 'right', marginRight: 16, lineHeight: '30px'}}>
              <span style={{color: '#D54941'}}>*</span>输出价格
              </div>
              <div>
                <Input value={this.state.outputTokenPrice}
                      onChange={(e) => this.setState({outputTokenPrice: e.target.value})} style={{width: 400}}
                      placeholder='请输入'/> $/M token
              </div>
            </div>
            </> : <><div style={{display: 'flex', marginBottom: 24}}>
              <div style={{width: 100, textAlign: 'right', marginRight: 16, lineHeight: '30px'}}>
              <span style={{color: '#D54941'}}>*</span>价格
              </div>
              <div>
                <Input value={this.state.countPrice}
                      onChange={(e) => this.setState({countPrice: e.target.value})} style={{width: 400}}
                      placeholder='请输入'/> $/次
              </div>
            </div></>}

          <div style={{display: 'flex', marginBottom: 24}}>
            <div style={{width: 100, textAlign: 'right', marginRight: 16, lineHeight: '30px'}}>
            <span style={{color: '#D54941'}}>*</span>排序
            </div>
            <div>
              <Input value={this.state.sortNo}
                     onChange={(e) => this.setState({sortNo: e.target.value})} style={{width: 400}}
                     placeholder='请输入'/>
            </div>
          </div>

          <div style={{display: 'flex', marginBottom: 24}}>
            <div style={{width: 100, textAlign: 'right', marginRight: 16, lineHeight: '30px'}}>
            <span style={{color: '#D54941'}}>*</span>是否上架
            </div>
            <div>
              <Switch size="medium" value={this.state.status==='enable'} onChange={(value) => {
                this.setState({
                  status: value?'enable':'disable',
                })
              }} />
              <div style={{color: 'rgba(0,0,0,0.4)', fontSize: 12,}}>关闭后，用户无法使用该模型</div>
            </div>
          </div>
        </div>
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

      <div className="search-box">
        <div className='search-item'>
          <div className="search-item-label">模型</div>
          <div className="search-item-right">
            <Select value={this.state.searchModel} style={{ width: 200 }} onChange={v => {
              this.setState({ searchModel: v })
              this.filters['model'] = v
              this.reload()
            }}>
              {this.state.aiModels.map(ws => {
               return <Option value={ws}>{ws}</Option>
              })}
            </Select>
          </div>
        </div>


        <div className='accountGroup-btn'>
          <div className="search-query-btn" onClick={() => this.reload()}>查询</div>
          <div className="search-reset-btn" onClick={() => this.reset()}>重置</div>
        </div>
      </div>

      <div className="table-operations">
        <div className={"search-query-btn"} onClick={this.add.bind(this)}>新增</div>
        <div className={this.state.selectedRowKeys && this.state.selectedRowKeys.length > 0 ? "search-delete-btn" : "search-reset-btn"}
              onClick={() => {
                this.delete()
              }}>批量删除</div>
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
