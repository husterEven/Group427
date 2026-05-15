一、用户相关类
1. 用户（User）
属性：

userId - Long - 用户唯一标识

nickname - String - 用户昵称

avatarUrl - String - 头像URL地址

bio - String - 个人简介

mobile - String - 手机号

email - String - 邮箱地址

passwordHash - String - 登录密码哈希值

wechatOpenId - String - 微信OpenID

weiboUid - String - 微博UID

basicVerified - Boolean - 基础认证状态（是否完成手机/邮箱验证）

realNameVerified - Boolean - 实名认证状态

professionVerified - Boolean - 专业认证状态

verificationLevel - Integer - 认证等级（0-未认证，1-基础，2-实名，3-专业加V）

riskAssessmentLevel - String - 风险测评等级（保守/稳健/平衡/进取）

points - Integer - 积分总数

level - Integer - 用户等级

registerTime - Date - 注册时间

lastLoginTime - Date - 最后登录时间

isMuted - Boolean - 是否被禁言

muteExpireTime - Date - 禁言截止时间

isBanned - Boolean - 是否被封禁

操作：

registerByMobile(mobile:String, verificationCode:String, password:String) - 通过手机号和验证码完成账号注册

registerByEmail(email:String, password:String, emailCode:String) - 通过邮箱和验证码完成账号注册

thirdPartyLogin(platform:String, authCode:String) - 通过微信/微博第三方授权完成登录

completeBasicVerification(verifyType:Integer, contact:String, code:String) - 完成手机或邮箱基础验证，解锁发帖评论权限

submitRealNameVerification(idCardFront:String, idCardBack:String, faceImage:String) - 提交身份证照片和人脸识别进行实名认证

submitProfessionVerification(certificateUrl:String, educationUrl:String) - 提交从业资格证和学历证明进行专业认证

completeRiskAssessment(answers:List<Answer>) - 提交风险评估问卷答案，完成风险测评

updateProfile(nickname:String, avatar:String, bio:String, tags:List<String>) - 更新个人资料信息

setPreferences(focusMarkets:List<String>, riskType:String) - 设置投资偏好和风险偏好

setPrivacySettings(profileVisibility:Integer, postVisibility:Integer, followVisibility:Integer) - 设置个人资料和动态的可见范围

viewAchievements() - 查看个人成就数据（发帖数、精华数、勋章等）

getPointsDetail() - 获取积分明细和获取记录

upgradeLevel() - 根据积分自动升级用户等级

2. 用户认证记录（UserVerification）
属性：

recordId - Long - 认证记录唯一标识

userId - Long - 提交认证的用户ID

verificationType - Integer - 认证类型（1-实名认证，2-专业认证）

submittedMaterialUrl - String - 提交的证件材料URL

faceImageUrl - String - 人脸识别照片URL

auditStatus - Integer - 审核状态（0-待审核，1-通过，2-拒绝）

auditorId - Long - 审核人ID

auditTime - Date - 审核时间

rejectReason - String - 拒绝原因

操作：

submit(userId:Long, type:Integer, materials:Map<String,String>) - 提交认证申请材料

approve(recordId:Long, auditorId:Long) - 审核通过认证申请

reject(recordId:Long, auditorId:Long, reason:String) - 审核拒绝并填写拒绝原因

3. 风险评估问卷答案（RiskAssessmentAnswer）
属性：

answerId - Long - 答案记录唯一标识

userId - Long - 答题用户ID

questionnaireVersion - String - 问卷版本号

answerDetailJson - String - 答案详情（JSON格式）

resultLevel - String - 测评结果等级

completeTime - Date - 完成时间

操作：

submitAnswers(userId:Long, answers:List<QuestionAnswer>, version:String) - 提交问卷答案

calculateRiskLevel(answers:List<QuestionAnswer>) - 根据答案计算风险等级

4. 用户偏好设置（UserPreference）
属性：

preferenceId - Long - 偏好设置唯一标识

userId - Long - 用户ID

focusMarkets - String - 关注市场（逗号分隔）

riskType - String - 风险偏好类型

investmentExperience - String - 投资经验标签

操作：

savePreferences(userId:Long, markets:List<String>, risk:String, experience:String) - 保存用户偏好设置

getPreferences(userId:Long) - 获取用户偏好设置

5. 隐私设置（PrivacySetting）
属性：

settingId - Long - 设置唯一标识

userId - Long - 用户ID

profileVisibility - Integer - 个人资料可见范围

postVisibility - Integer - 发帖动态可见范围

followVisibility - Integer - 关注粉丝列表可见范围

操作：

savePrivacySettings(userId:Long, profileVis:Integer, postVis:Integer, followVis:Integer) - 保存隐私设置

6. 用户成就（UserAchievement）
属性：

achievementId - Long - 成就记录唯一标识

userId - Long - 用户ID

totalPostCount - Integer - 累计发帖数量

essencePostCount - Integer - 精华帖数量

influencePoints - Integer - 影响力积分值

honorBadges - String - 荣誉勋章列表

操作：

viewAchievements(userId:Long) - 查看用户成就数据

updatePostStatistics(userId:Long, postCountDelta:Integer, essenceDelta:Integer) - 更新发帖统计数据

二、内容相关类
7. 帖子（Post）
属性：

postId - Long - 帖子唯一标识

authorId - Long - 作者用户ID

title - String - 帖子标题

content - String - 纯文本内容

richTextContent - String - 富文本内容

contentType - Integer - 内容类型（1-普通，2-长文，3-投票，4-实时动态）

sectionId - Integer - 所属板块ID

zoneId - Integer - 所属专区ID

isPinned - Boolean - 是否置顶

isEssence - Boolean - 是否精华帖

auditStatus - Integer - 审核状态

likeCount - Integer - 点赞数

favoriteCount - Integer - 收藏数

shareCount - Integer - 转发数

commentCount - Integer - 评论数

viewCount - Integer - 浏览量

publishTime - Date - 发布时间

auditTime - Date - 审核时间

操作：

publish(authorId:Long, title:String, content:String, sectionId:Integer, zoneId:Integer) - 发布新帖子

edit(postId:Long, newTitle:String, newContent:String) - 编辑已发布的帖子

delete(postId:Long, userId:Long) - 删除帖子（用户或管理员）

pin(postId:Long, operatorId:Long) - 置顶帖子（管理员）

setEssence(postId:Long, operatorId:Long) - 设为精华帖（管理员）

approve(postId:Long, auditorId:Long) - 审核通过帖子

reject(postId:Long, auditorId:Long, reason:String) - 审核驳回帖子

addLike(postId:Long, userId:Long) - 增加点赞

addFavorite(postId:Long, userId:Long) - 增加收藏

addShare(postId:Long, userId:Long) - 增加转发

8. 投票帖信息（VotePost）
属性：

voteId - Long - 投票唯一标识

postId - Long - 关联的帖子ID

voteTitle - String - 投票标题

optionsJson - String - 选项列表

durationHours - Integer - 投票持续时长

isAnonymous - Boolean - 是否匿名投票

startTime - Date - 投票开始时间

endTime - Date - 投票结束时间

totalVoteCount - Integer - 总投票人数

操作：

createVote(postId:Long, title:String, options:List<String>, duration:Integer, isAnonymous:Boolean) - 创建投票

castVote(voteId:Long, userId:Long, optionIndex:Integer) - 参与投票

viewVoteResult(voteId:Long) - 查看投票结果统计

9. 投票记录（VoteRecord）
属性：

recordId - Long - 投票记录唯一标识

voteId - Long - 投票ID

userId - Long - 投票用户ID

selectedOptionIndex - Integer - 选择的选项索引

voteTime - Date - 投票时间

操作：

recordVote(voteId:Long, userId:Long, optionIndex:Integer) - 记录用户投票

10. 附件（Attachment）
属性：

attachmentId - Long - 附件唯一标识

postId - Long - 所属帖子ID

fileName - String - 文件名

fileType - Integer - 文件类型（1-PDF，2-Excel）

fileUrl - String - 文件存储URL

fileSize - Long - 文件大小

auditStatus - Integer - 审核状态

downloadCount - Integer - 下载次数

uploadTime - Date - 上传时间

操作：

upload(postId:Long, file:MultipartFile, fileType:Integer) - 上传附件

audit(attachmentId:Long, auditorId:Long, status:Integer) - 审核附件

preview(attachmentId:Long) - 在线预览附件

download(attachmentId:Long, userId:Long) - 下载附件

11. 评论（Comment）
属性：

commentId - Long - 评论唯一标识

postId - Long - 所属帖子ID

parentCommentId - Long - 父级评论ID

authorId - Long - 评论作者ID

content - String - 评论内容

likeCount - Integer - 点赞数

replyCount - Integer - 回复数

auditStatus - Integer - 审核状态

publishTime - Date - 发布时间

atUserIds - String - @提醒的用户ID列表

操作：

publishComment(postId:Long, authorId:Long, content:String, atUsers:List<Long>) - 发表一级评论

replyComment(parentCommentId:Long, postId:Long, authorId:Long, content:String) - 回复评论（楼中楼）

deleteComment(commentId:Long, userId:Long) - 删除评论

likeComment(commentId:Long, userId:Long) - 点赞评论

12. 用户互动记录（UserInteraction）
属性：

interactionId - Long - 互动记录唯一标识

userId - Long - 操作用户ID

targetType - Integer - 目标类型（1-帖子，2-评论）

targetId - Long - 目标ID

interactionType - Integer - 互动类型（1-点赞，2-收藏，3-转发）

createTime - Date - 互动时间

操作：

recordLike(userId:Long, targetType:Integer, targetId:Long) - 记录点赞

recordFavorite(userId:Long, targetId:Long) - 记录收藏

cancelInteraction(userId:Long, targetType:Integer, targetId:Long, type:Integer) - 取消互动

13. 盘中实时动态（RealtimeDynamic）
属性：

dynamicId - Long - 动态唯一标识

authorId - Long - 作者ID

content - String - 动态内容

imageUrls - String - 配图URL列表

likeCount - Integer - 点赞数

commentCount - Integer - 评论数

publishTime - Date - 发布时间

操作：

publishDynamic(authorId:Long, content:String, images:List<String>) - 发布实时动态

deleteDynamic(dynamicId:Long, userId:Long) - 删除动态

三、板块与专区类
14. 板块（Section）
属性：

sectionId - Integer - 板块唯一标识

sectionName - String - 板块名称

sectionType - Integer - 板块类型

description - String - 板块简介

sortOrder - Integer - 排序权重

status - Integer - 状态

createTime - Date - 创建时间

操作：

addSection(name:String, type:Integer, description:String, order:Integer) - 新增板块

editSection(sectionId:Integer, name:String, description:String, order:Integer) - 编辑板块信息

deleteSection(sectionId:Integer) - 删除板块

reorder(sectionIds:List<Integer>) - 调整板块排序

15. 专区（Zone）
属性：

zoneId - Integer - 专区唯一标识

zoneName - String - 专区名称

sectionId - Integer - 所属板块ID

description - String - 专区简介

iconUrl - String - 专区图标URL

postCount - Integer - 帖子数量

status - Integer - 状态

操作：

createZone(name:String, sectionId:Integer, description:String, icon:String) - 创建专区

editZone(zoneId:Integer, name:String, description:String, icon:String) - 编辑专区信息

deleteZone(zoneId:Integer) - 删除专区

四、社交关系类
16. 关注关系（Follow）
属性：

relationId - Long - 关注关系唯一标识

followerId - Long - 关注者用户ID

followeeId - Long - 被关注者用户ID

isStarred - Boolean - 是否设为星标

followTime - Date - 关注时间

操作：

follow(followerId:Long, followeeId:Long) - 关注用户

unfollow(followerId:Long, followeeId:Long) - 取消关注

setStar(followerId:Long, followeeId:Long) - 设为星标特别关注

unsetStar(followerId:Long, followeeId:Long) - 取消星标

17. 私信（PrivateMessage）
属性：

messageId - Long - 私信唯一标识

senderId - Long - 发送者ID

receiverId - Long - 接收者ID

contentType - Integer - 内容类型

content - String - 消息内容

isRead - Boolean - 是否已读

sendTime - Date - 发送时间

操作：

sendMessage(senderId:Long, receiverId:Long, contentType:Integer, content:String) - 发送私信

deleteMessage(messageId:Long, userId:Long) - 删除私信

markAsRead(messageId:Long, userId:Long) - 标记私信为已读

18. 群组（Group）
属性：

groupId - Long - 群组唯一标识

ownerId - Long - 群主用户ID

groupName - String - 群组名称

description - String - 群组简介

avatarUrl - String - 群组头像URL

investmentTags - String - 投资品类标签

mode - Integer - 群组模式

memberCount - Integer - 成员数量

postCount - Integer - 群内帖子数量

status - Integer - 状态

createTime - Date - 创建时间

auditTime - Date - 审核时间

操作：

createGroup(ownerId:Long, name:String, description:String, tags:List<String>, mode:Integer) - 创建群组

editGroup(groupId:Long, name:String, description:String, tags:List<String>, mode:Integer) - 编辑群组信息

approveGroup(groupId:Long, auditorId:Long) - 审核通过群组创建申请

setGroupMode(groupId:Long, mode:Integer) - 设置群组准入模式

setMemberPermission(groupId:Long, role:Integer, permissions:Map<String,Boolean>) - 设置成员发言权限

19. 群组成员（GroupMember）
属性：

memberId - Long - 成员关系唯一标识

groupId - Long - 群组ID

userId - Long - 用户ID

role - Integer - 角色

joinType - Integer - 加入方式

isMuted - Boolean - 是否被禁言

joinTime - Date - 加入时间

操作：

join(groupId:Long, userId:Long, joinType:Integer) - 加入群组

leave(groupId:Long, userId:Long) - 退出群组

invite(groupId:Long, inviterId:Long, inviteeId:Long) - 邀请成员加入

approveApplication(groupId:Long, applicantId:Long, approverId:Long) - 审核通过入群申请

removeMember(groupId:Long, operatorId:Long, targetId:Long) - 移除成员

setAdmin(groupId:Long, operatorId:Long, targetId:Long, isAdmin:Boolean) - 设置/取消管理员

20. 群组帖子（GroupPost）
属性：

groupPostId - Long - 群组帖子唯一标识

groupId - Long - 所属群组ID

authorId - Long - 作者ID

content - String - 帖子内容

attachmentUrl - String - 附件URL

likeCount - Integer - 点赞数

commentCount - Integer - 评论数

publishTime - Date - 发布时间

操作：

publishGroupPost(groupId:Long, authorId:Long, content:String, attachment:String) - 发布群组帖子

deleteGroupPost(groupPostId:Long, userId:Long) - 删除群组帖子

commentGroupPost(groupPostId:Long, userId:Long, content:String) - 评论群组帖子

五、内容聚合与搜索类
21. 热榜条目（HotTopic）
属性：

entryId - Long - 热榜条目唯一标识

entryType - Integer - 条目类型（1-话题，2-个股）

name - String - 话题名称或股票代码/名称

heatValue - Integer - 热度值

period - Integer - 统计周期（1-日榜，2-周榜）

rank - Integer - 榜单排名

postCount - Integer - 关联发帖量

interactionCount - Integer - 互动量

updateTime - Date - 更新时间

操作：

calculateHeat(postCount:Integer, commentCount:Integer, likeCount:Integer, timeDecayFactor:Double) - 计算内容热度值

generateRanking(period:Integer, limit:Integer) - 生成指定周期的热力榜单

22. 用户行为记录（UserBehavior）
属性：

behaviorId - Long - 行为唯一标识

userId - Long - 用户ID

behaviorType - Integer - 行为类型

targetId - Long - 目标ID

targetType - Integer - 目标类型

behaviorTime - Date - 行为时间

sessionId - String - 会话ID

操作：

record(userId:Long, behaviorType:Integer, targetId:Long, targetType:Integer) - 记录用户行为

analyzeProfile(userId:Long) - 分析用户画像用于推荐

23. 搜索记录（SearchRecord）
属性：

searchId - Long - 搜索记录唯一标识

userId - Long - 搜索用户ID

keyword - String - 搜索关键词

resultCount - Integer - 搜索结果数量

searchTime - Date - 搜索时间

操作：

recordSearch(userId:Long, keyword:String, resultCount:Integer) - 记录用户搜索行为

getHotSearchKeywords(period:String, limit:Integer) - 获取热门搜索关键词

六、管理运营类
24. 审核队列（AuditQueue）
属性：

auditItemId - Long - 审核项唯一标识

contentType - Integer - 内容类型

contentId - Long - 内容ID

userId - Long - 提交用户ID

submitTime - Date - 提交时间

auditStatus - Integer - 审核状态

auditorId - Long - 审核人ID

auditTime - Date - 审核时间

rejectReason - String - 驳回原因

priority - Integer - 优先级

操作：

enqueue(contentType:Integer, contentId:Long, userId:Long, priority:Integer) - 将内容加入审核队列

approve(auditItemId:Long, auditorId:Long) - 审核通过内容

reject(auditItemId:Long, auditorId:Long, reason:String) - 审核驳回内容

markSuspicious(auditItemId:Long, operatorId:Long) - 标记为可疑内容进入人工复核

25. 举报记录（Report）
属性：

reportId - Long - 举报唯一标识

reporterId - Long - 举报人用户ID

targetType - Integer - 被举报对象类型

targetId - Long - 被举报对象ID

reason - String - 举报原因

status - Integer - 处理状态

handlerId - Long - 处理人ID

result - String - 处理结果说明

reportTime - Date - 举报时间

handleTime - Date - 处理时间

操作：

submit(reporterId:Long, targetType:Integer, targetId:Long, reason:String) - 提交举报

handle(reportId:Long, handlerId:Long, result:String, action:Integer) - 处理举报并执行对应操作

26. 敏感词库（SensitiveWord）
属性：

wordId - Long - 敏感词唯一标识

word - String - 敏感词内容

wordType - Integer - 词类型

severityLevel - Integer - 严重等级

addTime - Date - 添加时间

addBy - Long - 添加人ID

操作：

addWord(word:String, wordType:Integer, severity:Integer, operatorId:Long) - 添加敏感词

removeWord(wordId:Long) - 删除敏感词

detect(content:String) - 检测内容是否包含敏感词，返回命中列表

27. 用户处罚记录（UserPunishment）
属性：

punishmentId - Long - 处罚唯一标识

userId - Long - 被处罚用户ID

punishmentType - Integer - 处罚类型

reason - String - 处罚原因

durationDays - Integer - 处罚时长

startTime - Date - 处罚开始时间

endTime - Date - 处罚结束时间

operatorId - Long - 操作管理员ID

operateTime - Date - 操作时间

操作：

apply(userId:Long, type:Integer, reason:String, durationDays:Integer, operatorId:Long) - 对用户执行分级处罚

lift(punishmentId:Long, operatorId:Long) - 解除处罚

queryHistory(userId:Long) - 查询用户历史处罚记录

28. 平台统计数据（PlatformStatistic）
属性：

statId - Long - 统计唯一标识

statDate - Date - 统计日期

dau - Integer - 日活跃用户数

mau - Integer - 月活跃用户数

newUserCount - Integer - 新增用户数

postCount - Integer - 发帖总数

commentCount - Integer - 评论总数

likeCount - Integer - 点赞总数

avgPostPerUser - Double - 人均发帖数

avgInteractionPerUser - Double - 人均互动次数

retentionRateDay1 - Double - 次日留存率

retentionRateDay7 - Double - 7日留存率

retentionRateDay30 - Double - 30日留存率

操作：

calculateDau(date:Date) - 计算指定日期日活跃用户数

generateActivityReport(startDate:Date, endDate:Date) - 生成时间段内的活跃度报告

exportReport(reportType:String, dateRange:DateRange) - 导出统计报表（Excel格式）

29. 用户行为监控（UserBehaviorMonitor）
属性：

monitorId - Long - 监控记录唯一标识

userId - Long - 被监控用户ID

postFrequencyLastHour - Integer - 过去1小时发帖频率

commentFrequencyLastHour - Integer - 过去1小时评论频率

contentRepeatRate - Double - 内容重复率

externalLinkRatio - Double - 广告外链占比

isSuspicious - Boolean - 是否异常标记

monitorTime - Date - 监控时间

操作：

detectHighFrequencyUser(userId:Long, timeWindow:Integer) - 检测指定时间窗口内的高频发帖/评论用户

markSuspiciousAccount(userId:Long, operatorId:Long, reason:String) - 标记异常账号供人工复核

30. 运营操作日志（OperationLog）
属性：

logId - Long - 日志唯一标识

operatorId - Long - 操作管理员ID

operationType - Integer - 操作类型

targetType - String - 操作对象类型

targetId - Long - 操作对象ID

operationDetail - String - 操作详情

operateTime - Date - 操作时间

ipAddress - String - 操作IP地址

操作：

logOperation(operatorId:Long, opType:Integer, targetType:String, targetId:Long, detail:String, ip:String) - 记录管理员操作日志

queryOperationHistory(operatorId:Long, startTime:Date, endTime:Date) - 查询操作历史记录

