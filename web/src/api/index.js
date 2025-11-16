


import axios from 'axios'

axios.interceptors.request.use(
    (config) => {
        // 在发送请求前做的事情
        return config;
    },
    (error) => {
        // 对请求错误做处理（如网络异常）
        return Promise.reject(error);
    }
);

axios.interceptors.response.use(
    async (response) => {
        console.log('response', response.data.code, response.data.message);
        if (response.data.code === 401 && ['用户登录失效，请重新登录', '账号已经被登出'].indexOf(response.data.message) >= 0) {
            window.location.href = '/'
            return;
        }
        return response; // 直接返回后端接口的 data 部分
    },
    (error) => {
        // 对响应错误做处理（如 HTTP 状态码非 2xx）
        if (error.response) {
            switch (error.response.status) {
                case 401:
                    console.error('未授权，请重新登录');
                    break;
                case 404:
                    console.error('请求资源不存在');
                    break;
                case 500:
                    console.error('服务器错误');
                    break;
                default:
                    console.error('请求失败', error.message);
            }
        } else if (error.request) {
            console.error('无响应（网络或跨域问题）');
        } else {
            console.error('请求配置错误', error.message);
        }
        return Promise.reject(error); // 继续抛出错误，供业务代码捕获
    }
);


