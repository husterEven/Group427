import React, { useEffect, useState } from 'react'
import { useParams } from 'react-router-dom'
import { Card, Button, Avatar, Typography, Space, Tabs, Tag, Input, List, message, Modal } from 'antd'
import { TeamOutlined, SendOutlined, UserAddOutlined } from '@ant-design/icons'
import { socialApi } from '@/services/api'
import { useAuthStore } from '@/store'
import type { Group, GroupMember, GroupPost, FollowUser } from '@/types'
import dayjs from 'dayjs'

const { Title, Text } = Typography

const GroupDetailPage: React.FC = () => {
  const { groupId } = useParams<{ groupId: string }>()
  const { user: me } = useAuthStore()
  const [group, setGroup] = useState<Group | null>(null)
  const [members, setMembers] = useState<GroupMember[]>([])
  const [posts, setPosts] = useState<GroupPost[]>([])
  const [newPost, setNewPost] = useState('')
  const [inviteOpen, setInviteOpen] = useState(false)
  const [following, setFollowing] = useState<FollowUser[]>([])

  useEffect(() => {
    if (!groupId) return
    socialApi.getGroupDetail(Number(groupId)).then((r) => setGroup(r.data.data))
    socialApi.getMembers(Number(groupId)).then((r) => setMembers(r.data.data || []))
    socialApi.getGroupPosts(Number(groupId)).then((r) => setPosts(r.data.data || []))
  }, [groupId])

  const handleJoin = async () => { await socialApi.joinGroup(Number(groupId)); message.success('已加入'); const r = await socialApi.getGroupDetail(Number(groupId)); setGroup(r.data.data) }
  const handleLeave = async () => { await socialApi.leaveGroup(Number(groupId)); message.success('已退出'); setGroup((g) => g ? { ...g, myRole: null } : null) }

  const handleInviteOpen = async () => {
    setInviteOpen(true)
    const r = await socialApi.getFollowing(me!.userId)
    const memberIds = new Set(members.map((m) => m.userId))
    setFollowing((r.data.data || []).filter((f: FollowUser) => !memberIds.has(f.userId)))
  }

  const handleInvite = async (userId: number) => {
    await socialApi.inviteMember(Number(groupId), userId)
    message.success('邀请成功')
    setFollowing((prev) => prev.filter((f) => f.userId !== userId))
    const r = await socialApi.getMembers(Number(groupId))
    setMembers(r.data.data || [])
  }

  const handlePost = async () => {
    if (!newPost.trim() || !groupId) return
    const r = await socialApi.createGroupPost(Number(groupId), newPost)
    setPosts([r.data.data, ...posts])
    setNewPost('')
  }

  if (!group) return null

  return (
    <div style={{ maxWidth: 700 }}>
      <Card>
        <Space>
          <Avatar icon={<TeamOutlined />} size={48} />
          <div>
            <Title level={4} style={{ margin: 0 }}>{group.groupName}</Title>
            <Text type="secondary">{group.memberCount} 成员 · 创建于 {dayjs(group.createdAt).format('YYYY-MM-DD')}</Text>
          </div>
        </Space>
        <div style={{ marginTop: 16 }}>
          {group.myRole === null ? (
            <Button type="primary" onClick={handleJoin}>加入群组</Button>
          ) : (
            <Space>
              {group.myRole === 2 && <Tag color="gold">群主</Tag>}
              {group.myRole === 1 && <Tag color="blue">管理员</Tag>}
              {group.myRole !== null && group.myRole >= 1 && (
                <Button icon={<UserAddOutlined />} onClick={handleInviteOpen}>邀请好友</Button>
              )}
              <Button danger onClick={handleLeave}>退出群组</Button>
            </Space>
          )}
        </div>
      </Card>

      <Card style={{ marginTop: 16 }}>
        <Tabs items={[
          {
            key: 'posts', label: '群帖子',
            children: (
              <div>
                {group.myRole !== null && (
                  <div style={{ display: 'flex', gap: 8, marginBottom: 16 }}>
                    <Input.TextArea value={newPost} onChange={(e) => setNewPost(e.target.value)} placeholder="在群内发帖..." autoSize={{ minRows: 2 }} />
                    <Button type="primary" icon={<SendOutlined />} onClick={handlePost}>发布</Button>
                  </div>
                )}
                {posts.map((p) => (
                  <div key={p.groupPostId} style={{ padding: '12px 0', borderBottom: '1px solid #f5f5f5' }}>
                    <Space><Avatar src={p.author.avatarUrl} /><Text strong>{p.author.nickname}</Text><Text type="secondary">{dayjs(p.publishTime).fromNow()}</Text></Space>
                    <div style={{ marginTop: 8 }}><Text>{p.content}</Text></div>
                  </div>
                ))}
              </div>
            ),
          },
          {
            key: 'members', label: `成员 (${members.length})`,
            children: members.map((m) => (
              <div key={m.userId} style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', padding: '8px 0' }}>
                <Space><Avatar>{m.nickname?.[0]}</Avatar><Text>{m.nickname}</Text>{m.role === 2 && <Tag color="gold">群主</Tag>}{m.role === 1 && <Tag color="blue">管理</Tag>}</Space>
                {group.myRole === 2 && m.role !== 2 && (
                  <Space>
                    <Button size="small" onClick={() => socialApi.setRole(Number(groupId), m.userId, m.role === 1 ? 0 : 1).then(() => {
                      setMembers((prev) => prev.map((mem) => mem.userId === m.userId ? { ...mem, role: mem.role === 1 ? 0 as const : 1 as const } : mem))
                    })}>{m.role === 1 ? '取消管理' : '设为管理'}</Button>
                    <Button size="small" danger onClick={() => socialApi.kickMember(Number(groupId), m.userId).then(() => {
                      setMembers((prev) => prev.filter((mem) => mem.userId !== m.userId))
                    })}>踢出</Button>
                  </Space>
                )}
              </div>
            )),
          },
        ]} />
      </Card>

      <Modal title="邀请好友加入群组" open={inviteOpen} onCancel={() => setInviteOpen(false)} footer={null}>
        {following.length === 0 ? (
          <Text type="secondary">没有可邀请的好友</Text>
        ) : (
          following.map((f) => (
            <div key={f.userId} style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', padding: '8px 0' }}>
              <Space><Avatar src={f.avatarUrl}>{f.nickname?.[0]}</Avatar><Text>{f.nickname}</Text></Space>
              <Button size="small" type="primary" onClick={() => handleInvite(f.userId)}>邀请</Button>
            </div>
          ))
        )}
      </Modal>
    </div>
  )
}

export default GroupDetailPage
