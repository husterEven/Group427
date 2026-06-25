import React, { useEffect, useState } from 'react'
import { useParams, useNavigate } from 'react-router-dom'
import { Card, Avatar, Space, Typography, Tag, Button, Divider, Input, List, Spin, message, Progress, Radio, Collapse, Popconfirm } from 'antd'
import {
  LikeOutlined, LikeFilled, StarOutlined, StarFilled,
  MessageOutlined, EyeOutlined, ShareAltOutlined, SendOutlined,
  EditOutlined, DeleteOutlined,
} from '@ant-design/icons'
import { postApi, commentApi, voteApi } from '@/services/api'
import { useAuthStore } from '@/store'
import type { Post, Comment, VoteDetail } from '@/types'
import dayjs from 'dayjs'

const { Title, Text, Paragraph } = Typography
const { TextArea } = Input

const PostDetailPage: React.FC = () => {
  const { postId } = useParams<{ postId: string }>()
  const navigate = useNavigate()
  const { user } = useAuthStore()
  const [post, setPost] = useState<Post | null>(null)
  const [comments, setComments] = useState<Comment[]>([])
  const [loading, setLoading] = useState(true)
  const [commentText, setCommentText] = useState('')
  const [replyTo, setReplyTo] = useState<{ id: number; nickname: string } | null>(null)
  const [vote, setVote] = useState<VoteDetail | null>(null)
  const [repliesOpen, setRepliesOpen] = useState<Set<number>>(new Set())
  const [repliesMap, setRepliesMap] = useState<Record<number, Comment[]>>({})
  const [loadingReplies, setLoadingReplies] = useState<Set<number>>(new Set())

  const fetchPost = async () => {
    if (!postId) return
    setLoading(true)
    try {
      const [postRes, commentRes] = await Promise.all([
        postApi.getDetail(Number(postId)),
        commentApi.getList(Number(postId)),
      ])
      const p = postRes.data.data
      setPost(p)
      setComments(commentRes.data.data.records)
      if (p.contentType === 1) {
        try {
          const voteRes = await voteApi.getByPost(Number(postId))
          if (voteRes.data.data) setVote(voteRes.data.data)
        } catch { /* vote may not exist */ }
      }
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => { fetchPost() }, [postId])

  const handleLike = async () => {
    if (!postId || !post) return
    const res = await postApi.toggleLike(Number(postId))
    const data = res.data.data
    setPost({ ...post, isLiked: data.isLiked, likeCount: data.likeCount })
  }

  const handleCollect = async () => {
    if (!postId || !post) return
    const res = await postApi.toggleCollect(Number(postId))
    const data = res.data.data
    setPost({ ...post, isCollected: data.isCollected, collectCount: data.collectCount })
  }

  const handleDelete = async () => {
    if (!postId) return
    await postApi.delete(Number(postId))
    message.success('已删除')
    navigate('/')
  }

  const handleComment = async () => {
    if (!postId || !commentText.trim()) return
    const data: { content: string; parentCommentId?: number } = { content: commentText }
    if (replyTo) data.parentCommentId = replyTo.id
    await commentApi.create(Number(postId), data)
    message.success('评论成功')
    setCommentText('')
    setReplyTo(null)
    fetchPost()
  }

  const handleVote = async (optionIndex: number) => {
    if (!vote) return
    try {
      const res = await voteApi.submit(vote.voteId, optionIndex)
      if (res.data.data) setVote(res.data.data)
      message.success('投票成功')
    } catch { /* handled by interceptor */ }
  }

  const handleCommentLike = async (commentId: number) => {
    try {
      const res = await commentApi.toggleLike(commentId)
      const data = res.data.data
      setComments((prev) =>
        prev.map((c) =>
          c.commentId === commentId
            ? { ...c, isLiked: data.isLiked, likeCount: data.likeCount }
            : c
        )
      )
    } catch { /* handled by interceptor */ }
  }

  const toggleReplies = async (commentId: number) => {
    if (repliesOpen.has(commentId)) {
      setRepliesOpen((prev) => { const s = new Set(prev); s.delete(commentId); return s })
      return
    }
    setLoadingReplies((prev) => new Set(prev).add(commentId))
    try {
      const res = await commentApi.getReplies(commentId)
      setRepliesMap((prev) => ({ ...prev, [commentId]: res.data.data.records }))
      setRepliesOpen((prev) => new Set(prev).add(commentId))
    } catch { /* handled by interceptor */ }
    setLoadingReplies((prev) => { const s = new Set(prev); s.delete(commentId); return s })
  }

  const handleReplyClick = (comment: Comment) => {
    if (comment.isDeleted) return
    setReplyTo({ id: comment.commentId, nickname: comment.author.nickname })
  }

  if (loading) return <Spin size="large" style={{ display: 'flex', justifyContent: 'center', marginTop: 100 }} />
  if (!post) return <Text type="secondary">帖子不存在</Text>

  return (
    <div style={{ maxWidth: 800 }}>
      <Card>
        <Title level={3}>{post.title}</Title>
        <Space style={{ marginBottom: 16 }}>
          <Avatar src={post.author.avatarUrl} onClick={() => navigate(`/profile/${post.author.userId}`)} style={{ cursor: 'pointer' }} />
          <div>
            <div><Text strong style={{ cursor: 'pointer' }} onClick={() => navigate(`/profile/${post.author.userId}`)}>{post.author.nickname}</Text></div>
            <Text type="secondary">{dayjs(post.publishTime).format('YYYY-MM-DD HH:mm')} · {post.viewCount} 阅读</Text>
          </div>
          <Tag color="blue" style={{ marginLeft: 'auto' }}>
            {post.contentType === 1 ? '投票帖' : post.contentType === 2 ? '图文帖' : '普通帖'}
          </Tag>
        </Space>
        <Divider />
        <div dangerouslySetInnerHTML={{ __html: post.content }} style={{ minHeight: 200, lineHeight: 1.8 }} />

        {/* 投票 */}
        {vote && (
          <Card title={vote.voteTitle} style={{ marginTop: 16, background: '#fafafa' }}>
            {vote.isExpired ? (
              vote.options.map((opt) => (
                <div key={opt.index} style={{ marginBottom: 8 }}>
                  <Progress percent={Math.round(opt.percentage)} format={() => `${opt.text} (${opt.count}票)`} />
                </div>
              ))
            ) : (
              <Radio.Group
                value={vote.mySelection ?? undefined}
                onChange={(e) => handleVote(e.target.value)}
                disabled={vote.mySelection !== null}
              >
                {vote.options.map((opt) => (
                  <Radio key={opt.index} value={opt.index} style={{ display: 'block', marginBottom: 8 }}>
                    {opt.text}
                  </Radio>
                ))}
              </Radio.Group>
            )}
            <Text type="secondary">{vote.totalCount} 人已投票 · 截止 {dayjs(vote.endTime).format('MM-DD HH:mm')}</Text>
          </Card>
        )}

        {/* 操作栏 */}
        <div style={{ display: 'flex', gap: 24, marginTop: 24, padding: '12px 0', borderTop: '1px solid #f0f0f0' }}>
          <Button icon={post.isLiked ? <LikeFilled /> : <LikeOutlined />} onClick={handleLike} type="text">
            {post.likeCount > 0 ? post.likeCount : '点赞'}
          </Button>
          <Button icon={post.isCollected ? <StarFilled /> : <StarOutlined />} onClick={handleCollect} type="text">
            {post.collectCount > 0 ? post.collectCount : '收藏'}
          </Button>
          <Button icon={<ShareAltOutlined />} type="text" onClick={() => message.info('链接已复制')}>转发</Button>
          {user?.userId === post.author.userId && (
            <Space style={{ marginLeft: 'auto' }}>
              <Button icon={<EditOutlined />} type="text" onClick={() => navigate(`/editor/${postId}`)}>编辑</Button>
              <Popconfirm title="确定删除这条帖子吗？" onConfirm={handleDelete} okText="确定" cancelText="取消">
                <Button icon={<DeleteOutlined />} type="text" danger>删除</Button>
              </Popconfirm>
            </Space>
          )}
          <span style={{ marginLeft: 'auto' }}>
            <EyeOutlined style={{ marginRight: 4 }} />{post.viewCount}
            <MessageOutlined style={{ marginLeft: 16, marginRight: 4 }} />{post.commentCount}
          </span>
        </div>
      </Card>

      {/* 评论 */}
      <Card title={`评论 (${post.commentCount})`} style={{ marginTop: 16 }}>
        <div style={{ marginBottom: 16 }}>
          {replyTo && <Tag closable onClose={() => setReplyTo(null)}>回复 @{replyTo.nickname}</Tag>}
          <TextArea rows={3} value={commentText} onChange={(e) => setCommentText(e.target.value)} placeholder="发表你的观点..." />
          <Button type="primary" icon={<SendOutlined />} onClick={handleComment} style={{ marginTop: 8 }}>
            发表评论
          </Button>
        </div>
        <List
          dataSource={comments}
          renderItem={(comment) => (
            <List.Item style={{ borderBottom: '1px solid #f5f5f5', display: 'block', padding: '12px 0' }}>
              <List.Item.Meta
                avatar={<Avatar src={comment.author?.avatarUrl} />}
                title={
                  <Space>
                    <Text strong>{comment.author?.nickname || '未知用户'}</Text>
                    <Text type="secondary">{dayjs(comment.publishTime).fromNow()}</Text>
                  </Space>
                }
                description={
                  <div>
                    <div>{comment.isDeleted ? <Text type="secondary">该评论已被删除</Text> : comment.content}</div>
                    <Space size={12} style={{ marginTop: 4 }}>
                      <Button type="text" size="small" onClick={() => handleReplyClick(comment)}>回复</Button>
                      <Button type="text" size="small"
                        icon={comment.isLiked ? <LikeFilled style={{ color: '#1677ff' }} /> : <LikeOutlined />}
                        onClick={() => handleCommentLike(comment.commentId)}>
                        {comment.likeCount || ''}
                      </Button>
                      {user?.userId === comment.author?.userId && !comment.isDeleted && (
                        <Popconfirm title="删除这条评论？" onConfirm={async () => {
                          await commentApi.delete(comment.commentId)
                          message.success('已删除')
                          fetchPost()
                        }} okText="确定" cancelText="取消">
                          <Button type="text" size="small" danger>删除</Button>
                        </Popconfirm>
                      )}
                      {(comment.replyCount ?? 0) > 0 && (
                        <Button type="link" size="small"
                          onClick={() => toggleReplies(comment.commentId)}
                          loading={loadingReplies.has(comment.commentId)}>
                          {repliesOpen.has(comment.commentId) ? '收起回复' : `${comment.replyCount} 条回复`}
                        </Button>
                      )}
                    </Space>
                    {/* 楼中楼回复 */}
                    {repliesOpen.has(comment.commentId) && repliesMap[comment.commentId] && (
                      <div style={{ marginTop: 8, paddingLeft: 24, borderLeft: '2px solid #e8e8e8' }}>
                        {repliesMap[comment.commentId].map((reply) => (
                          <div key={reply.commentId} style={{ padding: '6px 0' }}>
                            <Space>
                              <Text strong>{reply.author?.nickname || '未知用户'}</Text>
                              <Text type="secondary">{dayjs(reply.publishTime).fromNow()}</Text>
                            </Space>
                            <div>{reply.isDeleted ? <Text type="secondary">该评论已被删除</Text> : reply.content}</div>
                          </div>
                        ))}
                      </div>
                    )}
                  </div>
                }
              />
            </List.Item>
          )}
        />
      </Card>
    </div>
  )
}

export default PostDetailPage
