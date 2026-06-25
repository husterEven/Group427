import React, { useEffect, useState } from 'react'
import { Card, Progress, Row, Col, Typography, Statistic } from 'antd'
import { TrophyOutlined, FileTextOutlined, StarFilled, FireOutlined } from '@ant-design/icons'
import { userApi } from '@/services/api'
import type { UserAchievement } from '@/types'

const { Title, Text } = Typography

const AchievementPage: React.FC = () => {
  const [ach, setAch] = useState<UserAchievement | null>(null)

  useEffect(() => {
    userApi.getAchievement().then((r) => setAch(r.data.data))
  }, [])

  const badges = [
    { name: '发帖新人', desc: '发布首篇帖子', earned: (ach?.totalPostCount || 0) >= 1, icon: <FileTextOutlined /> },
    { name: '发帖达人', desc: '累计发帖10篇', earned: (ach?.totalPostCount || 0) >= 10, icon: <FireOutlined /> },
    { name: '精华作者', desc: '获得1篇精华帖', earned: (ach?.essencePostCount || 0) >= 1, icon: <StarFilled /> },
    { name: '价值投资达人', desc: '获得5篇精华帖', earned: (ach?.essencePostCount || 0) >= 5, icon: <TrophyOutlined /> },
  ]

  return (
    <Card title={<Title level={4}><TrophyOutlined /> 成就系统</Title>} style={{ maxWidth: 700 }}>
      <Row gutter={24} style={{ marginBottom: 32 }}>
        <Col span={8}><Statistic title="累计发帖" value={ach?.totalPostCount || 0} prefix={<FileTextOutlined />} /></Col>
        <Col span={8}><Statistic title="精华帖" value={ach?.essencePostCount || 0} prefix={<StarFilled style={{ color: '#faad14' }} />} /></Col>
        <Col span={8}><Statistic title="影响力积分" value={(ach?.totalPostCount || 0) * 10 + (ach?.essencePostCount || 0) * 100} prefix={<FireOutlined />} /></Col>
      </Row>

      <Title level={5}>荣誉勋章</Title>
      <div style={{ display: 'flex', flexWrap: 'wrap', gap: 16 }}>
        {badges.map((b) => (
          <Card key={b.name} size="small" style={{ width: 200, opacity: b.earned ? 1 : 0.4 }}>
            <div style={{ textAlign: 'center', fontSize: 32 }}>{b.icon}</div>
            <div style={{ textAlign: 'center' }}><Text strong>{b.name}</Text></div>
            <div style={{ textAlign: 'center' }}><Text type="secondary" style={{ fontSize: 12 }}>{b.earned ? '已获得' : b.desc}</Text></div>
          </Card>
        ))}
      </div>
    </Card>
  )
}

export default AchievementPage
