import React, { useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { Card, List, Button, Tag, Typography, Empty } from 'antd'
import { BellOutlined, HeartOutlined, MessageOutlined, UserAddOutlined, MailOutlined } from '@ant-design/icons'
import { notifApi } from '@/services/api'
import type { Notification } from '@/types'
import dayjs from 'dayjs'

const { Text } = Typography

const notifyIcons: Record<number, React.ReactNode> = {
  0: <BellOutlined />, 1: <HeartOutlined style={{ color: '#ff4d4f' }} />,
  2: <MessageOutlined style={{ color: '#1677ff' }} />, 3: <UserAddOutlined style={{ color: '#52c41a' }} />,
  4: <MailOutlined style={{ color: '#722ed1' }} />, 5: <BellOutlined style={{ color: '#faad14' }} />,
}

const NotificationsPage: React.FC = () => {
  const navigate = useNavigate()
  const [list, setList] = useState<Notification[]>([])

  useEffect(() => {
    notifApi.getList().then((r) => setList(r.data.data.records))
  }, [])

  const handleClick = (n: Notification) => {
    notifApi.markRead(n.notificationId)
    if (n.targetType !== null && n.targetId) {
      if (n.targetType === 0) navigate(`/post/${n.targetId}`)
      else if (n.targetType === 2) navigate(`/profile/${n.targetId}`)
      else if (n.targetType === 3) navigate(`/groups/${n.targetId}`)
    }
  }

  return (
    <Card title={<span><BellOutlined /> 消息通知</span>} style={{ maxWidth: 650 }}
      extra={<Button type="link" onClick={() => notifApi.markAllRead().then(() => setList((l) => l.map((n) => ({ ...n, isRead: true }))))}>全部已读</Button>}>
      <List
        dataSource={list}
        locale={{ emptyText: <Empty description="暂无通知" /> }}
        renderItem={(n) => (
          <List.Item onClick={() => handleClick(n)} style={{ cursor: 'pointer', padding: '12px 16px', opacity: n.isRead ? 0.5 : 1 }}>
            <List.Item.Meta
              avatar={<span style={{ fontSize: 24 }}>{notifyIcons[n.notifyType] || <BellOutlined />}</span>}
              title={<Text strong>{n.title}</Text>}
              description={n.content}
            />
            <Text type="secondary" style={{ fontSize: 12 }}>{dayjs(n.createdAt).fromNow()}</Text>
          </List.Item>
        )}
      />
    </Card>
  )
}

export default NotificationsPage
