import React, {Component} from 'react'
import {
  Button,
  Col,
  DatePicker,
  Form,
  Icon,
  Input,
  message,
  Modal,
  Radio,
  Row,
  Switch,
  Select,
  Upload,
  Cascader,
  Tree,
  InputNumber,
  TimePicker
} from 'antd'
import axios from 'axios'
import SelectLabel from "../../components/common/selectLabel";
import InputImageMsg from "../msg/inputImageMsg";
import InputNumberRange from "./InputNumberRange";
import QueueAnim from 'rc-queue-anim';

const Search = Input.Search
const confirm = Modal.confirm
const Option = Select.Option;
const {Item: FormItem} = Form
const {TextArea} = Input
const Dragger = Upload.Dragger
const {RangePicker} = DatePicker;
const { TreeNode } = Tree;

// 卡片的表单界面
class MyForm extends Component {
  constructor(props) {
    super(props);
    this.state = {
      loading: false,
      // 当前的状态
      formState: {
        oper: 'create'
      },
      // 需要隐藏的字段
      hiddenItems: [],
      show: false,
    };
    this.hiddenItemsByKey = {};
    // 只有新建、修改有取消按钮，所以需要计算出上一个状态
    if (this.props.query.oper == 'edit') {
      this.state.formState = {
        oper: 'view',
        _id: this.props.query._id
      }
    }
    let oldFormConfig = this.props.formConfig || {};

    // 表单的状态模式，不同状态下，显示的按钮不一样
    // 这个状态配置一直不变所以不用放到state里
    this.formConfig = {
      ...this.props.formConfig,
      // none状态，显示新建按钮，所以字段无数据，不可编辑
      // 这个状态没啥具体作用，主要是create状态点取消的时候进入此状态
      none: {
        button: [
          'create', 'backToList'
        ],
        input: [],
        inputDisable: [],
        ...oldFormConfig.none,
      },
      // view状态下显示
      view: {
        button: [
          'create', 'backToList'
        ],
        input: [],
        inputDisable: ['_id', '__v', 'createTime', 'updateTime'],
        ...oldFormConfig.view,
      },
      create: {
        button: [
          'save', 'backToList'//, 'cancel'
        ],
        input: [/.+/],
        inputDisable: ['_id', '__v', 'createTime', 'updateTime'],
        ...oldFormConfig.create,
      }
    }
    // 基本上，新建和编辑都是相同配置
    this.formConfig.edit = {
      ...this.formConfig.create,
      ...oldFormConfig.edit,
    };
  }

  // 根据按钮编码返回display值
  _buttonDisplayCalc(buttonCode, button) {
    return (this.formConfig[this.state.formState.oper] || {button: []}).button.indexOf(buttonCode) == -1
        ? null
        : button
  }

  // 进行规则校验
  _matchRule(rule, value) {
    // 只要一个匹配到enable就可以结束
    let match = false;
    if (typeof(rule) == 'string') {
      match = rule == value
    } else {
      match = rule.test(value)
    }
    return match
  }

  // 根据输入项编码返回disable值
  _itemDisableCalc(inputCode) {
    // 如果loading则直接禁用
    if (this.state.loading) {
      return true
    }
    const config = this.formConfig[this.state.formState.oper];
    if (!config) return false;
    // 校验是否需要disable
    if (config.inputDisable.find((r) => {
      return this._matchRule(r, inputCode);
    })) {
      // 存在disable就可以结束
      return true;
    }
    // 校验是否可编辑，可以是正则，或者是字符串
    if (config.input.find((r) => {
      return this._matchRule(r, inputCode);
    })) {
      // 存在enable就可以结束
      return false;
    }
    return true;
  }

  // 判断某字段是否需要显示
  _itemShowCalc(inputCode, bindFormItemValue) {
    if (bindFormItemValue) {
      let needHiddenItems = this.props.needHiddenItems;
      if (needHiddenItems) {    //  如果有设置needHiddenItems,按照needHiddenItems的值进行判断
        let bindFormItemValues = [bindFormItemValue];     //  兼容旧逻辑
        if (bindFormItemValue instanceof Array) {         //  如果是数组就直接赋值
          bindFormItemValues = bindFormItemValue;
        }
        //  如果数组中有一个是满足的就显示  or逻辑
        let key = bindFormItemValues.find(bindFormItemValue =>
            //  bindFormItemValue设置了多个字段时需要同时满足才能显示  and逻辑
            !Object.keys(bindFormItemValue).find(key => bindFormItemValue[key].indexOf(needHiddenItems[key]) === -1)
        );
        if (/addMethod|addMembers/.test(inputCode)) {
          console.log({key, inputCode, needHiddenItems, bindFormItemValue})
        }
        if (!key) return false;
      }
    }
    const hiddenItems = this.state.hiddenItems;
    // 校验是否需要隐藏
    if (hiddenItems.find((r) => {
      return this._matchRule(r, inputCode);
    })) {
      // 存在匹配就隐藏
      return false;
    }
    // 默认都显示
    return true;
  }

  async componentDidMount() {
    await this.changeState(this.props.query)
  }

  async componentWillReceiveProps(nextProps) {
    if (!this.state.show) this.setState({show: true});   //  初始化HiddenConfig后再显示QueueAnim,让表单第一次显示时默认隐藏的item不会先显示再隐藏
  }

  async componentDidUpdate(prevProps) {
    if (!this.state.show) {
      setTimeout(() => {
        this.setState({show: true})
      }, 100)
    }   //  初始化HiddenConfig后再显示QueueAnim,让表单第一次显示时默认隐藏的item不会先显示再隐藏
  }

  // 从上一个状态中恢复
  async changeState(newState) {
    // 备份状态
    this.oldFormState = this.state.formState
    // 切状态
    this.setState({keys: [0], formState: newState, loading: true})
    // 更新form的值
    if (newState._id) {
      await this.props.formLoad(this.props.form, newState);
    } else if (newState.oper != 'view') {
      this.props.form.resetFields()
    }
    if (typeof this.props.initForm === "function") {
      this.props.initForm(this.props)
    }
    // 解除loading
    this.setState({loading: false})
  }

  // 基本通用的按钮
  async create() {
    this.changeState({oper: 'create'})
  }

  async edit() {
    this.changeState({oper: 'edit', _id: this.state.formState._id})
  }

  async backToList() {
    if (typeof this.props.backToListBtnCallback == 'function') {
      this.props.backToListBtnCallback()
    } else {
      this.props.history.push('list')
    }
  }

  async save() {
    this.setState({loading: true});
    let newState = null;
    if (typeof this.props.formSaveByValidated == 'function') {
      await new Promise((resolve, reject) => {
        this.props.form.validateFields(async (err, form) => {
          try {
            newState = await this.props.formSaveByValidated(err, form);
          } catch (e) {
            console.log(e)
          } finally {
            resolve()
          }
        })
      });
    } else if (typeof this.props.formSave == 'function') {
      newState = await this.props.formSave(this.props.form);
    }
    if (newState) this.changeState(newState);
    this.setState({loading: false});
  }

  async delete() {
    confirm({
      title: '确定要删除这条数据？',
      content: '',
      okText: '确定',
      okType: 'danger',
      cancelText: '取消',
      onOk: async () => {
        this.setState({loading: true})
        await axios.post(`${baseUrl}/delete`, [this.state.formState._id])
        message.success('操作成功')
        this.changeState({oper: 'none'})
      },
      onCancel() {
      }
    })
  }

  async cancel() {
    confirm({
      title: '取消后已编辑的缓存数据会丢失，确定要取消吗？',
      content: '',
      okText: '确定',
      okType: 'danger',
      cancelText: '取消',
      onOk: async () => {
        this.changeState(this.oldFormState)
      },
      onCancel() {
      }
    })
  }

  _arrayToFormOption(options) {
    if (typeof options === "function") options = options();
    return options.map(option => <Option {...option} key={option.value}>{option.label}</Option>);
  }

  _renderTreeNodes = data =>
      data.map(item => {
        return (
            <TreeNode title={item.name} key={item._id} dataRef={item}>
              {item.submenus && item.submenus.length ? this._renderTreeNodes(item.submenus) : ''}
            </TreeNode>
        );
      });

  beforeUpload = (file, fileList, regexp) => {
    if (!regexp.test(file.name.toLowerCase())) {
      message.error('文件名不合法');
      return false;
    }
    return true;
  };

  normFile = ({maxLength, filterError}, e) => {
    console.log('Upload event:', e);
    if (Array.isArray(e)) {
      return e;
    }

    if(!e.file.status){
      return;
    }

    const isUploadSuccess = file => file.status == 'done' && file.response && file.response.code == 1;

    let fileList = e.fileList;
    if (isUploadSuccess(e.file)) {   //  npm run build 之后所有的上传操作都是done
      message.success(`${e.file.name} 上传成功`);
      if (maxLength) {
        fileList = fileList.slice(fileList.length - maxLength, fileList.length)
      }
    } else if (e.file.status === "error" || e.file.status === "done") {   //  上传失败的情况有两种,status=='error' 或 status=='done'但response.code!=1
      message.error(e.file.response && e.file.response.message || `${e.file.name} 上传失败`);
      if (filterError) fileList = fileList.filter(isUploadSuccess);
    }
    return fileList;
  };

  normalize = (value, prevValue, allValues) => {
    if (value) {
      value.forEach(file => {
        if (file.filepath) {
          file.uid = file.filepath;
          file.status = 'done';
          file.url = `/api/consumer/res/${file.filepath}`;
        }
      });
    }
    return value
  };

  textAreaRules = (opt) => [
    {
      required: 'required' in opt ? opt.required : true,
      transform: (value) => (value || '').trim(),
      message: '不能为空'
    },
    {
      type: 'array',
      transform: value => value.split('\n').filter(ad => ad),
      max: 'maxLineNum' in opt ? opt.maxLineNum : 500,
      message: '录入的信息过多，请使用文件上传数据',
    },
    {
      transform: typeof opt.transformByLine === 'function' ? value => value.map(opt.transformByLine) : undefined,
      validator: (rule, value, callback) => {
        try {
          if (typeof opt.validatorByLine == 'function' && value.find(v => !opt.validatorByLine(v))) return callback(rule.message);
        } catch (e) {
          return callback(e.message);
        }
        return callback();
      },
      message: opt.validatorByLineMessage || '输入数据错误',
    },
  ].concat(opt.rules || []);

  getCombineKey = (opt, key) => opt.key ? `${opt.key}.${key}` : key;

  getTextAreaOruploadFile = (opt) => {
    let addMethodKey = this.getCombineKey(opt, 'addMethod');
    const {addMethod = {}, addData = {}, uploadFile = {}, addMethodOnChange} = opt;
    let addMethodItem = addMethod.item;
    if (typeof addMethodOnChange == 'function' && !(addMethodItem && addMethodItem.onChange)) {
      addMethodItem = {
        ...addMethodItem,
        onChange: (value) => addMethodOnChange({[addMethodKey]: value})
      }
    }
    return [{
      options: [
        {label: '页面录入', value: '1'},
        {label: '上传文件', value: '2'},
      ],
      ...opt,
      ...addMethod,
      key: addMethodKey,
      itemType: 'Select',
      form: {
        ...opt.formItemLayout,
        label: (opt.label || '') + '录入方式',
        ...addMethod.form
      },
      item: {
        placeholder:"选择数据录入方式",
        ...addMethodItem
      },
    }, {
      bindFormItemValue: {
        ...opt.bindFormItemValue,
        [addMethodKey]: ['1'],
      },    //  只有在对应字段的值为指定的值时才显示
      key: this.getCombineKey(opt,'addData'),
      itemType: 'TextArea',
      ...addData,
      form: {
        ...opt.formItemLayout,
        label: '录入信息',
        ...addData.form,
      },
      fieldOpt: {
        rules: [
          ...this.textAreaRules(addData.lineRules || opt),
          ...addData.fieldOpt && addData.fieldOpt.rules || [],
        ],
        ...addData.fieldOpt,
      },
    }, {
      bindFormItemValue: {
        ...opt.bindFormItemValue,
        [addMethodKey]: ['2'],
      },
      key: this.getCombineKey(opt, 'uploadFile'),
      itemType: 'Upload',
      ...uploadFile,
      form: {
        ...opt.formItemLayout,
        label: '上传文件',
        ...uploadFile.form,
      },
      fieldOpt: {
        rules: [
          {
            required: true,
            message: '不能为空'
          },
          ...uploadFile.fieldOpt && uploadFile.fieldOpt.rules || [],
        ],
        maxLength: 1,
        filterError: true,
        ...uploadFile.fieldOpt,
      },
    }].concat((opt.items || []).map(item => ({
      bindFormItemValue: {
        ...opt.bindFormItemValue,
        [addMethodKey]: item.bindFormItemValueAddMethod,
      },
      key: this.getCombineKey(opt, item.key),
      ...item,
    })));
  };

  render() {
    const {getFieldDecorator} = this.props.form;

    const formItemLayout = {
      colon: false,
      ...this.props.formItemLayout,
      labelCol: {
        span: 4,
        ...(this.props.formItemLayout || {}).labelCol,
      }, wrapperCol: {
        span: 16,
        ...(this.props.formItemLayout || {}).wrapperCol,
      },
    };

    const formItemss = (this.props.formItemss || []).map(item => {
      if (!item.combineComponent) return item;
      switch (item.combineComponent) {
        case 'addData':
          return this.getTextAreaOruploadFile(item);
        default:
          return {other: `combineComponent ${item.combineComponent} not exists`}
      }
    }).flat();

    const formItems = formItemss.filter(opt => this._itemShowCalc(opt.key, opt.bindFormItemValue)).map((opt) => {
          const item = (() => {
            let item;
            let disabled = this._itemDisableCalc(opt.key);
            switch (opt.itemType) {
              case 'Select':
                item = opt.select || opt.item;
                return <Select {...item} disabled={disabled}>
                  {this._arrayToFormOption(opt.options || item.options)}
                </Select>;
              case 'Cascader':
                return <Cascader {...opt.cascader || opt.item} options={opt.options} disabled={disabled}>
                </Cascader>;
              case 'RangePicker':
                return <RangePicker showTime={{
                  format: 'HH:mm:ss'
                }} format="YYYY-MM-DD HH:mm:ss" placeholder={['开始时间', '结束时间']} {...opt.item}/>
              case 'Radio':
                item = opt.radio || opt.item;
                return <Radio.Group {...item} options={opt.options || item.options} disabled={disabled}/>
              case 'TextArea':
                return <TextArea {...opt.textArea || opt.item} rows={5} disabled={disabled}></TextArea>
              case 'DatePicker':
                return <DatePicker disabled={disabled} format="YYYY-MM-DD HH:mm:ss" showTime={{}}
                                   {...opt.datePicker || opt.item} style={{width: '100%', ...(opt.inputNumber || opt.item || {}).style}}/>
              case 'Upload':
                item = opt.upload || opt.item;
                opt.fieldOpt = {
                  valuePropName: 'fileList',
                  //处理上传事件,可以把 onChange 的参数（如 event）转化为控件的值
                  getValueFromEvent: this.normFile.bind(this, opt.fieldOpt),
                  // 转换默认的 value 给控件
                  normalize: this.normalize,
                  ...opt.fieldOpt,
                };
                let regExp = item && item.regExp || /^.+\.(csv|txt)$/;
                if (!(regExp instanceof RegExp)) {
                  regExp = new RegExp(regExp)
                }
                return <Upload beforeUpload={(file, fileList) => this.beforeUpload(file, fileList, regExp)} {...item}>
                  <Button>
                    <Icon type="upload"/>
                    上传
                  </Button>{item.placeholder}
                </Upload>;
              case 'InputImageMsg':
                return <InputImageMsg {...opt.inputImageMsg || opt.item} disabled={disabled}/>;
              case 'switch':
                if (!opt.fieldOpt || !opt.fieldOpt.valuePropName) {
                  opt.fieldOpt = {
                    ...opt.fieldOpt,
                    valuePropName: 'checked',
                  }
                }
                return <Switch {...opt.item} disabled={disabled}/>;
              case 'SelectLabel':
                return <SelectLabel {...opt.selectLabel || opt.item} disabled={disabled}></SelectLabel>
              case 'InputNumberRange':
                return <InputNumberRange {...opt.item} />;
              case 'InputNumber':
                return <InputNumber {...opt.inputNumber || opt.item} style={{width: '100%', ...(opt.inputNumber || opt.item || {}).style}} disabled={disabled}></InputNumber>
              case 'Custom':
                return opt.component;
              case 'Input':
              default:
                return <Input {...opt.input || opt.item} disabled={disabled}/>
            }
          })();
          const field = opt.key ? getFieldDecorator(opt.key, opt.fieldOpt)(item) : '';
          return <Col key={opt.key} span={24} {...opt.col} style={opt.style}>
            <FormItem {...formItemLayout} {...opt.form}>
              {field}
              {opt.other}
            </FormItem>
          </Col>
        }
    );

    let btns = <Row>
      <Col className="table-operations" offset={this.props.btnPosition == 'bottom' ? 4 : 0}>
        {/*{this._buttonDisplayCalc('create', <Button disabled={this.state.loading} type="primary"*/}
        {/*                                           onClick={this.create.bind(this)}>新建</Button>)}*/}
        {/*{this._buttonDisplayCalc('edit', <Button disabled={this.state.loading} type="primary"*/}
        {/*                                         onClick={this.edit.bind(this)}>修改</Button>)}*/}
        {/*{this._buttonDisplayCalc('save', <Button disabled={this.state.loading} type="primary"*/}
        {/*                                         onClick={this.save.bind(this)}>保存</Button>)}*/}
        {/*{this._buttonDisplayCalc('delete', <Button disabled={this.state.loading} type="danger"*/}
        {/*                                           onClick={this.delete.bind(this)}>删除</Button>)}*/}
        {/*{this._buttonDisplayCalc('cancel', <Button disabled={this.state.loading} type="danger"*/}
        {/*                                           onClick={this.cancel.bind(this)}>取消</Button>)}*/}
        {/*{this.props.backToList && this._buttonDisplayCalc('backToList', <Button disabled={this.state.loading} type="danger"*/}
        {/*                                                                        onClick={this.backToList.bind(this)}>返回列表</Button>)}*/}

        {/*{this._buttonDisplayCalc('create', <Button disabled={this.state.loading} type="primary"*/}
        {/*                                           onClick={this.create.bind(this)}>新建</Button>)}*/}
        {this._buttonDisplayCalc('create', <div className="search-query-btn" onClick={this.create.bind(this)}>新建</div>)}
        {/*{this._buttonDisplayCalc('edit', <Button disabled={this.state.loading} type="primary"*/}
        {/*                                         onClick={this.edit.bind(this)}>修改</Button>)}          */}
        {this._buttonDisplayCalc('edit', <div className="search-reset-btn" onClick={this.edit.bind(this)}>修改</div>)}
        {/*{this._buttonDisplayCalc('save', <Button disabled={this.state.loading} type="primary"*/}
        {/*                                         onClick={this.save.bind(this)}>保存</Button>)}          */}
        {this._buttonDisplayCalc('save', <div className="search-query-btn" onClick={this.save.bind(this)}>保存</div>)}
        {this._buttonDisplayCalc('delete', <Button disabled={this.state.loading} type="danger"
                                                   onClick={this.delete.bind(this)}>删除</Button>)}
        {/*{this._buttonDisplayCalc('cancel', <Button disabled={this.state.loading} type="danger"*/}
        {/*                                           onClick={this.cancel.bind(this)}>取消</Button>)}*/}
        {this._buttonDisplayCalc('cancel', <div className="search-reset-btn" onClick={this.cancel.bind(this)}>取消</div>)}
        {/*{this._buttonDisplayCalc('backToList', <Button disabled={this.state.loading} type="danger"*/}
        {/*                                               onClick={this.backToList.bind(this)}>返回列表</Button>)}         */}
        {this._buttonDisplayCalc('backToList', <div className="search-reset-btn" onClick={this.backToList.bind(this)}>返回列表</div>)}
      </Col>
    </Row>;

    let showBtn = !!this.formConfig[this.state.formState.oper];

    return (
        <Form>
          { this.formConfig[this.state.formState.oper] && this.formConfig[this.state.formState.oper].button && this.formConfig[this.state.formState.oper].button.length == 0  ? '' : showBtn && this.props.btnPosition != 'bottom' ? [btns, <hr/>] : ''}
          <Row gutter={24} style={{
            paddingTop: 8
          }}>
            {this.state.show ? <QueueAnim className="demo-content" appear={false}> {formItems} </QueueAnim> : ''}
          </Row>
          {showBtn && this.props.btnPosition == 'bottom' ? btns : ''}
        </Form>)
  }
}

const NewForm = Form.create()(MyForm);

export default NewForm
