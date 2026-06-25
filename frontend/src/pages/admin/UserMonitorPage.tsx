import React, { useState } from 'react'
import { Card, Table, Tag, Button, Space, Input, Typography } from 'antd'
import { userApi } from '@/services/api'
import type { FollowUser } from '@/types'

const { Text } = Typography

const UserMonitorPage: React.FC = () => {
  const [keyword, setKeyword] = useState('')
  const [users, setUsers] = useState<FollowUser[]>([])
  const [loading, setLoading] = useState(false)

  const handleSearch = async () => {
    if (!keyword) return
    setLoading(true)
    const r = await userApi.searchUsers(keyword)
    setUsers(r.data.data.records)
    setLoading(false)
  }

  // Mock behavior data
  const mockData = users.map((u) => ({
    ...u,
    postFreq: Math.floor(Math.random() * 20),
    commentFreq: Math.floor(Math.random() * 50),
    isAbnormal: Math.random() < 0.15,
  }))

  const columns = [
    { title: '用户', render: (_: unknown, r: typeof mockData[0]) => <Text strong>{r.nickname}</Text> },
    { title: '近7天发帖', render: (_: unknown, r: typeof mockData[0]) => <Text type={r.postFreq > 15 ? 'danger' : undefined}>{r.postFreq}</Text> },
    { title: '近7天评论', render: (_: unknown, r: typeof mockData[0]) => <Text type={r.commentFreq > 40 ? 'danger' : undefined}>{r.commentFreq}</Text> },
    {
      title: '状态',
      render: (_: unknown, r: typeof mockData[0]) => r.isAbnormal
        ? <Tag color="red">异常高活跃</Tag>
        : <Tag color="green">正常</Tag>,
    },
    {
      title: '操作',
      render: () => (
        <Space>
          <Button size="small">查看详情</Button>
          <Button size="small" danger>限制</Button>
        </Space>
      ),
    },
  ]

  return (
    <div>
      <h2 className="page-title">用户监控</h2>
      <Card>
        <div style={{ marginBottom: 16, display: 'flex', gap: 12 }}>
          <Input.Search placeholder="搜索用户" value={keyword} onChange={(e) => setKeyword(e.target.value)} onSearch={handleSearch} style={{ maxWidth: 300 }} />
        </div>
        <Table dataSource={users.length > 0 ? mockData : []} columns={columns} loading={loading} rowKey="userId" locale={{ emptyText: '请搜索用户' }} />
      </Card>
    </div>
  )
}

export default UserMonitorPage
