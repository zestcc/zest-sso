package cn.zest.sso.server.service;

import cn.zest.sso.common.exception.ErrorCode;
import cn.zest.sso.common.exception.SsoException;
import cn.zest.sso.server.domain.entity.SsoAccessReviewCampaign;
import cn.zest.sso.server.domain.entity.SsoAccessReviewItem;
import cn.zest.sso.server.domain.entity.SsoRole;
import cn.zest.sso.server.domain.entity.SsoUser;
import cn.zest.sso.server.domain.entity.SsoUserRole;
import cn.zest.sso.server.domain.mapper.SsoAccessReviewCampaignMapper;
import cn.zest.sso.server.domain.mapper.SsoAccessReviewItemMapper;
import cn.zest.sso.server.domain.mapper.SsoRoleMapper;
import cn.zest.sso.server.domain.mapper.SsoUserMapper;
import cn.zest.sso.server.domain.mapper.SsoUserRoleMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AccessReviewService {

    private final SsoAccessReviewCampaignMapper campaignMapper;
    private final SsoAccessReviewItemMapper itemMapper;
    private final SsoUserMapper userMapper;
    private final SsoUserRoleMapper userRoleMapper;
    private final SsoRoleMapper roleMapper;

    @Transactional(rollbackFor = Exception.class)
    public SsoAccessReviewCampaign createCampaign(String name, String description, LocalDateTime dueAt, String createdBy) {
        SsoAccessReviewCampaign campaign = new SsoAccessReviewCampaign();
        campaign.setName(name);
        campaign.setDescription(description);
        campaign.setDueAt(dueAt);
        campaign.setCreatedBy(createdBy);
        campaign.setStatus(SsoAccessReviewCampaign.STATUS_DRAFT);
        campaignMapper.insert(campaign);
        return campaign;
    }

    @Transactional(rollbackFor = Exception.class)
    public SsoAccessReviewCampaign activateCampaign(Long campaignId) {
        SsoAccessReviewCampaign campaign = requireCampaign(campaignId);
        if (!SsoAccessReviewCampaign.STATUS_DRAFT.equals(campaign.getStatus())) {
            throw new SsoException(ErrorCode.BAD_REQUEST, "仅草稿活动可激活");
        }
        populateItems(campaign.getId());
        campaign.setStatus(SsoAccessReviewCampaign.STATUS_ACTIVE);
        campaignMapper.updateById(campaign);
        return campaign;
    }

    @Transactional(rollbackFor = Exception.class)
    public SsoAccessReviewItem decide(Long itemId, String decision, String reviewer) {
        SsoAccessReviewItem item = itemMapper.selectById(itemId);
        if (item == null) {
            throw new SsoException(ErrorCode.NOT_FOUND, "复核项不存在");
        }
        if (!SsoAccessReviewItem.DECISION_PENDING.equals(item.getDecision())) {
            throw new SsoException(ErrorCode.BAD_REQUEST, "该复核项已处理");
        }
        if (!SsoAccessReviewItem.DECISION_APPROVED.equals(decision)
                && !SsoAccessReviewItem.DECISION_REVOKED.equals(decision)) {
            throw new SsoException(ErrorCode.BAD_REQUEST, "无效决策");
        }
        item.setDecision(decision);
        item.setReviewer(reviewer);
        item.setReviewedAt(LocalDateTime.now());
        itemMapper.updateById(item);

        if (SsoAccessReviewItem.DECISION_REVOKED.equals(decision)) {
            revokeRole(item.getUserId(), item.getRoleCode());
        }
        maybeCompleteCampaign(item.getCampaignId());
        return item;
    }

    public Page<SsoAccessReviewCampaign> pageCampaigns(int page, int size, String status) {
        LambdaQueryWrapper<SsoAccessReviewCampaign> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(status)) {
            wrapper.eq(SsoAccessReviewCampaign::getStatus, status);
        }
        wrapper.orderByDesc(SsoAccessReviewCampaign::getCreateTime);
        return campaignMapper.selectPage(new Page<>(page, size), wrapper);
    }

    public Page<SsoAccessReviewItem> pageItems(Long campaignId, int page, int size, String decision) {
        LambdaQueryWrapper<SsoAccessReviewItem> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SsoAccessReviewItem::getCampaignId, campaignId);
        if (StringUtils.hasText(decision)) {
            wrapper.eq(SsoAccessReviewItem::getDecision, decision);
        }
        wrapper.orderByAsc(SsoAccessReviewItem::getUsername);
        return itemMapper.selectPage(new Page<>(page, size), wrapper);
    }

    public Map<String, Object> campaignSummary(Long campaignId) {
        SsoAccessReviewCampaign campaign = requireCampaign(campaignId);
        long pending = itemMapper.selectCount(new LambdaQueryWrapper<SsoAccessReviewItem>()
                .eq(SsoAccessReviewItem::getCampaignId, campaignId)
                .eq(SsoAccessReviewItem::getDecision, SsoAccessReviewItem.DECISION_PENDING));
        long approved = itemMapper.selectCount(new LambdaQueryWrapper<SsoAccessReviewItem>()
                .eq(SsoAccessReviewItem::getCampaignId, campaignId)
                .eq(SsoAccessReviewItem::getDecision, SsoAccessReviewItem.DECISION_APPROVED));
        long revoked = itemMapper.selectCount(new LambdaQueryWrapper<SsoAccessReviewItem>()
                .eq(SsoAccessReviewItem::getCampaignId, campaignId)
                .eq(SsoAccessReviewItem::getDecision, SsoAccessReviewItem.DECISION_REVOKED));
        Map<String, Object> summary = new HashMap<>();
        summary.put("campaign", campaign);
        summary.put("pending", pending);
        summary.put("approved", approved);
        summary.put("revoked", revoked);
        return summary;
    }

    private void populateItems(Long campaignId) {
        List<SsoUserRole> userRoles = userRoleMapper.selectList(null);
        Map<Long, String> roleIdToCode = new HashMap<>();
        for (SsoRole role : roleMapper.selectList(null)) {
            roleIdToCode.put(role.getId(), role.getCode());
        }
        for (SsoUserRole userRole : userRoles) {
            SsoUser user = userMapper.selectById(userRole.getUserId());
            if (user == null) {
                continue;
            }
            String roleCode = roleIdToCode.get(userRole.getRoleId());
            if (roleCode == null) {
                continue;
            }
            SsoAccessReviewItem item = new SsoAccessReviewItem();
            item.setCampaignId(campaignId);
            item.setUserId(user.getId());
            item.setUsername(user.getUsername());
            item.setRoleCode(roleCode);
            item.setDecision(SsoAccessReviewItem.DECISION_PENDING);
            itemMapper.insert(item);
        }
    }

    private void revokeRole(Long userId, String roleCode) {
        SsoRole role = roleMapper.selectOne(new LambdaQueryWrapper<SsoRole>().eq(SsoRole::getCode, roleCode));
        if (role == null) {
            return;
        }
        userRoleMapper.delete(new LambdaQueryWrapper<SsoUserRole>()
                .eq(SsoUserRole::getUserId, userId)
                .eq(SsoUserRole::getRoleId, role.getId()));
    }

    private void maybeCompleteCampaign(Long campaignId) {
        long pending = itemMapper.selectCount(new LambdaQueryWrapper<SsoAccessReviewItem>()
                .eq(SsoAccessReviewItem::getCampaignId, campaignId)
                .eq(SsoAccessReviewItem::getDecision, SsoAccessReviewItem.DECISION_PENDING));
        if (pending == 0) {
            SsoAccessReviewCampaign campaign = new SsoAccessReviewCampaign();
            campaign.setId(campaignId);
            campaign.setStatus(SsoAccessReviewCampaign.STATUS_COMPLETED);
            campaignMapper.updateById(campaign);
        }
    }

    private SsoAccessReviewCampaign requireCampaign(Long campaignId) {
        SsoAccessReviewCampaign campaign = campaignMapper.selectById(campaignId);
        if (campaign == null) {
            throw new SsoException(ErrorCode.NOT_FOUND, "复核活动不存在");
        }
        return campaign;
    }
}
