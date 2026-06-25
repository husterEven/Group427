import React, { useState } from 'react'
import { useNavigate, Link } from 'react-router-dom'
import { Form, Input, Button, Tabs, message, Divider } from 'antd'
import { PhoneOutlined, MailOutlined, WechatOutlined, WeiboOutlined } from '@ant-design/icons'
import { authApi, userApi } from '@/services/api'
import { useAuthStore } from '@/store'

const LoginPage: React.FC = () => {
  const navigate = useNavigate()
  const { setAuth } = useAuthStore()
  const [loading, setLoading] = useState(false)

  const handleLogin = async (values: { account: string; password: string }) => {
    setLoading(true)
    try {
      const loginRes = await authApi.login(values)
      const { accessToken, refreshToken, tokenType, expiresIn } = loginRes.data.data
      // 先存 token，再调用 getMe()，否则请求不带认证信息
      localStorage.setItem('accessToken', accessToken)
      localStorage.setItem('refreshToken', refreshToken)
      const userRes = await userApi.getMe()
      setAuth({ accessToken, refreshToken, tokenType, expiresIn }, userRes.data.data)
      message.success('登录成功')
      navigate('/')
    } finally {
      setLoading(false)
    }
  }

  return (
    <div style={{ maxWidth: 400, margin: '80px auto', padding: 32, background: '#fff', borderRadius: 12, boxShadow: '0 2px 12px rgba(0,0,0,0.08)' }}>
      <h1 style={{ textAlign: 'center', marginBottom: 32, fontSize: 28, fontWeight: 700, color: '#1677ff' }}>投资论坛</h1>
      <Tabs
        centered
        items={[
          {
            key: 'mobile',
            label: <span><PhoneOutlined /> 手机号登录</span>,
            children: (
              <Form onFinish={handleLogin} layout="vertical" size="large">
                <Form.Item name="account" rules={[{ required: true, message: '请输入手机号' }]}>
                  <Input placeholder="手机号" prefix={<PhoneOutlined />} />
                </Form.Item>
                <Form.Item name="password" rules={[{ required: true, message: '请输入密码' }]}>
                  <Input.Password placeholder="登录密码" />
                </Form.Item>
                <Form.Item>
                  <Button type="primary" htmlType="submit" loading={loading} block>
                    登录
                  </Button>
                </Form.Item>
              </Form>
            ),
          },
          {
            key: 'email',
            label: <span><MailOutlined /> 邮箱登录</span>,
            children: (
              <Form onFinish={handleLogin} layout="vertical" size="large">
                <Form.Item name="account" rules={[{ required: true, message: '请输入邮箱' }]}>
                  <Input placeholder="邮箱地址" prefix={<MailOutlined />} />
                </Form.Item>
                <Form.Item name="password" rules={[{ required: true, message: '请输入密码' }]}>
                  <Input.Password placeholder="登录密码" />
                </Form.Item>
                <Form.Item>
                  <Button type="primary" htmlType="submit" loading={loading} block>
                    登录
                  </Button>
                </Form.Item>
              </Form>
            ),
          },
        ]}
      />
      <Divider plain>第三方登录</Divider>
      <div style={{ display: 'flex', justifyContent: 'center', gap: 24 }}>
        <Button shape="circle" size="large" icon={<WechatOutlined style={{ color: '#07c160' }} />}
          onClick={() => message.info('微信登录开发中')} />
        <Button shape="circle" size="large" icon={<WeiboOutlined style={{ color: '#e6162d' }} />}
          onClick={() => message.info('微博登录开发中')} />
      </div>
      <div style={{ textAlign: 'center', marginTop: 16 }}>
        还没有账号？<Link to="/register">立即注册</Link>
      </div>
    </div>
  )
}

export default LoginPage
