import React, {Component} from 'react'
import {routerRedux, Router, Route, Switch} from 'dva/router';
import dynamic from 'dva/dynamic';
import AuthorizedRoute from 'components/AuthorizedRoute'
import NProgress from 'nprogress'
import 'nprogress/nprogress.css'

import MyProvider from "./nb-intl/MyProvider";

class ProgressBar extends Component {

  componentWillMount() {
    NProgress.start()
  }
  componentWillUnmount() {
    NProgress.done()
  }

  render() {
    return null
  }
}
dynamic.setDefaultLoadingComponent(() => {
  return <ProgressBar/>
  // return <Spin size="large" className='globalSpin'/>;
});

const {ConnectedRouter} = routerRedux;
function RouterConfig({history, app}) {
  const Common = dynamic({
    component: () => import ('./routes/common')
  });

  const BackStage = dynamic({
    component: () => import ('./routes/BackStage')
  });

  return (
      <MyProvider>
        <ConnectedRouter history={history}>
          <Switch>
            <AuthorizedRoute path={["/admin/", "/cloud/", "/cloud2/"]} component={BackStage}/>
            <Route path="/" component={Common}/>
          </Switch>
        </ConnectedRouter>
      </MyProvider>
  );
}

export default RouterConfig;
