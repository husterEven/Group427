import React, { useEffect, useState, useRef } from 'react'
import { useParams } from 'react-router-dom'
import { Card, Input, Button, Avatar, List, Typography, Spin, message } from 'antd'
import { SendOutlined } from '@ant-design/icons'
import { socialApi, userApi } from '@/services/api'
import { useAuthStore } from '@/store'
import type { PrivateMessage, UserBrief } from '@/types'
import dayjs from 'dayjs'

const { Text } = Typography

const ChatPage: React.FC = () => {
  const { userId } = useParams<{ userId: string }>()
  const { user: me } = useAuthStore()
  const [messages, setMessages] = useState<PrivateMessage[]>([])
  const [targetUser, setTargetUser] = useState<UserBrief | null>(null)
  const [text, setText] = useState('')
  const [loading, setLoading] = useState(true)
  const bottomRef = useRef<HTMLDivElement>(null)

  useEffect(() => {
    if (!userId) return
    userApi.getUserById(Number(userId)).then((r) => {
      setTargetUser({ userId: r.data.data.userId, nickname: r.data.data.nickname, avatarUrl: r.data.data.avatarUrl, bio: '', gender: 0, verificationLevel: 0, level: 0, points: 0, isFollowed: false, createdAt: '' })
    })
    socialApi.getMessages(Number(userId)).then((r) => { setMessages(r.data.data || []); setLoading(false) })
    socialApi.markAllRead()
  }, [userId])

  useEffect(() => { bottomRef.current?.scrollIntoView({ behavior: 'smooth' }) }, [messages])

  const handleSend = async () => {
    if (!userId || !text.trim()) return
    await socialApi.sendMessage(Number(userId), text)
    setText('')
    const r = await socialApi.getMessages(Number(userId))
    setMessages(r.data.data || [])
  }

  return (
    <Card title={`与 ${targetUser?.nickname || '...'} 的私信`} style={{ maxWidth: 650, height: 'calc(100vh - 160px)', display: 'flex', flexDirection: 'column' }}
      bodyStyle={{ flex: 1, display: 'flex', flexDirection: 'column', overflow: 'hidden' }}>
      <div style={{ flex: 1, overflow: 'auto', paddingRight: 8 }}>
        <Spin spinning={loading}>
          {messages.map((m) => (
            <div key={m.messageId} style={{ display: 'flex', justifyContent: m.isMine ? 'flex-end' : 'flex-start', marginBottom: 12 }}>
              {!m.isMine && <Avatar src={targetUser?.avatarUrl} size={32} style={{ marginRight: 8 }} />}
              <div style={{ maxWidth: '70%', background: m.isMine ? '#1677ff' : '#f0f0f0', color: m.isMine ? '#fff' : '#333', padding: '8px 14px', borderRadius: 12 }}>
                <div>{m.content}</div>
                <div style={{ fontSize: 11, opacity: 0.7, textAlign: 'right' }}>{dayjs(m.sendTime).format('HH:mm')}</div>
              </div>
              {m.isMine && <Avatar src={me?.avatarUrl} size={32} style={{ marginLeft: 8 }} />}
            </div>
          ))}
          <div ref={bottomRef} />
        </Spin>
      </div>
      <div style={{ display: 'flex', gap: 8, marginTop: 12, paddingTop: 12, borderTop: '1px solid #f0f0f0' }}>
        <Input.TextArea value={text} onChange={(e) => setText(e.target.value)} placeholder="输入私信内容..."
          autoSize={{ minRows: 1, maxRows: 4 }} onPressEnter={(e) => { if (!e.shiftKey) { e.preventDefault(); handleSend() } }} />
        <Button type="primary" icon={<SendOutlined />} onClick={handleSend}>发送</Button>
      </div>
    </Card>
  )
}

export default ChatPage
