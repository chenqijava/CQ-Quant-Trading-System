




import React, {Component} from 'react'
import {connect} from "dva";
import {
    Button,
} from 'antd'
import { Button as TButton } from 'tdesign-react'


class SysPermissionButton extends Component {
    constructor(props) {
        super(props);
        this.state = {
          loading: false,
        };
    }

    render() {
        let {permissions, permission, buttonClass} = this.props;
        console.log('permissions', permissions, permission, this.props)
        let show = permissions.filter(item => item.key === permission && item.type === 'button').length > 0;
        return (
            show ? buttonClass === 'TButton' ? <TButton {...{...this.props, permissions: undefined, buttonClass: undefined}}>
            {this.props.children}
            </TButton> : <Button {...{...this.props, permissions: undefined, buttonClass: undefined}}>
                {this.props.children}
            </Button> : ''
        )
    }
}

export default connect(({user}) => ({
    permissions: user.permissions
}))(SysPermissionButton)

