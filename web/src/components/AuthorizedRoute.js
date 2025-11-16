import React, {Component} from 'react'
import {Redirect, Route, Link} from 'dva/router'
import {connect} from 'dva';

class AuthorizedRoute extends Component {

  constructor(props) {
    super(props);
  }

  async componentWillMount() {
    let { component, ...rest } = this.props
    // 没有检查过user，则检查一次
    if (!rest.user.checked) {
      this.props.dispatch({type: 'user/stat'})
    }
  }

  render() {
    let { component, ...rest } = this.props
    if (!rest.user.checked) {
      return null
    }

    const CustomComponent = component

    return (
      <Route
        {...rest}
        render={props =>
          rest.user.info ? (
            <CustomComponent {...props} />
          ) : (
            <Redirect
              to='/'
            />
          )
        }
      />
    )
  }
}

export default connect(({user}) => ({user}))(AuthorizedRoute)
