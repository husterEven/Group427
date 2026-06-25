import React from 'react'
import { Outlet, useNavigate, useLocation } from 'react-router-dom'
import { Layout, Menu } from 'antd'
import {
  DashboardOutlined, AuditOutlined, WarningOutlined,
  StopOutlined, UserSwitchOutlined,
} from '@ant-design/icons'
import type { MenuProps } from 'antd'

const { Sider, Content } = Layout

const menuItems: MenuProps['items'] = [
  { key: '/admin/dashboard', icon: <DashboardOutlined />, label: '数据大盘' },
  { key: '/admin/audit', icon: <AuditOutlined />, label: '内容审核' },
  { key: '/admin/reports', icon: <WarningOutlined />, label: '举报处理' },
  { key: '/admin/punishments', icon: <StopOutlined />, label: '处罚管理' },
  { key: '/admin/users', icon: <UserSwitchOutlined />, label: '用户监控' },
]

const AdminLayout: React.FC = () => {
  const navigate = useNavigate()
  const location = useLocation()

  return (
    <Layout style={{ minHeight: '100vh' }}>
      <Sider width={200} style={{ background: '#001529' }}>
        <div style={{ height: 64, display: 'flex', alignItems: 'center', justifyContent: 'center', color: '#fff', fontSize: 18, fontWeight: 700 }}>
          管理后台
        </div>
        <Menu
          theme="dark"
          mode="inline"
          selectedKeys={[location.pathname]}
          items={menuItems}
          onClick={({ key }) => navigate(key)}
        />
      </Sider>
      <Content style={{ padding: 24, background: '#f5f5f5' }}>
        <Outlet />
      </Content>
    </Layout>
  )
}

export default AdminLayout
