import React, { useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { Card, Button, List, Avatar, Typography, Modal, Input, Space, Tag, message } from 'antd'
import { PlusOutlined, TeamOutlined } from '@ant-design/icons'
import { socialApi } from '@/services/api'
import type { Group } from '@/types'

const { Title, Text } = Typography

const GroupsPage: React.FC = () => {
  const navigate = useNavigate()
  const [groups, setGroups] = useState<Group[]>([])
  const [modalOpen, setModalOpen] = useState(false)
  const [groupName, setGroupName] = useState('')

  useEffect(() => {
    socialApi.getGroups().then((r) => setGroups(r.data.data || []))
  }, [])

  const handleCreate = async () => {
    if (!groupName.trim()) return
    const r = await socialApi.createGroup({ groupName })
    setGroups([...groups, r.data.data])
    setModalOpen(false)
    setGroupName('')
    message.success('群组创建成功')
  }

  return (
    <div style={{ maxWidth: 700 }}>
      <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: 16 }}>
        <Title level={4}><TeamOutlined /> 我的群组</Title>
        <Button type="primary" icon={<PlusOutlined />} onClick={() => setModalOpen(true)}>创建群组</Button>
      </div>
      <List
        grid={{ gutter: 16, column: 2 }}
        dataSource={groups}
        renderItem={(g) => (
          <List.Item>
            <Card hoverable onClick={() => navigate(`/groups/${g.groupId}`)} size="small">
              <Space direction="vertical" style={{ width: '100%' }}>
                <Space>
                  <Avatar icon={<TeamOutlined />} />
                  <div>
                    <Text strong>{g.groupName}</Text>
                    <div><Text type="secondary">{g.memberCount} 成员 · {g.status === 1 ? '正常' : g.status === 0 ? '已解散' : '禁言中'}</Text></div>
                  </div>
                </Space>
                {g.myRole === 2 && <Tag color="gold">群主</Tag>}
                {g.myRole === 1 && <Tag color="blue">管理员</Tag>}
              </Space>
            </Card>
          </List.Item>
        )}
        locale={{ emptyText: '暂未加入任何群组' }}
      />
      <Modal title="创建群组" open={modalOpen} onCancel={() => setModalOpen(false)} onOk={handleCreate} okText="创建">
        <Input placeholder="群组名称" value={groupName} onChange={(e) => setGroupName(e.target.value)} maxLength={100} />
      </Modal>
    </div>
  )
}

export default GroupsPage
