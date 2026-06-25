import React, { useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { Card, List, Avatar, Typography, Badge, Spin } from 'antd'
import { socialApi } from '@/services/api'
import type { Conversation } from '@/types'
import dayjs from 'dayjs'

const { Text } = Typography

const MessagesPage: React.FC = () => {
  const navigate = useNavigate()
  const [convs, setConvs] = useState<Conversation[]>([])
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    socialApi.getConversations().then((r) => { setConvs(r.data.data || []); setLoading(false) })
  }, [])

  return (
    <Card title="私信" style={{ maxWidth: 650 }}>
      <Spin spinning={loading}>
        <List
          dataSource={convs}
          renderItem={(c) => (
            <List.Item onClick={() => navigate(`/messages/${c.targetUser.userId}`)} style={{ cursor: 'pointer', padding: '12px 16px' }}>
              <List.Item.Meta
                avatar={
                  <Badge count={c.unreadCount} size="small">
                    <Avatar src={c.targetUser.avatarUrl} />
                  </Badge>
                }
                title={<Text strong>{c.targetUser.nickname}</Text>}
                description={
                  <Text type="secondary" ellipsis style={{ maxWidth: 400 }}>
                    {c.lastMessage}
                  </Text>
                }
              />
              <Text type="secondary" style={{ fontSize: 12 }}>{dayjs(c.lastTime).fromNow()}</Text>
            </List.Item>
          )}
          locale={{ emptyText: '暂无私信会话' }}
        />
      </Spin>
    </Card>
  )
}

export default MessagesPage
