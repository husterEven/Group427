import React, { useEffect, useState } from 'react'
import { Card, Radio, Button, Typography, message } from 'antd'
import { LockOutlined } from '@ant-design/icons'
import { userApi } from '@/services/api'

const { Title, Text } = Typography

const PrivacyPage: React.FC = () => {
  const [visibility, setVisibility] = useState(2)

  useEffect(() => {
    userApi.getPrivacy().then((r) => { if (r.data.data) setVisibility(r.data.data.profileVisibility) })
  }, [])

  const handleSave = async () => {
    await userApi.updatePrivacy({ profileVisibility: visibility as 0 | 1 | 2 })
    message.success('隐私设置已更新')
  }

  return (
    <Card title={<Title level={4}><LockOutlined /> 隐私设置</Title>} style={{ maxWidth: 600 }}>
      <div style={{ marginBottom: 24 }}>
        <Title level={5}>个人资料可见范围</Title>
        <Radio.Group value={visibility} onChange={(e) => setVisibility(e.target.value)}>
          <Radio value={2} style={{ display: 'block', marginBottom: 12 }}>公开 — 所有人可见</Radio>
          <Radio value={1} style={{ display: 'block', marginBottom: 12 }}>仅好友 — 互相关注的人可见</Radio>
          <Radio value={0} style={{ display: 'block', marginBottom: 12 }}>仅自己 — 完全隐藏个人资料</Radio>
        </Radio.Group>
        <Text type="secondary">此设置影响个人主页、发帖动态、关注粉丝列表的可见范围。</Text>
      </div>
      <Button type="primary" onClick={handleSave}>保存设置</Button>
    </Card>
  )
}

export default PrivacyPage
