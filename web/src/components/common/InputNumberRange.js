import React, {Component} from 'react'
import {InputNumber} from 'antd'

class InputNumberRange extends Component {
    constructor(props) {
        super(props);
        this.state = {
            loading: false,

            value: {
                max: 0,
                min: 0
            }
        };
    }

    // 首次加载数据
    async componentWillMount() {
    }

    onChange(key, number) {
        const props = {
            ...this.state,
            ...this.props
        };
        let {value} = props;
        value = {
            ...value,
            [key]: number
        };
        this.setState({value});
        if (this.props.onChange) {
            this.props.onChange(value)
        }
    }

    render() {
        const props = {
            ...this.state,
            ...this.props
        };
        // delete props.value;
        let {size, value} = props;
        let {max, min} = value || {};

        return [<InputNumber value={min} onChange={this.onChange.bind(this, 'min')}/>, '-',
            <InputNumber min={min} value={max} onChange={this.onChange.bind(this, 'max')}/>];
    }
}

export default InputNumberRange
