import React, { useEffect, useState } from 'react'
import { Card, Table, Tag, Button, message } from 'antd'
import { UndoOutlined } from '@ant-design/icons'
import { adminApi } from '@/services/api'
import type { Punishment } from '@/types'
import dayjs from 'dayjs'

const PunishmentsPage: React.FC = () => {
  const [data, setData] = useState<Punishment[]>([])
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    adminApi.getPunishments({}).then((r) => { setData(r.data.data.records); setLoading(false) })
  }, [])

  const handleRevoke = async (id: number) => {
    await adminApi.revokePunishment(id)
    message.success('已撤销')
    setData((prev) => prev.map((p) => p.punishmentId === id ? { ...p, isActive: false } : p))
  }

  const columns = [
    { title: '处罚用户', render: (_: unknown, r: Punishment) => r.user?.nickname },
    { title: '类型', dataIndex: 'punishmentType', render: (t: number) => ({ 0: '警告', 1: '禁言', 2: '封号' }[t]) },
    { title: '原因', dataIndex: 'reason', ellipsis: true },
    { title: '操作人', render: (_: unknown, r: Punishment) => r.operator?.nickname },
    { title: '时长', render: (_: unknown, r: Punishment) => r.durationDays === 0 ? '永久' : `${r.durationDays}天` },
    { title: '状态', render: (_: unknown, r: Punishment) => r.isActive ? <Tag color="red">生效中</Tag> : <Tag>已撤销</Tag> },
    { title: '时间', render: (_: unknown, r: Punishment) => dayjs(r.createdAt).format('MM-DD HH:mm') },
    {
      title: '操作',
      render: (_: unknown, r: Punishment) => r.isActive ? (
        <Button size="small" icon={<UndoOutlined />} onClick={() => handleRevoke(r.punishmentId)}>撤销</Button>
      ) : null,
    },
  ]

  return (
    <div>
      <h2 className="page-title">处罚管理</h2>
      <Card><Table dataSource={data} columns={columns} loading={loading} rowKey="punishmentId" /></Card>
    </div>
  )
}

export default PunishmentsPage
