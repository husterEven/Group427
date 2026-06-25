import React, { useEffect, useState } from 'react'
import { useNavigate, useParams } from 'react-router-dom'
import { Tabs, Card, List, Tag, Space, Avatar, Typography, Button, Spin, Empty } from 'antd'
import { LikeOutlined, EyeOutlined, MessageOutlined, FireOutlined } from '@ant-design/icons'
import { postApi } from '@/services/api'
import type { Post } from '@/types'
import dayjs from 'dayjs'

const { Text, Paragraph } = Typography

const HomePage: React.FC = () => {
  const navigate = useNavigate()
  const { sectionId, zoneId } = useParams()
  const [posts, setPosts] = useState<Post[]>([])
  const [loading, setLoading] = useState(true)
  const [activeTab, setActiveTab] = useState('latest')

  const fetchPosts = async (sort = 'latest') => {
    setLoading(true)
    try {
      const params: Record<string, unknown> = { sort }
      if (sectionId) params.sectionId = Number(sectionId)
      if (zoneId) params.zoneId = Number(zoneId)
      const res = await postApi.getList(params)
      setPosts(res.data.data.records)
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    fetchPosts(activeTab)
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [sectionId, zoneId, activeTab])

  const tabItems = [
    { key: 'latest', label: '最新' },
    { key: 'hot', label: <span><FireOutlined /> 最热</span> },
    { key: 'popular', label: '最受欢迎' },
  ]

  return (
    <div style={{ maxWidth: 800 }}>
      <Tabs activeKey={activeTab} onChange={(k) => { setActiveTab(k); fetchPosts(k) }} items={tabItems}>
      </Tabs>
      <Spin spinning={loading}>
        {posts.length === 0 && !loading ? (
          <Empty description="暂无帖子，快来发布第一篇吧" />
        ) : (
          <List
            dataSource={posts}
            renderItem={(post) => (
              <Card
                hoverable
                style={{ marginBottom: 12 }}
                onClick={() => navigate(`/post/${post.postId}`)}
              >
                <Space align="start" style={{ width: '100%' }}>
                  <Avatar src={post.author.avatarUrl} onClick={(e) => { e.stopPropagation(); navigate(`/profile/${post.author.userId}`) }} />
                  <div style={{ flex: 1 }}>
                    <Space>
                      <Text strong style={{ fontSize: 16 }}>{post.title}</Text>
                      {post.isPinned ? <Tag color="red">置顶</Tag> : null}
                      {post.isEssence ? <Tag color="gold">精华</Tag> : null}
                      {post.contentType === 1 ? <Tag color="blue">投票</Tag> : null}
                      {post.contentType === 2 ? <Tag color="purple">图文</Tag> : null}
                    </Space>
                    <Paragraph ellipsis={{ rows: 2 }} style={{ color: '#666', marginTop: 4 }}>
                      {post.content.replace(/<[^>]*>/g, '').substring(0, 200)}
                    </Paragraph>
                    <Space style={{ marginTop: 8 }} size={16}>
                      <Text type="secondary">{post.author.nickname}</Text>
                      <Text type="secondary">{post.sectionName && `${post.sectionName}${post.zoneName ? ` · ${post.zoneName}` : ''}`}</Text>
                      <Text type="secondary">{dayjs(post.publishTime).fromNow()}</Text>
                      <span><EyeOutlined /> {post.viewCount}</span>
                      <span><LikeOutlined /> {post.likeCount}</span>
                      <span><MessageOutlined /> {post.commentCount}</span>
                    </Space>
                  </div>
                </Space>
              </Card>
            )}
          />
        )}
      </Spin>
    </div>
  )
}

export default HomePage
