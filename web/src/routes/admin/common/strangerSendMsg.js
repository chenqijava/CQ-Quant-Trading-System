import React, {Component} from 'react'
import {BrowserRouter as Router, Route, Link} from 'react-router-dom'
import {
  Icon,
  Table,
  Divider,
  message,
  Row,
  Col,
  Button,
  Radio,
  //Alert,
  Select,
  Card,
  Avatar,
  Upload,
  Input,
  Tabs,
  DatePicker,
  Modal,
  Switch,
  Tag, TimePicker, InputNumber,
  Tooltip
} from 'antd'
import axios from 'axios'
import {formatDate} from 'components/DateFormat'
import accountStatus from 'components/accountStatus'
import taskStatuss from 'components/taskStatuss'
import EditStrangerSendMsgTaskParamsBtn from '../../../components/common/EditStrangerSendMsgTaskParamsBtn'

import InputVoiceMsg from "components/msg/inputVoiceMsg";
import InputVideoMsg from "components/msg/inputVideoMsg";
import InputCardMsg from "components/msg/inputCardMsg";
import InputFwdMsg from "components/msg/inputFwdMsg";
import InputImageMsg from "components/msg/inputImageMsg";
import InputTextMsg from "components/msg/inputTextMsg";
import InputFinderFeed from "components/msg/inputFinderFeed";
import InputIdCard from "components/msg/inputIdCard";
import CardIntroImage from "images/cardmsg_intro.jpg";
import SelectAccounts from "components/select/SelectAccounts";
import moment from "moment";
import tTypes from "components/taskTypes";
import addMethods from "components/addFriendAddMethods";
import addDataTypes from "components/addFriendDataTypes";

import messageUtils from 'components/messageUtils'
import TaskProcessDetail  from "./taskProcessDetail";

import { Pagination, Breadcrumb, Dialog, Steps, Space, Upload as TUpload, Alert, Tooltip as TTooltip, Button as TButton} from 'tdesign-react';
import DialogApi from './dialog/DialogApi';

const {Option, OptGroup} = Select;
const {TabPane} = Tabs;
const {TextArea} = Input;
const confirm = Modal.confirm;
const Search = Input.Search;
const {BreadcrumbItem} = Breadcrumb;
const { StepItem } = Steps;

// 针对当前页面的基础url
const baseUrl = '/api/friend';
const consumer = '/api/consumer/friend';
//定时任务url
const baseTaskUrl = '/api/consumer/task';
// 通用参数设置
const params = [{
  type: 'strangerSendMsg',
  code: 'syncData',
  desc: '前置同步账号',
}];

const gridStyle = {
  width: '50%',
  padding: "10px"
};
const gridStyle2 = {
  width: '50%',
  padding: "10px",
  textAlign: 'center',
};

const searchItems = [
  { desc: '任务描述', key: 'desc' }
];

const steps = [
  { title: 'chooseAccounts', content: '选择账号' },
  { title: 'setMsgObject', content: '设置消息对象' },
  { title: 'setChatroomContent', content: '设置群发内容' },
];
const uploadProps = {
  name: 'file',
  multiple: false,
  action: `/api/consumer/res/upload/strangerSendMsg`,
  beforeUpload(file, fileList) {
    let csv = /^.+\.csv/.test(file.name.toLowerCase());
    let txt = /^.+\.txt/.test(file.name.toLowerCase());
    if (!csv && !txt) {
      message.error('文件名不合法')
    }
    return csv || txt
  }
};

const getUploadImageProps = (regExpStr = 'jpg|png|jpeg') => {
  let regExp = new RegExp(`^.+\\.(${regExpStr})$`)
  const uploadProps = {
    name: 'file',
    multiple: false,
    action: `/api/consumer/res/upload/wxHeadUpload`,
    beforeUpload(file, fileList) {
      const isLt2M = file.size / 1024 / 1024 < 3;
      if (!isLt2M) {
        message.error('文件必须小于3MB!');
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

const materials = [
  { label: '文本', value: 'text' }, 
  { label: '图片', value: 'image' },
  { label: '转发', value: 'link' }, 
  { label: '语音', value: 'voice' },
  { label: '场景', value: 'scene' }, 
  //{ label: '剧本', value: 'script' },
]

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
        total: 0,
        pageSize: 10,
        current: 1,
        showTotal: (total, range) => `共 ${total} 条`,
        position: 'both',
        showSizeChanger: true,
        pageSizeOptions: ['10', '20', '30', '40', '50', '100', '200', '300', '400', '500'],
      },


      choosenPagination: {
        total: 0,
        pageSize: 10,
        current: 1,
        showTotal: (total, range) => `共 ${total} 条`,
        position: 'both'
      },

      choosenTab: 'accountGroup',      //accountGroup/account
      selectedRowKeys: [],
      accountGroupSelectedRowKeys: [],
      addMethod: '1',        //录入方式   1：页面录入  2：上传文件
      uploadFile: null,   
      addDataFilePath:null,     //上传文件服务器地址
      accountCount: 0,

      addDataType: '1',      //录入信息类型       1: username   2: 手机号

      accountLoading: false,
      groups: [],
      selectAccountGroup: [],
      onlineStatus: {},       //账号在线状态 {accID: onlineStatus}

      labelLoading: false,
      labels: [],
      labelMap: {},
      selectedLabels: [],
      taskTotal: 1000,

      sendMethod: '1',
      sendDateTime: '',

      activeTabKey: 'text',      //发送文字、图片、语音、链接
      text: '',                    //文字
      templateValues: '',         //  模版消息的替换内容
      images: [],
      voices: [],
      videos: [],
      position: '',//转发时发送一条文本,文本的位置,top,bottom

      friendSyncNum: 10,

      msgLimit: 0,//每个账号发消息数量限制,0:不限制
      accountPercentage:50,// 最少百分比
      minSendSuccessCount:3,// 最少发送数量      在使用分批次发送时，最少百分比和最少发送数量 用来控制任务是否继续执行  该批账号有百分之多少的账号发送成功次数小于最少发送数量，则任务停止
      accountLimit:500,//同时执行任务账号
      everyDayMsgLimit:'',//每个账号每天发送个数限制, 固定账号个数时,每个账号每天发消息数量大于等于这个值以后,账号自动下线
      distributionModel:'1',//分配模式 一次性分配
      startHour:8,//每天开始时间

      beginInterval: 0,

      interval: 30,

      addInterval: 30,

      editInterval: 3,
      beEditText:'',

      friendAddScenes: [],
      friendAddScene: null,

      sendHistory: [],

      uploadParamFiles: [],        //上传变量文件

      addEmoji:'0',//是否自动添加表情

      config: {},   // 用户参数存储

      needPinMsg: false, // 是否需要pin消息
      sameParam:false,  //同一账号发送变量内容是否相同
      batchOp: false,  //分批次发送
      showTable: false,
      scrollY: 0,
      tableContent: null,
      process: false, //进度,
      quartzDetail : null, //任务进度
      statusName: {
        'all': '全部',
      },
      statuses: [
        { label: '全部', value: 'all' },
      ],
      status: 'all',
      currentStep: 0,
      chooseGroup: false,
      accountGroupList: [],
      accountList: [],
      chooseType: '',
      updateParamsVisible: false, //修改参数
      creatTaskVisible: false, //新建任务
      chooseAccounts: false, //选择账号
      setMsgObject: false, //设置消息对象
      setChatroomContent: false, //设置群发内容
      uploadStatus: '',            //图片上传状态  uploading/done
      uploadAccountAvatarFile: '',
      filters: {},
      chooseTypeName: '选择',
      material: 'text', //素材类型
      materialContent: '', //素材
      materials: [],
      materialContents: [],
      materialContentsMap: {},
      fwd: '',//转发
      channelUrl: '',
      msgIDs: '',//消息id
      lookTaskVisible: '', //查看
      lookTaskDesc: '',
      lookAddMethod: '',
      lookAddData: '',
      lookTimingTimes: 1,
      lookTimingInterval: 1,
      lookTimingIntervalUnit: 'm',
      lookAddEmoji: '0',
      lookActiveTabKey: '',
      lookText: '',
      lookAccountGroupIds: [],
      lookIds: [],
      lookChoosenTab: '',
      lookFilePath:'',
      lookImage:'',
      lookExecuteType:1,
      lookSendTime:''
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
      //onlineStatus: '1',
      type: this.query.type || 'sendStrangerMsg',   //选中的任务类型
    };
      // sorter的写法保持与mongo sort的写法一致，避免与服务器、数据库直接的数据阻抗失衡
      // 例如：sorter={createTime: -1}
      // 注意：Table控件仅支持单列排序，不支持多列同时排序
      this.sorter = {
        createTime: -1
      };
    // 提前写在constructor则Table首次加载时生效
    // 但是因为没有放到state中，所以除sorter外，都不建议提前设置，保持不受控状态是最好的
    this.selectedFriends = []        //  选中要发送的好友名片
    
  }

  // 首次加载数据
  async componentWillMount() {
    this.reload()
    this.reloadConfig();
    //this.searchAllLabel();
    this.loadAccountGroup();
    this.loadAccount();
  }

  handleResize = () => {
    if (this.state.tableContent) {
      this.setState({scrollY: this.state.tableContent.getBoundingClientRect().height - 80,})
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
    let res = await axios.post(`${baseTaskUrl}/${pagination.pageSize}/${pagination.current}`, { filters, sorter });
    pagination.total = res.data.total;

    this.setState({
      loading: false,
      data: res.data.data,
      pagination,
      selectedRowKeys: []
    });
    this.selectedRows = [];
    this.filters = filters;
    this.sorter = sorter
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

  async loadAccountGroup() {
    let res = await axios.post(`/api/consumer/accountGroup/10000/1`, {
      accFilter: null,
      filters: null,
      sorter: this.state.sorter,
      unBanned: true
    });
    this.setState({ accountGroupList: res.data.data.data })
  }

  async loadAccount() {
    let res = await axios.post(`/api/consumer/account/10000/1`, {
      filters: null,
      sorter: this.state.sorter,
      unBanned: true
    });

    this.setState({ accountList: res.data.data.data })
  }

  async reloadConfig() {
    let config = {};
    for (let i in params) {
      let param = params[i];
      let res = await axios.get(`/api/consumer/userParams/${param.type || pageType}/get/${param.code}`);
      if (res.data.code) {
        config[`${param.type}-${param.code}`] = param.unit ? res.data.value / param.unit : res.data.value;
      }
    }
    this.setState({config});
  }

  async searchAllLabel() {
    this.setState({labelLoading: true});
    let res = await axios.post(`/api/consumer/friendLabel/10000/1`, {filters: {}});
    let map = {};
    for (const label of res.data.data) {
      map[label._id] = label.label;
    }
    this.setState({labelLoading: false, labels: res.data.data, labelTotal: res.data.total, labelMap: map});
  }

  /**
   * 修改参数
   **/
  async onParamsSetChange(param, value) {
    this.setState({loading: true});
    let res = await axios.post(`/api/consumer/userParams/${param.type || pageType}/set/${param.code}`, {value: param.unit ? Number(value) * param.unit : value});
    if (res.data.code == 1) {
      // 修改配置后重新加载所有配置
      await this.reloadConfig();
      message.success('操作成功');
    } else {
      Modal.error({title: res.data.message});
    }
    this.setState({loading: false})
  }
  async componentDidUpdate(prevProps) {

  }


  async onDateTimeChange(value, dateString) {
    console.log('Selected Time: ', value);
    console.log('Formatted Selected Time: ', dateString);
    this.setState({sendDateTime: value});
  }

  onSendMsgTabChange = async (activeTabKey) => {
    this.setState({activeTabKey: activeTabKey});
    if (activeTabKey == 'friendAddScene') {
      this.selectAllFriendAddScenes()
    }
  };

  async selectAccountsOnChange(data) {
    if(data == null){
      return;
    }
    this.setState(data)
  }

  checkTextReplaceRules(text, data) {
    return messageUtils.checkTextReplaceRules(this.state,text,data)
  }

  // 输入文本以后,验证文本
  _checkText=(data)=>{
    data.text = this.state.text;
    let error = this.checkTextReplaceRules(data.text, data);
    if (error) {
      return error;
    }
    if (this.state.beEditText) {
      let beEditText = this.state.beEditText.trim();
      if (beEditText) {
        let error = this.checkTextReplaceRules(beEditText, data);
        if (error) {
          return error
        }
        data.data = {
          beEditText,
          editInterval: this.state.editInterval || 0,
        }
      }
    }
    return false;
  }

  //提交数据
  submitData = async()=> {
    if (!this.state.taskDesc) {
      message.error('请填写任务描述');
      return
    }
    if (this.state.sendMethod === '2' && !this.state.sendDateTime) {
      message.error('请填写定时发送日期时间');
      return
    }

    let msgLimit = this.state.msgLimit //每个账号发消息数量限制,0:不限制
    let everyDayMsgLimit = this.state.everyDayMsgLimit //每天每个账号发消息数量限制,0:不限制
    let batchOp = this.state.batchOp //是否分批操作
    if (this.state.distributionModel == '3'){
      // msgLimit = 0;// 固定分配模式下,不限制,使用everyDayMsgLimit 限制每个账号每天发送消息数
      batchOp = true;// 固定分配模式下,需要分批操作
    }else{
      everyDayMsgLimit = 0
    }

    if (this.state.choosenTab === 'account' && this.state.selectedRowKeys.length === 0) {
      message.error("请选择账号");
      return
    }
    if (this.state.choosenTab === 'accountGroup') {
      if (this.state.accountGroupSelectedRowKeys.length === 0) {
        message.error("请选择分组");
        return
      }
    }
    const sendFriend = this.state.addDataType == 'friend'

   if (this.state.addMethod === '2' && this.state.addDataFilePath==null) {
    message.error("请上传文件");
    return
  }
  if(this.state.addMethod === '1'){
      if (!this.state.addData || !this.state.addData.trim()) {
        message.error("请录入信息");
        return
      } else {
        let targets = [];
        let ads = this.state.addData.trim().split("\n");
        for (let ad of ads) {
          if (ad.trim()) {
            targets.push(ad.trim());
          }
        }
      if (targets== null || targets.length ===0 ) {
          message.error("请录入信息");
          return
        }
      }
    }
      


    // 选择动态分配模式,需要选择分组
    if(this.state.distributionModel=='2' && this.state.choosenTab != 'accountGroup' && this.state.accountGroupSelectedRowKeys.length == 0){
      message.error("选中了动态分配,请选择发消息的分组");
      return;
    }

    let data = {
      taskDesc: this.state.taskDesc,            //  任务描述
      ids: this.state.selectedRowKeys,
      addMethod: this.state.addMethod,           //录入方式   1：页面录入  2：上传文件
      addDataType: this.state.addDataType,       //录入信息类型       1: username   2: 手机号
      addData: this.state.addData,  
      addDataFilePath:this.state.addDataFilePath,             //录入信息
      //basename: this.state.uploadFile ? this.state.uploadFile[0].response.basename : '',
      filepath: this.state.uploadFile ? this.state.uploadFile.response.filepath : '',
      filename: this.state.uploadFile ? this.state.uploadFile.name : '',
      activeTabKey: this.state.activeTabKey,
      beginInterval: this.state.beginInterval,
      interval: this.state.interval,
      addInterval: this.state.addInterval,
      sendMethod: this.state.sendMethod,
      sendDateTime: this.state.sendDateTime,
      images:this.state.images,
      // friendSyncNum: this.state.friendSyncNum,    //  每个账号分配的任务数
      uploadParamFiles: this.state.uploadParamFiles.map(file => ({filepath: file.response.filepath,basename:file.response.basename, filename: file.name})),//变量相关文件
      addEmoji:this.state.addEmoji,//是否自动添加表情,1:添加,0:不添加(默认)
      msgLimit,//每个账号发消息数量限制,0:不限制
      everyDayMsgLimit,//每天每个账号发消息数量限制,0:不限制
      accountPercentage:this.state.accountPercentage,//最少百分比，用于判断任务是否停止
      minSendSuccessCount:this.state.minSendSuccessCount,//最少发送数量
      accountLimit:this.state.accountLimit,//同时执行任务账号
      distributionModel:this.state.distributionModel,//分配模式
      needPinMsg: this.state.needPinMsg,//是否需要pin消息
      sameParam: this.state.sameParam,//是否需要pin消息
      batchOp: batchOp,//是否分批操作
      startHour: this.state.startHour,//每天开始时间
      labels: this.state.selectedLabels,  //  要发送的好友的标签
      taskTotal: this.state.taskTotal,    //  要发送的数量
    };
    if (this.state.choosenTab === 'accountGroup') {
      data.accountGroupIds = this.state.accountGroupSelectedRowKeys;
    }
    switch (this.state.activeTabKey) {
      case 'text':
        if (this.state.text) {
          let error = messageUtils.checkText(this.state,data);
          if (error){
            return message.error(error);;
          }
        } else {
          message.error('请输入文字内容');
          return
        }
        break;
      case 'picture':
        if (this.state.uploadStatus == 'uploading') {
          message.error('图片正在上传中...');
          return
        }
        if (this.state.images) {
          // if (this.state.images[0].status !== "done") {
          //   message.error('图片未上传成功，请重新上传');
          //   return
          // }
          // data.files = this.state.images.map(file => ({
          //   fileName: file.response.newname,
          //   fileType: file.type,
          // }));
          if (this.state.images.response) {
            let files = [];
            let file = {};
            file.fileName = this.state.images.response.newname ? this.state.images.response.newname : null;
            file.fileType = this.state.images.type ? this.state.images.type : null;
            files.push(file);
            data.files = files;
          }
          // data.fileName = this.state.images[0].response.newname;
          // data.thumbUrl = this.state.images[0].response.thumbUrl;
          // data.fileType = this.state.images[0].type;
          // data.filename = this.state.images[0].name;

          if (this.state.text){
            let error = messageUtils.checkText(this.state,data);
            if (error){
              return message.error(error);;
            }
          }
        } else {
          message.error('请上传一张图片');
          return
        }
        break;
      case 'card':
        if (!this.state.card) {
          message.error('未填写卡片消息');
          return
        }
        let card = this.state.card;
        let {url, text} = card;
        if (!text || text.indexOf(url) === -1) {
          let split = '';
          if (text && [' ', '\n'].indexOf(text[text.length - 1]) === -1) {    //  链接和前面的文字之间需要有空格存在
            split = '\n';
          }
          card.text = (text || '') + split + url;
        }
        data.data = {
          card
        };
        break;
      case 'fwd':
        if (!this.state.fwd) {
          message.error('未填写转发消息');
          return
        }
        if (this.state.text) {
          let error = messageUtils.checkText(this.state,data);
          if (error){
            return message.error(error);;
          }
        }
        let fwd = {url: this.state.channelUrl, msgIDs: this.state.msgIDs};
        let position = this.state.position;
        data.data = {
          fwd,
          position,
        };
        break;
      case 'voice':
        if (this.state.voices.length === 0) {
          message.error('请上传语音');
          return;
        }
        if (this.state.voices[0].status !== "done") {
          message.error('语音未上传成功，请重新上传');
          return;
        }
        data.data = {
          fileName: this.state.voices[0].response.newname,
          fileType: this.state.voices[0].type,
          newFileName: this.state.voices[0].response.newFileName,
          duration: this.state.voices[0].response.duration,
        };
        break;
      case 'video':
        if (this.state.videos.length === 0) {
          message.error('请上传语音');
          return;
        }
        if (this.state.videos[0].status !== "done") {
          message.error('视频未上传成功，请重新上传');
          return;
        }
        data.data = {
          video: {
            name: this.state.videos[0].name,
            filepath: this.state.videos[0].response.filepath,
          }
        };
        break;
      case 'friendAddScene':
        //素材选择
        if (this.state.material == 'text') {
          let material = this.state.materialContentsMap[this.state.materialContent];
          if (!material) {
            message.error('无素材内容');
            return
          }
          //文本
          if (material.content) {
            data.text = material.content;
            data.type = 'text';
          } else {
            message.error('无素材文本');
            return
          }
        } else if (this.state.material == 'image') {
          let material = this.state.materialContentsMap[this.state.materialContent];
          if (!material) {
            message.error('无素材内容');
            return
          }
          //图片
          data.type = 'picture';
          
         if (material.content) {
           //存在文本, 即图+文模式
           data.text = material.content;
         }

          // [
          //   {
          //     "_id": "67dd31e4a3a61d003b8d3be2",
          //     "filepath": "image/upload_8b4ac00a35e28033106491ca96bcd4cb.jpg",
          //     "name": "photo_2025-03-14_13-56-18.jpg"
          //   }
          // ]
          if (material.image && material.image.length) {
            for (let con of material.image) {
              let files = [];
              let file = {};
              //let fileSpilt = con.filepath.split('.');
              file.fileName = con.filepath ? con.filepath : null;
              file.fileType = con.name ? 'image/' + con.name.split('.')[1] : null;
              files.push(file);
              data.files = files;
            }
          } else {
            message.error('无素材图片');
            return
          }
        } else if (this.state.material == 'link') {
          let material = this.state.materialContentsMap[this.state.materialContent];
          if (!material) {
            message.error('无素材内容');
            return
          }
          //转发
          if (!material.fwd) {
            message.error('未填写转发消息');
            return
          }

          let strs = material.fwd.split('/');
          let channelUrl = strs.slice(0,strs.length-1).join('/');
          let channelMsgIDs = strs[strs.length-1];

          if (material.fwd != undefined && !/^https?:\/\/.+/.test(material.fwd)) {
            message.error('请输入正确的链接')
            return
          }

          // msgIDs
          if (!channelMsgIDs || !/^\d+([,#]\d+)*\d*$/.test(channelMsgIDs)) {
            message.error('请输入正确的链接,没有消息id')
            return
          }
          data.type = 'fwd';
          let fwd = {url: channelUrl, msgIDs: channelMsgIDs};
          let position = this.state.position;
          data.data = {
            fwd,
            position,
          };
        } else if (this.state.material == 'voice') {
          let material = this.state.materialContentsMap[this.state.materialContent];
          if (!material) {
            message.error('无素材内容');
            return
          }
          //语音
          if (!material.voice) {
            message.error('无素材语音');
            return;
          }
          for (let con of material.voice) {
            let file = {};
            //let fileSpilt = con.filepath.split('.');
            //file.fileName = fileSpilt.length ? fileSpilt[0] : null;
            file.fileName = con.filepath ? con.filepath : null;
            //file.fileType = con.name ? 'image/' + con.name.split('.')[1] : null;
            file.fileType = "audio/mpeg";
            file.newFileName = con.filepath ? con.filepath : null;
            file.duration = "";
            //file.newFileName = con.name ? con.name : null;
            //file.duration = 3;
            data.data = file;
          }
          // data.data = {
          //   fileName: this.state.voices[0].response.newname,
          //   fileType: this.state.voices[0].type,
          //   newFileName: this.state.voices[0].response.newFileName,
          //   duration: this.state.voices[0].response.duration,
          // };
          data.type = 'voice';
        } else if (this.state.material == 'scene') {
          //场景
          if (!this.state.friendAddScene) {
            message.error('请选择场景');
            return;
          }
          data.data = {
            friendAddScene: this.state.friendAddScene
          };
        }
        break;
      default:
        message.error('消息类型错误');
        return;
    }

    try{
      this.setState({loading: true});
      let result = await axios.post(`${baseTaskUrl}/createTask`, data);
      if(result.data.code !== 1){
      // this.setState({loading: false});
        message.error(result.data.message);
        return;
        //Modal.error({ title: '失败', content: result.data.message || "unknown", okText: "关闭" });
      }else{
        let sendHistory = this.state.sendHistory;
        sendHistory.push(data);
        console.log(sendHistory);
        this.setState({sendMsgVisible: false, loading: false,addDataFilePath:null,images:null, sendHistory});
        if (this.state.sendMethod === '1') {
          if (result.data.failedType && result.data.failedType.length) {
            message.error(result.data.message);
          } else {
            message.success('操作成功');
          }
        } else {
          message.success('任务创建成功');
        }
      }
    }catch (e) {
      this.setState({loading: false});
      //Modal.error({ title: '错误', content: e.message || "unknown", okText: "关闭" });
      message.error(res.data.message || '任务创建失败')
    }
    this.setState({creatTaskVisible:false, currentStep: 0, loading: false, accountGroupSelectedRowKeys: []});
    this.cancel()
    this.reload()
  };

  onUploadChange = async(info) => {
    console.log("上传信息",info[0].response.data.filepath);
    if (info.length > 0) {
      this.setState({addDataFilePath: info[0].response.data.filepath});
      return
    }

   
    // if (info.file.status) {
    //   this.setState({uploadFile: [info.file]});
    // }
    // console.log(info.file);
  };

  onUploadAccountAvatarChange = async (info) => {
    if (info.length > 0) {
      this.setState({images: info[0].response.data.filepath});
      return
    }
    if (info && info.file && info.file.status) {
      this.setState({images: info.fileList, uploadStatus: info.file.status});
    }
  }
  
  onUploadParamChange = async(info) => {

    if (info.file.status) {
      this.setState({uploadParamFiles:info.fileList});
    }
  };

  onAddDataChange = async(e)=> {
    let addData = e.target.value;
    let targets = [];
    let ads = addData.trim().split("\n");
    for (let ad of ads) {
      let sp = ad.split(',');
      sp[0] = sp[0].replace(/[^\d]/g, '');
      ad = sp.join(',');
      if (!/^\+?\d*(,.*)?$/.test(ad)) {
        message.error("通过通讯录添加好友,只能输入手机号,分割符只能用英文逗号(,)");
        return
      }
      if (ad.trim()) {
        targets.push(ad.trim());
      }
    }
    if (targets.length > 500) {
      message.error('录入的信息过多(大于500)，请使用"上传文件"功能上传数据');
      return
    }
    this.setState({addData: e.target.value})
  };

  async selectAllFriendAddScenes() {
    let res = await axios.post(`/api/consumer/material/scene/1000/1`, {projection: {interactiveContent: 0}});
    this.setState({friendAddScenes: res.data.data})
  }

  async changeTaskType(e) {
    e = { target: { value: e } };
    this.setState({ status: e.target.value });
    if (e.target.value == 'all') {
      delete this.filters.status;
    } else {
      this.filters.status = e.target.value;
    }
    //this.reload()
  }

  async reset() {
    this.setState({status: 'all'})
    delete this.filters.status
    delete this.filters.desc
    delete this.state.filters.desc
    this.load(this.state.pagination, this.filters, this.sorter)
  }

  async taskFinishForce(r) {
    // if (r.status == 'success') {
    //   return message.error('任务已经结束,无法进行操作')
    // }
    DialogApi.warning({
      title: '确定要强制停止这个任务吗？',
      content: '',
      onOkTxt: '确定',
      onCancelTxt: '取消',
      onOk: async () => {
        this.setState({ loading: true });
        let result = await axios.post(`/api/task/finishForce`, { ids: [r._id] });
        if (!result.data.code) {
          message.error('停止任务失败，' + result.data.message)
          this.setState({ loading: false });
        } else {
          message.success('操作成功');
          this.reload()
        }
      },
      onCancel() {
      }
    })
  }

  async lookDetail(r) {
    const param = r.params;
    console.log(param)
    this.setState({ lookChoosenTab: 'account', lookIds: r.ids,lookTaskVisible: true });
    if (param.groupIds && param.groupIds.length > 0) {
      this.setState({ lookChoosenTab: 'accountGroup', lookAccountGroupIds: param.groupIds });
    } 
    if (param) {
      this.setState({ lookTaskDesc: param.taskDesc, lookAddMethod: param.addMethod, 
        lookAddData: param.addDatas, lookActiveTabKey: param.activeTabKey ,
        lookFilePath: param.filepath,lookImage:param.imageFilePath,lookExecuteType:param.sendMethod,
        lookSendTime:r.executeTime,lookAccountGroupIds:param.groupIds});
      this.setState({ lookText: param.content, lookTimingTimes: param.timingTimes, lookTimingInterval: param.timingInterval, lookTimingIntervalUnit: param.timingIntervalUnit });
    }
  }

  async delete() {
    let keys = [];
    if (this.state.selectedRowKeys.length == 0) {
      message.error('请先选择数据');
      return
    }
    for (const row of this.selectedRows) {
      keys.push(row._id);
    }

    if (keys.length == 0) return;

    DialogApi.warning({
      title: '确定要删除这些数据？',
      content: '',
      onOkText: '确定',
      onCancelText: '取消',
      onOk: async () => {
        this.setState({ loading: true })
        let res = await axios.post(`${baseTaskUrl}/delete`, keys)
        if (res.data.code == 1) {
          message.success('操作成功');
          this.reload()
        } else {
          message.error(result.data.message)
          this.setState({ loading: false });
        }
        this.reload()
      },
      onCancel() {
      }
    })
  }
  
  async taskFinish(r) {
    if (r.status == 'success') {
      return message.error('任务已经结束,无法进行操作')
    }
    DialogApi.warning({
      title: '确定要结束这个任务吗？',
      content: '',
      onOkTxt: '确定',
      onCancelTxt: '取消',
      onOk: async () => {
        this.setState({ loading: true });
        let data = { ids: [r._id] }
        let result = await axios.post(`${baseTaskUrl}/finish`, data);
        if (result.data.code != 1) {
          message.error('结束任务失败，' + result.data.message)
        } else {
          message.success('操作成功');
          this.reload()
        }
      },
      onCancel() {
      }
    })
  }
  
  async taskDetail(r) {
    //let params = [`id=${r._id}`, 'from=quartz'];
    // if (this.filters.type && this.filters.type != 'all') {
    //   params.push(['type', this.filters.type].join('='))
    // }

    this.setState({process: true, quartzDetail: r._id})  
  }

  createOpen = (r) => {
    this.setState({creatTaskVisible: true, chooseAccounts: true, setChatroomContent: false, images:null})  
  }

  async chooseGroup (choose) {
    this.setState({chooseGroup: true, chooseType: choose, choosenTab: choose})
    if (choose == 'accountGroup') {
      this.setState({ chooseTypeName: '选择分组' })
    } else {
      this.setState({ chooseTypeName: '选择账号' })
    } 
  }

  updateParams = (r) => {
    this.setState({updateParamsVisible: true})  
  }

  async updateParamsConfirm(r) {
    if (!this.state.accountLimit) {
      return message.error('请输入同时发送账号数量！');
    }
    
    //TODO
    //let subData = [];

    this.setState({loading: true});
    DialogApi.warning({
      title: '确定要修改任务的参数么',
      content: '',
      onOkTxt: '确定',
      onCancelTxt: '取消',
      onOk: async () => {
        this.setState({ loading: true });
        let result = await axios.post(`/api/consumer/task/saveGroupTask`, { 
          ids: [r._id],

        });
        if (result.data.code != 1) {
          message.error('修改参数失败，' + result.data.message)
          this.setState({ loading: false });
        } else {
          message.success('操作成功');
          this.reload()
        }
      },
      onCancel() {
      }
    })
  }

  async chatroomContentType(e){
    e = { target: { value: e } };
    this.setState({ activeTabKey: e.target.value });
    if (e.target.value == 'friendAddScene') {
      //查询通用素材
      this.selectAllCommonMaterials(this.state.material);
    }
  }

  async choooseMaterialType(e) {
    e = { target: { value: e } };
    this.setState({ material: e.target.value, materialContent: '', friendAddScene: '', friendAddScenes: []});
    if (e.target.value === 'text' || e.target.value === 'image' || e.target.value === 'link' || e.target.value === 'voice') {
      //查询通用素材
      this.selectAllCommonMaterials(e.target.value);

    } else if(e.target.value === 'scene'){
      //查询场景
      this.selectAllFriendAddScenes();

    } else if(e.target.value === 'script'){
      //查询剧本
      this.selectAllChatroomScenes();
    }
  }

  async selectAllCommonMaterials(type) {
    let res = await axios.post(`/api/consumer/material/1000/1`, {filters: {materialType: type}});
    this.setState({materialContents: res.data.data})

    let materialContentsMap = {};
    for (let c of res.data.data) {
      materialContentsMap[c._id] = c
    }
    
    this.setState({ materialContentsMap })
  }

  // async selectAllFriendAddScenes() {
  //   let res = await axios.post(`/api/consumer/friendAddScene/1000/1`, {projection: {interactiveContent: 0}});
  //   this.setState({friendAddScenes: res.data.data})
  // }

  async selectAllChatroomScenes() {
    let res = await axios.post(`/api/consumer/material/script/1000/1`, {projection: {interactiveContent: 0}});
    this.setState({chatroomScenes: res.data.data})
    // let chatroomScenesMap = {};
    // for (let c of res.data.data) {
    //   chatroomScenesMap[c._id] = c.name
    // }
    // this.setState({chatroomScenesMap})
  }

  onFwdDataChange = async (e) => {
    let url = e.target.value;

    let strs = url.split('/');
    let channelUrl = strs.slice(0,strs.length-1).join('/');
    let channelMsgIDs = strs[strs.length-1];

    if (url != undefined && !/^https?:\/\/.+/.test(url)) {
      message.error('请输入正确的链接')
      return
    }

    // msgIDs
    if (!channelMsgIDs || !/^\d+([,#]\d+)*\d*$/.test(channelMsgIDs)) {
      message.error('请输入正确的链接,没有消息id')
      return
    }

    this.setState({ fwd: e.target.value, channelUrl: channelUrl,  msgIDs: channelMsgIDs})
  };

  cancel = async () => {
    this.setState({ creatTaskVisible: false, currentStep: 0, chooseAccounts: false, setChatroomContent: false, accountGroupSelectedRowKeys: []})
    this.setState({ taskDesc: '', addDataType: '1', selectedLabels: [], taskTotal: 1000, addMethod: '1', addData: '', uploadFile: null, msgLimit: '', interval: 30, everyDayMsgLimit: '', fwd: ''})
    this.setState({ addInterval: 30, accountLimit: 500, startHour: 8, sendMethod: '1', addEmoji: '0', activeTabKey: 'text', text: '', uploadAccountAvatarFile: '', fwd: '', position: '', voices: [], images: [], material:'text', materialContent: '', friendAddScene: ''})
  }

  refTableContent = (ref) => {
    if (ref && ref.getBoundingClientRect) {
      this.setState({showTable: true, scrollY: ref.getBoundingClientRect().height - 80, tableContent: ref})
    }
  }

  // 把columns放到render中，虽然损失部分性能，但是能方便参数中的匿名回调获取实例状态
  render() {

    const sendMethods = [{label: '立即发送', value: '1'}, {label: '定时发送', value: '2'}];
    //分配模式,
    // 一次性分配:就是选中账号一次性分配给任务
    // 动态分配: 选择分组账号,固定账号个数,任务执行过程中自动补足指定个数账号
    const distributionModels = [{label: '一次性分配', value: '1'}, {label: '动态分配', value: '2'},{label: '固定分配', value: '3'}]


    const columns = [
      {
        title: '好友名称',
        dataIndex: 'nickname',
        key: 'nickname',
        render: (v, r)=> {
          return (<div><Avatar src={r.smallHeadPic}/>{v}</div>)
        }
      }, {
        title: '所属账号',
        dataIndex: 'ownerAccid.nickname',
        key: 'ownerAccid.nickname',
        render: (v, r)=> {
          let status = accountStatus.find(ws => ws.value === r.ownerAccid.onlineStatus);
          if (status) {
            return <div>{v} <Tag color={status.color}>{status.label}</Tag></div>
          } else {
            return <div>{v}</div>
          }
        }
      }, {
        title: '标签',
        dataIndex: 'labels',
        key: 'labels',
        render: (v)=> {
          if (v.length) {
            let arr = [];
            for (const id of v) {
              if (this.state.labelMap[id]) arr.push(this.state.labelMap[id]);
            }
            return `${arr.join(",")}`
          } else
            return '0个标签'
        }
      }
    ];

    const sendHistoryColumns = [{
      title: '发送方式',
      dataIndex: 'sendMethod',
      key: 'sendMethod',
      render: (v, r) => sendMethods.find(m => m.value === v).label,
    }, {
      title: '发送时间',
      dataIndex: 'sendDateTime',
      key: 'sendDateTime',
      render: formatDate,
    }, {
      title: '发送类型',
      dataIndex: 'type',
      key: 'type',
      render: (v, r) => {
        switch (v) {
          case 'text':
            return '文字';
          case 'picture':
            return '图片';
          case 'voice':
            return '语音';
          case 'appshare':
            return '小程序';
          case 'idcard':
            return '好友名片';
        }
      }
    }, {
      title: '发送内容',
      dataIndex: 'text',
      key: 'text',
    }];

    const taskColumns = [
      {
        title: '任务描述',
        dataIndex: 'desc',
        key: 'desc',
        width: 300,
        ellipsis: true,
        render: (v, r) => {
          return <div style={{ 'word-wrap': 'break-word', 'white-space': 'normal', width: '260px' }}>
            {
              v || '陌生人消息(分批)'
            }
          </div>
        }
      }, {
        title: '总数',
        dataIndex: 'total',
        key: 'total',
        width: 120,
        ellipsis: true,
        render: (v,r) => {
          if (r.status == 'failed' || r.status == 'success') {
          return r.success + r.failed;
          }
          return r.total;
        }
      }, {
        title: '成功',
        dataIndex: 'success',
        key: 'success',
        width: 120,
        ellipsis: true,
      }, {
        title: '失败',
        dataIndex: 'failed',
        key: 'failed',
        width: 120,
        ellipsis: true,
      }, {
        title: '状态',
        dataIndex: 'status',
        key: 'status',
        width: 120,
        ellipsis: true,
        render: (status, r) => {
          let result = taskStatuss[status] || status;
          if (status == 'failed' || status == 'success') {
            return '完成';
          }
          return result;
        }
      }, {
        title: '创建时间',
        dataIndex: 'createTime',
        key: 'createTime',
        render: formatDate,
        width: 160,
        ellipsis: true,
      }, {
        title: '完成时间',
        dataIndex: 'finishTime',
        key: 'finishTime',
        render: formatDate,
        width: 160,
        ellipsis: true,
      }, {
        title: '操作',
        dataIndex: 'oper',
        key: 'oper',
        width: 280,
        ellipsis: true,
        render: (v, r) => {
          if (r.status == 'failed' || r.status == 'success') {
            return (<div>
              <Button type="link" onClick={() => { this.lookDetail(r) }}>查看</Button>
              <Button type="link" onClick={() => { this.taskDetail(r) }}>进度</Button>
              {/* <Button type="link" onClick={() => { this.updateParams(r) }}>修改参数</Button> */}
              {/* <EditStrangerSendMsgTaskParamsBtn task={r} reload={async () => this.reload()} /> */}
              {/* <Button type="link" onClick={() => { this.restartTask(r) }}>重启任务</Button> */}
            </div>)
          } else {
            return (<div>
              <Button type="link" onClick={() => { this.lookDetail(r) }}>查看</Button>
              <Button type="link" onClick={() => { this.taskDetail(r) }}>进度</Button>
              <Button type="link" onClick={() => { this.taskFinish(r) }}>结束任务</Button>
            </div>)
          }
        }
      }
    ];
    return (<div>
      <Breadcrumb>
        <BreadcrumbItem>群发管理</BreadcrumbItem>
        <BreadcrumbItem>RCS群发</BreadcrumbItem>
      </Breadcrumb>

      <div style={{padding: '20px 0px' }}>
        <Alert
          title="提示"
          message={(<p>
            <ul style={{listStyle: 'none', marginLeft: '-20px'}}>
              <li>1.请先少量发送测试文案，如果没有进垃圾收件箱，再进行大批量群发。</li>
            </ul>
          </p>)}
        />
      </div>

      <div className="search-box">
        {searchItems.map(fOpt => {
          return <div className='search-item'>
            <div className="search-item-label">{fOpt.desc}</div>
            <div className="search-item-right">
              <Input style={{
                width: 200
              }} placeholder={'请输入'} value={this.state.filters[fOpt.key]} values={{ value: fOpt.desc }} defaultValue={fOpt.defaultValue} onChange={(value) => {
                value = value.target.value;
                if (value) {
                  this.filters[fOpt.key] = value;
                  this.state.filters[fOpt.key] = value;
                }
                else {
                  delete this.filters[fOpt.key];
                  delete this.state.filters[fOpt.key]
                }
                delete fOpt.defaultValue;     //  查询一次后删除默认值,防止切到其它页面再切回来之后还有默认值

                this.setState({ filters: this.state.filters });
                // this.reload()
              }} />
            </div>
          </div>
        })}

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
        <div>
          <div className="search-query-btn" onClick={this.createOpen.bind(this)}>新增</div>
          <div className={this.state.selectedRowKeys && this.state.selectedRowKeys.length > 0 ? "search-delete-btn" : "search-reset-btn"} onClick={() => this.delete()}>批量删除</div>
        </div>

        <div className="tableSelectedCount">{`已选${this.state.selectedRowKeys.length}项`}</div>
        <div className="tableContent" ref={this.refTableContent} style={{height: 'calc(100vh - 589px)'}}>
          <div>
            {this.state.showTable ? <Table
              tableLayout="fixed"
              scroll={{ y: this.state.scrollY, x: 1000 }}
              pagination={this.state.pagination} rowSelection={{
                selectedRowKeys: this.state.selectedRowKeys,
                onChange: this.onRowSelectionChange.bind(this)
              }} columns={taskColumns} rowKey='_id' dataSource={this.state.data} loading={this.state.loading} /> : ''}
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
        header='进度'
        top={20}
        width={'80%'}
        visible={this.state.process}
        confirmBtn={null}
        forceRender={true}
        destroyOnClose={true}
        onClose={() => { this.setState({ process: false }) }}
        onCancel={() => { this.setState({ process: false }) }}
      >
        <div><TaskProcessDetail quartzDetail={this.state.quartzDetail}
          reload={this.reload.bind(this)} />
        </div>
      </Dialog>
      <Dialog
        header="创建任务"
        width={970}
        visible={this.state.creatTaskVisible}
        style={{
          position: 'fixed', // 固定定位
          left: '50%',
          top: '50%',
          transform: 'translate(-50%, -50%)', // 中心点定位
          margin: 0 // 清除默认margin
        }}
        forceRender={true}
        destroyOnClose={true}
        onConfirm={async () => {
        }} confirmLoading={this.state.loading}
        onClose={this.cancel}
        onCancel={this.cancel}
        footer={
          <div>

            <div className="search-reset-btn" onClick={() => this.cancel()} style={{ marginRight: '8px' }}>取消</div>
            <TButton className="search-query-btn" onClick={this.submitData}>确认</TButton>
          </div>
        }
      >
          <div style={{ marginLeft: 152 }}>
            <div style={{ display: 'flex', marginBottom: 24 }}>
              <div style={{ width: 84, textAlign: 'right', marginRight: 16, lineHeight: '30px' }}><span style={{ color: '#D54941' }}>*</span>任务描述</div>
              <div style={{ display: 'flex' }}>
                <Input placeholder='请输入任务描述' value={this.state.taskDesc} style={{ width: 400 }} onChange={(e) => {
                  this.state.taskDesc = e.target.value
                  this.setState({ taskDesc: this.state.taskDesc })
                }} />
              </div>
            </div>

          {this.state.chooseAccounts ?
          <div style={{ display: 'flex' }}>
            {this.state.choosenTab === 'accountGroup' ?
              <div className="dialog_item" style={{  marginBottom: '24px' }}>
                   <div style={{width: 84, textAlign: 'right', marginRight: 16, lineHeight: '30px' }}><span style={{ color: '#D54941' }}>*</span><span>选择账号</span></div>
                   <div className='dialog_item_input' style={{ display: 'flex',width: 400 }}>
                  <Select
                    mode={'multiple'}
                    style={{ width: 400}}
                    value={this.state.accountGroupSelectedRowKeys}
                    onChange={(value) => this.setState({ accountGroupSelectedRowKeys: value })}
                    filterOption={(input, option) => {
                      return (option.props.children[0].props && option.props.children[0].props.children ? option.props.children[0].props.children : option.props.children[0]).toLowerCase().indexOf(input.toLowerCase()) >= 0
                    }}
                    >
                     {this.state.accountGroupSelectedRowKeys.length!=0?
                    this.state.accountGroupList.map(res => {
                      let label = [];
                      label.push(res.groupName);
                      return <Select.Option key={res._id} value={res._id}>
                        {`${label.join('-')}`}
                      </Select.Option>
                    }):''}
                  </Select>

                </div>
              </div>
              : ''
            }

            {this.state.choosenTab === 'account' ?
              <div className="dialog_item" style={{  marginBottom: '24px' }}>
              <div style={{width: 84, textAlign: 'right', marginRight: 16, lineHeight: '30px' }}><span style={{ color: '#D54941' }}>*</span><span>选择账号</span></div>
              <div className='dialog_item_input' style={{ display: 'flex',width: 400 }}>
                  <Select
                mode={'multiple'}
                style={{ width: 400 }}
                value={this.state.selectedRowKeys}
                onChange={(value) => this.setState({ selectedRowKeys: value })}
                filterOption={(input, option) => {
                  return (option.props.children[0].props && option.props.children[0].props.children ? option.props.children[0].props.children : option.props.children[0]).toLowerCase().indexOf(input.toLowerCase()) >= 0
                }}
                >
                  {this.state.selectedRowKeys.length!=0?this.state.accountList.map(res => {
                    let label = [];
                    label.push(res.phone);
                    return <Select.Option key={res._id} value={res._id}>
                      {`${label.join('-')}`}
                    </Select.Option>
                  }):""}
                </Select>
                </div>
              </div>
              : ''
            }
            <span className='required_label' style={{marginLeft: 24 }}></span>
            <div className="search-query-btn" onClick={() => this.chooseGroup('accountGroup')}>选择分组</div>
            <div className="search-query-btn" onClick={() => this.chooseGroup('account')}>选择账号</div>
          </div>
          : ''
        }

            {this.state.addDataType === 'friend' ?
              <div style={{ display: 'flex', marginBottom: 24 }}>
                <div style={{ width: 84, textAlign: 'right', marginRight: 16, lineHeight: '30px' }}><span style={{ color: '#D54941' }}>*</span>好友标签</div>
                <div style={{ display: 'flex' }}>
                  <Select
                    mode={'multiple'}
                    placeholder="输入关键字搜索"
                    style={{ width: 317 }}
                    value={this.state.selectedLabels}
                    onChange={(value) => this.setState({ selectedLabels: value })}
                    filterOption={(input, option) => {
                      return (option.props.children[0].props && option.props.children[0].props.children ? option.props.children[0].props.children : option.props.children[0]).toLowerCase().indexOf(input.toLowerCase()) >= 0
                    }}
                  >
                    {this.state.labels.map(res => {
                      let label = [];
                      label.push(res.label);
                      return <Select.Option key={res._id} value={res._id}>
                        {`${label.join('-')}`}
                      </Select.Option>
                    })}
                  </Select>
                </div>
              </div> : ''
            }

            {this.state.addDataType === 'friend' ?
              <div style={{ display: 'flex', marginBottom: 24 }}>
                <div style={{ width: 100, marginLeft: -10, lineHeight: '30px' }}><span style={{ color: '#D54941' }}>*</span>发送好友数量</div>
                <div style={{ display: 'flex', marginLeft: 10}}>
                  <InputNumber min={0} value={this.state.taskTotal} style={{ width: 400 }} onChange={(e) => {
                    this.setState({ taskTotal: e })
                  }} />
                </div>
              </div>
              : ''
            }

            {this.state.addDataType === '1' || this.state.addDataType === '2' ?
              <div style={{ display: 'flex', marginBottom: 24 }}>
                <div style={{ width: 84, textAlign: 'right', marginRight: 16, lineHeight: '30px' }}><span style={{ color: '#D54941' }}></span><span>录入方式</span></div>
                <div style={{ display: 'flex' }}>
                  <Radio.Group value={this.state.addMethod} style={{ width: 400 }} onChange={(e) => this.setState({ addMethod: e.target.value })}>
                  {addMethods.filter(v => v.value !== '3').map(ws => {
                      return <Radio value={ws.value}>{ws.label}</Radio>
                    })}
                  </Radio.Group>
                </div>
              </div>
              : ''
            }

            {this.state.addMethod === "1"  && (
              <div style={{ display: 'flex', marginBottom: 24 }}>
                <div style={{ width: 84, textAlign: 'right', marginRight: 16, lineHeight: '30px' }}><span style={{ color: '#D54941' }}>*</span>群发对象</div>
                <div style={{ display: 'flex' }}>
                  <TextArea style={{ width: 400 }} placeholder='一行为一条数据，使用回车键(Enter)换行。数量大于500条时，请选择上传文件' autosize={{ minRows: 4 }}
                    value={this.state.addData}
                    onChange={this.onAddDataChange.bind(this)} />
                </div>
              </div>
            )}

            {this.state.addMethod === "2" && (
              <div style={{ display: 'flex', marginBottom: 24 }}>
                <div style={{ width: 84, textAlign: 'right', marginRight: 16, lineHeight: '30px' }}>上传文件</div>
                <div>
                  <TUpload
                    {...uploadProps}
                    fileList={this.state.uploadFile ? [this.state.uploadFile] : null}
                    onChange={this.onUploadChange.bind(this)}
                    onRemove={() => this.setState({ uploadFile: null })}
                  />
                </div>
              </div>
            )}
          </div>
    
          <div style={{ marginLeft: 152 }}>


            <div style={{ display: 'flex', marginBottom: 24 }}>
              <div style={{ width: 100, marginLeft: -5, lineHeight: '30px' }}><span style={{ color: '#D54941' }}></span>群发内容类型</div>
              <div style={{ display: 'flex', marginLeft: 6 }}>
                <Radio.Group onChange={(e) => {
                  this.setState({ activeTabKey: e.target.value })
                  this.setState({ material: '', materialContent: '', friendAddScene: '', friendAddScenes: []});
                  if (e.target.value == 'friendAddScene') {
                    //查询通用素材
                    this.selectAllCommonMaterials(this.state.material)
                  }
                }} value={this.state.activeTabKey}>
                  <Radio value={"text"}>文字</Radio>
                  <Radio value={"picture"}>图文</Radio>
                  {//<Radio value={'friendAddScene'}>从素材库选择</Radio>
                  }
                </Radio.Group>
              </div>
            </div>


            {this.state.activeTabKey == 'text'?
              <div style={{ display: 'flex', marginBottom: 24 }}>
                <div style={{ width: 84, textAlign: 'right', marginRight: 16, lineHeight: '30px' }}><span style={{ color: '#D54941' }}>*</span>群发内容</div>
                <div style={{ display: 'flex' }}>
                  <TextArea style={{ width: 400 }} placeholder='请输入内容' autosize={{ minRows: 4 }}
                    value={this.state.text}
                    onChange={(e) =>{this.setState({text: e.target.value})}} />
                </div>
              </div>
              : ''
            }

            {this.state.activeTabKey == 'picture' ?
              <div style={{ display: 'flex', marginBottom: 24 }}>
                <div style={{ width: 84, textAlign: 'right', marginRight: 16, lineHeight: '30px' }}>上传图片</div>
                <div style={{ color: 'rgba(0, 0, 0, 0.40)' }}>
                  <TUpload
                    {...getUploadImageProps()}
                    files={this.state.images ? [this.state.images] : []}
                    onChange={this.onUploadAccountAvatarChange.bind(this)}
                    theme="image"
                    imageViewerProps ={{"images":["/api/consumer/res/download/" + this.state.images]}}
                    onRemove={() => this.setState({ images: null })}
                  />
                  {/* <InputImageMsg value={this.state.images} onUploadChange={this.onUploadChange.bind(this)} hidePlus={false} /> */}
                  <div style={{ fontSize: 12 }}>请选择jpg、png、jpeg格式的文件上传，文件小于3M</div>
                </div>
              </div>
              : ''
            }

              
              <div style={{ display: 'flex', marginBottom: 24 }}>
              <div style={{ width: 84, textAlign: 'right', marginRight: 16, lineHeight: '30px' }}><span style={{ color: '#D54941' }}></span>发送时间</div>
              <div style={{ display: 'flex' }}>
                <Select value={this.state.sendMethod} style={{ width: 200 }} onChange={(e) => this.setState({ sendMethod: e })}>
                  {
                    sendMethods.map(ws => {
                      return <Option value={ws.value}>{ws.label}</Option>
                    })
                  }
                </Select>
                {this.state.sendMethod == "2" && (
                  <DatePicker showTime onChange={this.onDateTimeChange.bind(this)} />
                )}
              </div>
            </div>
          </div>

      </Dialog>
      <Dialog
        header={this.state.chooseTypeName}
        visible={this.state.chooseGroup}
        forceRender={true}
        destroyOnClose={true}
        onConfirm={() => { this.setState({ chooseGroup: false, selectGroupStatus: false }) }}
        confirmLoading={this.state.loading}
        onCancel={() => { this.setState({ chooseGroup: false }) }}
        onClose={() => { this.setState({ chooseGroup: false }) }}
        style={{
          width: '80%',
          maxWidth: '1400px',
          position: 'fixed', // 固定定位
          left: '50%',
          top: '50%',
          transform: 'translate(-50%, -50%)', // 中心点定位
          margin: 0 // 清除默认margin
        }}
        contentStyle={{ padding: '20px' }} // 调整内容区内边距
      >
        <div><SelectAccounts title={this.state.chooseType} reload={this.reload.bind(this)} onChange={this.selectAccountsOnChange.bind(this)} /></div>
      </Dialog>

      <Dialog
        header='查看'
        visible={this.state.lookTaskVisible}
        width={970}
        confirmBtn={null}
        onClose={() => { this.setState({ lookTaskVisible: false }) }}
        onCancel={() => { this.setState({ lookTaskVisible: false }) }}
        style={{
          position: 'fixed', // 固定定位
          left: '50%',
          top: '50%',
          transform: 'translate(-50%, -50%)', // 中心点定位
          margin: 0 // 清除默认margin
        }}
      >
        <div style={{ marginLeft: 152 }}>
          {this.state.lookChoosenTab === 'accountGroup' ?
            <div style={{ display: 'flex', marginBottom: 24 }}>
              <div style={{ width: 84, textAlign: 'right', marginRight: 16, lineHeight: '30px' }}><span style={{ color: '#D54941' }}>*</span>选择账号</div>
              <div style={{ display: 'flex' }}>
                <Select
                  mode={'multiple'}
                  placeholder="输入关键字搜索"
                  style={{ width: 400}}
                  value={this.state.lookAccountGroupIds}
                  filterOption={(input, option) => {
                    return (option.props.children[0].props && option.props.children[0].props.children ? option.props.children[0].props.children : option.props.children[0]).toLowerCase().indexOf(input.toLowerCase()) >= 0
                  }} disabled>
                  {this.state.accountGroupList.map(res => {
                    let label = [];
                    label.push(res.groupName);
                    return <Select.Option key={res._id} value={res._id}>
                      {`${label.join('-')}`}
                    </Select.Option>
                  })}
                </Select>
              </div>
            </div>
            : ''
          }
          {this.state.lookChoosenTab === 'account' ?
            <div style={{ display: 'flex', marginBottom: 24 }}>
              <div style={{ width: 84, textAlign: 'right', marginRight: 16, lineHeight: '30px' }}><span style={{ color: '#D54941' }}>*</span>选择账号</div>
              <div style={{ display: 'flex' }}>
                <Select
                  mode={'multiple'}
                  placeholder="输入关键字搜索"
                  style={{ width: 400}}
                  value={this.state.lookIds}
                  filterOption={(input, option) => {
                    return (option.props.children[0].props && option.props.children[0].props.children ? option.props.children[0].props.children : option.props.children[0]).toLowerCase().indexOf(input.toLowerCase()) >= 0
                  }} disabled>
                  {this.state.accountList.map(res => {
                    let label = [];
                    label.push(res.phone);
                    return <Select.Option key={res._id} value={res._id}>
                      {`${label.join('-')}`}
                    </Select.Option>
                  })}
                </Select>
              </div>
            </div>
            : ''
          }

          <div style={{ display: 'flex', marginBottom: 24 }}>
            <div style={{ width: 84, textAlign: 'right', marginRight: 16, lineHeight: '30px' }}><span style={{ color: '#D54941' }}>*</span>任务描述</div>
            <div style={{ display: 'flex' }}>
              <Input value={this.state.lookTaskDesc} style={{ width: 400 }} disabled />
            </div>
          </div>

          <div style={{ display: 'flex', marginBottom: 24 }}>
            <div style={{ width: 84, textAlign: 'right', marginRight: 16, lineHeight: '30px' }}><span style={{ color: '#D54941' }}>*</span><span>录入方式</span></div>
            <div style={{ display: 'flex' }}>
              <Radio.Group value={this.state.lookAddMethod} style={{ width: 400 }} disabled>
                  {addMethods.filter(v => v.value !== '3').map(ws => {
                      return <Radio value={ws.value}>{ws.label}</Radio>
                    })}
                  </Radio.Group>
            </div>
          </div>

          {this.state.lookAddMethod === '1' ?
          <div style={{ display: 'flex', marginBottom: 24 }}>
            <div style={{ width: 84, textAlign: 'right', marginRight: 16, lineHeight: '30px' }}><span style={{ color: '#D54941' }}>*</span>群发对象</div>
            <div style={{ display: 'flex' }}>
              <TextArea style={{ width: 400 }} placeholder='一行一条数据,使用回车键(Enter)换行' autosize={{ minRows: 4 }}
                value={this.state.lookAddData} disabled />
            </div>
          </div>
        : ''
        }

      {this.state.lookAddMethod === '2' ?
          <div style={{ display: 'flex', marginBottom: 24 }}>
                <div style={{ width: 84, textAlign: 'right', marginRight: 16, lineHeight: '30px' }}>上传文件</div>
                <div>
                  <a href={`/api/consumer/res/download/${this.state.lookFilePath}`}  download={this.state.lookFilePath.includes('\\')?this.state.lookFilePath.substring(this.state.lookFilePath.lastIndexOf('\\')+1):this.state.lookFilePath.substring(this.state.lookFilePath.lastIndexOf('/')+1)} style={{color: '#5d5d5d',lineHeight: '30px'}}>
                       {this.state.lookFilePath.includes('\\')?this.state.lookFilePath.substring(this.state.lookFilePath.lastIndexOf('\\')+1):this.state.lookFilePath.substring(this.state.lookFilePath.lastIndexOf('/')+1)} &nbsp;&nbsp;(点击下载文件)
                    </a>
                </div>
              </div>
        : ''
        }

      
          <div style={{ display: 'flex', marginBottom: 24 }}>
            <div style={{ width: 100, marginLeft: -5, lineHeight: '30px' }}><span style={{ color: '#D54941' }}>*</span>群发内容类型</div>
            <div style={{ display: 'flex', marginLeft: 6 }}>
              <Radio.Group value={this.state.lookActiveTabKey} disabled>
                <Radio value={"text"}>文字</Radio>
                <Radio value={"picture"}>图片</Radio>
              </Radio.Group>
            </div>
          </div>

          {this.state.lookActiveTabKey === 'text' ? 
          <div style={{ display: 'flex', marginBottom: 24 }}>
            <div style={{ width: 84, textAlign: 'right', marginRight: 16, lineHeight: '30px' }}><span style={{ color: '#D54941' }}>*</span>群发内容</div>
            <div style={{ display: 'flex' }}>
              <TextArea style={{ width: 400 }} autosize={{ minRows: 4 }}
                value={this.state.lookText} disabled />
            </div>
          </div>
          : ''
        }

        {this.state.lookActiveTabKey === 'picture' ? 
          <div style={{ display: 'flex', marginBottom: 24 }}>
                <div style={{ width: 84, textAlign: 'right', marginRight: 16, lineHeight: '30px' }}>上传文件</div>
                <div>
                      <div key={`image-${this.state.lookImage}`}
                        style={{ display: 'inline-block'}}
                        onClick={() => {
                          this.setState({
                            imageBigVisible: true,
                            imageBigUrl: `/api/consumer/res/download/${this.state.lookImage}`
                          })
                        }}>
                        <Avatar shape="square" size={80}
                          src={`/api/consumer/res/download/${this.state.lookImage}`} />
                      </div> 
                </div>

          </div>
          : ''
        }
 
          <div style={{ display: 'flex', marginBottom: 24 }}>
              <div style={{ width: 84, textAlign: 'right', marginRight: 16, lineHeight: '30px' }}><span style={{ color: '#D54941' }}></span>发送时间</div>
              <div style={{ display: 'flex',lineHeight: '30px'  }}>
                {formatDate(this.state.lookSendTime)}{this.state.lookExecuteType==1?'(即刻执行)':'(定时执行)'}
              </div>
            </div>
        </div>
      </Dialog>

    </div>)
  }
}

export default MyComponent
