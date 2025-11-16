import React, {Component} from 'react'
import {BrowserRouter as Router, Route, Link} from 'react-router-dom'
import {
  Icon,
  Table,
  Divider,
  Upload,
  message,
  Input,
  Button,
  Modal
} from 'antd'
import axios from 'axios'
import {formatDate} from 'components/DateFormat'

class Home extends Component {

  constructor(props) {
    super(props);
  }

  // 首次加载数据
  async componentWillMount() {
  }
  render() {
    return <div>hi</div>
  }
}

export default Home
