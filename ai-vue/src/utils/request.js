// 引入axios
import axios from 'axios'
import { ElMessage } from 'element-plus'


// 创建axios实例
const service = axios.create({
  baseURL: '/api', // 请求的前缀
  timeout: 5000, // 请求的超时时间
})

// 请求拦截器
service.interceptors.request.use(
  (config) => {
    // 在发送请求之前做些什么
    const token = localStorage.getItem('token')
    if (token) {
      config.headers['token'] = token
    }
    return config
  },
  (error) => {
    // 对请求错误做些什么
    return Promise.reject(error)
  }
)

// 响应拦截器
service.interceptors.response.use(
  (response) => {
    // 对响应数据做点什么
    const { data, config } = response
    // 后端返回格式: { code, msg, data }, 成功code为'200'
    if (data.code === '200') {
      return data.data
    } else {
      // 业务错误
      const msg = data.msg || '请求失败'
      // token相关错误码: A0230(token无效/过期)、A0231(禁止访问)、A0301(未授权)
      if (data.code === 'A0230' || data.code === 'A0231' || data.code === 'A0301' || data.code === '-1') {
        ElMessage.error('登录过期，请重新登录')
        localStorage.removeItem('token')
        localStorage.removeItem('userInfo')
        window.location.href = '/auth/login'
      } else {
        ElMessage.error(msg)
      }
      return Promise.reject(msg)
    }
  },
  (error) => {
    // 对响应错误做点什么
    const status = error.response?.status
    const msg = error.response?.data?.msg || error.response?.data?.message
    if (status === 401) {
      ElMessage.error(msg || '用户名或密码错误')
    } else if (status) {
      ElMessage.error(msg || `请求失败(${status})`)
    } else {
      ElMessage.error('网络连接异常，请检查网络')
    }
    return Promise.reject(error)
  }
)

export default service