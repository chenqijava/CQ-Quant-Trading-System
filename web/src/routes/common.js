import React, {Component} from 'react'
import {BrowserRouter as Router, Route, Switch} from 'react-router-dom'

import Login from './login'
import MainPage from './mainPage.js'

class Common extends Component {
  render() {
    return (
      <Switch>
        <Route exact={true} path="/" component={Login}/>
        <Route exact={true} path="/main" component={MainPage}/>
      </Switch>
    )
  }
}

export default Common
