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

const Search = Input.Search;
const confirm = Modal.confirm;
const Option = Select.Option;
const {Item: FormItem} = Form;
const {TextArea} = Input;
const Dragger = Upload.Dragger;
const {RangePicker} = DatePicker;
const {TreeNode} = Tree;

class SelectLabel extends Component {
    constructor(props) {
        super(props);
        this.state = {
            loading: false,
            labels: [],
        };

        this.sorter = {
            createTime: -1
        };
    }

    // 首次加载数据
    async componentWillMount() {
        this.searchAllLabel(this.sorter);
    }

    async searchAllLabel(sorter) {
        this.setState({loading: true});
        let res = await axios.post(`/api/consumer/friendLabel/10000/1`, {filters: {}, sorter});
        let map = {};
        for (const label of res.data.data) {
            map[label._id] = label.label;
        }
        this.setState({loading: false, labels: res.data.data, labelTotal: res.data.total, labelMap: map});
    }

    labelSelectChange(v) {
        let labels;
        if (v === 'all') {
        } else if (!v) {
            labels = [];
        } else {
            labels = {$in: [v]};
        }
        if (this.props.onChange) {
            this.props.onChange(labels)
        }
    }

    render() {
        const {size, value} = this.props;
        const props = {...this.props};
        // delete props.value;
        if (value != undefined) {
            if (value.$in && value.$in.length > 0) {
                props.value = value.$in
            } else if (typeof value == 'object' && value.length == 0) {
                props.value = ''
            } else {
                props.value = 'all'
            }
        }
        console.log({value,props});
        return (
            <Select placeholder={"标签查询"}  {...props} loading={this.props.loading || this.state.loading} onChange={this.labelSelectChange.bind(this)}
                    showSearch allowClear filterOption={(input, option) => option.props.children.toLowerCase().indexOf(input.toLowerCase()) >= 0}
                    dropdownMatchSelectWidth={false}>
                {this.props.hasAll == false ? '' : <Select.Option key='all' value='all'> 全部标签 </Select.Option>}
                <Select.Option key='' value=''> 无标签 </Select.Option>
                {this.state.labels.map(item => (
                    <Select.Option key={item._id} value={item._id}>
                        {item.label}
                    </Select.Option>
                ))}
            </Select>)
    }
}

export default SelectLabel
