import React, {Component} from 'react'
import {Button, Col, DatePicker, Form, Icon, Input, message, Modal, Radio, Row, Select, Upload, Tree, TimePicker} from 'antd'
import axios from 'axios'
import moment from 'moment'

const Search = Input.Search
const confirm = Modal.confirm
const Option = Select.Option;
const {Item: FormItem} = Form
const {TextArea} = Input
const Dragger = Upload.Dragger
const {RangePicker} = DatePicker;
const { TreeNode } = Tree;

// 针对当前页面的基础url
const baseUrl = '/api/consumer/role';

// 卡片的表单界面
class MyForm extends Component {
  constructor(props) {
    super(props);
    this.state = {
      loading: false,
      // 当前的状态
      formState: {
        oper: 'none'
      },
      menus:[{name:'test',key:'key'}],
      checkedKeys: [],
      permissions: [],
      // 需要隐藏的字段
      hiddenItems: [],
    }
    // 只有新建、修改有取消按钮，所以需要计算出上一个状态
    if (this.props.query.oper == 'edit') {
      this.state.formState = {
        oper: 'view',
        _id: this.props.query._id
      }
    }

    // 表单的状态模式，不同状态下，显示的按钮不一样
    // 这个状态配置一直不变所以不用放到state里
    this.formConfig = {
      // none状态，显示新建按钮，所以字段无数据，不可编辑
      // 这个状态没啥具体作用，主要是create状态点取消的时候进入此状态
      none: {
        button: [
          'create', 'backToList'
        ],
        input: [],
        inputDisable: []
      },
      // view状态下显示
      view: {
        button: [
          'create', 'backToList'
        ],
        input: [],
        inputDisable: ['_id', '__v', 'createTime', 'updateTime']
      },
      create: {
        button: [
          'save', 'cancel'
        ],
        input: [/.+/],
        inputDisable: ['_id', '__v', 'createTime', 'updateTime']
      }
    }
    // 基本上，新建和编辑都是相同配置
    this.formConfig.edit = this.formConfig.create;

    this.hiddenConfig = {
      // type选择对应值后需要显示的参数,未选中会被隐藏
      // type: {
      //   0: ['keyword'],
      // }
    }
  }

  // 根据按钮编码返回display值
  _buttonDisplayCalc(buttonCode, button) {
    return this.formConfig[this.state.formState.oper].button.indexOf(buttonCode) == -1
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

  _setHiddenItems(name, value) {
    let hiddenItems = this.state.hiddenItems;
    const hiddenConfig = this.hiddenConfig[name];
    for (let v in hiddenConfig) {
      if (value !== v)
        hiddenItems = hiddenItems.concat(hiddenConfig[v]);
    }
    // 过滤掉需要显示的
    hiddenItems = hiddenItems.filter(it =>
      !(hiddenConfig[value] || []).find(v => {
        if (typeof it === 'object' && it.source) {  //如果是正则表达式,则比较source是否相同
          return it.source === v.source;
        } else {
          return it == v;
        }
      })
    );
    this.setState({hiddenItems});
  }

  // 判断某字段是否需要显示
  _itemShowCalc(inputCode) {
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
    await this.loadPermissions();
    await this.changeState(this.props.query)
  }

  async loadPermissions() {
    let menus = [];
    try {
      let result = await axios.get('/api/common/user/loadPermissions');
      if (result.data.code) {
        this.submenusMap = {};     // 用来记录各菜单的子菜单
        result.data.permissions.forEach(pm => {
          if (!pm.parent) {       // 没有parent的是一级菜单
            menus.push(pm);
          } else {                // 将菜单放到父菜单的数组中
            let id = pm.parent;
            if (!this.submenusMap[id]) {
              this.submenusMap[id] = [];
            }
            this.submenusMap[id].push(pm)
          }
          let id = pm._id;
          if (!this.submenusMap[id]) {
            this.submenusMap[id] = [];
          }
          pm.submenus = this.submenusMap[id];  // 链接子菜单数组
        })
      }
    } finally {
      this.setState({menus});
    }
  }

  // 从上一个状态中恢复
  async changeState(newState) {
    // 备份状态
    this.oldFormState = this.state.formState
    // 切状态
    this.setState({keys: [0], formState: newState, loading: true})
    let checkedKeys = [];
    let permissions = [];
    // 更新form的值
    if (newState._id) {
      // 读取服务器数据，然后赋值给表单，看看赋值后是否需要清理一下错误提示
      let res = await axios.get(`${baseUrl}/card/${newState._id}`)
      let domain = res.data.data
      if (domain) {
        permissions = domain.permissions;
        checkedKeys = permissions.filter(p => !this.submenusMap[p] || this.submenusMap[p].length === 0);
        this.props.form.setFieldsValue(domain);
      } else {
        Modal.error({title: '数据不存在'})
      }
    } else {
      this.props.form.resetFields()
    }
    // 解除loading
    this.setState({loading: false, permissions, checkedKeys})
  }

  // 基本通用的按钮
  async create() {
    this.changeState({oper: 'create'})
  }

  async edit() {
    this.changeState({oper: 'edit', _id: this.state.formState._id})
  }

  async backToList() {
    this.props.history.push('list')
  }

  async save() {
    this.props.form.validateFields(async (err, form) => {
      if (!err) {
        this.setState({loading: true});
        form.permissions = this.state.permissions;
        // 过滤掉null
        let res = await axios.post(`${baseUrl}/card/save`, form)
        if (res.data.code == 1) {
          this.changeState({oper: 'view', _id: res.data._id})
          message.success('操作成功')
        } else {
          Modal.error({title: res.data.message})
          this.setState({loading: false})
        }
      }
    })
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
    return options.map(option => <Option key={option.value}>{option.label}</Option>);
  }

  onCheck = (checkedKeys, info) => {
    this.setState({checkedKeys,permissions:info.halfCheckedKeys.concat(checkedKeys)});
  };

  _renderTreeNodes = data =>
    data.sort((a, b) => a.index - b.index).map(item => {
        return (
          <TreeNode title={item.name} key={item._id} dataRef={item}>
            {item.submenus && item.submenus.length ? this._renderTreeNodes(item.submenus) : ''}
          </TreeNode>
        );
    });

  render() {
    const {getFieldDecorator} = this.props.form

    const formItemLayout = {
      colon: false,
      labelCol: {
        span: 8
      }, wrapperCol: {
        span: 16
      }
    };

    const formItemss = [
      {
        key: '_id',
        style: {display: 'none'},
        form: {label: '主键'},
      }, {
        key: '__v',
        style: {display: 'none'},
        form: {label: '乐观锁'},
      }, {
        key: 'name',
        form: {label: '名称'},
        fieldOpt: {
          rules: [
            {
              required: true,
              message: '不能为空'
            },
          ]
        },
      }, {
        // key: 'permissions',
        form: {label: '权限'},
        other: <Tree checkable multiple defaultExpandAll onCheck={this.onCheck} checkedKeys={this.state.checkedKeys}
                     selectedKeys={this.state.permissions} disabled={this._itemDisableCalc('permissions')}>
          {this._renderTreeNodes(this.state.menus)}
        </Tree>
      }, {
        key: 'createTime',
        form: {label: '创建时间'},
        fieldOpt: {
          // 转换默认的 value 给控件
          normalize: (value, prevValue, allValues) => value && moment(value),
        },
        itemType: 'DatePicker',
      }];

    const formItems = formItemss.filter(opt => this._itemShowCalc(opt.key)).map((opt) => {
        const item = (() => {
          let disabled = this._itemDisableCalc(opt.key);
          switch (opt.itemType) {
            case 'Select':
              return <Select {...opt.select} disabled={disabled}>
                {this._arrayToFormOption(opt.options)}
              </Select>;
            case 'Radio':
              return <Radio.Group {...opt.radio} options={opt.options} disabled={disabled}/>
            case 'TextArea':
              return <TextArea rows={5} disabled={disabled}></TextArea>
            case 'DatePicker':
              return <DatePicker disabled={disabled} format="YYYY-MM-DD HH:mm:ss" placeholder='' showTime={{}}/>
            case 'Upload':
              return <Upload {...opt.upload}>
                <Button>
                  <Icon type="upload"/>
                  上传
                </Button>
              </Upload>;
            case 'Input':
            default:
              return <Input {...opt.input} disabled={disabled}/>
          }
        })();
        const field = opt.key ? getFieldDecorator(opt.key, opt.fieldOpt)(item) : '';
        return <Col span={6} {...opt.col} style={opt.style}>
          <FormItem {...formItemLayout} {...opt.form}>
            {field}
            {opt.other}
          </FormItem>
        </Col>
      }
    );

    return (<Form>
      <Row>
        <Col className="table-operations">
          {this._buttonDisplayCalc('create', <Button disabled={this.state.loading} type="primary"
                                                     onClick={this.create.bind(this)}>新建</Button>)}
          {this._buttonDisplayCalc('edit', <Button disabled={this.state.loading} type="primary"
                                                   onClick={this.edit.bind(this)}>修改</Button>)}
          {this._buttonDisplayCalc('save', <Button disabled={this.state.loading} type="primary"
                                                   onClick={this.save.bind(this)}>保存</Button>)}
          {this._buttonDisplayCalc('delete', <Button disabled={this.state.loading} type="danger"
                                                     onClick={this.delete.bind(this)}>删除</Button>)}
          {this._buttonDisplayCalc('cancel', <Button disabled={this.state.loading} type="danger"
                                                     onClick={this.cancel.bind(this)}>取消</Button>)}
          {this._buttonDisplayCalc('backToList', <Button disabled={this.state.loading} type="danger"
                                                         onClick={this.backToList.bind(this)}>返回列表</Button>)}
        </Col>
      </Row>
      <hr/>
      <Row gutter={24} style={{
        paddingTop: 8
      }}>
        {formItems}
      </Row>
    </Form>)
  }
}

const NewForm = Form.create()(MyForm)

class MyComponent extends Component {
  // 这个组件作为card表单的父容器，只起到一个作用
  // 就是初始化的时候，解析一下url，把需要传递的初始化参数（例如oper，_id等）传给card表单
  // 其它的新建、保存、修改等操作，统一由表单完成
  constructor(props) {
    super(props);
    // 提前解析一下query的参数
    if (this.props.location.search.length > 0) {
      this.query = this.props.location.search.substring(1).split('&').map((v) => {
        let obj = {}
        let sp = v.split('=')
        obj[sp[0]] = sp[1]
        return obj
      }).reduce((obj, p) => {
        return {
          ...obj,
          ...p
        }
      })
    }

    if (!this.query || !this.query.oper) {
      // 如果没有，则默认为none
      this.query = {
        oper: 'none'
      }
    }
  }

  render() {
    return (<div>
      <h3>角色管理</h3>
      <hr/>
      <NewForm {...this.props} query={this.query}/>
    </div>)
  }
}

export default MyComponent
