import React from 'react'
import { Card, Row, Col, Statistic, Table } from 'antd'
import { UserOutlined, FileTextOutlined, MessageOutlined, RiseOutlined } from '@ant-design/icons'

const AdminDashboard: React.FC = () => {
  return (
    <div>
      <h2 className="page-title">数据大盘</h2>
      <Row gutter={16}>
        <Col span={6}><Card><Statistic title="日活用户" value={12580} prefix={<UserOutlined />} /></Card></Col>
        <Col span={6}><Card><Statistic title="月活用户" value={89420} prefix={<UserOutlined />} /></Card></Col>
        <Col span={6}><Card><Statistic title="今日发帖" value={356} prefix={<FileTextOutlined />} /></Card></Col>
        <Col span={6}><Card><Statistic title="今日评论" value={2483} prefix={<MessageOutlined />} /></Card></Col>
      </Row>
      <Row gutter={16} style={{ marginTop: 16 }}>
        <Col span={6}><Card><Statistic title="新增用户" value="+218" prefix={<RiseOutlined />} valueStyle={{ color: '#3f8600' }} /></Card></Col>
        <Col span={6}><Card><Statistic title="总帖子数" value={128900} /></Card></Col>
        <Col span={6}><Card><Statistic title="总评论数" value={886000} /></Card></Col>
        <Col span={6}><Card><Statistic title="待审核" value={47} valueStyle={{ color: '#cf1322' }} /></Card></Col>
      </Row>
    </div>
  )
}

export default AdminDashboard
