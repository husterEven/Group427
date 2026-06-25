import React, { useEffect, useState } from 'react'
import { Card, List, Tag, Button, Radio, message, Typography } from 'antd'
import { SafetyCertificateOutlined, IdcardOutlined, AuditOutlined } from '@ant-design/icons'
import { userApi } from '@/services/api'
import type { VerificationRecord } from '@/types'
import dayjs from 'dayjs'

const { Title, Text } = Typography

const VerificationPage: React.FC = () => {
  const [records, setRecords] = useState<VerificationRecord[]>([])
  const [type, setType] = useState(0)

  useEffect(() => {
    userApi.getVerification().then((r) => setRecords(r.data.data))
  }, [])

  const handleSubmit = async () => {
    await userApi.submitVerification(type)
    message.success('申请已提交，等待审核')
    const r = await userApi.getVerification()
    setRecords(r.data.data)
  }

  const typeLabel = (t: number) => ({ 0: '身份证', 1: '学生证', 2: '驾驶证' }[t] || '未知')
  const statusLabel = (s: number) => ({ 0: { text: '待审核', color: 'processing' }, 1: { text: '已通过', color: 'success' }, 2: { text: '已驳回', color: 'error' } }[s] || { text: '未知', color: 'default' })

  return (
    <Card title={<Title level={4}><SafetyCertificateOutlined /> 认证中心</Title>} style={{ maxWidth: 600 }}>
      <div style={{ marginBottom: 24, padding: 16, background: '#fafafa', borderRadius: 8 }}>
        <Radio.Group value={type} onChange={(e) => setType(e.target.value)}>
          <Radio.Button value={0}><IdcardOutlined /> 基础认证（身份证）</Radio.Button>
          <Radio.Button value={1}>学生证</Radio.Button>
          <Radio.Button value={2}>驾驶证</Radio.Button>
        </Radio.Group>
        <div style={{ marginTop: 12 }}>
          <Text type="secondary">提示：请准备清晰证件照片，审核需1-3个工作日。</Text>
        </div>
        <Button type="primary" onClick={handleSubmit} style={{ marginTop: 8 }}>提交认证申请</Button>
      </div>

      <List
        header={<Text strong>认证记录</Text>}
        dataSource={records}
        renderItem={(r) => (
          <List.Item>
            <List.Item.Meta
              avatar={<AuditOutlined style={{ fontSize: 24 }} />}
              title={`${typeLabel(r.verificationType)} · ${dayjs(r.createdAt).format('YYYY-MM-DD')}`}
              description={<Tag color={statusLabel(r.auditStatus).color}>{statusLabel(r.auditStatus).text}</Tag>}
            />
          </List.Item>
        )}
        locale={{ emptyText: '暂无认证记录' }}
      />
    </Card>
  )
}

export default VerificationPage
