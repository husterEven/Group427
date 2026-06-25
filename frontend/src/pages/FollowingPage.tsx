import React, { useEffect, useState } from 'react'
import { useParams } from 'react-router-dom'
import { Card, List, Avatar, Button, Space, Tabs, Typography } from 'antd'
import { socialApi } from '@/services/api'
import { useAuthStore } from '@/store'
import type { FollowUser } from '@/types'

const { Text } = Typography

const FollowingPage: React.FC = () => {
  const { userId } = useParams()
  const { user: me } = useAuthStore()
  const [following, setFollowing] = useState<FollowUser[]>([])
  const [followers, setFollowers] = useState<FollowUser[]>([])
  const targetId = userId ? Number(userId) : (me?.userId || 0)

  useEffect(() => {
    socialApi.getFollowing(targetId).then((r) => setFollowing(r.data.data || []))
    socialApi.getFollowers(targetId).then((r) => setFollowers(r.data.data || []))
  }, [targetId])

  const renderList = (users: FollowUser[], type: 'following' | 'followers') => (
    <List
      dataSource={users}
      renderItem={(u) => (
        <List.Item>
          <List.Item.Meta
            avatar={<Avatar src={u.avatarUrl} />}
            title={<Space>{u.nickname}{u.isStarred && <Text type="warning">★ 星标</Text>}{u.isMutual && <Text type="secondary">互相关注</Text>}</Space>}
            description={u.bio}
          />
          <Space>
            {type === 'following' && (
              <Button size="small" onClick={() => socialApi.setStar(u.userId, !u.isStarred).then(() => {
                setFollowing((prev) => prev.map((f) => f.userId === u.userId ? { ...f, isStarred: !f.isStarred } : f))
              })}>
                {u.isStarred ? '取消星标' : '设为星标'}
              </Button>
            )}
            <Button size="small" onClick={() => socialApi.toggleFollow(u.userId).then(() => {
              if (type === 'following') setFollowing((p) => p.filter((f) => f.userId !== u.userId))
            })}>取消关注</Button>
          </Space>
        </List.Item>
      )}
      locale={{ emptyText: '暂无数据' }}
    />
  )

  return (
    <Card style={{ maxWidth: 700 }}>
      <Tabs items={[
        { key: 'following', label: `我关注的 (${following.length})`, children: renderList(following, 'following') },
        { key: 'followers', label: `关注我的 (${followers.length})`, children: renderList(followers, 'followers') },
      ]} />
    </Card>
  )
}

export default FollowingPage
