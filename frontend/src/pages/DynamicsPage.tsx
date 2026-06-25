import React, { useEffect, useState } from 'react'
import { Card, Select, Button, Input, List, Avatar, Typography, Spin } from 'antd'
import { SendOutlined, LikeOutlined } from '@ant-design/icons'
import { dynamicApi } from '@/services/api'
import type { Dynamic } from '@/types'
import dayjs from 'dayjs'

const { Text } = Typography

const DynamicsPage: React.FC = () => {
  const [feed, setFeed] = useState('following')
  const [dynamics, setDynamics] = useState<Dynamic[]>([])
  const [text, setText] = useState('')
  const [loading, setLoading] = useState(true)

  const fetch = async (f = feed) => {
    const r = await dynamicApi.getFeed(f)
    setDynamics(r.data.data.records)
    setLoading(false)
  }

  useEffect(() => { setLoading(true); fetch() }, [feed])

  const handlePost = async () => {
    if (!text.trim()) return
    await dynamicApi.create(text)
    setText('')
    fetch()
  }

  return (
    <div style={{ maxWidth: 650 }}>
      <Card>
        <div style={{ display: 'flex', gap: 8, marginBottom: 16 }}>
          <Input.TextArea value={text} onChange={(e) => setText(e.target.value)} placeholder="分享你的盘中观点..."
            autoSize={{ minRows: 2 }} maxLength={2000} showCount />
          <Button type="primary" icon={<SendOutlined />} onClick={handlePost}>发布</Button>
        </div>
        <Select value={feed} onChange={setFeed} options={[
          { value: 'following', label: '关注动态' },
          { value: 'hot', label: '热门动态' },
        ]} style={{ width: 140 }} />
      </Card>

      <Spin spinning={loading}>
        <List
          style={{ marginTop: 16 }}
          dataSource={dynamics}
          renderItem={(d) => (
            <Card size="small" style={{ marginBottom: 8 }}>
              <List.Item.Meta
                avatar={<Avatar src={d.author.avatarUrl} />}
                title={<Text strong>{d.author.nickname}</Text>}
                description={
                  <div>
                    <Text>{d.content}</Text>
                    <div><Text type="secondary">{dayjs(d.publishTime).fromNow()}</Text> · <LikeOutlined /> {d.likeCount}</div>
                  </div>
                }
              />
            </Card>
          )}
        />
      </Spin>
    </div>
  )
}

export default DynamicsPage
