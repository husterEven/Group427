package com.forum.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.forum.common.PageResult;
import com.forum.common.SecurityUtil;
import com.forum.dto.AuditActionRequest;
import com.forum.dto.ReportHandleRequest;
import com.forum.entity.*;
import com.forum.mapper.*;
import com.forum.service.AdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AdminServiceImpl implements AdminService {

    private final AuditQueueMapper auditQueueMapper;
    private final ReportMapper reportMapper;
    private final UserPunishmentMapper userPunishmentMapper;
    private final UserMapper userMapper;
    private final PostMapper postMapper;
    private final CommentMapper commentMapper;
    private final AttachmentMapper attachmentMapper;
    private final SectionMapper sectionMapper;
    private final ZoneMapper zoneMapper;
    private final SecurityUtil securityUtil;

    @Override
    public PageResult<AuditQueue> getAuditQueue(int page, int pageSize) {
        QueryWrapper<AuditQueue> wrapper = new QueryWrapper<>();
        wrapper.eq("audit_status", 0)
               .orderByAsc("created_at");
        Page<AuditQueue> mpPage = new Page<>(page, pageSize);
        Page<AuditQueue> result = auditQueueMapper.selectPage(mpPage, wrapper);
        return PageResult.of(result.getRecords(), result.getTotal(), page, pageSize);
    }

    @Override
    @Transactional
    public void audit(Long auditItemId, AuditActionRequest req) {
        AuditQueue item = auditQueueMapper.selectById(auditItemId);
        if (item == null) {
            throw new RuntimeException("审核项不存在");
        }

        item.setAuditStatus(req.getAuditStatus());
        item.setAuditorId(securityUtil.getCurrentUserId());
        item.setAuditedAt(LocalDateTime.now());
        if (req.getAuditComment() != null) {
            item.setAuditComment(req.getAuditComment());
        }
        auditQueueMapper.updateById(item);

        Integer contentType = item.getContentType();
        Long contentId = item.getContentId();
        Integer targetStatus = req.getAuditStatus();

        if (contentType == 0) {
            Post post = new Post();
            post.setPostId(contentId);
            post.setAuditStatus(targetStatus);
            postMapper.updateById(post);
        } else if (contentType == 1) {
            Comment comment = new Comment();
            comment.setCommentId(contentId);
            comment.setAuditStatus(targetStatus);
            commentMapper.updateById(comment);
        } else if (contentType == 2) {
            Attachment attachment = new Attachment();
            attachment.setAttachmentId(contentId);
            attachment.setAuditStatus(targetStatus);
            attachmentMapper.updateById(attachment);
        }
    }

    @Override
    public PageResult<Report> getReports(int page, int pageSize) {
        QueryWrapper<Report> wrapper = new QueryWrapper<>();
        wrapper.orderByDesc("created_at");
        Page<Report> mpPage = new Page<>(page, pageSize);
        Page<Report> result = reportMapper.selectPage(mpPage, wrapper);
        return PageResult.of(result.getRecords(), result.getTotal(), page, pageSize);
    }

    @Override
    @Transactional
    public void handleReport(Long reportId, ReportHandleRequest req) {
        Report report = reportMapper.selectById(reportId);
        if (report == null) {
            throw new RuntimeException("举报不存在");
        }

        report.setStatus(req.getStatus());
        report.setHandlerId(securityUtil.getCurrentUserId());
        report.setHandleResult(req.getHandleResult());
        report.setHandledAt(LocalDateTime.now());
        reportMapper.updateById(report);

        Long targetUserId = findTargetUserId(report.getTargetType(), report.getTargetId());

        int handleResult = req.getHandleResult();

        if (handleResult == 1) {
            createPunishment(targetUserId, 0, req);
        } else if (handleResult == 2) {
            softDeleteContent(report.getTargetType(), report.getTargetId());
            createPunishment(targetUserId, 1, req);
        } else if (handleResult == 3) {
            User user = userMapper.selectById(targetUserId);
            if (user != null) {
                user.setIsBanned(1);
                userMapper.updateById(user);
            }
            createPunishment(targetUserId, 2, req);
        }
    }

    @Override
    public PageResult<UserPunishment> getPunishments(int page, int pageSize) {
        QueryWrapper<UserPunishment> wrapper = new QueryWrapper<>();
        wrapper.orderByDesc("created_at");
        Page<UserPunishment> mpPage = new Page<>(page, pageSize);
        Page<UserPunishment> result = userPunishmentMapper.selectPage(mpPage, wrapper);
        return PageResult.of(result.getRecords(), result.getTotal(), page, pageSize);
    }

    @Override
    @Transactional
    public void revokePunishment(Long punishmentId) {
        UserPunishment punishment = userPunishmentMapper.selectById(punishmentId);
        if (punishment == null) {
            throw new RuntimeException("处罚记录不存在");
        }

        punishment.setIsActive(0);
        userPunishmentMapper.updateById(punishment);

        if (punishment.getPunishmentType() != null && punishment.getPunishmentType() == 2) {
            User user = userMapper.selectById(punishment.getUserId());
            if (user != null) {
                user.setIsBanned(0);
                userMapper.updateById(user);
            }
        }
    }

    @Override
    public Map<String, Object> getDashboard() {
        Map<String, Object> map = new HashMap<>();
        map.put("dau", 12580);
        map.put("mau", 89420);

        LocalDate today = LocalDate.now();

        QueryWrapper<Post> todayPostWrapper = new QueryWrapper<>();
        todayPostWrapper.apply("DATE(publish_time) = {0}", today);
        map.put("todayPosts", postMapper.selectCount(todayPostWrapper));

        QueryWrapper<Comment> todayCommentWrapper = new QueryWrapper<>();
        todayCommentWrapper.apply("DATE(publish_time) = {0}", today);
        map.put("todayComments", commentMapper.selectCount(todayCommentWrapper));

        QueryWrapper<User> todayUserWrapper = new QueryWrapper<>();
        todayUserWrapper.apply("DATE(created_at) = {0}", today);
        map.put("newUsers", userMapper.selectCount(todayUserWrapper));

        map.put("totalPosts", postMapper.selectCount(null));
        map.put("totalComments", commentMapper.selectCount(null));

        QueryWrapper<AuditQueue> pendingWrapper = new QueryWrapper<>();
        pendingWrapper.eq("audit_status", 0);
        map.put("pendingAudits", auditQueueMapper.selectCount(pendingWrapper));

        return map;
    }

    private Long findTargetUserId(Integer targetType, Long targetId) {
        if (targetType == null) {
            return null;
        }
        if (targetType == 0) {
            Post post = postMapper.selectById(targetId);
            return post != null ? post.getAuthorId() : null;
        } else if (targetType == 1) {
            Comment comment = commentMapper.selectById(targetId);
            return comment != null ? comment.getAuthorId() : null;
        }
        return null;
    }

    private void softDeleteContent(Integer targetType, Long targetId) {
        if (targetType == 0) {
            Post post = new Post();
            post.setPostId(targetId);
            post.setIsDeleted(1);
            postMapper.updateById(post);
        } else if (targetType == 1) {
            Comment comment = new Comment();
            comment.setCommentId(targetId);
            comment.setIsDeleted(1);
            commentMapper.updateById(comment);
        }
    }

    private void createPunishment(Long userId, int punishmentType, ReportHandleRequest req) {
        if (userId == null) {
            return;
        }
        UserPunishment punishment = new UserPunishment();
        punishment.setUserId(userId);
        punishment.setPunishmentType(req.getPunishmentType() != null ? req.getPunishmentType() : punishmentType);
        punishment.setReason("举报处理");
        punishment.setOperatorId(securityUtil.getCurrentUserId());
        punishment.setDurationDays(req.getDurationDays() != null ? req.getDurationDays() : 0);
        punishment.setIsActive(1);
        punishment.setCreatedAt(LocalDateTime.now());
        if (req.getDurationDays() != null && req.getDurationDays() > 0) {
            punishment.setExpireAt(LocalDateTime.now().plusDays(req.getDurationDays()));
        }
        userPunishmentMapper.insert(punishment);
    }
}
