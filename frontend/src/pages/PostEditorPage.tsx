import React, { useState, useEffect } from 'react'
import { useNavigate, useParams } from 'react-router-dom'
import { Card, Input, Select, Button, Upload, message, Tabs, DatePicker } from 'antd'
import { PlusOutlined, UploadOutlined } from '@ant-design/icons'
import { postApi, sectionApi, voteApi } from '@/services/api'
import { useAppStore } from '@/store'
import type { Post, Section, Zone } from '@/types'
import dayjs from 'dayjs'

const { TextArea } = Input

const PostEditorPage: React.FC = () => {
  const navigate = useNavigate()
  const { postId } = useParams()
  const { sections } = useAppStore()
  const [title, setTitle] = useState('')
  const [content, setContent] = useState('')
  const [contentType, setContentType] = useState(0)
  const [sectionId, setSectionId] = useState<number | undefined>()
  const [zoneId, setZoneId] = useState<number | undefined>()
  const [zones, setZones] = useState<Zone[]>([])
  const [submitting, setSubmitting] = useState(false)
  const [mode, setMode] = useState<'normal' | 'long' | 'vote'>('normal')
  const [voteTitle, setVoteTitle] = useState('')
  const [voteOptions, setVoteOptions] = useState<string[]>(['', ''])
  const [voteEndTime, setVoteEndTime] = useState('')

  useEffect(() => {
    if (postId) {
      postApi.getDetail(Number(postId)).then((res) => {
        const p: Post = res.data.data
        setTitle(p.title)
        setContent(p.content)
        setSectionId(p.sectionId)
        setZoneId(p.zoneId)
        setContentType(p.contentType)
      })
    }
  }, [postId])

  useEffect(() => {
    if (sectionId) {
      const sec = sections.find((s) => s.sectionId === sectionId)
      if (sec) setZones(sec.zones)
    }
  }, [sectionId, sections])

  const handleSectionChange = (id: number) => {
    setSectionId(id)
    setZoneId(undefined)
    const sec = sections.find((s) => s.sectionId === id)
    if (sec) setZones(sec.zones)
  }

  const handleSubmit = async () => {
    if (!title.trim()) { message.warning('请输入标题'); return }
    if (!sectionId) { message.warning('请选择板块'); return }
    if (mode === 'vote') {
      const validOptions = voteOptions.filter((o) => o.trim())
      if (!voteTitle.trim()) { message.warning('请输入投票标题'); return }
      if (validOptions.length < 2) { message.warning('请至少填写2个投票选项'); return }
    }
    setSubmitting(true)
    try {
      const finalContentType = mode === 'vote' ? 1 : mode === 'long' ? 2 : 0
      const data = { title, content, contentType: finalContentType, sectionId, zoneId }
      if (postId) {
        await postApi.update(Number(postId), data)
        message.success('编辑成功')
      } else {
        const res = await postApi.create(data)
        const newPostId = res.data.data.postId
        if (mode === 'vote') {
          const validOptions = voteOptions.filter((o) => o.trim())
          await voteApi.create(newPostId, {
            voteTitle: voteTitle.trim(),
            options: validOptions,
            endTime: voteEndTime || '',
          })
        }
        message.success('发布成功')
        navigate(`/post/${newPostId}`)
        return
      }
      navigate(-1)
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <div style={{ maxWidth: 800 }}>
      <Card>
        <Tabs activeKey={mode} onChange={(k) => setMode(k as typeof mode)} items={[
          { key: 'normal', label: '普通发帖' },
          { key: 'long', label: '长文分析' },
          { key: 'vote', label: '投票调研' },
        ]} />
        <Input
          size="large"
          placeholder="输入标题（最多200字）"
          value={title}
          onChange={(e) => setTitle(e.target.value)}
          maxLength={200}
          style={{ marginBottom: 16 }}
        />
        <div style={{ display: 'flex', gap: 12, marginBottom: 16 }}>
          <Select
            placeholder="选择板块"
            value={sectionId}
            onChange={handleSectionChange}
            style={{ width: 180 }}
            options={sections.map((s) => ({ value: s.sectionId, label: s.sectionName }))}
          />
          <Select
            placeholder="选择分区（可选）"
            value={zoneId}
            onChange={setZoneId}
            allowClear
            style={{ width: 180 }}
            options={zones.map((z) => ({ value: z.zoneId, label: z.zoneName }))}
          />
        </div>

        {mode === 'vote' ? (
          <div style={{ marginBottom: 16 }}>
            <Input placeholder="投票标题" value={voteTitle} onChange={(e) => setVoteTitle(e.target.value)}
              style={{ marginBottom: 8 }} />
            {voteOptions.map((opt, idx) => (
              <Input key={idx} placeholder={`选项 ${idx + 1}`} value={opt}
                onChange={(e) => { const arr = [...voteOptions]; arr[idx] = e.target.value; setVoteOptions(arr) }}
                style={{ marginBottom: 4 }} />
            ))}
            <Button type="dashed" onClick={() => setVoteOptions([...voteOptions, ''])} block icon={<PlusOutlined />}
              disabled={voteOptions.length >= 10}>
              添加选项
            </Button>
            <DatePicker
              showTime
              placeholder="截止时间（可选）"
              onChange={(d) => setVoteEndTime(d ? d.format('YYYY-MM-DDTHH:mm:ss') : '')}
              style={{ marginTop: 8, width: '100%' }}
            />
          </div>
        ) : (
          <TextArea
            rows={mode === 'long' ? 20 : 10}
            placeholder={mode === 'long' ? '使用富文本编写长文分析，支持标题、引用、列表、表格...' : '输入正文内容，支持基础格式'}
            value={content}
            onChange={(e) => setContent(e.target.value)}
            style={{ marginBottom: 16, fontSize: mode === 'long' ? 15 : 14 }}
          />
        )}

        <Upload listType="picture" maxCount={9} action="/api/v1/attachments/upload">
          <Button icon={<UploadOutlined />}>上传图片/附件</Button>
        </Upload>

        <div style={{ marginTop: 20 }}>
          <Button type="primary" onClick={handleSubmit} loading={submitting} size="large" style={{ marginRight: 12 }}>
            {postId ? '保存修改' : '发布帖子'}
          </Button>
          <Button onClick={() => navigate(-1)}>取消</Button>
        </div>
      </Card>
    </div>
  )
}

export default PostEditorPage
