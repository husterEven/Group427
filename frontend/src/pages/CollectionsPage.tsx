import React, { useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { Card, List, Avatar, Typography, Spin, Empty, Space, Tag } from 'antd'
import { StarFilled, EyeOutlined, LikeOutlined } from '@ant-design/icons'
import { postApi } from '@/services/api'
import type { Post } from '@/types'
import dayjs from 'dayjs'

const { Text } = Typography

const CollectionsPage: React.FC = () => {
  const navigate = useNavigate()
  const [list, setList] = useState<Post[]>([])
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    postApi.getCollections().then((r) => { setList(r.data.data.records); setLoading(false) })
  }, [])

  return (
    <div style={{ maxWidth: 700 }}>
      <Card title={<span><StarFilled style={{ color: '#faad14' }} /> 我的收藏</span>}>
        <Spin spinning={loading}>
          {list.length === 0 && !loading ? <Empty description="还没有收藏任何帖子" /> : (
            list.map((p) => (
              <div key={p.postId} className="post-card" onClick={() => navigate(`/post/${p.postId}`)}>
                <Space><Text strong style={{ fontSize: 15 }}>{p.title}</Text></Space>
                <div><Text type="secondary">{p.author.nickname} · {dayjs(p.publishTime).fromNow()} · <EyeOutlined /> {p.viewCount} · <LikeOutlined /> {p.likeCount}</Text></div>
              </div>
            ))
          )}
        </Spin>
      </Card>
    </div>
  )
}

export default CollectionsPage
