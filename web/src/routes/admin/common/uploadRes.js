import React, {Component} from 'react'
import moment from 'moment';
import iconv from 'iconv-lite';
import {BrowserRouter as Router, Route} from 'react-router-dom'
import {
  Button,
  Icon,
  Table,
  Divider,
  Upload,
  message,
  Input,
  Modal,
  Select,
  InputNumber,
  Radio,
  Tooltip,
  Alert,
  Tabs
} from 'antd'
import axios from 'axios'
import {formatDate} from 'components/DateFormat'
import TimerWrapper from "../../../components/TimerWrapper";
import CreateSocks5 from "../common/setButtons/CreateSocks5"
import {connect} from 'dva'
import { Pagination, Breadcrumb, Dialog, Tag,Dropdown ,Link,Space} from 'tdesign-react';
import ExternalAccountFilter from "../../../components/common/ExternalAccountFilter";
import MyTranslate from "../../../components/common/MyTranslate";
import {FormattedMessage, useIntl, injectIntl} from 'react-intl'
import DialogApi from "../common/dialog/DialogApi";
const {TabPane} = Tabs;

const {Option} = Select
const Search = Input.Search;
const confirm = Modal.confirm;

// 针对当前页面的基础url
const baseUrl = '/api/consumer/uploadRes';

const uploadProps = {
  name: 'file',
  multiple: false,
  action: `/api/consumer/res/upload`,
};

const socks5Params = ['ip','port', 'username', 'password', 'desc'];
const encodingsToTry = ['UTF-8', 'GBK', 'GB2312'];

const resTypes = [
  {label: '昵称库', value: 'nickname'},
  {label: '签名库', value: 'signature'},
  // {label: '朋友圈库', value: 'timeline'},
  // {label: '朋友圈文图库', value: 'timelineMixture'},
  {label: '头像库', value: 'avatar'},
  // {label: '背景图片库', value: 'backgroundImage'},
  // {label: 'ID库', value: 'alias'},
];

class MyComponent extends Component {
  constructor(props) {
    super(props);
    // data由服务器返回表格的数据
    // loading由客户端控制
    // pagination控制表格分页功能，其中数据总数pagination.total由服务器返回
    // 以上变量受控，因为需要传递给Table控件
    this.state = {
      restype: 'nickname',
      choosenTab: 'nickname',
      fileList: [],
      addView: false,
      data: [],
      signatureData:[],
      avatarData:[],
      loading: false,
      isAlive: '',
      pagination: {
        pageSize: 10,
        current: 1,
        showTotal: (total, range) => `共 ${total} 条`,
        position: 'both',
        showSizeChanger: true,
        pageSizeOptions: ['10', '20', '30', '40', '50', '100', '200', '300', '400', '500', '1000', '2000']
      },
      signaturePagination: {
        pageSize: 10,
        current: 1,
        showTotal: (total, range) => `共 ${total} 条`,
        position: 'both',
        showSizeChanger: true,
        pageSizeOptions: ['10', '20', '30', '40', '50', '100', '200', '300', '400', '500', '1000', '2000']
      },
      avatarPagination: {
        pageSize: 10,
        current: 1,
        showTotal: (total, range) => `共 ${total} 条`,
        position: 'both',
        showSizeChanger: true,
        pageSizeOptions: ['10', '20', '30', '40', '50', '100', '200', '300', '400', '500', '1000', '2000']
      },
      nicknameSelectedRowKeys: [],
      avatarSelectedRowKeys: [],
      signatureSelectedRowKeys: [],
      tableData: [],
      users: [],
      userVisible: false,
      userID: '',
      platformVisible: false,
      platform: '',

      scrollY1: 0,
      scrollY2: 0,
      belongUser: '全部',
      userID2Name: {},
      userRadioOptions: [],
      maxConnections: 5, // 初始化最大连接数
    };
    // 选中行的数据保存在selectedRows变量中
    // 之所以不放入state，因为这些变量是单向流动的，即Table控件产生这些数据，通过callback更新到变量this.selectedRows
    this.signatureSelectedRows = []
    this.nicknameSelectedRows = []
    this.avatarSelectedRows = []
    // filters的写法保持与mongo filter的写法一致，避免与服务器、数据库直接的数据阻抗失衡
    // 例如： filters={username: 'foo'}
    // 因为json协议无法传输regex，所以字符类型的值，都会在服务器端转成regex，以提高匹配度，损失部分查询性能
    this.filters = {}
    // sorter的写法保持与mongo sort的写法一致，避免与服务器、数据库直接的数据阻抗失衡
    // 例如：sorter={createTime: -1}
    // 注意：Table控件仅支持单列排序，不支持多列同时排序
    this.sorter = {
      createTime: -1
    }
    // 提前写在constructor则Table首次加载时生效
    // 但是因为没有放到state中，所以除sorter外，都不建议提前设置，保持不受控状态是最好的
  }

  // 首次加载数据
  async componentWillMount() {
    this.reload();
    this.signatureReload();
    this.avatarReload();
  }
  async addRes(){
    if (this.state.resname.trim() === "") {
      message.error(this.props.intl.formatMessage({id: '请输入{value}'}, {
        value: this.props.intl.formatMessage({id: '资源名称'}),
      }));
      return;
    }
    if(this.state.fileList.length === 0){
      message.error(this.props.intl.formatMessage({id: '请先上传文件'}));
      return;
    }

    this.setState({loading: true});
    try{
      let result = await axios.post(`${baseUrl}/addRes`, {
        restype: this.state.restype,
        resname: this.state.resname.trim(),
        basename: this.state.fileList[0].response.basename,
        filename: this.state.fileList[0].name
      });
      this.setState({loading: false});
      if(result.data.code !== 1){
        Modal.error({
          title: this.props.intl.formatMessage({id: '失败'}),
          content: result.data.message || "unknown",
        });
      }else{
        message.success(this.props.intl.formatMessage({id: '提交成功'}));
        this.setState({fileList: [], resname: ""});
        this.reload();
      }
    }catch (e) {
      this.setState({loading: false});
      Modal.error({
        title: this.props.intl.formatMessage({id: '错误'}),
        content: e.message || "unknown",
      });
    }
    this.setState({addView:false})
  }

  async reload() {
    // 重新加载，一般是页面第一次加载的时候来一下
    this.load(this.state.pagination, this.filters, this.sorter,'nickname')
  }
  async signatureReload() {
    // 重新加载，一般是页面第一次加载的时候来一下
    this.load(this.state.signaturePagination, this.filters, this.sorter,'signature')
  }
  async avatarReload() {
    // 重新加载，一般是页面第一次加载的时候来一下
    this.load(this.state.avatarPagination, this.filters, this.sorter,'avatar')
  }
  async load(pagination, filters, sorter,restype) {
    // 这里的参数是格式好的，无需修改，无阻抗失衡，直接发给服务器
    this.setState({loading: true});
    let res = await axios.post(`${baseUrl}/${pagination.pageSize}/${pagination.current}`, {filters, sorter, restype:restype});
    if(restype === 'nickname'){
      pagination.total = res.data.total;
      this.setState({
        loading: false,
        data: res.data.data,
        pagination:pagination,
        //nicknameSelectedRowKeys: []
      });
      //this.nicknameSelectedRows = [];
    }else if (restype === 'signature'){
      pagination.total = res.data.total;
      this.setState({
        loading: false,
        signatureData: res.data.data,
        signaturePagination:pagination,
        //signatureSelectedRowKeys: []
      });
      //this.signatureSelectedRows = [];
    }else{
      pagination.total = res.data.total;
      this.setState({
        loading: false,
        avatarData: res.data.data,
        avatarPagination:pagination,
        //avatarSelectedRowKeys: []
      });
      //this.avatarSelectedRows = [];
    }

    this.filters = filters;
    this.sorter = sorter
  }


  async onRowSelectionChange(selectedRowKeys, selectedRows) {
    // 选中状态的数据，因为无需受控，就不记录在state里了，提高效率
    if(this.state.choosenTab === 'nickname'){
      this.setState({nicknameSelectedRowKeys:selectedRowKeys})
      this.nicknameSelectedRows = selectedRows
    }else if (this.state.choosenTab === 'signature'){
      this.setState({signatureSelectedRowKeys:selectedRowKeys})
      this.signatureSelectedRows = selectedRows
    }else{
      this.setState({avatarSelectedRowKeys:selectedRowKeys})
      this.avatarSelectedRows = selectedRows
    }
  }

  async changeTab(key) {
    this.setState({choosenTab: key});
    await this.load(this.state.pagination, this.filters, this.sorter,key)
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
    // 暂时不用Table的filter，不太好用
    await this.load(pagination, this.filters, sort,this.state.choosenTab)
  }

  onBeforeUpload(file, fileList) {
    if(this.state.restype === "avatar" || this.state.restype === "timelineMixture" || this.state.restype === "backgroundImage"){
      const size = 20;
      const isLt2M = file.size / 1024 / 1024 < size;
      if (!isLt2M) {
        message.error(this.props.intl.formatMessage({id: '文件必须小于{value}!'}, {value: size + "M"}));
        return false
      }

      let zip = /^.+\.zip/.test(file.name.toLowerCase());
      if(!zip){
        message.error(this.props.intl.formatMessage({id: '文件名不合法,必须为{value}'}, {value: "zip"}));
      }
      return zip;
    }else{
      const size = 1;
      const isLt2M = file.size / 1024 / 1024 < size;
      if (!isLt2M) {
        message.error(this.props.intl.formatMessage({id: '文件必须小于{value}!'}, {value: size + "M"}));
        return false
      }

      let csv = /^.+\.csv/.test(file.name);
      let txt = /^.+\.txt/.test(file.name);
      if (!csv && !txt) {
        message.error(this.props.intl.formatMessage({id: '文件名不合法,必须为{value}'}, {value: "txt,csv"}));
      }
      return csv || txt
    }
  }

  onUploadChange(info){
    if(!info.file.status){
      return;
    }
    this.setState({fileList: info.fileList});

    if(info.file.status === 'done'){
      message.success(this.props.intl.formatMessage({id: '{value}上传成功'}, {value: info.file.name}));
    } else if (info.file.status === 'error') {
      message.error(this.props.intl.formatMessage({id: '{value}上传失败'}, {value: info.file.name}));
      this.setState({fileList: []});
    }
  };

  async delete(id) {
    let deleteKeys = [];
    if(id){
      deleteKeys = [id]
    } else {
      if(this.state.choosenTab === 'nickname'){
        deleteKeys = this.state.nicknameSelectedRowKeys
      }else if(this.state.choosenTab === 'avatar'){
        deleteKeys = this.state.avatarSelectedRowKeys
      }else{
        deleteKeys = this.state.signatureSelectedRowKeys
      }
    }

    if(deleteKeys.length === 0){
      message.error('请先选择数据')
      return
    }

    DialogApi.warning({
      title: this.props.intl.formatMessage({id: '确定要删除这些数据？'}),
      content: <div>
      </div>,
      onOk: async() => {
        this.setState({loading: true});
        await axios.post(`${baseUrl}/deleteBatch`, deleteKeys);
        if(this.state.choosenTab === 'nickname'){
          this.setState({nicknameSelectedRowKeys: []});
        }else if(this.state.choosenTab === 'avatar'){
          this.setState({avatarSelectedRowKeys: []});
        }else{
          this.setState({signatureSelectedRowKeys: []});
        }
        message.success(this.props.intl.formatMessage({id: '操作成功'}));
        await this.load(this.state.pagination, this.filters, this.sorter,this.state.choosenTab)
      },
      onCancel: () => {
      },
    })
    //
    // Modal.confirm({
    //   title: this.props.intl.formatMessage({id: '确定要删除这些数据吗？'}),
    //   content: '',
    //   okType: 'danger',
    //   onOk: async() => {
    //     this.setState({loading: true});
    //     await axios.post(`${baseUrl}/delete`, [id]);
    //     message.success(this.props.intl.formatMessage({id: '操作成功'}));
    //     this.reload()
    //   },
    //   onCancel() {
    //   }
    // })
  }

  // async delete(index) {
  //   console.log("index为：",index)
  //   if(index){
  //     this.setState({selectedRowKeys:[index]})
  //   } else if (this.state.selectedRowKeys.length == 0) {
  //     message.error('请先选择数据')
  //     return
  //   }
  //
  //   DialogApi.warning({
  //     title: this.props.intl.formatMessage({id: '确定要删除这些数据？'}),
  //     content: <div>
  //     </div>,
  //     onOk: async() => {
  //       this.setState({loading: true});
  //       await axios.post(`/api/socks5/delete`, this.state.selectedRowKeys);
  //       message.success('操作成功');
  //       this.reload()
  //     },
  //     onCancel: () => {
  //     },
  //   })
  // }

  showUserVisible = async()=> {
    if (this.state.selectedRowKeys.length == 0) {
      message.error('请先选择数据');
      return
    }
    this.setState({userVisible: true});
  };

  async setUser() {
    if (this.state.selectedRowKeys.length == 0) {
      message.error('请先选择数据');
      return
    }
    if (!this.state.userID) {
      message.error('请先选择用户');
      return
    }

    this.setState({loading: true});
    await axios.post(`${baseUrl}/setUser`, {ids: this.state.selectedRowKeys, userID: this.state.userID});
    message.success('操作成功');
    this.setState({userVisible: false});
    this.reload()
  }

  async setPlatform() {
    if (this.state.selectedRowKeys.length == 0) {
      message.error('请先选择数据');
      return
    }
    if (!this.state.platform) {
      message.error('请填写平台编号');
      return
    }

    this.setState({loading: true});
    await axios.post(`${baseUrl}/setPlatform`, {ids: this.state.selectedRowKeys, platform: this.state.platform});
    message.success('操作成功');
    this.setState({platformVisible: false});
    this.reload()
  }


  refTableContent1 = (ref) => {
    if (ref && ref.getBoundingClientRect) {
      console.log("当前高度",ref.getBoundingClientRect().height)
      this.setState({showTable: true, scrollY: ref.getBoundingClientRect().height - 60, tableContent: ref})
    }
  }

  // 分页变化时更新状态
  handlePaginationChange = (paginationInfo) => {
    const { current, pageSize } = paginationInfo;
    const startIndex = (current - 1) * pageSize;
    const endIndex = startIndex + pageSize;
    const paginatedData = this.state.dataAddFull.slice(startIndex, endIndex);
    this.setState({
      dataAdd: paginatedData,
      paginationAdd: {
        ...this.state.paginationAdd,
        current,
        pageSize,
        total: this.state.dataAddFull.length
      }
    });
  };

  // 把columns放到render中，虽然损失部分性能，但是能方便参数中的匿名回调获取实例状态
  render() {
    const {tableData, file,maxConnections,currentPage,pageSize} = this.state;
    const startIndex = (currentPage - 1) * pageSize;
    const endIndex = startIndex + pageSize;
    const paginatedData = tableData.slice(startIndex, endIndex);
    const columns = [
      {
        title: '资源库',
        dataIndex: 'restype',
        key: 'restype',
        translateRender: true,
        render: (v)=> {
          for(const t of resTypes){
            if(v === t.value){
              return t.label;
            }
          }
          return "未知";
        }
      }, {
        title: '资源名称',
        dataIndex: 'resname',
        key: 'resname'
      },
      {
        title: '资源数量',
        dataIndex: 'resnumber',
        key: 'resnumber',
      },
      {
        title: '创建时间',
        dataIndex: 'createTime',
        key: 'createTime',
        render: formatDate
      }, {
        title: '操作',
        dataIndex: 'oper',
        key: 'oper',
        translateRender: true,
        render: (v, r) => {
          return (<div>
            {/*<a href={`/api/consumer/res/${r.filepath}?filename=${encodeURIComponent(r.filename)}`}>下载</a>*/}
            <Button type="link" style={{marginRight:-20}}><a href={`/api/consumer/res/${r.filepath}?filename=${encodeURIComponent(`${r.resname}_${resTypes.find(rt => rt.value === r.restype).label}`)}`}>下载</a></Button>
            <Divider type="vertical"/>
            <Button type="link" onClick={() => {
              this.delete(r._id);
            }}>删除</Button>
          </div>)
        }
      }
    ];

    return (<MyTranslate><ExternalAccountFilter>
      <div>
        <Breadcrumb>
          <Breadcrumb.BreadcrumbItem>素材管理</Breadcrumb.BreadcrumbItem>
          <Breadcrumb.BreadcrumbItem>资料库</Breadcrumb.BreadcrumbItem>
        </Breadcrumb>
        <Alert
          message={
            <div>
              提示
              <br />
              1、昵称和签名请上传txt或csv文件，一行一条数据；<br />
              2、头像请上传zip包，图片后缀为jpg或png，图片必须在zip包根目录下；<br />
              3、一次只能上传一个文件，同一资源库下资源名称不可重复，上传成功后可以查看资源数量是否正确。
            </div>
          }
          type="info"
          showIcon
          style={{
            margin: '20px 0',
            backgroundColor: '#EEF3FF',
            borderColor: '#EEF3FF'
          }}
        />
        <div>
          <div className="search-query-btn" onClick={()=> this.setState({addView:true})}>新增</div>
          <div className="search-query-btn" onClick={()=>this.delete()}>删除</div>
        </div>
        {this.state.choosenTab === 'nickname' ? <><div className="tableSelectedCount">{`已选${this.state.nicknameSelectedRowKeys.length}项`}</div></> : ''}
        {this.state.choosenTab === 'signature' ? <><div className="tableSelectedCount">{`已选${this.state.signatureSelectedRowKeys.length}项`}</div></> : ''}
        {this.state.choosenTab === 'avatar' ? <><div className="tableSelectedCount">{`已选${this.state.avatarSelectedRowKeys.length}项`}</div></> : ''}
        <Tabs defaultActiveKey={this.state.choosenTab} onChange={this.changeTab.bind(this)} style={{ marginTop: '5px' }}>
          <TabPane tab="昵称库" key="nickname">
            <div className="main-content"  style={{ marginTop: '-5px' }}>
              <div className="tableContent" style={{height: `${this.state.scrollY}px`}} ref={this.refTableContent1}>
                <div>
                  { this.state.showTable ? <Table
                    tableLayout="fixed"
                    scroll={{y: this.state.scrollY, x: 1000}}
                    pagination={this.state.pagination} rowSelection={{
                    nicknameSelectedRowKeys: this.state.nicknameSelectedRowKeys,
                    onChange: this.onRowSelectionChange.bind(this)
                  }} columns={columns} rowKey='_id' dataSource={this.state.data} loading={this.state.loading}/> : '' }
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
          </TabPane>
          <TabPane tab="签名库" key="signature">
            <div className="main-content"  style={{ marginTop: '-5px' }}>
              <div className="tableContent" style={{height: `${this.state.scrollY}px`}}>
                <div>
                  { this.state.showTable ? <Table
                    tableLayout="fixed"
                    scroll={{y: this.state.scrollY, x: 1000}}
                    pagination={this.state.signaturePagination} rowSelection={{
                    selectedRowKeys: this.state.signatureSelectedRowKeys,
                    onChange: this.onRowSelectionChange.bind(this)
                  }} columns={columns} rowKey='_id' dataSource={this.state.signatureData} loading={this.state.loading}/> : '' }
                </div>
              </div>
              <Pagination
                showJumper
                total={this.state.signaturePagination.total}
                current={this.state.signaturePagination.current}
                pageSize={this.state.signaturePagination.pageSize}
                onChange={this.handleTableChange.bind(this)}
              />
            </div>
          </TabPane>
          <TabPane tab="头像库" key="avatar">
            <div className="main-content"  style={{ marginTop: '-5px' }}>
              <div className="tableContent" style={{height: `${this.state.scrollY}px`}}>
                <div>
                  { this.state.showTable ? <Table
                    tableLayout="fixed"
                    scroll={{y: this.state.scrollY, x: 1000}}
                    pagination={this.state.avatarPagination} rowSelection={{
                    selectedRowKeys: this.state.avatarSelectedRowKeys,
                    onChange: this.onRowSelectionChange.bind(this)
                  }} columns={columns} rowKey='_id' dataSource={this.state.avatarData} loading={this.state.loading}/> : '' }
                </div>
              </div>
              <Pagination
                showJumper
                total={this.state.avatarPagination.total}
                current={this.state.avatarPagination.current}
                pageSize={this.state.avatarPagination.pageSize}
                onChange={this.handleTableChange.bind(this)}
              />
            </div>
          </TabPane>
        </Tabs>


        <Dialog
          visible={this.state.addView}
          onCancel={() => {
            this.setState({addView: false})
          }}
          onClose={() => {
            this.setState({addView: false})
          }}
          onConfirm={this.addRes.bind(this)}
          header="新增"
          style={{
            width: '60%',
            maxWidth: '800px',
            position: 'fixed', // 固定定位
            left: '50%',
            top: '50%',
            transform: 'translate(-50%, -50%)', // 中心点定位
            margin: 0 // 清除默认margin
          }}
          contentStyle={{ padding: '20px' }} // 调整内容区内边距
        >
          <div style={{marginLeft: 121}}>
            <div style={{display: 'flex',marginBottom: 24}}>
              <div style={{width: 84, textAlign: 'right', marginRight: 16}}><span style={{color:'#D54941'}}>*</span>类型</div>
              <div>
                <Radio.Group onChange={(e) => this.setState({restype: e.target.value})} value={this.state.restype}>
                  <Radio value={"nickname"}>昵称</Radio>
                  <Radio value={"signature"}>签名</Radio>
                  <Radio value={"avatar"}>头像</Radio>
                </Radio.Group>
              </div>
            </div>
            <div style={{display: 'flex',marginBottom: 24}}>
              <div style={{width: 84, textAlign: 'right', marginRight: 16}}><span style={{color:'#D54941'}}>*</span>选择文件</div>
              <div style={{ display: 'flex', alignItems: 'center', gap: '10px' }}>
                <Upload {...uploadProps} fileList={this.state.fileList} onChange={this.onUploadChange.bind(this)} beforeUpload={this.onBeforeUpload.bind(this)}>
                  <Button>
                    上传文件
                  </Button>
                </Upload>
              </div>
            </div>
            <div style={{display: 'flex',marginBottom: 24}}>
              <div style={{width: 84, textAlign: 'right', marginRight: 16, lineHeight: '30px'}}><span style={{color:'#D54941'}}>*</span>资源名称</div>
              <div style={{display: 'flex'}}>
                <Input placeholder={"输入资源名称"} style={{width: 400}} onChange={e => this.setState({resname: e.target.value})} value={this.state.resname}/>
              </div>
            </div>
          </div>
        </Dialog>
      </div>
    </ExternalAccountFilter></MyTranslate>)
  }
}

export default injectIntl(MyComponent)

// export default connect(({user}) => ({
//   userID: user.info.userID,
//   createVps: user.info.createVps,
// }))(MyComponent)
