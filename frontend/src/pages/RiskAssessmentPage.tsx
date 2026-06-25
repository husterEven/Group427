import React, { useEffect, useState } from 'react'
import { Card, Radio, Button, Result, Typography, Space, message } from 'antd'
import { SafetyCertificateOutlined } from '@ant-design/icons'
import { userApi } from '@/services/api'
import type { RiskAssessment } from '@/types'
import dayjs from 'dayjs'

const { Title } = Typography

const questions = [
  { q: '您的投资经验年限？', opts: ['1年以下', '1-3年', '3-5年', '5-10年', '10年以上'] },
  { q: '您可承受的最大投资回撤？', opts: ['不超过5%', '5%-10%', '10%-20%', '20%-30%', '30%以上'] },
  { q: '您倾向的投资品类？（多选）', opts: ['A股', '基金', '期货', '港股', '美股'] },
  { q: '您的投资目标？', opts: ['保本增值', '稳定收益', '跑赢通胀', '追求高收益'] },
  { q: '预期年化收益率？', opts: ['5%以下', '5%-10%', '10%-20%', '20%以上'] },
]

const RiskAssessmentPage: React.FC = () => {
  const [result, setResult] = useState<RiskAssessment | null>(null)
  const [step, setStep] = useState(0)
  const [answers, setAnswers] = useState<string[]>([])

  useEffect(() => {
    userApi.getRiskAssessment().then((r) => { if (r.data.data) setResult(r.data.data) })
  }, [])

  const handleAnswer = (opt: string) => {
    const next = [...answers, opt]
    setAnswers(next)
    if (step < questions.length - 1) {
      setStep(step + 1)
    } else {
      const level = calcLevel(next)
      userApi.submitRiskAssessment(level).then((r) => setResult(r.data.data))
      message.success('测评完成')
    }
  }

  const calcLevel = (ans: string[]): string => {
    const score = ans.reduce((s, a) => s + (a.length > 3 ? 2 : 1), 0)
    if (score <= 6) return '保守型'
    if (score <= 8) return '稳健型'
    if (score <= 10) return '平衡型'
    return '进取型'
  }

  if (result) {
    return (
      <Card style={{ maxWidth: 600 }}>
        <Result
          icon={<SafetyCertificateOutlined />}
          title={`风险等级：${result.resultLevel}`}
          subTitle={`测评时间：${dayjs(result.completeTime).format('YYYY-MM-DD HH:mm')}`}
          extra={
            <Space>
              <Button type="primary" onClick={() => { setResult(null); setStep(0); setAnswers([]) }}>重新测评</Button>
              <Button onClick={() => window.history.back()}>返回</Button>
            </Space>
          }
        />
      </Card>
    )
  }

  return (
    <Card title={<Title level={4}>投资者适当性风险评估</Title>} style={{ maxWidth: 600 }}>
      <div style={{ marginBottom: 8 }}>
        <Title level={5}>第 {step + 1} / {questions.length} 题</Title>
        <p style={{ fontSize: 16, marginBottom: 16 }}>{questions[step].q}</p>
        <Radio.Group onChange={(e) => handleAnswer(e.target.value)}>
          {questions[step].opts.map((o) => (
            <Radio key={o} value={o} style={{ display: 'block', marginBottom: 10, fontSize: 15 }}>{o}</Radio>
          ))}
        </Radio.Group>
      </div>
    </Card>
  )
}

export default RiskAssessmentPage
