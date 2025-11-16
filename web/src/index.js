import dva from 'dva';
import './index.css';
import 'tdesign-react/es/style/index.css';
import createHistory from 'history/createBrowserHistory';
import 'quill/dist/quill.snow.css'; // 引入 Quill 基础样式
// import createHistory from 'history/createHashHistory';
import createLoading from 'dva-loading';
import { Pagination } from 'tdesign-react';
Pagination.defaultProps = {
  ...Pagination.defaultProps,
  pageSizeOptions: [10, 20, 30, 40, 50, 100, 200, 300, 400, 500]
}
import api from './api'; // 引入 api 配置文件

window.finishProgressBar()

// 1. Initialize
const app = dva({
  history: createHistory()
});

// 2. Plugins
app.use(createLoading());

// 3. Model
app.model(require('./models/user').default);
app.model(require('./models/locale').default);

// 4. Router
app.router(require('./router').default);

// 5. Start
app.start('#root');
