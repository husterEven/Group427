package com.forum.service.impl;

import com.forum.common.SecurityUtil;
import com.forum.dto.AuditActionRequest;
import com.forum.dto.ReportHandleRequest;
import com.forum.entity.*;
import com.forum.mapper.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AdminServiceImpl 管理服务 控制结构单元测试")
class AdminServiceImplTest {

    @Mock private AuditQueueMapper auditQueueMapper;
    @Mock private ReportMapper reportMapper;
    @Mock private UserPunishmentMapper userPunishmentMapper;
    @Mock private UserMapper userMapper;
    @Mock private PostMapper postMapper;
    @Mock private CommentMapper commentMapper;
    @Mock private AttachmentMapper attachmentMapper;
    @Mock private SectionMapper sectionMapper;
    @Mock private ZoneMapper zoneMapper;
    @Mock private SecurityUtil securityUtil;

    @InjectMocks
    private AdminServiceImpl adminService;

    @BeforeEach
    void setUp() {
        lenient().when(securityUtil.getCurrentUserId()).thenReturn(1L);
    }

    @Nested
    @DisplayName("audit() - 审核操作 (if-else if-else: contentType 分支)")
    class Audit {

        @Test
        @DisplayName("审核项不存在应抛出异常 (if: item==null=true)")
        void auditItemNotFound_shouldThrow() {
            when(auditQueueMapper.selectById(999L)).thenReturn(null);

            AuditActionRequest req = new AuditActionRequest();
            req.setAuditStatus(1);

            RuntimeException ex = assertThrows(RuntimeException.class,
                    () -> adminService.audit(999L, req));
            assertEquals("审核项不存在", ex.getMessage());
        }

        @Test
        @DisplayName("审核comment注释 (if: auditComment!=null=true)")
        void auditWithComment_shouldSaveComment() {
            AuditQueue item = new AuditQueue();
            item.setAuditItemId(1L);
            item.setContentType(0);
            item.setContentId(10L);
            when(auditQueueMapper.selectById(1L)).thenReturn(item);

            AuditActionRequest req = new AuditActionRequest();
            req.setAuditStatus(1);
            req.setAuditComment("内容合规");

            adminService.audit(1L, req);

            ArgumentCaptor<AuditQueue> captor = ArgumentCaptor.forClass(AuditQueue.class);
            verify(auditQueueMapper).updateById(captor.capture());
            assertEquals("内容合规", captor.getValue().getAuditComment());
        }

        @Test
        @DisplayName("审核Post (if: contentType==0=true)")
        void auditPost_shouldUpdatePostStatus() {
            AuditQueue item = new AuditQueue();
            item.setAuditItemId(1L);
            item.setContentType(0);
            item.setContentId(10L);
            when(auditQueueMapper.selectById(1L)).thenReturn(item);

            AuditActionRequest req = new AuditActionRequest();
            req.setAuditStatus(1);

            adminService.audit(1L, req);

            ArgumentCaptor<Post> captor = ArgumentCaptor.forClass(Post.class);
            verify(postMapper).updateById(captor.capture());
            assertEquals(10L, captor.getValue().getPostId());
            assertEquals(1, captor.getValue().getAuditStatus());
        }

        @Test
        @DisplayName("审核Comment (else if: contentType==1=true)")
        void auditComment_shouldUpdateCommentStatus() {
            AuditQueue item = new AuditQueue();
            item.setAuditItemId(1L);
            item.setContentType(1);
            item.setContentId(20L);
            when(auditQueueMapper.selectById(1L)).thenReturn(item);

            AuditActionRequest req = new AuditActionRequest();
            req.setAuditStatus(2);

            adminService.audit(1L, req);

            ArgumentCaptor<Comment> captor = ArgumentCaptor.forClass(Comment.class);
            verify(commentMapper).updateById(captor.capture());
            assertEquals(20L, captor.getValue().getCommentId());
            assertEquals(2, captor.getValue().getAuditStatus());
        }

        @Test
        @DisplayName("审核Attachment (else if: contentType==2=true)")
        void auditAttachment_shouldUpdateAttachmentStatus() {
            AuditQueue item = new AuditQueue();
            item.setAuditItemId(1L);
            item.setContentType(2);
            item.setContentId(30L);
            when(auditQueueMapper.selectById(1L)).thenReturn(item);

            AuditActionRequest req = new AuditActionRequest();
            req.setAuditStatus(1);

            adminService.audit(1L, req);

            ArgumentCaptor<Attachment> captor = ArgumentCaptor.forClass(Attachment.class);
            verify(attachmentMapper).updateById(captor.capture());
            assertEquals(30L, captor.getValue().getAttachmentId());
            assertEquals(1, captor.getValue().getAuditStatus());
        }

        @Test
        @DisplayName("未知contentType=99不做任何类型更新 (else: 不进入任何分支)")
        void auditUnknownContentType_shouldSkipUpdates() {
            AuditQueue item = new AuditQueue();
            item.setAuditItemId(1L);
            item.setContentType(99);
            item.setContentId(40L);
            when(auditQueueMapper.selectById(1L)).thenReturn(item);

            AuditActionRequest req = new AuditActionRequest();
            req.setAuditStatus(1);

            adminService.audit(1L, req);

            verify(postMapper, never()).updateById(any());
            verify(commentMapper, never()).updateById(any());
            verify(attachmentMapper, never()).updateById(any());
        }

        @Test
        @DisplayName("审核通过(auditStatus=1)")
        void auditApprove_shouldSetStatus1() {
            AuditQueue item = new AuditQueue();
            item.setAuditItemId(1L);
            item.setContentType(0);
            item.setContentId(10L);
            when(auditQueueMapper.selectById(1L)).thenReturn(item);

            AuditActionRequest req = new AuditActionRequest();
            req.setAuditStatus(1);

            adminService.audit(1L, req);

            ArgumentCaptor<AuditQueue> captor = ArgumentCaptor.forClass(AuditQueue.class);
            verify(auditQueueMapper).updateById(captor.capture());
            assertEquals(1, captor.getValue().getAuditStatus());
            assertEquals(1L, captor.getValue().getAuditorId());
            assertNotNull(captor.getValue().getAuditedAt());
        }
    }

    @Nested
    @DisplayName("handleReport() - 举报处理 (if-else if-else: handleResult 分支)")
    class HandleReport {

        @Test
        @DisplayName("举报不存在应抛出异常 (if: report==null=true)")
        void reportNotFound_shouldThrow() {
            when(reportMapper.selectById(999L)).thenReturn(null);

            ReportHandleRequest req = new ReportHandleRequest();
            req.setStatus(1);
            req.setHandleResult(0);

            RuntimeException ex = assertThrows(RuntimeException.class,
                    () -> adminService.handleReport(999L, req));
            assertEquals("举报不存在", ex.getMessage());
        }

        @Test
        @DisplayName("仅警告(handleResult=1) (if: handleResult==1=true)")
        void handleResultWarning_shouldOnlyCreatePunishment() {
            Report report = new Report();
            report.setReportId(100L);
            report.setTargetType(0);
            report.setTargetId(10L);
            when(reportMapper.selectById(100L)).thenReturn(report);

            Post post = new Post();
            post.setPostId(10L);
            post.setAuthorId(5L);
            when(postMapper.selectById(10L)).thenReturn(post);

            ReportHandleRequest req = new ReportHandleRequest();
            req.setStatus(1);
            req.setHandleResult(1);

            adminService.handleReport(100L, req);

            verify(userPunishmentMapper).insert(any(UserPunishment.class));
            verify(postMapper, never()).updateById(any());
            verify(commentMapper, never()).updateById(any());
            verify(userMapper, never()).updateById(any());
        }

        @Test
        @DisplayName("警告+删帖(handleResult=2) (else if: handleResult==2=true)")
        void handleResultDeletePost_shouldSoftDeleteAndPunish() {
            Report report = new Report();
            report.setReportId(100L);
            report.setTargetType(0);
            report.setTargetId(10L);
            when(reportMapper.selectById(100L)).thenReturn(report);

            Post post = new Post();
            post.setPostId(10L);
            post.setAuthorId(5L);
            when(postMapper.selectById(10L)).thenReturn(post);

            ReportHandleRequest req = new ReportHandleRequest();
            req.setStatus(1);
            req.setHandleResult(2);

            adminService.handleReport(100L, req);

            ArgumentCaptor<Post> captor = ArgumentCaptor.forClass(Post.class);
            verify(postMapper).updateById(captor.capture());
            assertEquals(1, captor.getValue().getIsDeleted());
            verify(userPunishmentMapper).insert(any(UserPunishment.class));
        }

        @Test
        @DisplayName("封号(handleResult=3) (else if: handleResult==3=true)")
        void handleResultBan_shouldBanUserAndPunish() {
            Report report = new Report();
            report.setReportId(100L);
            report.setTargetType(0);
            report.setTargetId(10L);
            when(reportMapper.selectById(100L)).thenReturn(report);

            Post post = new Post();
            post.setPostId(10L);
            post.setAuthorId(5L);
            when(postMapper.selectById(10L)).thenReturn(post);

            User user = new User();
            user.setUserId(5L);
            when(userMapper.selectById(5L)).thenReturn(user);

            ReportHandleRequest req = new ReportHandleRequest();
            req.setStatus(1);
            req.setHandleResult(3);

            adminService.handleReport(100L, req);

            ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
            verify(userMapper).updateById(captor.capture());
            assertEquals(1, captor.getValue().getIsBanned());
            verify(userPunishmentMapper).insert(any(UserPunishment.class));
        }

        @Test
        @DisplayName("封号但用户不存在应跳过封禁 (if: user!=null=false)")
        void handleResultBan_userNotFound_shouldSkipBan() {
            Report report = new Report();
            report.setReportId(100L);
            report.setTargetType(0);
            report.setTargetId(10L);
            when(reportMapper.selectById(100L)).thenReturn(report);

            Post post = new Post();
            post.setPostId(10L);
            post.setAuthorId(5L);
            when(postMapper.selectById(10L)).thenReturn(post);
            when(userMapper.selectById(5L)).thenReturn(null);

            ReportHandleRequest req = new ReportHandleRequest();
            req.setStatus(1);
            req.setHandleResult(3);

            adminService.handleReport(100L, req);

            verify(userPunishmentMapper).insert(any(UserPunishment.class));
            verify(userMapper, never()).updateById(any());
        }

        @Test
        @DisplayName("举报处理应对Comment类型进行软删除 (else if: targetType==1)")
        void handleResultDeleteComment_shouldSoftDeleteComment() {
            Report report = new Report();
            report.setReportId(100L);
            report.setTargetType(1);
            report.setTargetId(20L);
            when(reportMapper.selectById(100L)).thenReturn(report);

            Comment comment = new Comment();
            comment.setCommentId(20L);
            comment.setAuthorId(5L);
            when(commentMapper.selectById(20L)).thenReturn(comment);

            ReportHandleRequest req = new ReportHandleRequest();
            req.setStatus(1);
            req.setHandleResult(2);

            adminService.handleReport(100L, req);

            ArgumentCaptor<Comment> captor = ArgumentCaptor.forClass(Comment.class);
            verify(commentMapper).updateById(captor.capture());
            assertEquals(1, captor.getValue().getIsDeleted());
        }

        @Test
        @DisplayName("举报处理应更新举报状态和处理信息")
        void handleReport_shouldUpdateReportStatus() {
            Report report = new Report();
            report.setReportId(100L);
            report.setTargetType(0);
            report.setTargetId(10L);
            when(reportMapper.selectById(100L)).thenReturn(report);

            Post post = new Post();
            post.setPostId(10L);
            post.setAuthorId(5L);
            when(postMapper.selectById(10L)).thenReturn(post);

            ReportHandleRequest req = new ReportHandleRequest();
            req.setStatus(2);
            req.setHandleResult(1);

            adminService.handleReport(100L, req);

            ArgumentCaptor<Report> captor = ArgumentCaptor.forClass(Report.class);
            verify(reportMapper).updateById(captor.capture());
            assertEquals(2, captor.getValue().getStatus());
            assertEquals(1L, captor.getValue().getHandlerId());
            assertNotNull(captor.getValue().getHandledAt());
        }
    }

    @Nested
    @DisplayName("revokePunishment() - 撤销处罚 (if: punishmentType==2)")
    class RevokePunishment {

        @Test
        @DisplayName("处罚不存在应抛出异常 (if: punishment==null=true)")
        void punishmentNotFound_shouldThrow() {
            when(userPunishmentMapper.selectById(999L)).thenReturn(null);

            RuntimeException ex = assertThrows(RuntimeException.class,
                    () -> adminService.revokePunishment(999L));
            assertEquals("处罚记录不存在", ex.getMessage());
        }

        @Test
        @DisplayName("撤销封号(punishmentType=2)应解封用户 (if: type==2=true)")
        void revokeBan_shouldUnbanUser() {
            UserPunishment punishment = new UserPunishment();
            punishment.setPunishmentId(1L);
            punishment.setUserId(5L);
            punishment.setPunishmentType(2);
            when(userPunishmentMapper.selectById(1L)).thenReturn(punishment);

            User user = new User();
            user.setUserId(5L);
            when(userMapper.selectById(5L)).thenReturn(user);

            adminService.revokePunishment(1L);

            ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
            verify(userMapper).updateById(captor.capture());
            assertEquals(0, captor.getValue().getIsBanned());

            ArgumentCaptor<UserPunishment> pCaptor = ArgumentCaptor.forClass(UserPunishment.class);
            verify(userPunishmentMapper).updateById(pCaptor.capture());
            assertEquals(0, pCaptor.getValue().getIsActive());
        }

        @Test
        @DisplayName("撤销警告(punishmentType=0)不应解封 (if: type==2=false)")
        void revokeWarning_shouldNotUnban() {
            UserPunishment punishment = new UserPunishment();
            punishment.setPunishmentId(1L);
            punishment.setUserId(5L);
            punishment.setPunishmentType(0);
            when(userPunishmentMapper.selectById(1L)).thenReturn(punishment);

            adminService.revokePunishment(1L);

            verify(userMapper, never()).updateById(any());

            ArgumentCaptor<UserPunishment> pCaptor = ArgumentCaptor.forClass(UserPunishment.class);
            verify(userPunishmentMapper).updateById(pCaptor.capture());
            assertEquals(0, pCaptor.getValue().getIsActive());
        }

        @Test
        @DisplayName("撤销处罚但用户不存在应跳过解封 (if: user!=null=false)")
        void revokeBan_userNotFound_shouldSkipUnban() {
            UserPunishment punishment = new UserPunishment();
            punishment.setPunishmentId(1L);
            punishment.setUserId(5L);
            punishment.setPunishmentType(2);
            when(userPunishmentMapper.selectById(1L)).thenReturn(punishment);
            when(userMapper.selectById(5L)).thenReturn(null);

            adminService.revokePunishment(1L);

            verify(userMapper, never()).updateById(any());
            verify(userPunishmentMapper).updateById(any());
        }

        @Test
        @DisplayName("撤销punishmentType=null不应解封 (if: type!=null && type==2 -> null短路)")
        void revokeNullType_shouldNotUnban() {
            UserPunishment punishment = new UserPunishment();
            punishment.setPunishmentId(1L);
            punishment.setUserId(5L);
            punishment.setPunishmentType(null);
            when(userPunishmentMapper.selectById(1L)).thenReturn(punishment);

            adminService.revokePunishment(1L);

            verify(userMapper, never()).updateById(any());
        }
    }

    @Nested
    @DisplayName("createPunishment() - 创建处罚 (私有方法，通过handleReport间接测试)")
    class CreatePunishment {

        @Test
        @DisplayName("user为null时应直接返回 (if: userId==null -> return)")
        void nullUserId_shouldReturn() {
            Report report = new Report();
            report.setReportId(100L);
            report.setTargetType(null); // 无法获取到用户ID
            report.setTargetId(10L);
            when(reportMapper.selectById(100L)).thenReturn(report);

            ReportHandleRequest req = new ReportHandleRequest();
            req.setStatus(1);
            req.setHandleResult(1);

            adminService.handleReport(100L, req);

            verify(userPunishmentMapper, never()).insert(any());
        }

        @Test
        @DisplayName("带durationDays>0应设置expireAt (if: durationDays!=null && >0=true)")
        void withDurationDays_shouldSetExpireAt() {
            Report report = new Report();
            report.setReportId(100L);
            report.setTargetType(0);
            report.setTargetId(10L);
            when(reportMapper.selectById(100L)).thenReturn(report);

            Post post = new Post();
            post.setPostId(10L);
            post.setAuthorId(5L);
            when(postMapper.selectById(10L)).thenReturn(post);

            ReportHandleRequest req = new ReportHandleRequest();
            req.setStatus(1);
            req.setHandleResult(1);
            req.setDurationDays(7);

            adminService.handleReport(100L, req);

            ArgumentCaptor<UserPunishment> captor = ArgumentCaptor.forClass(UserPunishment.class);
            verify(userPunishmentMapper).insert(captor.capture());
            UserPunishment saved = captor.getValue();
            assertEquals(5L, saved.getUserId());
            assertEquals(1, saved.getIsActive());
            assertEquals(7, saved.getDurationDays());
            assertNotNull(saved.getExpireAt());
        }

        @Test
        @DisplayName("durationDays=0不应设置expireAt (if: >0=false)")
        void zeroDurationDays_shouldNotSetExpireAt() {
            Report report = new Report();
            report.setReportId(100L);
            report.setTargetType(0);
            report.setTargetId(10L);
            when(reportMapper.selectById(100L)).thenReturn(report);

            Post post = new Post();
            post.setPostId(10L);
            post.setAuthorId(5L);
            when(postMapper.selectById(10L)).thenReturn(post);

            ReportHandleRequest req = new ReportHandleRequest();
            req.setStatus(1);
            req.setHandleResult(1);
            req.setDurationDays(0);

            adminService.handleReport(100L, req);

            ArgumentCaptor<UserPunishment> captor = ArgumentCaptor.forClass(UserPunishment.class);
            verify(userPunishmentMapper).insert(captor.capture());
            assertNull(captor.getValue().getExpireAt());
        }

        @Test
        @DisplayName("punishmentType应由req优先 (三元: req.punishmentType!=null?req.punishmentType:defaultType)")
        void punishmentTypeFromRequest_shouldTakePriority() {
            Report report = new Report();
            report.setReportId(100L);
            report.setTargetType(0);
            report.setTargetId(10L);
            when(reportMapper.selectById(100L)).thenReturn(report);

            Post post = new Post();
            post.setPostId(10L);
            post.setAuthorId(5L);
            when(postMapper.selectById(10L)).thenReturn(post);

            ReportHandleRequest req = new ReportHandleRequest();
            req.setStatus(1);
            req.setHandleResult(1);
            req.setPunishmentType(99);

            adminService.handleReport(100L, req);

            ArgumentCaptor<UserPunishment> captor = ArgumentCaptor.forClass(UserPunishment.class);
            verify(userPunishmentMapper).insert(captor.capture());
            assertEquals(99, captor.getValue().getPunishmentType());
        }
    }

    @Nested
    @DisplayName("findTargetUserId() - 查找目标用户ID (私有方法)")
    class FindTargetUserId {

        @Test
        @DisplayName("targetType=null应返回null (if: null==null -> return null)")
        void nullTargetType_shouldReturnNull() {
            Report report = new Report();
            report.setReportId(100L);
            report.setTargetType(null);
            report.setTargetId(10L);
            when(reportMapper.selectById(100L)).thenReturn(report);

            ReportHandleRequest req = new ReportHandleRequest();
            req.setStatus(1);
            req.setHandleResult(1);

            adminService.handleReport(100L, req);

            verify(userPunishmentMapper, never()).insert(any());
        }

        @Test
        @DisplayName("targetType=0 (Post)应返回作者ID")
        void postTarget_shouldReturnAuthorId() {
            Report report = new Report();
            report.setReportId(100L);
            report.setTargetType(0);
            report.setTargetId(10L);
            when(reportMapper.selectById(100L)).thenReturn(report);

            Post post = new Post();
            post.setPostId(10L);
            post.setAuthorId(7L);
            when(postMapper.selectById(10L)).thenReturn(post);

            ReportHandleRequest req = new ReportHandleRequest();
            req.setStatus(1);
            req.setHandleResult(1);

            adminService.handleReport(100L, req);

            ArgumentCaptor<UserPunishment> captor = ArgumentCaptor.forClass(UserPunishment.class);
            verify(userPunishmentMapper).insert(captor.capture());
            assertEquals(7L, captor.getValue().getUserId());
        }

        @Test
        @DisplayName("targetType=1 (Comment)应返回评论作者ID")
        void commentTarget_shouldReturnAuthorId() {
            Report report = new Report();
            report.setReportId(100L);
            report.setTargetType(1);
            report.setTargetId(20L);
            when(reportMapper.selectById(100L)).thenReturn(report);

            Comment comment = new Comment();
            comment.setCommentId(20L);
            comment.setAuthorId(8L);
            when(commentMapper.selectById(20L)).thenReturn(comment);

            ReportHandleRequest req = new ReportHandleRequest();
            req.setStatus(1);
            req.setHandleResult(1);

            adminService.handleReport(100L, req);

            ArgumentCaptor<UserPunishment> captor = ArgumentCaptor.forClass(UserPunishment.class);
            verify(userPunishmentMapper).insert(captor.capture());
            assertEquals(8L, captor.getValue().getUserId());
        }

        @Test
        @DisplayName("Post不存在应返回null (三元: post!=null?authorId:null)")
        void postNotFound_shouldReturnNull() {
            Report report = new Report();
            report.setReportId(100L);
            report.setTargetType(0);
            report.setTargetId(10L);
            when(reportMapper.selectById(100L)).thenReturn(report);
            when(postMapper.selectById(10L)).thenReturn(null);

            ReportHandleRequest req = new ReportHandleRequest();
            req.setStatus(1);
            req.setHandleResult(1);

            adminService.handleReport(100L, req);

            verify(userPunishmentMapper, never()).insert(any());
        }
    }

    @Nested
    @DisplayName("getDashboard() - 数据仪表盘")
    class GetDashboard {

        @Test
        @DisplayName("仪表盘应包含所有统计字段")
        void dashboard_shouldContainAllStats() {
            when(postMapper.selectCount(any())).thenReturn(10L);
            when(commentMapper.selectCount(any())).thenReturn(25L);
            when(userMapper.selectCount(any())).thenReturn(3L);

            Map<String, Object> result = adminService.getDashboard();

            assertNotNull(result);
            assertTrue(result.containsKey("dau"));
            assertTrue(result.containsKey("mau"));
            assertTrue(result.containsKey("todayPosts"));
            assertTrue(result.containsKey("todayComments"));
            assertTrue(result.containsKey("newUsers"));
            assertTrue(result.containsKey("totalPosts"));
            assertTrue(result.containsKey("totalComments"));
            assertTrue(result.containsKey("pendingAudits"));
            assertEquals(12580, result.get("dau"));
            assertEquals(89420, result.get("mau"));
        }
    }
}
