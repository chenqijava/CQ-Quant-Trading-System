import React, {Component} from 'react'
import {Icon, message, Upload, Input} from 'antd'
const {TextArea} = Input;

class InputTextMsg extends Component {

  constructor(props) {
    super(props);
    this.state = {
    }
  }

  async componentWillMount() {
  }

  render() {
    return (
      <div className="clearfix">
        <TextArea {...this.props} rows={5} value={this.props.text} onChange={this.props.handleTextChange}/>
      </div>
    )
  }
}

export default InputTextMsg;
