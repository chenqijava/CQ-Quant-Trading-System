import React, {Component} from 'react'
import {
  Icon,
  message,
  Upload,
  Input,
  Alert,
  InputNumber,
  Form,
  Row,
  Col,
  Select,
} from 'antd'
const Option = Select.Option;
const {TextArea} = Input;
import CardIntroImage from "../../images/cardmsg_intro.jpg";
import NewForm from "components/common/newForm";

class InputCardMsg extends Component {

  constructor(props) {
    super(props);
    this.state = {
      value: {},
      error: {
        // top: {
        //   validateStatus: 'error',
        //   hasFeedback: true,
        //   help: 'help',
        // }
      },
    }
  }

  async componentWillMount() {
  }

  async setErrors(errors) {
    let error = {};
    errors.forEach(e => error[e.code] = {
      validateStatus: 'error',
      hasFeedback: true,
      help: e.message,
      ...e,
    });
    await this.setState({error})
  }

  async triggerChange(changedValue) {
    const {onChange} = this.props;

    console.log(changedValue)
    let newValue = {
      ...this.state.value,
      ...this.props.value,
      ...changedValue,
    };

    await this.setState({value: newValue});

    let {
      accID,
      msgIDs,
      url,
    } = newValue;
    let errors = [];
    if (accID && !Number(accID)) {
      errors.push({
        code: 'accID',
        message: '请输入正确频道ID'
      });
    }

    //从url中解析 url,和msgIds

    let strs = url.split('/');
    let channelUrl = strs.slice(0,strs.length-1).join('/')
    let channelMsgIDs = strs[strs.length-1]
    // msgIDs
    if (!channelMsgIDs || !/^\d+([,#]\d+)*\d*$/.test(channelMsgIDs)) {
      errors.push({
        code: 'url',
        message: '请输入正确的链接,没有消息id'
      });
    }

    if (url != undefined && !/^https?:\/\/.+/.test(url)) {
      errors.push({
        code: 'url',
        message: '请输入正确的链接'
      });
    }

    if (errors.length > 0) {
      newValue = null;
    }
    await this.setErrors(errors);
    if (onChange) {

      let newValue = {
        url:channelUrl,
        msgIDs:channelMsgIDs,
      }
      console.log('newValue:',newValue)
      onChange(newValue);
    }
  }

  _arrayToFormOption(options) {
    if (typeof options === "function") options = options();
    return options.map(option => <Option key={option.value}>{option.label}</Option>);
  }

  render() {
    const formItemLayout = {
      colon: false,
      labelCol: {
        span: 5,
      }, wrapperCol: {
        span: 19,
      },
    };

    const {size} = this.props;
    const value = this.props.value || this.state.value;

    const onChange = (e) => e.target && e.target.value;     //  将组件onChange转换成需要的值
    const formItemss = [{
      // key: 'msgIDs',
      // form: {
      //   label: '消息ID',
      //   required: true,
      // },
      // item: {
      //   placeholder: "一组内多个消息ID用,分隔;多组消息用#号分隔,轮流向录入数据发送一组消息",
      // },
      // fieldOpt: {
      //   initialValue:  value['msgIDs']||'',
      // },
      // onChange,
    // }, {
    //   key: 'accID',
    //   form: {
    //     label: '频道ID',
    //   },
    //   item: {
    //     placeholder: "",
    //   },
    //   onChange,
    }, {
      key: 'url',
      form: {
        label: '消息链接',
        // extra: '需要跳转的链接',
        required: true,
      },
      item: {
        placeholder: "多个消息ID用 , 分割,如: https://t.me/Group003gr/125,126",
      },
      fieldOpt: {
        initialValue:  value['url']||'',
      },
      onChange,
    }].map(opt => {
      if (opt.onChange) {
        opt.item = {
          onChange: (e) => this.triggerChange({[opt.key]: opt.onChange(e)}),
          ...opt.item,
        }
      }
      opt.form = {
        ...this.state.error[opt.key],
        ...opt.form,
      };
      return opt;
    });


    return (
        <div className="clearfix">
          {
            this.props.showTips ?
                <Alert message={<img src={CardIntroImage} alt=""/>}
                       description={<div><span>卡片消息各个部分数据如上图所示</span><br/><b>其中链接为必填项</b><span>，其他项选填，为空则图中对应部分内容可能为空</span></div>}
                       type="info"
                />
                : null
          }
          {
            this.props.hasOwnProperty("cardName") ?
                <Input addonBefore="名称" placeholder="仅用于区分不同的卡片" value={this.props.cardName} onChange={this.props.handleCardNameChange}/>
                : null
          }
          <NewForm {...{formItemss}} needHiddenItems={this.state.needHiddenItems} query={{oper: 'noButton'}}/>
        </div>
    )
  }
}

export default InputCardMsg;
