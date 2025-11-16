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
    Select,
    Upload,
    Tree,
    TimePicker
} from 'antd'
import axios from 'axios'
import tTypes from '../../components/taskTypes'
import NewForm from "../../components/common/newForm";
import { Dialog } from 'tdesign-react';
import DialogApi from '../../routes/admin/common/dialog/DialogApi';

const Search = Input.Search;
const confirm = Modal.confirm;
const Option = Select.Option;
const {Item: FormItem} = Form;
const {TextArea} = Input;
const Dragger = Upload.Dragger;
const {RangePicker} = DatePicker;
const {TreeNode} = Tree;

const consumer = '/api/consumer/accountGroup';

class SelectLabel extends Component {
    constructor(props) {
        super(props);
        this.state = {
            loading: false,
            visible: false,
        };
    }

    // 首次加载数据
    async componentWillMount() {
    }

    async load() {
        this.formProps.form.setFieldsValue(this.props.task.params)
    }

    async submitData(form) {
        await this.setState({loading: true});
        try {
            let data = {
                id: this.props.task._id,
                ...form,
            };
            let result = await axios.post(`/api/consumer/task/saveGroupTask`, data);
            if (result.data.code != 1) {
                message.error('失败，' + result.data.message)
            } else {
                message.success('操作成功');
            }
        } catch (e) {
        }
        await this.reload();
        await this.setState({loading: false, visible: false});
    }

    async confirm() {
        let form = await new Promise((resolve, reject) => {
            this.formProps.form.validateFields(async (err, form) => {
                if (err) reject(err);
                resolve(form)
            })
        });

        //let subData = form.subData;
        //delete form.subData;
        // for (const subDatum in subData) {
        //     form[`subData.${subDatum}`] = subData[subDatum];
        // }
        form[`subData.interval`] = form.interval;
        form[`subData.everyDayMsgLimit`] = form.everyDayMsgLimit;
        delete form.interval;
        delete form.everyDayMsgLimit;
        DialogApi.warning({
            title: `确定要修改${this.props.task.desc}任务的参数吗？`,
            content: <div>
            </div>,
            onOkTxt: '确定',
            onCancelTxt: '取消',
            onOk: this.submitData.bind(this, form),
            onCancel() {
            }
        })
    }

    async reload() {
        if (typeof this.props.reload == 'function') await this.props.reload()
    }

    render() {
        const formItemLayout = {
            colon: true,
            labelCol: {
                span: 11
            }, wrapperCol: {
                span: 13
            }
        };

        const formItemss = [
            {
                key: 'accountLimit',
                form: {
                    ...formItemLayout,
                    label: "同时发送账号数量"
                },
                itemType:"InputNumber",
            }, {
                key: 'msgLimit',
                form: {
                    ...formItemLayout,
                    label: <span>同账号最多发送?条消息(<span style={{color: 'red'}}>0不限制</span>)</span>
                },
                itemType:"InputNumber",
            }, {
                key: 'interval',
                form: {
                    ...formItemLayout,
                    label: "同账号发送消息时间间隔(s)",
                },
                itemType:"InputNumber",
            },
            // {
            //     key: 'everyDayMsgLimit',
            //     form: {
            //         ...formItemLayout,
            //         label: "每个账号每天最大发送数",
            //     },
            //     itemType:"InputNumber",
            // }
        ];
        return [
            <Button type="link" onClick={async () => {
                await this.setState({visible: true,});
            }}>修改参数</Button>,
            <Dialog
                width={570}
                visible={this.state.visible}
                loading={this.state.loading}
                onConfirm={this.confirm.bind(this)}
                header={`修改(${this.props.task.desc})任务参数`}
                onClose={() => { this.setState({ visible: false }) }}
                onCancel={() => { this.setState({ visible: false }) }}
            >
                <NewForm {...this.props} formItemLayout={{ labelCol: { span: 5 } }} needHiddenItems={this.state.needHiddenItems} {...{ formItemss }} query={{ oper: 'form' }} initForm={(formProps) => {
                    this.formProps = formProps;
                    this.load();
                }} />
                {this.props.children}
            </Dialog>
        ];
    }
}

export default SelectLabel
