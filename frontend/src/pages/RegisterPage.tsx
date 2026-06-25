import React, { useState } from 'react'
import { useNavigate, Link } from 'react-router-dom'
import { Form, Input, Button, Tabs, Checkbox, message } from 'antd'
import { PhoneOutlined, MailOutlined } from '@ant-design/icons'
import { authApi, userApi } from '@/services/api'
import { useAuthStore } from '@/store'

const RegisterPage: React.FC = () => {
  const navigate = useNavigate()
  const { setAuth } = useAuthStore()
  const [loading, setLoading] = useState(false)

  const handleRegister = async (values: { nickname: string; account: string; password: string }) => {
    setLoading(true)
    try {
      const regRes = await authApi.register(values)
      const { accessToken, refreshToken, tokenType, expiresIn } = regRes.data.data
      localStorage.setItem('accessToken', accessToken)
      localStorage.setItem('refreshToken', refreshToken)
      const userRes = await userApi.getMe()
      setAuth({ accessToken, refreshToken, tokenType, expiresIn }, userRes.data.data)
      message.success('注册成功，已自动登录')
      navigate('/')
    } finally {
      setLoading(false)
    }
  }

  return (
    <div style={{ maxWidth: 420, margin: '60px auto', padding: 32, background: '#fff', borderRadius: 12, boxShadow: '0 2px 12px rgba(0,0,0,0.08)' }}>
      <h1 style={{ textAlign: 'center', marginBottom: 24, fontSize: 24, fontWeight: 700 }}>创建账号</h1>
      <Tabs
        centered
        items={[
          {
            key: 'mobile',
            label: <span><PhoneOutlined /> 手机号注册</span>,
            children: (
              <Form onFinish={handleRegister} layout="vertical" size="large">
                <Form.Item name="nickname" rules={[{ required: true, min: 2, max: 20, message: '昵称长度2-20个字符' }]}>
                  <Input placeholder="设置昵称" />
                </Form.Item>
                <Form.Item name="account" rules={[{ required: true, message: '请输入手机号' }]}>
                  <Input placeholder="手机号" />
                </Form.Item>
                <Form.Item name="code">
                  <Input placeholder="验证码（测试用，无需填写）" />
                </Form.Item>
                <Form.Item name="password" rules={[{ required: true, min: 6, max: 32, message: '密码6-32位' }]}>
                  <Input.Password placeholder="设置登录密码（6-32位）" />
                </Form.Item>
                <Form.Item name="agreement" valuePropName="checked" rules={[{ required: true, message: '请同意用户协议' }]}>
                  <Checkbox>我已阅读并同意《用户服务协议》</Checkbox>
                </Form.Item>
                <Form.Item>
                  <Button type="primary" htmlType="submit" loading={loading} block>立即注册</Button>
                </Form.Item>
              </Form>
            ),
          },
          {
            key: 'email',
            label: <span><MailOutlined /> 邮箱注册</span>,
            children: (
              <Form onFinish={handleRegister} layout="vertical" size="large">
                <Form.Item name="nickname" rules={[{ required: true, min: 2, max: 20, message: '昵称长度2-20个字符' }]}>
                  <Input placeholder="设置昵称" />
                </Form.Item>
                <Form.Item name="account" rules={[{ required: true, type: 'email', message: '请输入有效邮箱' }]}>
                  <Input placeholder="常用邮箱" />
                </Form.Item>
                <Form.Item name="password" rules={[{ required: true, min: 6, max: 32, message: '密码6-32位' }]}>
                  <Input.Password placeholder="设置登录密码（6-32位）" />
                </Form.Item>
                <Form.Item name="agreement" valuePropName="checked" rules={[{ required: true, message: '请同意用户协议' }]}>
                  <Checkbox>我已阅读并同意《用户服务协议》</Checkbox>
                </Form.Item>
                <Form.Item>
                  <Button type="primary" htmlType="submit" loading={loading} block>立即注册</Button>
                </Form.Item>
              </Form>
            ),
          },
        ]}
      />
      <div style={{ textAlign: 'center' }}>
        已有账号？<Link to="/login">去登录</Link>
      </div>
    </div>
  )
}

export default RegisterPage
