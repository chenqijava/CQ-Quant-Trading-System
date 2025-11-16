import React, {Component} from 'react'

class MyComponent extends Component {
  constructor(props) {
    super(props);
    this.state = {
    };
  }

  async componentWillMount() {
  }

  render() {
    let loading = this.props.loading || this.state.loading;

    console.log(this.props)

    return this.props.visible ? <div className='mask' style={{backgroundColor: 'rgba(0, 0, 0, 0.5)'}}>
        <div className="cust-dialog" style={{width: this.props.width ? this.props.width : 480}}>
            <div className="cust-dialog-header">
                <div style={{width: 24,height: 24}}>
                <svg xmlns="http://www.w3.org/2000/svg" width="24" height="24" viewBox="0 0 24 24" fill="none">
                <path d="M12 23C18.0751 23 23 18.0751 23 12C23 5.92487 18.0751 1 12 1C5.92487 1 1 5.92487 1 12C1 18.0751 5.92487 23 12 23ZM7.49985 10.5858L10.4999 13.5858L16.4999 7.58578L17.9141 8.99999L10.4999 16.4142L6.08564 12L7.49985 10.5858Z" fill="#2BA471"/>
                </svg>
                </div>
            <div className="cust-dialog-header-title">
                {this.props.title}
            </div>
            </div>
            <div className="cust-dialog-body">
                {this.props.children}
            </div>
            <div className="cust-dialog-footer">
                { this.props.onCancel ? <div className="search-reset-btn" onClick={() => this.props.onCancel && this.props.onCancel()}>{this.props.onCancelTxt || '取消'}</div> : ''}
                { this.props.onOk ? <div className="search-query-btn" onClick={() => this.props.onOk && this.props.onOk()}>{this.props.onOkTxt || '确定'}</div> : '' }
            </div>
        </div>
    </div> : ''
  }
}

export default MyComponent
