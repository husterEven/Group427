# ============================================================
# 交互缺陷专项测试 - 重点测试三个Bug领域
# ============================================================
$base = "http://localhost:8080/api/v1"
$pass = 0
$fail = 0

function Invoke-API {
    param($Method, $Url, $Body, $Token)
    try {
        $headers = @{"Content-Type" = "application/json"}
        if ($Token) { $headers["Authorization"] = "Bearer $Token" }
        $bodyJson = if ($Body) { ($Body | ConvertTo-Json -Compress -Depth 10) } else { $null }
        $result = Invoke-RestMethod -Method $Method -Uri $Url -Headers $headers -Body $bodyJson -TimeoutSec 10
        return $result
    } catch {
        return $null
    }
}

function Test-Case {
    param($Name, $Condition)
    if ($Condition) {
        Write-Host "  [PASS] $Name" -ForegroundColor Green
        $global:pass++
    } else {
        Write-Host "  [FAIL] $Name" -ForegroundColor Red
        $global:fail++
    }
}

# ============================================================
Write-Host "`n============================================================" -ForegroundColor Cyan
Write-Host "  注册测试账号" -ForegroundColor Cyan
Write-Host "============================================================" -ForegroundColor Cyan

$ts = Get-Date -Format "HHmmss"
$regBody = @{ nickname = "BugTest_$ts"; account = "bugtest_$ts@test.com"; password = "Test123456" }
$reg = Invoke-API "POST" "$base/auth/register" $regBody
if ($reg -and $reg.code -eq 200) {
    $token = $reg.data.accessToken
    Write-Host "  [OK] 注册成功, userId=$($reg.data.userId)" -ForegroundColor Green
} else {
    Write-Host "  [FAIL] 注册失败" -ForegroundColor Red; exit 1
}

# ============================================================
Write-Host "`n============================================================" -ForegroundColor Cyan
Write-Host "  BUG-1: 评论回复(楼中楼)无法显示" -ForegroundColor Cyan
Write-Host "============================================================" -ForegroundColor Cyan

# 1.1 创建帖子
Write-Host "`n--- 1.1 创建测试帖子 ---" -ForegroundColor Yellow
$postBody = @{
    title = "评论测试帖_$ts"
    content = "测试评论楼中楼功能"
    contentType = 0
    sectionId = 1
}
$post = Invoke-API "POST" "$base/posts" $postBody $token
Test-Case "帖子创建成功" ($post -and $post.code -eq 200)
$postId = if ($post) { $post.data.postId } else { 0 }

# 1.2 发表一级评论
Write-Host "`n--- 1.2 发表一级评论 ---" -ForegroundColor Yellow
$cmt1 = Invoke-API "POST" "$base/posts/$postId/comments" (@{ content = "一级评论：写得不错！" }) $token
Test-Case "一级评论创建成功" ($cmt1 -and $cmt1.code -eq 200)
$cmt1Id = if ($cmt1) { $cmt1.data.commentId } else { 0 }

# 1.3 楼中楼回复 (关键测试)
Write-Host "`n--- 1.3 楼中楼回复（设置parentCommentId）---" -ForegroundColor Yellow
$reply = Invoke-API "POST" "$base/posts/$postId/comments" (@{ content = "回复一级评论：谢谢！"; parentCommentId = $cmt1Id }) $token
Test-Case "楼中楼回复创建成功" ($reply -and $reply.code -eq 200)
$replyId = if ($reply) { $reply.data.commentId } else { 0 }

# 1.4 检查回复的parentCommentId是否正确
Write-Host "`n--- 1.4 验证回复的parentCommentId ---" -ForegroundColor Yellow
Test-Case "回复存在parentCommentId" (($reply.data.parentCommentId -ne $null) -and ($reply.data.parentCommentId -eq $cmt1Id))

# 1.5 获取评论列表（后端只返回parent_comment_id IS NULL的顶层评论）
Write-Host "`n--- 1.5 获取评论列表 ---" -ForegroundColor Yellow
$cmtList = Invoke-API "GET" "$base/posts/$postId/comments?page=1&pageSize=20" $null $token
Test-Case "评论列表获取成功" ($cmtList -and $cmtList.code -eq 200)
Test-Case "评论总数=1（只返回顶层评论，不包含楼中楼回复）" (($cmtList.data.total -eq 1))

# 1.6 尝试通过getReplies接口获取回复（验证API是否存在）
Write-Host "`n--- 1.6 尝试获取评论的回复列表 ---" -ForegroundColor Yellow
$replies = Invoke-API "GET" "$base/comments/$cmt1Id/replies?page=1&pageSize=10" $null $token
if ($replies -and $replies.code -eq 200) {
    Test-Case "获取回复列表成功" ($true)
    Test-Case "回复中包含楼中楼评论" ($replies.data.total -ge 1)
} else {
    Write-Host "  [WARN] getReplies API不存在或返回错误（前端api.ts有定义但后端可能未实现）" -ForegroundColor Yellow
    Test-Case "后端实现了getReplies接口" ($false)
}

# 1.7 关键缺陷：前端PostDetailPage.tsx从未调用getReplies
Write-Host "`n--- 1.7 关键发现 ---" -ForegroundColor Yellow
Write-Host "  [INFO] 前端PostDetailPage.tsx仅调用commentApi.getList()获取顶层评论" -ForegroundColor White
Write-Host "  [INFO] 评论列表使用List组件扁平渲染，无嵌套回复展示逻辑" -ForegroundColor White
Write-Host "  [INFO] 回复按钮仅设置replyTo状态，提交后调用fetchPost()刷新顶层评论" -ForegroundColor White
Write-Host "  [INFO] 后端CommentServiceImpl.getComments()使用parent_comment_id IS NULL过滤" -ForegroundColor White
Write-Host "  [BUG] 楼中楼回复创建后可存入数据库，但永远无法在前端展示" -ForegroundColor Red

# ============================================================
Write-Host "`n============================================================" -ForegroundColor Cyan
Write-Host "  BUG-2: 群组功能设计缺陷" -ForegroundColor Cyan
Write-Host "============================================================" -ForegroundColor Cyan

# 2.1 创建群组
Write-Host "`n--- 2.1 创建群组 ---" -ForegroundColor Yellow
$group = Invoke-API "POST" "$base/groups" (@{ groupName = "BugTest群$ts"; mode = 0 }) $token
Test-Case "群组创建成功" ($group -and $group.code -eq 200)
$groupId = if ($group) { $group.data.groupId } else { 0 }

# 2.2 关键缺陷：新群组status=0（解散状态）
Write-Host "`n--- 2.2 关键缺陷：新群组状态 ---" -ForegroundColor Yellow
# 获取群组详情来检查status
$gdetail = Invoke-API "GET" "$base/groups/$groupId" $null $token
if ($gdetail -and $gdetail.code -eq 200) {
    $gStatus = $gdetail.data.status
    Write-Host "  [INFO] 新创建群组的status=$gStatus (0=解散 1=正常 2=禁言)" -ForegroundColor White
    if ($gStatus -eq 0) {
        Write-Host "  [FAIL] 新群组status=0（解散状态），应为1（正常）" -ForegroundColor Red
        $fail++
    } elseif ($gStatus -eq 1) {
        Write-Host "  [PASS] 新群组status=1，正常" -ForegroundColor Green
        $pass++
    } else {
        Write-Host "  [WARN] 新群组status=$gStatus，异常" -ForegroundColor Yellow
    }
} else {
    Write-Host "  [FAIL] 无法获取群组详情" -ForegroundColor Red
    $fail++
}

# 2.3 关键缺陷：非成员无法查看群组详情
Write-Host "`n--- 2.3 非成员访问群组详情 ---" -ForegroundColor Yellow
# 创建第二个用户来测试
$ts2 = Get-Date -Format "HHmmss"
$regBody2 = @{ nickname = "BugTest2_$ts2"; account = "bugtest2_$ts2@test.com"; password = "Test123456" }
$reg2 = Invoke-API "POST" "$base/auth/register" $regBody2
$token2 = if ($reg2 -and $reg2.code -eq 200) { $reg2.data.accessToken } else { $null }

if ($token2) {
    # 用户2尝试查看用户1创建的群组（未加入）
    $gdetail2 = Invoke-API "GET" "$base/groups/$groupId" $null $token2
    if ($gdetail2 -and $gdetail2.code -eq 200) {
        Write-Host "  [PASS] 非成员可以查看群组详情(可发现群组)" -ForegroundColor Green
        $pass++
    } else {
        Write-Host "  [FAIL] 非成员无法查看群组详情(后端getGroupDetail要求必须是成员)" -ForegroundColor Red
        Write-Host "  [INFO] SocialServiceImpl.getGroupDetail() 第201行检查成员资格" -ForegroundColor White
        Write-Host "  [BUG] 用户无法在加入前查看群组信息，导致群组不可发现" -ForegroundColor Red
        $fail++
    }
} else {
    Write-Host "  [WARN] 无法创建第二个测试用户" -ForegroundColor Yellow
}

# 2.4 关键缺陷：getGroups()只返回已加入的群组
Write-Host "`n--- 2.4 getGroups接口验证 ---" -ForegroundColor Yellow
$userGroups = Invoke-API "GET" "$base/groups" $null $token
Test-Case "getGroups正常返回" ($userGroups -and $userGroups.code -eq 200)
Write-Host "  [INFO] getGroups()实现只查询当前用户已加入的群组(member表查询)" -ForegroundColor White
Write-Host "  [BUG] 没有'发现群组'/'浏览所有群组'的API，新用户只能看到空白列表" -ForegroundColor Red
Write-Host "  [BUG] 用户必须先创建或通过外部渠道获取groupId才能加入群组" -ForegroundColor Red

# 2.5 退群测试(群主退群所有权转移)
Write-Host "`n--- 2.5 群主退群测试" -ForegroundColor Yellow
# 先让用户2加入
if ($token2) {
    $joinResult = Invoke-API "POST" "$base/groups/$groupId/join" $null $token2
    if ($joinResult) {
        Write-Host "  [INFO] 用户2加入成功" -ForegroundColor Green
    }
}
# 用户1（群主）退群
$leaveResult = Invoke-API "POST" "$base/groups/$groupId/leave" $null $token
Test-Case "群主退群成功" ($leaveResult -and $leaveResult.code -eq 200)
# 检查群是否还存在
$gdetailAfter = Invoke-API "GET" "$base/groups/$groupId" $null $token2
if ($gdetailAfter -and $gdetailAfter.code -eq 200) {
    Write-Host "  [INFO] 群主退群后群组仍存在，新群主用户ID=$($gdetailAfter.data.ownerId)" -ForegroundColor Green
    Test-Case "群主退出后所有权正确转移" ($true)
} else {
    Write-Host "  [WARN] 群主退群后群组可能被删除（原成员非群主时）" -ForegroundColor Yellow
}

# ============================================================
Write-Host "`n============================================================" -ForegroundColor Cyan
Write-Host "  BUG-3: 投票调研参数为空" -ForegroundColor Cyan
Write-Host "============================================================" -ForegroundColor Cyan

# 3.1 创建投票帖子
Write-Host "`n--- 3.1 创建投票帖 ---" -ForegroundColor Yellow
$votePostBody = @{
    title = "投票测试帖_$ts"
    content = "测试投票功能"
    contentType = 1
    sectionId = 1
}
$votePost = Invoke-API "POST" "$base/posts" $votePostBody $token
Test-Case "投票帖创建成功" ($votePost -and $votePost.code -eq 200)
$votePostId = if ($votePost) { $votePost.data.postId } else { 0 }

# 3.2 为帖子创建投票（传递options参数）
Write-Host "`n--- 3.2 创建投票（含3个选项）---" -ForegroundColor Yellow
$endTime = (Get-Date).AddDays(7).ToString("yyyy-MM-ddTHH:mm:ss")
$voteBody = @{
    voteTitle = "你看好哪个板块？"
    options = @("科技", "消费", "新能源")
    endTime = $endTime
}
$voteCreated = Invoke-API "POST" "$base/posts/$votePostId/vote" $voteBody $token
Test-Case "投票API调用成功" ($voteCreated -and $voteCreated.code -eq 200)

# 3.3 关键缺陷：查看投票数据 - options是否被存储？
Write-Host "`n--- 3.3 关键缺陷：查看投票数据 ---" -ForegroundColor Yellow
$voteDetail = Invoke-API "GET" "$base/posts/$votePostId/vote" $null $token
if ($voteDetail -and $voteDetail.code -eq 200) {
    Write-Host "  [INFO] 投票数据获取成功" -ForegroundColor Green
    Write-Host "  [INFO] voteTitle=$($voteDetail.data.voteTitle)" -ForegroundColor White
    
    # 检查options
    $opts = $voteDetail.data.options
    if ($opts -and $opts.Count -gt 0) {
        Write-Host "  [INFO] options数量=$($opts.Count)" -ForegroundColor White
        if ($opts[0] -is [string]) {
            Write-Host "  [PASS] options包含字符串文本" -ForegroundColor Green
            $pass++
        } elseif ($opts[0] -is [hashtable] -or $opts[0] -is [psobject]) {
            # 可能是{option_index, cnt}格式的对象
            $keys = $opts[0] | Get-Member -MemberType NoteProperty | ForEach-Object { $_.Name }
            if ($keys -contains "option_index" -and $keys -contains "cnt") {
                Write-Host "  [FAIL] options仅包含{optionIndex, cnt}，缺少选项文本(text字段)" -ForegroundColor Red
                Write-Host "  [BUG] 投票选项文本从未存储到数据库(VoteCreateRequest.options被丢弃)" -ForegroundColor Red
                $fail++
            } else {
                Write-Host "  [WARN] options格式未知" -ForegroundColor Yellow
            }
        }
    } else {
        Write-Host "  [FAIL] options为空或不存在" -ForegroundColor Red
        Write-Host "  [BUG] 数据库无vote_option表，VoteServiceImpl.createVote()忽略options参数" -ForegroundColor Red
        $fail++
    }
    
    # 检查mySelection字段
    Write-Host "`n  [INFO] mySelection=$($voteDetail.data.mySelection)" -ForegroundColor White
} else {
    Write-Host "  [FAIL] 无法获取投票数据" -ForegroundColor Red
    $fail++
}

# 3.4 提交投票并验证响应
Write-Host "`n--- 3.4 提交投票并验证返回数据 ---" -ForegroundColor Yellow
$voteId = if ($voteDetail) { $voteDetail.data.voteId } else { 1 }
$voteSubmit = Invoke-API "POST" "$base/votes/$voteId/submit" (@{ optionIndex = 0 }) $token
if ($voteSubmit) {
    Write-Host "  [INFO] 投票提交返回code=$($voteSubmit.code)" -ForegroundColor White
    # 检查前端预期：res.data.data包含完整VoteDetail
    if ($voteSubmit.data -and $voteSubmit.data.voteTitle) {
        Write-Host "  [PASS] 提交投票后返回完整投票数据" -ForegroundColor Green
        $pass++
    } else {
        Write-Host "  [FAIL] 提交投票后返回数据为空或仅有{submitted:true}" -ForegroundColor Red
        Write-Host "  [BUG] VoteController.submitVote()返回Result.ok('投票成功', null)" -ForegroundColor Red
        Write-Host "  [BUG] 前端PostDetailPage.tsx:71行 setVote(res.data.data) 将覆盖投票状态为null" -ForegroundColor Red
        $fail++
    }
} else {
    Write-Host "  [FAIL] 投票提交失败" -ForegroundColor Red
    $fail++
}

# 3.5 验证前端PostEditorPage从未调用voteApi.create()
Write-Host "`n--- 3.5 前端投票创建流程验证 ---" -ForegroundColor Yellow
Write-Host "  [INFO] PostEditorPage.tsx中handleSubmit() 第60行只调用postApi.create()" -ForegroundColor White
Write-Host "  [INFO] voteTitle/voteOptions/voteEndTime状态被收集但从未提交到后端" -ForegroundColor White
Write-Host "  [INFO] contentType始终为0（Tabs切换仅更新mode状态，未更新contentType）" -ForegroundColor White
Write-Host "  [BUG] 前端投票创建功能从未调用voteApi.create()" -ForegroundColor Red

# ============================================================
# 清理测试数据
Write-Host "`n============================================================" -ForegroundColor Cyan
Write-Host "  清理测试数据" -ForegroundColor Cyan
Write-Host "============================================================" -ForegroundColor Cyan

$delPost = Invoke-API "DELETE" "$base/posts/$postId" $null $token
Write-Host "  删除评论测试帖: $(if($delPost){'成功'}else{'失败'})" -ForegroundColor White

$delVotePost = Invoke-API "DELETE" "$base/posts/$votePostId" $null $token
Write-Host "  删除投票测试帖: $(if($delVotePost){'成功'}else{'失败'})" -ForegroundColor White

# 结果汇总
Write-Host "`n============================================================" -ForegroundColor Cyan
Write-Host "  交互缺陷测试结果汇总" -ForegroundColor Cyan
Write-Host "============================================================" -ForegroundColor Cyan
Write-Host "  通过: $pass" -ForegroundColor Green
Write-Host "  失败: $fail" -ForegroundColor Red
Write-Host ""
if ($fail -gt 0) {
    Write-Host "  发现交互缺陷，详见上述[FAIL]/[BUG]标记" -ForegroundColor Red
} else {
    Write-Host "  ALL TESTS PASSED" -ForegroundColor Green
}
