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
import InputImageMsg from "./inputImageMsg";
import SelectLabel from "../common/selectLabel";

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

    let newValue = {
      ...this.state.value,
      ...this.props.value,
      ...changedValue,
    };

    await this.setState({value: newValue});

    let {
      top,
      middle,
      foot,
      text,
      url,
      image,
    } = newValue;
    let errors = [];
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
    const inputItem = [{
      key: 'top',
      form: {
        label: '链接标题'
      },
      item: {
        placeholder: "链接标题",
      },
      onChange,
    }, {
      key: 'middle',
      form: {
        label: '链接正文'
      },
      item: {
        placeholder: "链接正文",
      },
      onChange,
    }, {
      key: 'image',
      form: {
        label: '链接封面',
        extra: '图片必须是正方形',
      },
      itemType:'InputImageMsg',
      item: {
        placeholder: "链接封面",
        uploadProps: {
          action: '/api/consumer/res/upload/CardMsgImage',
        },
      },
      onChange: (fileList) => {
        let file = fileList[0];
        if (!file) return undefined;
        return {
          name: file.name,
          filepath: file.response && file.response.filepath
        }
      }
    }, {
      key: 'url',
      form: {
        label: '链接',
        // extra: '需要跳转的链接',
        required: true,
      },
      item: {
        placeholder: "需要跳转的链接",
      },
      onChange,
    }, {
      key: 'text',
      form: {
        label: '卡片正文',
      },
      itemType: 'TextArea',
      item: {
        placeholder: "卡片正文,如果正文中有上面的链接,需要注意链接前后需要有空格或换行",
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

    const formItems = inputItem.map((opt) => {
          const item = (() => {
            let disabled = opt.disabled;
            switch (opt.itemType) {
              case 'Select':
                return <Select {...opt.item} {...opt.select} disabled={disabled}>
                  {this._arrayToFormOption(opt.options)}
                </Select>;
              case 'Cascader':
                return <Cascader {...opt.item} {...opt.cascader} options={opt.options} disabled={disabled}>
                </Cascader>;
              case 'Radio':
                return <Radio.Group {...opt.item} {...opt.radio} options={opt.options} disabled={disabled}/>
              case 'TextArea':
                return <TextArea {...opt.item} {...opt.textArea} rows={5} disabled={disabled}></TextArea>
              case 'DatePicker':
                return <DatePicker {...opt.item} disabled={disabled} format="YYYY-MM-DD HH:mm:ss" placeholder='' showTime={{}}/>
              case 'Upload':
                return <Upload {...opt.upload}>
                  <Button>
                    <Icon type="upload"/>
                    上传
                  </Button>
                </Upload>;
              case 'InputImageMsg':
                return <InputImageMsg {...opt.item} {...opt.inputImageMsg} disabled={disabled}/>;
              case 'SelectLabel':
                return <SelectLabel {...opt.item} {...opt.selectLabel} disabled={disabled}></SelectLabel>
              case 'InputNumber':
                return <InputNumber {...opt.item} {...opt.inputNumber} disabled={disabled}></InputNumber>
              case 'Input':
              default:
                return <Input {...opt.item} {...opt.input} disabled={disabled}/>
            }
          })();
          return <Col span={24} {...opt.col} style={opt.style}>
            <Form.Item {...formItemLayout} {...opt.form}>
              {item}
              {opt.other}
            </Form.Item>
          </Col>
        }
    );

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
          {formItems}
        </div>
    )
  }
}

export default InputCardMsg;
