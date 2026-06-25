import React, { useEffect } from 'react'
import { Outlet, useNavigate, Link } from 'react-router-dom'
import { Layout, Menu, Badge, Avatar, Dropdown, Input, Button, Space } from 'antd'
import {
  HomeOutlined, BellOutlined, MessageOutlined, UserOutlined,
  TeamOutlined, PlusOutlined, SearchOutlined, SettingOutlined,
  StarOutlined, LogoutOutlined, OrderedListOutlined,
} from '@ant-design/icons'
import { useAuthStore, useAppStore } from '@/store'
import { sectionApi, notifApi, socialApi, authApi } from '@/services/api'
import type { MenuProps } from 'antd'

const { Header, Content, Sider } = Layout

const MainLayout: React.FC = () => {
  const navigate = useNavigate()
  const { user, logout } = useAuthStore()
  const { sections, unreadCount, dmUnreadCount, setSections, setUnreadCount, setDmUnreadCount } = useAppStore()

  useEffect(() => {
    sectionApi.getAll().then((r) => setSections(r.data.data))
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [])

  useEffect(() => {
    notifApi.getUnreadCount().then((r) => setUnreadCount(r.data.data.total)).catch(() => {})
    socialApi.getDmUnreadCount().then((r) => setDmUnreadCount(r.data.data.total)).catch(() => {})
    const timer = setInterval(() => {
      notifApi.getUnreadCount().then((r) => setUnreadCount(r.data.data.total)).catch(() => {})
      socialApi.getDmUnreadCount().then((r) => setDmUnreadCount(r.data.data.total)).catch(() => {})
    }, 30000)
    return () => clearInterval(timer)
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [])

  const handleLogout = async () => {
    try { await authApi.logout() } catch { /* ignore */ }
    logout()
    navigate('/login')
  }

  const userMenu: MenuProps['items'] = [
    { key: 'profile', icon: <UserOutlined />, label: '个人主页', onClick: () => navigate(`/profile/${user?.userId}`) },
    { key: 'settings', icon: <SettingOutlined />, label: '账号设置', onClick: () => navigate('/settings/profile') },
    { key: 'collections', icon: <StarOutlined />, label: '我的收藏', onClick: () => navigate('/collections') },
    { type: 'divider' },
    { key: 'logout', icon: <LogoutOutlined />, label: '退出登录', onClick: handleLogout },
  ]

  const sectionItems: MenuProps['items'] = sections.map((s) => ({
    key: `s-${s.sectionId}`,
    icon: <OrderedListOutlined />,
    label: s.sectionName,
    children: s.zones?.map((z) => ({
      key: `z-${z.zoneId}`,
      label: z.zoneName,
      onClick: () => navigate(`/zone/${z.zoneId}`),
    })),
  }))

  return (
    <Layout style={{ minHeight: '100vh' }}>
      <Header style={{ display: 'flex', alignItems: 'center', background: '#fff', borderBottom: '1px solid #f0f0f0', padding: '0 24px', position: 'sticky', top: 0, zIndex: 100 }}>
        <Link to="/" style={{ fontSize: 20, fontWeight: 700, marginRight: 32, color: '#1677ff', whiteSpace: 'nowrap' }}>
          投资论坛
        </Link>
        <Input.Search
          placeholder="搜索帖子、用户、股票代码..."
          style={{ maxWidth: 360 }}
          onSearch={(v) => v && navigate(`/search?keyword=${encodeURIComponent(v)}`)}
          prefix={<SearchOutlined />}
        />
        <Space style={{ marginLeft: 'auto' }} size={20}>
          <Link to="/dynamics"><HomeOutlined style={{ fontSize: 20 }} /></Link>
          <Badge count={unreadCount} size="small">
            <Link to="/notifications"><BellOutlined style={{ fontSize: 20 }} /></Link>
          </Badge>
          <Badge count={dmUnreadCount} size="small">
            <Link to="/messages"><MessageOutlined style={{ fontSize: 20 }} /></Link>
          </Badge>
          <Link to="/groups"><TeamOutlined style={{ fontSize: 20 }} /></Link>
          <Button type="primary" icon={<PlusOutlined />} onClick={() => navigate('/editor')}>发帖</Button>
          <Dropdown menu={{ items: userMenu }} placement="bottomRight">
            <Avatar src={user?.avatarUrl} icon={<UserOutlined />} style={{ cursor: 'pointer' }} />
          </Dropdown>
        </Space>
      </Header>
      <Layout>
        <Sider width={200} style={{ background: '#fff', borderRight: '1px solid #f0f0f0' }}>
          <Menu mode="inline" style={{ height: '100%', borderRight: 0 }} items={sectionItems} />
        </Sider>
        <Content style={{ padding: 24, background: '#f5f5f5', minHeight: 'calc(100vh - 64px)' }}>
          <Outlet />
        </Content>
      </Layout>
    </Layout>
  )
}

export default MainLayout
