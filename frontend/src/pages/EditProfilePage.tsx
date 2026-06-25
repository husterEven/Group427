import React, { useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { Card, Form, Input, Button, Upload, Select, message, Avatar, Spin } from 'antd'
import { UserOutlined, UploadOutlined, LoadingOutlined } from '@ant-design/icons'
import type { UploadChangeParam } from 'antd/es/upload'
import { userApi } from '@/services/api'
import { useAuthStore } from '@/store'

const EditProfilePage: React.FC = () => {
  const { user, updateUser } = useAuthStore()
  const [form] = Form.useForm()
  const [loading, setLoading] = useState(false)
  const [uploading, setUploading] = useState(false)

  useEffect(() => {
    if (user) form.setFieldsValue(user)
  }, [user, form])

  const handleSave = async (values: Record<string, unknown>) => {
    setLoading(true)
    try {
      await userApi.updateMe(values)
      updateUser(values)
      message.success('资料已更新')
    } finally {
      setLoading(false)
    }
  }

  const handleUploadChange = (info: UploadChangeParam) => {
    if (info.file.status === 'uploading') {
      setUploading(true)
      return
    }
    if (info.file.status === 'done') {
      setUploading(false)
      const res = info.file.response
      if (res && res.code === 200 && res.data) {
        const avatarUrl = res.data.fileUrl
        updateUser({ avatarUrl })
        userApi.updateMe({ avatarUrl }).then(() => message.success('头像已更新'))
      } else {
        message.error(res?.message || '上传失败')
      }
    } else if (info.file.status === 'error') {
      setUploading(false)
      message.error('上传失败：' + (info.file.response?.message || info.file.error?.message || '网络错误'))
    }
  }

  return (
    <Card title="编辑个人资料" style={{ maxWidth: 600 }}>
      <div style={{ textAlign: 'center', marginBottom: 24 }}>
        <Spin spinning={uploading} indicator={<LoadingOutlined style={{ fontSize: 24 }} spin />}>
          <Avatar size={80} src={user?.avatarUrl} icon={<UserOutlined />} />
        </Spin>
        <div style={{ marginTop: 8 }}>
          <Upload
            showUploadList={false}
            action="/api/v1/attachments/upload"
            accept="image/*"
            headers={{ Authorization: `Bearer ${localStorage.getItem('accessToken') || ''}` }}
            onChange={handleUploadChange}
          >
            <Button icon={<UploadOutlined />} loading={uploading}>更换头像</Button>
          </Upload>
        </div>
      </div>
      <Form form={form} layout="vertical" onFinish={handleSave}>
        <Form.Item name="nickname" label="昵称" rules={[{ required: true, min: 2, max: 20 }]}>
          <Input />
        </Form.Item>
        <Form.Item name="bio" label="个人简介">
          <Input.TextArea rows={3} maxLength={200} placeholder="介绍一下你的投资背景..." />
        </Form.Item>
        <Form.Item name="gender" label="性别">
          <Select options={[{ value: 0, label: '保密' }, { value: 1, label: '男' }, { value: 2, label: '女' }]} />
        </Form.Item>
        <Form.Item>
          <Button type="primary" htmlType="submit" loading={loading}>保存修改</Button>
        </Form.Item>
      </Form>
    </Card>
  )
}

export default EditProfilePage
