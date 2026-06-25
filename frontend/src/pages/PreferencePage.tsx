import React, { useEffect, useState } from 'react'
import { Card, Checkbox, Radio, Button, Typography, message } from 'antd'
import { userApi } from '@/services/api'
import type { UserPreference } from '@/types'

const { Title } = Typography

const PreferencePage: React.FC = () => {
  const [pref, setPref] = useState<UserPreference>({ focusMarkets: '', riskType: '' })
  const [markets, setMarkets] = useState<string[]>([])

  useEffect(() => {
    userApi.getPreference().then((r) => {
      if (r.data.data) {
        setPref(r.data.data)
        setMarkets(r.data.data.focusMarkets ? r.data.data.focusMarkets.split(',') : [])
      }
    })
  }, [])

  const marketOptions = ['A股', '港股', '美股', '基金', '期货', '外汇', '债券', '数字货币']
  const riskOptions = [
    { value: '保守', label: '保守型 — 以保本为首要目标' },
    { value: '稳健', label: '稳健型 — 追求稳定收益' },
    { value: '平衡', label: '平衡型 — 愿意承担一定波动' },
    { value: '进取', label: '进取型 — 追求高收益，能承受较大回撤' },
  ]

  const handleSave = async () => {
    const data = { focusMarkets: markets.join(','), riskType: pref.riskType }
    await userApi.updatePreference(data)
    setPref(data)
    message.success('偏好已保存')
  }

  return (
    <Card title={<Title level={4}>投资偏好设置</Title>} style={{ maxWidth: 600 }}>
      <div style={{ marginBottom: 24 }}>
        <Title level={5}>关注市场</Title>
        <Checkbox.Group value={markets} onChange={(v) => setMarkets(v as string[])}>
          <div style={{ display: 'flex', flexWrap: 'wrap', gap: 8 }}>
            {marketOptions.map((m) => (
              <Checkbox key={m} value={m} style={{ marginRight: 12 }}>{m}</Checkbox>
            ))}
          </div>
        </Checkbox.Group>
      </div>

      <div style={{ marginBottom: 24 }}>
        <Title level={5}>风险偏好</Title>
        <Radio.Group value={pref.riskType} onChange={(e) => setPref({ ...pref, riskType: e.target.value })}>
          {riskOptions.map((r) => (
            <Radio key={r.value} value={r.value} style={{ display: 'block', marginBottom: 8 }}>{r.label}</Radio>
          ))}
        </Radio.Group>
      </div>

      <Button type="primary" onClick={handleSave}>保存偏好配置</Button>
    </Card>
  )
}

export default PreferencePage
