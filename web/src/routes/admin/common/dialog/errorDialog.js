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
                <path d="M12 23C18.0751 23 23 18.0751 23 12C23 5.92487 18.0751 1 12 1C5.92487 1 1 5.92487 1 12C1 18.0751 5.92487 23 12 23ZM8.81753 7.40346L11.9999 10.5858L15.1815 7.40414L16.5957 8.81835L13.4141 12L16.5957 15.1816L15.1815 16.5958L11.9999 13.4142L8.81753 16.5965L7.40332 15.1823L10.5856 12L7.40332 8.81767L8.81753 7.40346Z" fill="#D54941"/>
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
