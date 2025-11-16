const path = require('path');
import myTheme from './src/routes/cs/theme/index.js'

export default {
  es5ImcompatibleVersions: true,
  theme: myTheme,
  cssModulesExcludes: [
    path.resolve(__dirname, 'src/routes/cs/react-contextmenu.global.css'),
  ],
  entry: {
    index: './src/index.js',
    vendor: [
        'moment',
        'react',
        'react-dom'
    ],
    antd: [
      'antd/es/button',
      'antd/es/icon',
      'antd/es/table',
      'antd/es/date-picker',
      'antd/es/form',
      'antd/es/modal',
      'antd/es/grid',
      'antd/es/input',
      'antd/es/tabs',
      'antd/es/card',
      'antd/es/message',
      'antd/es/upload',
      'antd/es/cascader',
      'antd/es/progress',
      'antd/es/steps',
      'antd/es/avatar',
      'antd/es/badge',
      'antd/es/alert',
      'antd/es/comment',
      'antd/es/input-number',
      'antd/es/collapse',
    ],
    emoji: 'emoji-mart'
  },
  commons: [
    {
      names: ['vendor', 'antd', 'emoji'],
      minChunks: Infinity
    },
  ],
  extraBabelPlugins: [['import', { libraryName: 'antd', libraryDirectory: 'es', style: true }]],
  alias: {
    components: path.resolve(__dirname, 'src/components/'),
    images: path.resolve(__dirname, 'src/images/'),
  },
  html: {
    template: './src/index.ejs',
  },
  hash: true,
  ignoreMomentLocale: true,
  disableDynamicImport: false,
  publicPath: '/',
  proxy: {
    '/api': {
      target: 'http://127.0.0.1:8088',
      changeOrigin: true,
      secure: false,
    }
  }
};
