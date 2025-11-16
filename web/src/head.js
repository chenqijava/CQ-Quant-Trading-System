import React from "react";
import Helmet from "react-helmet";
import axios from 'axios'

// 通用全局参数设置
const params = [{
  type: 'webConfig',
  code: 'head',
}];

export default class Application extends React.Component {
  constructor() {
    super();

    this.state = {
      config: {}
    }
  }

  async componentWillMount() {
    this.reloadConfig();
  }

  async reloadConfig() {
    let config = {};
    for (let i in params) {
      let param = params[i];
      let res = await axios.get(`/api/common/params/${param.type || pageType}/get/${param.code}`);
      if (res.data.code) {
        config[`${param.type}-${param.code}`] = param.unit ? res.data.value / (param.unit || 1) : res.data.value;
      }
    }
    this.setState({config});
  }

  render() {
    const title = this.state.config['webConfig-head'] || '';

    return (
        <div className="application">
          <Helmet>
            <meta charSet="UTF-8"/>
            <meta name="viewport" content="width=device-width, initial-scale=1"/>
            <title>{title}</title>
            <link rel="shortcut icon" href="Logo-white@2x.png"/>
            <link rel="Bookmark" href="Logo-white@2x.png"/>
            <link rel="icon" href="Logo-white@2x.png" type="image/x-icon"/>
          </Helmet>
        </div>
    );
  }
};
