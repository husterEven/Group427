import React, { useEffect, useState } from 'react'
import { useParams, useNavigate } from 'react-router-dom'
import { Card, Avatar, Descriptions, Tag, Button, Space, Tabs, List, Typography, Progress, Divider, message } from 'antd'
import { UserOutlined, TrophyOutlined, StarFilled, SafetyCertificateOutlined } from '@ant-design/icons'
import { userApi, socialApi, postApi, dynamicApi } from '@/services/api'
import { useAuthStore } from '@/store'
import type { UserDetail, Post, Dynamic } from '@/types'
import dayjs from 'dayjs'

const { Title, Text } = Typography

const ProfilePage: React.FC = () => {
  const { userId } = useParams<{ userId: string }>()
  const navigate = useNavigate()
  const { user: me } = useAuthStore()
  const [profile, setProfile] = useState<UserDetail | null>(null)
  const [posts, setPosts] = useState<Post[]>([])
  const [dynamics, setDynamics] = useState<Dynamic[]>([])
  const [isFollowing, setIsFollowing] = useState(false)

  const isMe = me?.userId === Number(userId)

  useEffect(() => {
    if (!userId) return
    userApi.getUserById(Number(userId)).then((r) => {
      setProfile(r.data.data || null)
      setIsFollowing(r.data.data?.isFollowed || false)
    }).catch(() => {})
    postApi.getList({ authorId: Number(userId) }).then((r) => {
      setPosts(r.data.data?.records || [])
    }).catch(() => {})
    dynamicApi.getByUser(Number(userId)).then((r) => {
      setDynamics(r.data.data?.records || [])
    }).catch(() => {})
  }, [userId])

  const handleFollow = async () => {
    if (!userId) return
    await socialApi.toggleFollow(Number(userId))
    setIsFollowing(!isFollowing)
    message.success(isFollowing ? '已取消关注' : '已关注')
  }

  if (!profile) return null

  return (
    <div style={{ maxWidth: 800 }}>
      <Card>
        <div style={{ display: 'flex', gap: 24, alignItems: 'flex-start' }}>
          <Avatar size={80} src={profile.avatarUrl} icon={<UserOutlined />} />
          <div style={{ flex: 1 }}>
            <Space>
              <Title level={3} style={{ margin: 0 }}>{profile.nickname}</Title>
              {profile.verificationLevel >= 3 && <Tag color="gold">专业认证 V</Tag>}
              {profile.verificationLevel >= 2 && <SafetyCertificateOutlined style={{ color: '#52c41a' }} />}
            </Space>
            <div style={{ marginTop: 8 }}><Text type="secondary">{profile.bio || '这个人很懒，还没有填写简介'}</Text></div>
            <Space size={24} style={{ marginTop: 12 }}>
              {!isMe && (
                <Button type={isFollowing ? 'default' : 'primary'} onClick={handleFollow}>
                  {isFollowing ? '已关注' : '+ 关注'}
                </Button>
              )}
              {!isMe && <Button onClick={() => navigate(`/messages/${userId}`)}>发私信</Button>}
              {isMe && <Button onClick={() => navigate('/settings/profile')}>编辑资料</Button>}
            </Space>
          </div>
        </div>
        <Divider />
        <Descriptions column={4} size="small">
          <Descriptions.Item label="发帖">{profile.achievement?.totalPostCount || 0}</Descriptions.Item>
          <Descriptions.Item label="精华">{profile.achievement?.essencePostCount || 0}</Descriptions.Item>
          <Descriptions.Item label="粉丝">{profile.followerCount}</Descriptions.Item>
          <Descriptions.Item label="关注">{profile.followingCount}</Descriptions.Item>
        </Descriptions>
        <Descriptions column={2} size="small" style={{ marginTop: 12 }}>
          <Descriptions.Item label="等级">Lv.{profile.level}</Descriptions.Item>
          <Descriptions.Item label="积分">{profile.points}</Descriptions.Item>
        </Descriptions>
      </Card>

      <Card style={{ marginTop: 16 }}>
        <Tabs items={[
          {
            key: 'posts', label: `帖子 (${posts.length})`,
            children: posts.map((p) => (
              <div key={p.postId} className="post-card" onClick={() => navigate(`/post/${p.postId}`)}>
                <Text strong>{p.title}</Text>
                <div><Text type="secondary">{dayjs(p.publishTime).fromNow()} · {p.likeCount} 赞 · {p.commentCount} 评论</Text></div>
              </div>
            )),
          },
          {
            key: 'dynamics', label: `动态 (${dynamics.length})`,
            children: dynamics.map((d) => (
              <div key={d.dynamicId} style={{ padding: '12px 0', borderBottom: '1px solid #f5f5f5' }}>
                <Text>{d.content}</Text>
                <div><Text type="secondary">{dayjs(d.publishTime).fromNow()} · {d.likeCount} 赞</Text></div>
              </div>
            )),
          },
          {
            key: 'achievements', label: '成就',
            children: (
              <div style={{ display: 'flex', flexWrap: 'wrap', gap: 16 }}>
                <TrophyOutlined style={{ fontSize: 32, color: '#faad14' }} />
                <div>
                  <Text strong>发帖达人</Text>
                  <div><Text type="secondary">发布超过10篇帖子获得</Text></div>
                </div>
              </div>
            ),
          },
        ]} />
      </Card>
    </div>
  )
}

export default ProfilePage
