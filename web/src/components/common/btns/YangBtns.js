import React, {Component} from "react";
import {Alert, Button, DatePicker, Form, Input, InputNumber, message, Modal, Select, Tree, Upload,} from "antd";
import axios from "axios";
import DialogApi from '../../../routes/admin/common/dialog/DialogApi'

const Search = Input.Search;
const confirm = Modal.confirm;
const Option = Select.Option;
const { Item: FormItem } = Form;
const { TextArea } = Input;
const Dragger = Upload.Dragger;
const { RangePicker } = DatePicker;
const { TreeNode } = Tree;

const baseUrl = "/api/yang";

class YangBtns extends Component {
  constructor(props) {
    super(props);
    this.state = {
      loading: false,
      addFriendNum: 5,
    };
  }

  // 首次加载数据
  async componentWillMount() {}

  async reload() {
    if (typeof this.props.reload == "function") await this.props.reload();
  }

  async addFriend(ids) {
    if (!ids || ids.length == 0) {
      message.error('请先选择分组');
      return;
    }
    if (ids.length > 1) {
      message.error('只能选择一个分组');
      return;
    }

    let addFriendNum = 6;
    let addType = '2';

    DialogApi.warning({
      title: '养号之互加好友',
      width: 600,
      content: <div>
        <Alert message=""
               description={<div>注意系统会按输入的互加好友数将账号进行分配（可以认为是将账号分配到不同的养号分组），系统会尽量维持现有养号分组不变，但账号被封或者解绑会导致某些养号分组下账号数量不足，这种情况可能会对已有养号分组进行调整：<br/>
                1.当输入的互加好友数<strong> 小于等于 </strong>养号分组账号的数量时，不做任何调整。<br/>
                2.当输入的互加好友数<strong> 大于 </strong>养号分组账号的数量时，会调整养号分组账号的数量来满足新的互加好友数。<br/>
                3.调整的策略：优先用未分配过养号分组的账号来补充进现有的养号分组中，然后在不拆分养号分组的前提下，合并养号分组尽量满足互加好友数。<br/>
                4.互加好友数推荐设置为偶数。
                </div>}
               type="info"
        />
        <br/>
        <div>
          请输入互加好友数(2-20)：
          <InputNumber min={2} max={20} defaultValue={addFriendNum} onChange={(value) => {addFriendNum = value}} />
        </div>
        <br/>
        <div>
          添加方式：
          <Select style={{width: 200}} defaultValue={addType} onChange={(value) => {addType = value}}>
          {/*{*/}
          {/*  friendAddTypes.map(item => (*/}
          {/*    <Select.Option key={item.value} value={item.value}>*/}
          {/*      {item.label}*/}
          {/*    </Select.Option>*/}
          {/*  ))*/}
          {/*}*/}
          </Select>
        </div>
      </div>,
      onOkTxt: '确定',
      onCancelTxt: '取消',
      onOk: async() => {
        if (!addFriendNum) {
          message.error('请输入互加好友数');
          return Promise.reject();
        }

        this.setState({loading: true});
        try {
          let result = await axios.post(`${baseUrl}/addFriend`, {
            ids,
            addFriendNum,
            addType,
          });
          if (result.data.code !== 1) {
            DialogApi.error({title: '失败', content: result.data.message || "unknown error", onOkTxt: '关闭', onOk: () => {}});
            // Modal.error({title: '失败', content: result.data.message || "unknown error", okText: "关闭"});
          } else {
            message.success("操作成功");
          }
        } catch (e) {
          DialogApi.error({title: '错误', content: e.message || "unknown error", onOkTxt: '关闭', onOk: () => {}});
          // Modal.error({title: '错误', content: e.message || "unknown error", okText: "关闭"});
        } finally {
          this.setState({loading: false});
        }
      },
      onCancel: () => {
      }
    });
    // confirm({
    //   title: '养号之互加好友',
    //   icon: null,
    //   width: 600,
    //   content: <div>
    //     <Alert message=""
    //            description={<div>注意系统会按输入的互加好友数将账号进行分配（可以认为是将账号分配到不同的养号分组），系统会尽量维持现有养号分组不变，但账号被封或者解绑会导致某些养号分组下账号数量不足，这种情况可能会对已有养号分组进行调整：<br/>
    //             1.当输入的互加好友数<strong> 小于等于 </strong>养号分组账号的数量时，不做任何调整。<br/>
    //             2.当输入的互加好友数<strong> 大于 </strong>养号分组账号的数量时，会调整养号分组账号的数量来满足新的互加好友数。<br/>
    //             3.调整的策略：优先用未分配过养号分组的账号来补充进现有的养号分组中，然后在不拆分养号分组的前提下，合并养号分组尽量满足互加好友数。<br/>
    //             4.互加好友数推荐设置为偶数。
    //             </div>}
    //            type="info"
    //     />
    //     <br/>
    //     <div>
    //       请输入互加好友数(2-20)：
    //       <InputNumber min={2} max={20} defaultValue={addFriendNum} onChange={(value) => {addFriendNum = value}} />
    //     </div>
    //     <br/>
    //     <div>
    //       添加方式：
    //       <Select style={{width: 200}} defaultValue={addType} onChange={(value) => {addType = value}}>
    //       {
    //         friendAddTypes.map(item => (
    //           <Select.Option key={item.value} value={item.value}>
    //             {item.label}
    //           </Select.Option>
    //         ))
    //       }
    //       </Select>
    //     </div>
    //   </div>,
    //   okText: '确定',
    //   cancelText: '取消',
    //   onOk: async() => {
    //     if (!addFriendNum) {
    //       message.error('请输入互加好友数');
    //       return Promise.reject();
    //     }

    //     this.setState({loading: true});
    //     try {
    //       let result = await axios.post(`${baseUrl}/addFriend`, {
    //         ids,
    //         addFriendNum,
    //         addType,
    //       });
    //       if (result.data.code !== 1) {
    //         Modal.error({title: '失败', content: result.data.message || "unknown error", okText: "关闭"});
    //       } else {
    //         message.success("操作成功");
    //       }
    //     } catch (e) {
    //       Modal.error({title: '错误', content: e.message || "unknown error", okText: "关闭"});
    //     } finally {
    //       this.setState({loading: false});
    //     }
    //   },
    // });
  }

  render() {
    return <>
    {
      this.props.className ? <div {...this.props} onClick={() => {this.addFriend(this.props.ids)}}>养号之互加好友</div> :
      <Button {...this.props.btnProps} onClick={() => {this.addFriend(this.props.ids)}}>养号之互加好友</Button>
    }
    </>
  }
}

export default YangBtns;
