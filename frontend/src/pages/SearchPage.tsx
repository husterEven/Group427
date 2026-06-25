import React, { useEffect, useState } from 'react'
import { useSearchParams, useNavigate } from 'react-router-dom'
import { Card, Tabs, List, Avatar, Typography, Input, Space, Tag, Select } from 'antd'
import { LikeOutlined, EyeOutlined, MessageOutlined } from '@ant-design/icons'
import { userApi, postApi } from '@/services/api'
import type { Post, FollowUser } from '@/types'
import dayjs from 'dayjs'

const { Text } = Typography

const SearchPage: React.FC = () => {
  const [params] = useSearchParams()
  const keyword = params.get('keyword') || ''
  const navigate = useNavigate()
  const [posts, setPosts] = useState<Post[]>([])
  const [users, setUsers] = useState<FollowUser[]>([])
  const [sort, setSort] = useState('latest')

  useEffect(() => {
    if (!keyword) return
    postApi.getList({ keyword, sort }).then((r) => setPosts(r.data.data.records || []))
    userApi.searchUsers(keyword).then((r) => setUsers(r.data.data || []))
  }, [keyword, sort])

  return (
    <div style={{ maxWidth: 800 }}>
      <Input.Search defaultValue={keyword} placeholder="搜索关键词、用户、股票代码..." size="large"
        onSearch={(v) => navigate(`/search?keyword=${encodeURIComponent(v)}`)} style={{ marginBottom: 24 }} />
      <Card>
        <Tabs items={[
          {
            key: 'posts', label: `相关帖子 (${posts?.length || 0})`,
            children: (
              <div>
                <Select value={sort} onChange={setSort} options={[
                  { value: 'latest', label: '最新' }, { value: 'hot', label: '最热' },
                ]} style={{ width: 120, marginBottom: 16 }} />
                {posts.map((p) => (
                  <div key={p.postId} className="post-card" onClick={() => navigate(`/post/${p.postId}`)}>
                    <Space>
                      <Text strong style={{ fontSize: 16 }}>{p.title}</Text>
                      {p.isPinned ? <Tag color="red">置顶</Tag> : null}
                      {p.isEssence ? <Tag color="gold">精华</Tag> : null}
                    </Space>
                    <div><Text type="secondary" ellipsis>{p.content.replace(/<[^>]*>/g, '').substring(0, 150)}</Text></div>
                    <Space size={16} style={{ marginTop: 4 }}>
                      <Text type="secondary">{p.author.nickname}</Text>
                      <Text type="secondary">{dayjs(p.publishTime).fromNow()}</Text>
                      <span><EyeOutlined /> {p.viewCount}</span>
                      <span><LikeOutlined /> {p.likeCount}</span>
                      <span><MessageOutlined /> {p.commentCount}</span>
                    </Space>
                  </div>
                ))}
              </div>
            ),
          },
          {
            key: 'users', label: `相关用户 (${users?.length || 0})`,
            children: users.map((u) => (
              <div key={u.userId} className="user-card" onClick={() => navigate(`/profile/${u.userId}`)}>
                <Avatar src={u.avatarUrl} size={40} />
                <div style={{ marginLeft: 12 }}>
                  <Text strong>{u.nickname}</Text>
                  <div><Text type="secondary">{u.bio}</Text></div>
                </div>
              </div>
            )),
          },
        ]} />
      </Card>
    </div>
  )
}

export default SearchPage
