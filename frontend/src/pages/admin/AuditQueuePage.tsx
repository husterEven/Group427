import React, { useEffect, useState } from 'react'
import { Card, Table, Tag, Button, Space, Modal, Select, Input, message } from 'antd'
import { CheckOutlined, CloseOutlined } from '@ant-design/icons'
import { adminApi } from '@/services/api'
import type { AuditItem } from '@/types'
import dayjs from 'dayjs'

const AuditQueuePage: React.FC = () => {
  const [data, setData] = useState<AuditItem[]>([])
  const [loading, setLoading] = useState(true)
  const [modalOpen, setModalOpen] = useState(false)
  const [current, setCurrent] = useState<AuditItem | null>(null)
  const [comment, setComment] = useState('')

  useEffect(() => {
    adminApi.getAuditQueue({ auditStatus: 0 }).then((r) => { setData(r.data.data.records); setLoading(false) })
  }, [])

  const handleAudit = async (status: 1 | 2) => {
    if (!current) return
    await adminApi.audit(current.auditItemId, { auditStatus: status, auditComment: comment })
    message.success(status === 1 ? '已通过' : '已驳回')
    setData((prev) => prev.filter((i) => i.auditItemId !== current.auditItemId))
    setModalOpen(false)
  }

  const columns = [
    { title: '审核项ID', dataIndex: 'auditItemId', width: 80 },
    { title: '类型', dataIndex: 'contentType', width: 80, render: (t: number) => ({ 0: '帖子', 1: '评论', 2: '附件' }[t]) },
    { title: '预览', dataIndex: 'preview', ellipsis: true },
    { title: '提交人', render: (_: unknown, r: AuditItem) => r.submitter?.nickname },
    { title: '提交时间', render: (_: unknown, r: AuditItem) => dayjs(r.createdAt).format('MM-DD HH:mm') },
    {
      title: '操作', width: 200,
      render: (_: unknown, r: AuditItem) => (
        <Space>
          <Button type="primary" size="small" icon={<CheckOutlined />}
            onClick={() => { setCurrent(r); setModalOpen(true) }}>审核</Button>
        </Space>
      ),
    },
  ]

  return (
    <div>
      <h2 className="page-title">内容审核</h2>
      <Card>
        <Table dataSource={data} columns={columns} loading={loading} rowKey="auditItemId" />
      </Card>
      <Modal title="审核处理" open={modalOpen} onCancel={() => setModalOpen(false)}
        footer={[
          <Button key="reject" danger icon={<CloseOutlined />} onClick={() => handleAudit(2)}>驳回</Button>,
          <Button key="approve" type="primary" icon={<CheckOutlined />} onClick={() => handleAudit(1)}>通过</Button>,
        ]}>
        <p>内容预览：{current?.preview}</p>
        <Input.TextArea placeholder="审核意见（选填）" value={comment} onChange={(e) => setComment(e.target.value)} rows={3} />
      </Modal>
    </div>
  )
}

export default AuditQueuePage
