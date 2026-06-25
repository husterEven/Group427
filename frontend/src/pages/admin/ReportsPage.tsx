import React, { useEffect, useState } from 'react'
import { Card, Table, Tag, Button, Space, Select, message, Typography } from 'antd'
import { adminApi } from '@/services/api'
import type { ReportItem } from '@/types'
import dayjs from 'dayjs'

const { Text } = Typography

const ReportsPage: React.FC = () => {
  const [data, setData] = useState<ReportItem[]>([])
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    adminApi.getReports({}).then((r) => { setData(r.data.data.records); setLoading(false) })
  }, [])

  const handleAction = async (reportId: number, status: number, result: number, punishmentType?: number) => {
    await adminApi.handleReport(reportId, { status, handleResult: result, punishmentType, durationDays: punishmentType === 2 ? 0 : 7 })
    message.success('处理完成')
    setData((prev) => prev.filter((r) => r.reportId !== reportId))
  }

  const columns = [
    { title: '举报人', render: (_: unknown, r: ReportItem) => r.reporter?.nickname },
    { title: '目标类型', render: (_: unknown, r: ReportItem) => ({ 0: '帖子', 1: '评论', 2: '用户', 3: '私信' }[r.targetType]) },
    { title: '原因', dataIndex: 'reason', ellipsis: true },
    { title: '状态', dataIndex: 'status', render: (s: number) => ({ 0: <Tag>待处理</Tag>, 1: <Tag color="green">已处理</Tag>, 2: <Tag color="red">驳回</Tag> }[s]) },
    { title: '时间', render: (_: unknown, r: ReportItem) => dayjs(r.createdAt).format('MM-DD HH:mm') },
    {
      title: '操作', width: 280,
      render: (_: unknown, r: ReportItem) => r.status === 0 ? (
        <Space>
          <Button size="small" onClick={() => handleAction(r.reportId, 2, 0)}>驳回举报</Button>
          <Button size="small" onClick={() => handleAction(r.reportId, 1, 1, 0)}>警告用户</Button>
          <Button size="small" danger type="primary" onClick={() => handleAction(r.reportId, 1, 3, 1)}>删帖禁言</Button>
        </Space>
      ) : null,
    },
  ]

  return (
    <div>
      <h2 className="page-title">举报处理</h2>
      <Card><Table dataSource={data} columns={columns} loading={loading} rowKey="reportId" /></Card>
    </div>
  )
}

export default ReportsPage
