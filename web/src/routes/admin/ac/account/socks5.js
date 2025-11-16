import React, {Component} from 'react'
import iconv from 'iconv-lite';
import {Alert, Button, Input, message, Modal, Radio, Select, Spin, Switch, Table, Tabs, Tooltip, Upload} from 'antd'
import axios from 'axios'
import {formatDate} from 'components/DateFormat'
import {Breadcrumb, Dialog, Link, Pagination, Space} from 'tdesign-react';
import ExternalAccountFilter from "components/common/ExternalAccountFilter";
import platforms from "components/proxyAccountPlatforms";
import MyTranslate from "components/common/MyTranslate";
import {injectIntl} from 'react-intl'
import DialogApi from "../../common/dialog/DialogApi";

const {TabPane} = Tabs;

const {Option} = Select
const Search = Input.Search;
const confirm = Modal.confirm;

// 针对当前页面的基础url
const baseUrl = '/api/consumer/socks5';
const baseUrlProxy = '/api/consumer/proxyAccount';
const protocal = [{label: 'http', value: 'http'}, {label: 'socks5', value: 'socks5'}]
// const uploadProps = {
//   name: 'file',
//   multiple: false,
//   showUploadList: false,
//   action: `${baseUrl}/upload`,
//   beforeUpload(file, fileList) {
//     let csv = /^.+\.csv$/.test(file.name.toLowerCase());
//     let txt = /^.+\.txt/.test(file.name.toLowerCase());
//     if (!csv && !txt) {
//       message.error('文件名不合法')
//     }
//     return csv || txt
//   }
// };

const socks5Params = ['ip', 'port', 'username', 'password', 'desc'];
const encodingsToTry = ['UTF-8', 'GBK', 'GB2312'];
const typeMap = {'proxyList': 'addProxy', 'ipList': 'addIp'}

class MyComponent extends Component {
  constructor(props) {
    super(props);
    // data由服务器返回表格的数据
    // loading由客户端控制
    // pagination控制表格分页功能，其中数据总数pagination.total由服务器返回
    // 以上变量受控，因为需要传递给Table控件
    this.state = {
      choosenTab: 'ipList',
      defaultAdd: 'proxyList',
      showTable: true,
      ipAdd: {
        ip: '',
        port: '',
        username: '',
        password: '',
        desc: '',
      },
      proxyAdd: {
        desc: '',
        token: '',
        id: '',
        platform: '',
      },
      isUploading: false, // 新增状态，用于标记文件是否正在上传
      addProxyView: false,
      addIpView: false,
      addType: "addIp",
      data: [],
      accounts: [],
      proxyData: [],
      dataAdd: [],
      dataAddFull: [],
      loading: false,
      ipCheckStatus: '',
      pagination: {
        pageSize: 10,
        current: 1,
        showTotal: (total, range) => `共 ${total} 条`,
        position: 'both',
        showSizeChanger: true,
        pageSizeOptions: ['10', '20', '30', '40', '50', '100', '200', '300', '400', '500', '1000', '2000']
      },
      proxyPagination: {
        pageSize: 10,
        current: 1,
        showTotal: (total, range) => `共 ${total} 条`,
        position: 'both',
        showSizeChanger: true,
        pageSizeOptions: ['10', '20', '30', '40', '50', '100', '200', '300', '400', '500', '1000', '2000']
      },
      paginationAdd: {
        pageSize: 10,
        current: 1,
        showTotal: (total, range) => `共 ${total} 条`,
        position: 'both',
        showSizeChanger: true,
        pageSizeOptions: ['10', '20', '30', '40', '50', '100', '200', '300', '400', '500', '1000', '2000']
      },
      selectedRowKeys: [],
      proxySelectedRowKeys: [],
      proxySelectedRows: [],
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
    this.selectedRows = []
    this.proxySelectedRows = []
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


    this.isAdmin = this.props.userID == 'admin';
    //this.handleCSVUpload = this.handleCSVUpload.bind(this);
  }

  // 首次加载数据
  async componentWillMount() {
    this.reload();
    this.proxyReload();
    // this.reloadConfig()
    if (this.isAdmin)
      this.getAllUser();
  }

  async reloadConfig() {
    let accounts = await axios.get('/api/consumer/proxyAccount/getIpPlatforms');
    if (accounts.data && accounts.data.length > 0) {
      this.setState({accounts: accounts.data});
    }
    console.log("获取到的账号平台数据为：", accounts.data, accounts)
  }

  handleCSVUpload = (file) => {
    this.setState({isUploading: true}, () => {
      // 文件类型校验（允许csv）
      const isValidType = /\.(csv)$/i.test(file.name);
      if (!isValidType) {
        this.setState({isUploading: false});
        message.error('仅支持.csv文件');
        return false;
      }

      const reader = new FileReader();
      reader.onload = (e) => {
        if (!(e.target.result instanceof ArrayBuffer)) {
          message.error('文件读取失败');
          return;
        }

        let detectedEncoding = '';
        let decodedContent = '';
        let buffer = new Uint8Array(e.target.result);
        const encodingsToTry = ['UTF-8', 'GBK', 'GB2312', 'GB18030', 'Big5', 'Windows-1252'];
        for (const encoding of encodingsToTry) {
          try {
            if (encoding === 'UTF-8') {
              const decoder = new TextDecoder('utf-8');
              decodedContent = decoder.decode(buffer);
            } else {
              decodedContent = iconv.decode(buffer, encoding);
            }
            // 增强中文检测逻辑
            if (/[\u4e00-\u9fff]/.test(decodedContent)) {
              detectedEncoding = encoding;
              break;
            }
          } catch (error) {
            this.setState({isUploading: false});
            console.warn(`${encoding}解码失败`, error);
          }
        }

        console.log("当前编码为：", detectedEncoding)
        if (!detectedEncoding) {
          const decoder = new TextDecoder('utf-8');
          decodedContent = decoder.decode(buffer);
          //message.error('无法识别文件编码');
          //return;
        }

        // 自动检测分隔符（支持逗号、分号、制表符）
        const firstLine = decodedContent.split('\n')[0];
        const delimiter = firstLine.match(/[,;\t]/)?.[0] || ',';

        // 统一处理换行符
        const lines = decodedContent
          .replace(/\r\n/g, '\n')  // 统一换行符
          .split('\n')
          .filter(line => line.trim().length > 0);
        if (lines.length === 0) {
          this.setState({isUploading: false});
          message.error('未解析出数据');
          return false;
        }

        // 解析数据（兼容不同分隔符）
        const parsedData = lines.map(line => {
          const values = line.split(delimiter).map(v => v.trim());
          const socks5 = {};

          values.forEach((value, i) => {
            if (socks5Params[i] && value) {
              socks5[socks5Params[i]] = value;
            }
          });

          return {
            ...socks5,
            maxConn: this.state.maxConnections
          };
        })

        // 更新state
        this.setState({
          dataAdd: parsedData,
          dataAddFull: parsedData,
          isUploading: false,
          paginationAdd: {
            ...this.state.paginationAdd,
            total: parsedData.length
          }
        }, () => {
          this.handlePaginationChange({
            current: 1,
            pageSize: this.state.paginationAdd.pageSize
          });
        });
      };

      reader.readAsArrayBuffer(file);
    });
    return false; // 阻止默认上传行为
  };

  async reload() {
    // 重新加载，一般是页面第一次加载的时候来一下
    this.load(this.state.pagination, this.filters, this.sorter)
  }

  async proxyReload() {
    // 重新加载，一般是页面第一次加载的时候来一下
    // this.proxyLoad(this.state.proxyPagination, this.filters, this.sorter)
  }

  async proxyLoad(pagination, filters, sorter) {
    // 这里的参数是格式好的，无需修改，无阻抗失衡，直接发给服务器
    this.setState({loading: true})
    let res = await axios.post(`${baseUrlProxy}/${pagination.pageSize}/${pagination.current}`, {filters, sorter})
    pagination.total = res.data.data.total
    this.setState({loading: false, proxyData: res.data.data.data, pagination, proxySelectedRowKeys: []})
    this.proxySelectedRows = []
    this.filters = filters
    this.sorter = sorter
  }


  async load(pagination, filters, sorter) {
    // 这里的参数是格式好的，无需修改，无阻抗失衡，直接发给服务器
    this.setState({loading: true})
    if (filters.ipCheckStatus === 'all') {
      delete filters.ipCheckStatus
    }
    if (filters.desc === '') {
      delete filters.desc
    }
    let res = await axios.post(`${baseUrl}/${pagination.pageSize}/${pagination.current}`, {filters, sorter})
    pagination.total = res.data.data.total
    this.setState({loading: false, data: res.data.data.data, pagination, selectedRowKeys: []})
    this.selectedRows = []
    this.filters = filters
    this.sorter = sorter
  }

  async onRowSelectionChange(selectedRowKeys, selectedRows) {
    // 选中状态的数据，因为无需受控，就不记录在state里了，提高效率
    if (this.state.choosenTab === 'proxyList') {
      this.setState({proxySelectedRowKeys: selectedRowKeys})
      this.proxySelectedRows = selectedRows
    } else {
      this.setState({selectedRowKeys: selectedRowKeys})
      this.selectedRows = selectedRows
    }

  }

  async changeTab(key) {
    this.setState({choosenTab: key, addType: typeMap[key]});
    if (key === 'ipList') {
      this.reload()
    } else {
      this.proxyReload()
    }
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
    await this.load(pagination, this.filters, sort)
  }

  async getAllUser() {
    let res = await axios.get(`/api/consumer/user/getAllUser`);
    let arr = [{
      value: "全部",
      label: '全部'
    }];
    let obj = {};
    for (const user of res.data.data) {
      arr.push({
        value: user.userID,
        label: user.name
      });
      obj[user.userID] = user.name
    }
    this.setState({
      users: res.data.data,
      userID2Name: obj,
      userRadioOptions: arr
    });
  }

  // 比较通用的回调
  async create() {
    this.props.history.push('card?oper=create')
  }

  async edit() {
    if (this.state.selectedRowKeys.length == 0) {
      message.error('请先选择数据')
      return
    }
    if (this.state.selectedRowKeys.length > 1) {
      message.error('只能选择一条数据')
      return
    }
    this.props.history.push('card?oper=edit&_id=' + this.state.selectedRowKeys[0])
  }

  async view() {
    if (this.state.selectedRowKeys.length == 0) {
      message.error('请先选择数据')
      return
    }
    if (this.state.selectedRowKeys.length > 1) {
      message.error('只能选择一条数据')
      return
    }
    this.props.history.push('card?oper=view&_id=' + this.state.selectedRowKeys[0])
  }

  async proxyDelete(index) {
    if (index) {
      this.setState({proxySelectedRowKeys: [index]})
    } else if (this.state.proxySelectedRowKeys.length == 0) {
      message.error('请先选择数据')
      return
    }

    DialogApi.warning({
      title: this.props.intl.formatMessage({id: '确定要删除这些数据？'}),
      content: <div>
      </div>,
      onOk: async () => {
        this.setState({loading: true});
        await axios.post(`${baseUrlProxy}/delete`, this.state.proxySelectedRowKeys);
        message.success('操作成功');
        this.proxyReload()
      },
      onCancel: () => {
      },
    })
  }

  async delete(index) {
    console.log("index为：", index)
    if (index) {
      this.setState({selectedRowKeys: [index]})
    } else if (this.state.selectedRowKeys.length == 0) {
      message.error('请先选择数据')
      return
    }

    DialogApi.warning({
      title: this.props.intl.formatMessage({id: '确定要删除这些数据？'}),
      content: <div>
      </div>,
      onOk: async () => {
        this.setState({loading: true});
        await axios.post(`/api/consumer/socks5/delete`, this.state.selectedRowKeys);
        message.success('操作成功');
        this.reload()
      },
      onCancel: () => {
      },
    })
  }

  // async onUploadChange(info) {
  //   info.file.name = info.file.originFileObj.name;
  //   if (info.file.status === 'done') {
  //     message.success(`${info.file.name} 上传成功${info.file.response.total - info.file.response.repeat}个 重复${info.file.response.repeat}个`);
  //     this.reload()
  //   } else if (info.file.status === 'error') {
  //     message.error(`${info.file.name} 上传失败`)
  //   }
  // }
  onUploadChange = (info) => {
    if (info.file.status === 'done') {
      message.success(`${info.file.name} 上传成功`);
    } else if (info.file.status === 'error') {
      message.error(`${info.file.name} 上传失败`);
    }
  };

  showUserVisible = async () => {
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

  showPlatformVisible = async () => {
    if (this.state.selectedRowKeys.length == 0) {
      message.error('请先选择数据');
      return
    }
    this.setState({platformVisible: true});
  };

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

  //用户筛选
  async changeUserIDFilter(e) {
    this.setState({belongUser: e.target.value});
    if (e.target.value == '全部') {
      delete this.filters.belongUser;
    } else {
      this.filters['belongUser'] = e.target.value;
    }
    this.reload();
  }


  refTableContent1 = (ref) => {
    if (ref && ref.getBoundingClientRect) {
      console.log("当前高度", ref.getBoundingClientRect().height)
      this.setState({showTable: true, scrollY: ref.getBoundingClientRect().height - 100, tableContent: ref})
    }
  }

  async reset() {
    await this.setState({desc: '', ipCheckStatus: '全部'});
    this.filters = {};
    await this.reload()
  }

  createIpGroup = async () => {
    this.setState({createIpGroup: true})
  };

  handleCancel = () => {
    this.setState({createIpGroup: false});
  };

  // async delete() {
  //   if (this.state.selectedRowKeys.length == 0) {
  //     message.error('请先选择数据');
  //     return
  //   }
  //   confirm({
  //     title: '确定要删除这些数据？',
  //     content: '',
  //     okText: '确定',
  //     okType: 'danger',
  //     cancelText: '取消',
  //     onOk: async() => {
  //       this.setState({loading: true});
  //       await axios.post(`${baseUrl}/delete`, this.state.selectedRowKeys);
  //       message.success('操作成功');
  //       this.reload()
  //     },
  //     onCancel() {
  //     }
  //   })
  // }

  handleEditCell = (index, key, e) => {
    const newData = [...this.state.dataAddFull];
    newData[index][key] = e.target.value;
    this.setState({dataAdd: newData}, () => this.handlePaginationChange({
      current: this.state.paginationAdd.current,
      pageSize: this.state.paginationAdd.pageSize
    }));
  };

  handleDeleteDialogRow = (index) => {
    const newData = [...this.state.dataAddFull];
    newData.splice(index, 1);
    this.setState({
      dataAdd: newData, dataAddFull: newData,
      paginationAdd: {
        ...this.state.paginationAdd,
        total: this.state.dataAddFull.length,
      }
    }, () => this.handlePaginationChange({
      current: this.state.paginationAdd.current,
      pageSize: this.state.paginationAdd.pageSize
    }));
  };

  // 分页变化时更新状态
  handlePaginationChange = (paginationInfo) => {
    const {current, pageSize} = paginationInfo;
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


  async enableBtnClick(record, enable) {
    this.setState({loading: true});
    await axios.post(`${baseUrlProxy}/changeEnable/${record._id}`, {enable});
    message.success('操作成功');
    this.proxyReload()
  }

  handleAdd = async () => {
    if (this.state.addType === 'addIp') {
      const {ip, port, username, password, desc} = this.state.ipAdd;
      const allFieldsFilled = Object.values({
        ip,
        port,
        username,
        password,
      }).every(value => value && value.trim() !== '');
      if (!allFieldsFilled) {
        message.error('内容填写不完整')
        return;
      }
      this.state.dataAddFull[0] = this.state.ipAdd
      let  addRes =  await this.handleSubmitSingle()
      this.setState({dataAddFull: [], ipAdd: (addRes ? {} : this.state.ipAdd)})
    }
    await this.reload()
  }

  handleSubmitSingle = async () => {
    this.setState({loading: true})
    const incompleteIndex = this.state.dataAddFull.findIndex((item, index) => {
      const {ip, port, username, password} = item;
      const isValid = (value) => {
        return value !== undefined && value !== null &&
          (typeof value !== 'string' || value.trim() !== '');
      };
      console.log(ip, port, username, password)
      // 校验逻辑：字段是否为空或仅包含空格
      return ![ip, port, username, password].every(isValid);
    });
    console.log(incompleteIndex)
    if (incompleteIndex !== -1) {
      // 提示第 N 行不完整（行号从 1 开始）
      message.error(`第 ${incompleteIndex + 1} 行存在内容填写不完整`);
      return false; // 阻止提交
    }
    try {

      const response = await axios.post(`${baseUrl}/addIpSingle`, this.state.dataAddFull);
      console.log('提交成功:', response.data);

      if (response.data.code === 1) {
        message.success(`提交成功`)
        this.setState({addProxyView: false})
        await this.reload()
        return true
      }else {
        message.error(response.data.message)
        this.setState({addProxyView: true})
        return false
      }
    } catch (error) {
      console.error('提交失败:', error);
    }
    this.setState({loading: false})
    return true;
  };

  handleSubmit = async () => {
    this.setState({loading: true})
    const incompleteIndex = this.state.dataAddFull.findIndex((item, index) => {
      const {ip, port, username, password} = item;
      const isValid = (value) => {
        return value !== undefined && value !== null &&
          (typeof value !== 'string' || value.trim() !== '');
      };
      console.log(ip, port, username, password)
      // 校验逻辑：字段是否为空或仅包含空格
      return ![ip, port, username, password].every(isValid);
    });
    console.log(incompleteIndex)
    if (incompleteIndex !== -1) {
      // 提示第 N 行不完整（行号从 1 开始）
      message.error(`第 ${incompleteIndex + 1} 行存在内容填写不完整`);
      return false; // 阻止提交
    }
    try {
      const response = await axios.post(`${baseUrl}/addIp`, this.state.dataAddFull);
      console.log('提交成功:', response.data);
      this.setState({
        createIpGroup: false, dataAddFull: [], dataAdd: [], paginationAdd: {
          total: 0, current: 1, pageSize: 10
        }
      })
      await this.reload()

      let total =  response.data.data.total;
      let repeat =  response.data.data.repeat;
      let success =  response.data.data.success;
      let error =  response.data.data.error;


      // message.success(`提交成功,成功${response.data.total - response.data.repeat}个,重复${response.data.repeat}`)
      if (total === success) {
        await DialogApi.success(
          {
            title: '全部提交成功',
            content: <div>
              <p>共{total}条数据，成功{success}条</p>
            </div>,
            onOk: () => {
            }
          }
        )
      }else if (success > 0)  {
        await DialogApi.success(
          {
            title: '部分提交成功',
            content: <div>
              <p>共{total}条数据，成功{success}条，错误{error}条，重复{repeat}条</p>
            </div>,
            onOk: () => {
            }
          }
        )
      }else if (success === 0)  {
        await DialogApi.error(
          {
            title: '提交失败',
            content: <div>
              <p>共{total}条数据， 成功{success}条，错误{error}条，重复{repeat}条</p>
            </div>,
            onOk: () => {
            }
          }
        )
      }

    } catch (error) {
      console.error('提交失败:', error);
    }
    this.setState({loading: false})
  };

  // 把columns放到render中，虽然损失部分性能，但是能方便参数中的匿名回调获取实例状态
  render() {
    const {tableData, file, maxConnections, currentPage, pageSize} = this.state;
    const startIndex = (currentPage - 1) * pageSize;
    const endIndex = startIndex + pageSize;
    const paginatedData = tableData.slice(startIndex, endIndex);

    const columns = [
      {
        title: 'IP',
        dataIndex: 'ip',
        key: 'ip'
      },
      {
        title: '状态',
        dataIndex: 'ipCheckStatus',
        key: 'ipCheckStatus',
        render: (v) => {
          if (v === 'yes') {
            return '可用'
          } else if (v === 'no') {
            return '不可用'
          }else {
            return '检测中'
          }
        }
        // render: (_, record) => {
        //   if (!record.lastCheckNormalTime) {
        //     return '不可用';
        //   }
        //   const lastCheckTime = moment(record.lastCheckNormalTime);
        //   const now = moment();
        //   const diff = now.diff(lastCheckTime, 'minutes');
        //   return diff <= 1 ? '可用' : '不可用';
        // }
      },
      {
        title: '地区',
        dataIndex: 'countryName',
        key: 'countryName'
      },
      // {
      //   title: '最大连接数',
      //   dataIndex: 'maxConn',
      //   key: 'maxConn',
      //   render: (v)=> {
      //     return v ? v : 0
      //   }
      // },
      {
        title: '已连接数',
        dataIndex: 'vpsCount',
        key: 'vpsCount',
        render: (v) => {
          return v ? v : 0
        }
      },
      {
        title: '描述',
        dataIndex: 'desc',
        key: 'desc',
        render: (text) => {
          return (
            <Tooltip title={text}>
            <span style={{
              display: 'inline-block',
              width: '100%',
              whiteSpace: 'nowrap',
              overflow: 'hidden',
              textOverflow: 'ellipsis'
            }}>
              {text}
            </span>
            </Tooltip>
          );
        },
      },
      {
        title: '创建时间',
        dataIndex: 'createTime',
        key: 'createTime',
        render: (text) => {
          const formattedText = formatDate(text);
          return (
            <Tooltip title={formattedText}>
            <span style={{
              display: 'inline-block',
              width: '100%',
              whiteSpace: 'nowrap',
              overflow: 'hidden',
              textOverflow: 'ellipsis'
            }}>
              {formattedText}
            </span>
            </Tooltip>
          );
        },
      }, {
        title: '状态最后更新时间',
        dataIndex: 'lastCheckNormalTime',
        key: 'lastCheckNormalTime',
        render: (text) => {
          const formattedText = formatDate(text);
          return (
            <Tooltip title={formattedText}>
            <span style={{
              display: 'inline-block',
              width: '100%',
              whiteSpace: 'nowrap',
              overflow: 'hidden',
              textOverflow: 'ellipsis'
            }}>
              {formattedText}
            </span>
            </Tooltip>
          );
        },
      },
      {
        title: '操作',
        dataIndex: 'op',
        key: 'op',
        width: 130,
        ellipsis: true,
        render: (v, r, index) => {
          return (<div style={{ textAlign: 'center' }}>
            <Button type="link" onClick={() => {
              this.delete(r._id)
            }}>删除</Button>
          </div>)
        }
      },
    ];
    const columnsProxy = [
      {
        title: '描述',
        dataIndex: 'desc',
        key: 'desc',
        render: (text) => {
          return (
            <Tooltip title={text}>
            <span style={{
              display: 'inline-block',
              width: '100%',
              whiteSpace: 'nowrap',
              overflow: 'hidden',
              textOverflow: 'ellipsis'
            }}>
              {text}
            </span>
            </Tooltip>
          );
        },
      },
      {
        title: '平台',
        dataIndex: 'platform',
        key: 'platform'
      },
      {
        title: '账号id',
        dataIndex: 'id',
        key: 'id',
      }, {
        title: '创建时间',
        dataIndex: 'createTime',
        key: 'createTime',
        render: (text) => {
          const formattedText = formatDate(text);
          return (
            <Tooltip title={formattedText}>
            <span style={{
              display: 'inline-block',
              width: '100%',
              whiteSpace: 'nowrap',
              overflow: 'hidden',
              textOverflow: 'ellipsis'
            }}>
              {formattedText}
            </span>
            </Tooltip>
          );
        },
      },
      {
        title: '启用',
        dataIndex: 'enable',
        key: 'enable',
        render: (t, record, index) => {
          return (<Switch style={{width: '56px'}} checkedChildren="选定" unCheckedChildren="" checked={t}
                          onChange={(enable) => {
                            this.enableBtnClick(record, enable)
                          }}/>)
        }
      },
      {
        title: '操作',
        dataIndex: 'op',
        key: 'op',
        width: 130,
        ellipsis: true,
        render: (v, r, index) => {
          return (<div>
            <Button type="link" onClick={() => {
              this.proxyDelete(r._id)
            }}>删除</Button>
          </div>)
        }
      },
    ];
    const columnsAdd = [
      {
        title: 'IP',
        dataIndex: 'ip',
        key: 'ip',
      },
      {
        title: '用户名',
        dataIndex: 'username',
        key: 'username',
      },
      {
        title: '密码',
        dataIndex: 'password',
        key: 'password',
      },
      {
        title: '端口',
        dataIndex: 'port',
        key: 'port',
      },
      // {
      //   title: '最大连接数',
      //   dataIndex: 'maxConn',
      //   key: 'maxConn',
      //   render: (text, record, index) => (
      //     <Input
      //       value={text}
      //       onChange={(e) => this.handleEditCell(index, 'maxConn', e)}
      //     />
      //   )
      // },
      {
        title: '操作',
        key: 'action',
        render: (text, record, index) => (
          <Button type="link" onClick={() => this.handleDeleteDialogRow(index)}>删除</Button>
        )
      },
    ];


    // 处理文件上传
    const uploadProps = {
      name: 'file',
      multiple: false,
      showUploadList: false,
      beforeUpload: this.handleCSVUpload,
    };

    return (<MyTranslate><ExternalAccountFilter>
      <div>
        <Breadcrumb>
          <Breadcrumb.BreadcrumbItem>系统设置</Breadcrumb.BreadcrumbItem>
          <Breadcrumb.BreadcrumbItem>IP管理</Breadcrumb.BreadcrumbItem>
        </Breadcrumb>
        <Alert
          message={
            <div>
              提示
              <br/>
              1.目前系统仅支持socks5的代理协议<br/>
              2.为了降低账号风控，每个IP最大连接数为10，超过数量请上传新的IP
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

        <div className="search-box">
          <div className='search-item' style={{minWidth: 280}}>
            <div className="search-item-label">描述</div>
            <div className="search-item-right">
              <Input
                allowClear
                style={{width: 200}}
                placeholder="请输入内容"
                value={this.state.desc}
                // 补充事件绑定 ↓
                onChange={e => {
                  this.setState({desc: e.target.value})
                  this.filters['desc'] = e.target.value
                }
                }
                onPressEnter={e => {
                  this.setState({desc: e.target.value})
                  this.filters['desc'] = e.target.value
                  this.reload()
                }
                }

              />
            </div>
          </div>
          <div className='search-item'>
            <div className="search-item-label">状态</div>
            <div className="search-item-right">
              <Select value={this.state.ipCheckStatus} style={{width: 200}} onChange={v => {
                this.setState({ipCheckStatus: v})
                this.filters['ipCheckStatus'] = v
                this.reload()
              }}>
                <Option value="all">全部</Option>
                <Option value="yes">可用</Option>
                <Option value="no">不可用</Option>
                <Option value="checking">检测中</Option>
                {/*{accountStatus.map(ws => {*/}
                {/*  return <Option value={ws.value}>{ws.label}</Option>*/}
                {/*})}*/}
              </Select>
            </div>
          </div>
          <div className='accountGroup-btn'>
            <div className="search-query-btn" onClick={() => this.reload()}>查询</div>
            <div className="search-reset-btn" onClick={() => this.reset()}>重置</div>
          </div>
        </div>

        <div className="main-content" style={{marginTop: '20px'}}>
          <div>
            <div className="search-query-btn" onClick={() => this.setState({addProxyView: true})}>新增</div>
            <div className="search-query-btn" onClick={this.createIpGroup.bind(this)}>批量导入</div>
            <div
              className={this.state.selectedRowKeys && this.state.selectedRowKeys.length > 0 ? "search-delete-btn" : "search-reset-btn"}
              onClick={() => this.delete()}>批量删除
            </div>
          </div>
          <div className="tableSelectedCount">{`已选${this.state.selectedRowKeys.length}项`}</div>
          {/*<div className="tableContent" style={{height: `${this.state.scrollY-60}px`}}>*/}
          <div className="tableContent" style={{height: `${this.state.scrollY - 60}px`}}
               ref={this.refTableContent1}>
            <div>
              {this.state.showTable ? <Table
                tableLayout="fixed"
                scroll={{y: this.state.scrollY - 120, x: 1000}}
                pagination={this.state.pagination} rowSelection={{
                selectedRowKeys: this.state.selectedRowKeys,
                onChange: this.onRowSelectionChange.bind(this)
              }} columns={columns}
                rowKey='_id'
                dataSource={this.state.data}
                loading={this.state.loading} /> : ''}
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
          visible={this.state.createIpGroup}
          onCancel={() => {
            this.setState({
              createIpGroup: false, dataAddFull: [], dataAdd: [], paginationAdd: {
                total: 0, current: 1, pageSize: 10
              }
            })
          }}
          onClose={() => {
            this.setState({
              createIpGroup: false, dataAddFull: [], dataAdd: [], paginationAdd: {
                total: 0, current: 1, pageSize: 10
              }
            })
          }}
          onConfirm={this.handleSubmit.bind(this)}
          confirmLoading={this.state.loading}
          header="导入账号"
          style={{
            width: '80%',
            maxWidth: '1600px',
            position: 'fixed', // 固定定位
            left: '50%',
            top: '50%',
            transform: 'translate(-50%, -50%)', // 中心点定位
            margin: 0 // 清除默认margin
          }}
          contentStyle={{padding: '20px'}} // 调整内容区内边距
          // footer={[
          //   <div className='accountGroup-btn'>
          //     <div className="search-reset-btn" onClick={() => this.reset()}>取消</div>
          //     <div className="search-query-btn" onClick={() => this.reload()}>确认</div>
          //   </div>
          // ]}
        >


          <div style={{marginBottom: '20px'}}>
            <Space size={20} align="center">
              {/*<span style={{ fontSize: '14px', color: '#333',paddingLeft: '70px'}}>代理地址</span>*/}
              <span style={{fontSize: '14px', color: '#333'}}>代理地址</span>
              <div style={{display: 'flex', alignItems: 'center', gap: '10px'}}>
                <Upload {...uploadProps}>
                  <Button>
                    批量导入
                  </Button>
                </Upload>
                <Link theme="primary" style={{fontSize: '14px'}}><a target='_self'
                                                                    href={'/socks-example.csv'}>下载模版</a></Link>
              </div>
            </Space>
          </div>
          <div className="tableContent" ref={this.refTableContent1}>
            <div>
              {this.state.createIpGroup ? <Table
                tableLayout="fixed"
                scroll={{y: this.state.scrollY, x: 1000}}
                pagination={false} columns={columnsAdd} rowKey={(record, index) => index.toString()}
                dataSource={this.state.dataAdd} loading={this.state.loading}/> : ''}
            </div>
          </div>
          <Pagination
            showJumper
            total={this.state.paginationAdd.total}
            current={this.state.paginationAdd.current}
            pageSize={this.state.paginationAdd.pageSize}
            onChange={this.handlePaginationChange}
          />
        </Dialog>


        <Dialog
          header="新增"
          width={900}
          visible={this.state.addProxyView}
          onConfirm={this.handleAdd} confirmLoading={this.state.loading}
          onCancel={() => {
            this.setState({addProxyView: false, dataAddFull: [], addType: 'addIp', ipAdd: {}, proxyAdd: {}})
          }
          }
          onClose={() => {
            this.setState({addProxyView: false, dataAddFull: [], addType: 'addIp', ipAdd: {}, proxyAdd: {}})
          }}
        >
          <div style={{marginLeft: 121}}>
            {/*<div style={{display: 'flex', marginBottom: 24}}>*/}
            {/*  <div style={{width: 84, textAlign: 'right', marginRight: 16}}><span style={{color: '#D54941'}}>*</span>类型*/}
            {/*  </div>*/}
            {/*  <div>*/}
            {/*    <Radio.Group onChange={(e) => this.setState({addType: e.target.value})} value={this.state.addType}>*/}
            {/*      <Radio value={"addIp"}>IP地址</Radio>*/}
            {/*      <Radio value={"addProxy"}>代理账号</Radio>*/}
            {/*    </Radio.Group>*/}
            {/*  </div>*/}
            {/*</div>*/}
            {
              this.state.addType === 'addIp' ? <>
                  <div style={{display: 'flex', marginBottom: 24}}>
                    <div style={{width: 84, textAlign: 'right', marginRight: 16, lineHeight: '30px'}}>
                      描述
                    </div>
                    <div style={{display: 'flex'}}>
                      <Input placeholder={'请输入IP地址描述便于识别'} value={this.state.ipAdd.desc || ''}
                             style={{width: 400}} onChange={(e) => {
                        this.setState({ipAdd: {...this.state.ipAdd, desc: e.target.value}})
                      }}/>
                    </div>
                  </div>
                  <div style={{display: 'flex', marginBottom: 24}}>
                    <div style={{width: 84, textAlign: 'right', marginRight: 16, lineHeight: '30px'}}><span
                      style={{color: '#D54941'}}>*</span>IP地址
                    </div>
                    <div style={{display: 'flex'}}>
                      <Input placeholder={'请输入IP地址'} style={{width: 400}} value={this.state.ipAdd.ip || ''}
                             onChange={(e) => {
                               this.setState({ipAdd: {...this.state.ipAdd, ip: e.target.value}})
                             }}/>
                    </div>
                  </div>
                  <div style={{display: 'flex', marginBottom: 24}}>
                    <div style={{width: 84, textAlign: 'right', marginRight: 16, lineHeight: '30px'}}><span
                      style={{color: '#D54941'}}>*</span>账号
                    </div>
                    <div style={{display: 'flex'}}>
                      <Input placeholder={"请输入账号"} style={{width: 400}} value={this.state.ipAdd.username || ''}
                             onChange={(e) => {
                               this.setState({ipAdd: {...this.state.ipAdd, username: e.target.value}})
                             }}/>
                    </div>
                  </div>
                  <div style={{display: 'flex', marginBottom: 24}}>
                    <div style={{width: 84, textAlign: 'right', marginRight: 16, lineHeight: '30px'}}><span
                      style={{color: '#D54941'}}>*</span>密码
                    </div>
                    <div style={{display: 'flex'}}>
                      <Input placeholder={"请输入密码"} style={{width: 400}} value={this.state.ipAdd.password || ''}
                             onChange={(e) => {
                               this.setState({ipAdd: {...this.state.ipAdd, password: e.target.value}})
                             }}/>
                    </div>
                  </div>
                  <div style={{display: 'flex', marginBottom: 24}}>
                    <div style={{width: 84, textAlign: 'right', marginRight: 16, lineHeight: '30px'}}><span
                      style={{color: '#D54941'}}>*</span>端口
                    </div>
                    <div style={{display: 'flex'}}>
                      <Input placeholder={"请输入端口"} style={{width: 400}} value={this.state.ipAdd.port || ''}
                             onChange={(e) => {
                               this.setState({ipAdd: {...this.state.ipAdd, port: e.target.value}})
                             }}/>
                    </div>
                  </div>
                </> :
                <>
                  <div style={{display: 'flex', marginBottom: 24}}>
                    <div style={{width: 84, textAlign: 'right', marginRight: 16, lineHeight: '30px'}}><span
                      style={{color: '#D54941'}}>*</span>描述
                    </div>
                    <div style={{display: 'flex'}}>
                      <Input placeholder={'请输入IP地址描述便于识别'} value={this.state.proxyAdd.desc || ''}
                             style={{width: 400}} onChange={(e) => {
                        this.setState({proxyAdd: {...this.state.proxyAdd, desc: e.target.value}})
                      }}/>
                    </div>
                  </div>
                  <div style={{display: 'flex', marginBottom: 24}}>
                    <div style={{width: 84, textAlign: 'right', marginRight: 16, lineHeight: '30px'}}><span
                      style={{color: '#D54941'}}>*</span>平台
                    </div>
                    <div>
                      <Select style={{width: 400}} value={this.state.proxyAdd.platform || ''}
                              onChange={v => this.setState({proxyAdd: {...this.state.proxyAdd, platform: v}})}>
                        {platforms.map(p => (
                          <Option key={p.value} value={p.value}>
                            {p.label}
                          </Option>
                        ))}
                        {/*{this.state.allGroup.map(ws => {*/}
                        {/*  return <Option value={ws._id}>{ws.groupName}</Option>*/}
                        {/*})}*/}
                      </Select>
                    </div>
                  </div>
                  {this.state.proxyAdd.platform === 'aggregationPlatform' ? <>
                    <div style={{display: 'flex', marginBottom: 24}}>
                      <div style={{width: 84, textAlign: 'right', marginRight: 16, lineHeight: '30px'}}><span
                        style={{color: '#D54941'}}>*</span>选择账号
                      </div>
                      <div>
                        <Select style={{width: 400}} value={this.state.proxyAdd.account || ''}
                                onChange={v => this.setState({proxyAdd: {...this.state.proxyAdd, account: v}})}>
                          {this.state.accounts && this.state.accounts.map(p => (
                            <Option key={p.value} value={p.value}>
                              {p.label}
                            </Option>
                          ))}
                          {/*{this.state.allGroup.map(ws => {*/}
                          {/*  return <Option value={ws._id}>{ws.groupName}</Option>*/}
                          {/*})}*/}
                        </Select>
                      </div>
                    </div>
                    <div style={{display: 'flex', marginBottom: 24}}>
                      <div
                        style={{width: 104, textAlign: 'right', marginRight: 16, lineHeight: '30px', marginLeft: -20}}>
                        <span style={{color: '#D54941'}}>*</span>选择代理模式
                      </div>
                      <div>
                        <Select style={{width: 400}} value={this.state.proxyAdd.protocol || ''}
                                onChange={v => this.setState({proxyAdd: {...this.state.proxyAdd, protocol: v}})}>
                          {protocal.map(p => (
                            <Option key={p.value} value={p.value}>
                              {p.label}
                            </Option>
                          ))}
                          {/*{this.state.allGroup.map(ws => {*/}
                          {/*  return <Option value={ws._id}>{ws.groupName}</Option>*/}
                          {/*})}*/}
                        </Select>
                      </div>
                    </div>
                    <div style={{display: 'flex', marginBottom: 24}}>
                      <div style={{width: 84, textAlign: 'right', marginRight: 16, lineHeight: '30px'}}>IP重用次数</div>
                      <div style={{display: 'flex'}}>
                        <Input value={this.state.proxyAdd.maxVpsNum || ''} style={{width: 400}} onChange={(e) => {
                          this.setState({proxyAdd: {...this.state.proxyAdd, maxVpsNum: e.target.value}})
                        }}/>
                      </div>
                    </div>
                  </> : <>

                    <div style={{display: 'flex', marginBottom: 24}}>
                      <div style={{width: 84, textAlign: 'right', marginRight: 16, lineHeight: '30px'}}><span
                        style={{color: '#D54941'}}>*</span>Token/密钥
                      </div>
                      <div style={{display: 'flex'}}>
                        <Input style={{width: 400}} value={this.state.proxyAdd.token || ''} onChange={(e) => {
                          this.setState({proxyAdd: {...this.state.proxyAdd, token: e.target.value}})
                        }}/>
                      </div>
                    </div>
                    <div style={{display: 'flex', marginBottom: 24}}>
                      <div style={{width: 84, textAlign: 'right', marginRight: 16, lineHeight: '30px'}}>账号id</div>
                      <div style={{display: 'flex'}}>
                        <Input value={this.state.proxyAdd.id || ''} style={{width: 400}} onChange={(e) => {
                          this.setState({proxyAdd: {...this.state.proxyAdd, id: e.target.value}})
                        }}/>
                      </div>
                    </div>
                  </>}
                </>
            }
          </div>
        </Dialog>
        {this.state.isUploading && (
          <div
            style={{
              position: 'fixed',
              top: 0,
              left: 0,
              width: '100vw',
              height: '100vh',
              backgroundColor: 'rgba(0, 0, 0, 0.5)', // 半透明遮罩
              zIndex: 9999,
              display: 'flex',
              justifyContent: 'center',
              alignItems: 'center',
              pointerEvents: 'auto', // 允许内部元素交互
            }}
          >
            <div
              style={{
                backgroundColor: 'white',
                padding: '24px 32px',
                borderRadius: '8px',
                textAlign: 'center',
              }}
            >
              <Spin size="large" style={{marginBottom: '16px'}}/>
              <p>正在处理文件，请稍候...</p>
            </div>
          </div>
        )}
        {/*<Table pagination={this.state.pagination} rowSelection={{*/}
        {/*    selectedRowKeys: this.state.selectedRowKeys,*/}
        {/*    onChange: this.onRowSelectionChange.bind(this)*/}
        {/*  }} columns={columns} rowKey='_id' dataSource={this.state.data} loading={this.state.loading}*/}
        {/*       onChange={this.handleTableChange.bind(this)}/>*/}
      </div>
    </ExternalAccountFilter></MyTranslate>)
  }
}

export default injectIntl(MyComponent)

// export default connect(({user}) => ({
//   userID: user.info.userID,
//   createVps: user.info.createVps,
// }))(MyComponent)
