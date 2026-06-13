package cn.zest.sso.server.service;

import cn.zest.sso.common.enums.AuditEventType;
import cn.zest.sso.common.exception.ErrorCode;
import cn.zest.sso.common.exception.SsoException;
import cn.zest.sso.server.domain.dto.CreateGroupRequest;
import cn.zest.sso.server.domain.dto.UpdateGroupRequest;
import cn.zest.sso.server.domain.entity.SsoGroup;
import cn.zest.sso.server.domain.entity.SsoGroupRole;
import cn.zest.sso.server.domain.entity.SsoRole;
import cn.zest.sso.server.domain.entity.SsoUserGroup;
import cn.zest.sso.server.domain.mapper.SsoGroupMapper;
import cn.zest.sso.server.domain.mapper.SsoGroupRoleMapper;
import cn.zest.sso.server.domain.mapper.SsoRoleMapper;
import cn.zest.sso.server.domain.mapper.SsoUserGroupMapper;
import cn.zest.sso.server.domain.vo.GroupVO;
import cn.zest.sso.server.support.AdminAuditSupport;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class GroupService {

    private final SsoGroupMapper groupMapper;
    private final SsoGroupRoleMapper groupRoleMapper;
    private final SsoUserGroupMapper userGroupMapper;
    private final SsoRoleMapper roleMapper;
    private final AdminAuditSupport auditSupport;

    public Page<GroupVO> pageGroups(int page, int size, String keyword) {
        LambdaQueryWrapper<SsoGroup> wrapper = new LambdaQueryWrapper<SsoGroup>()
                .orderByAsc(SsoGroup::getCode);
        if (StringUtils.hasText(keyword)) {
            wrapper.and(w -> w.like(SsoGroup::getCode, keyword)
                    .or().like(SsoGroup::getName, keyword));
        }
        Page<SsoGroup> result = groupMapper.selectPage(new Page<>(page, size), wrapper);
        Page<GroupVO> voPage = new Page<>(page, size, result.getTotal());
        voPage.setRecords(result.getRecords().stream().map(this::toGroupVO).toList());
        return voPage;
    }

    public List<GroupVO> listAll() {
        return groupMapper.selectList(new LambdaQueryWrapper<SsoGroup>().orderByAsc(SsoGroup::getCode))
                .stream().map(this::toGroupVO).toList();
    }

    public GroupVO getById(Long id) {
        return toGroupVO(findGroup(id));
    }

    @Transactional(rollbackFor = Exception.class)
    public GroupVO createGroup(CreateGroupRequest request) {
        Long count = groupMapper.selectCount(new LambdaQueryWrapper<SsoGroup>()
                .eq(SsoGroup::getCode, request.getCode()));
        if (count > 0) {
            throw new SsoException(ErrorCode.CONFLICT, "用户组编码已存在");
        }
        SsoGroup group = new SsoGroup();
        group.setCode(request.getCode());
        group.setName(request.getName());
        group.setDescription(request.getDescription());
        groupMapper.insert(group);
        bindRoles(group.getId(), request.getRoleCodes());
        auditSupport.log(AuditEventType.GROUP_CREATE, group.getCode(), group.getName());
        return toGroupVO(group);
    }

    @Transactional(rollbackFor = Exception.class)
    public GroupVO updateGroup(Long id, UpdateGroupRequest request) {
        SsoGroup group = findGroup(id);
        if (request.getName() != null) {
            group.setName(request.getName());
        }
        if (request.getDescription() != null) {
            group.setDescription(request.getDescription());
        }
        groupMapper.updateById(group);
        if (request.getRoleCodes() != null) {
            groupRoleMapper.delete(new LambdaQueryWrapper<SsoGroupRole>()
                    .eq(SsoGroupRole::getGroupId, id));
            bindRoles(id, request.getRoleCodes());
        }
        auditSupport.log(AuditEventType.GROUP_UPDATE, group.getCode(), group.getName());
        return toGroupVO(groupMapper.selectById(id));
    }

    @Transactional(rollbackFor = Exception.class)
    public void deleteGroup(Long id) {
        SsoGroup group = findGroup(id);
        Long memberCount = userGroupMapper.selectCount(new LambdaQueryWrapper<SsoUserGroup>()
                .eq(SsoUserGroup::getGroupId, id));
        if (memberCount > 0) {
            throw new SsoException(ErrorCode.CONFLICT, "用户组仍有关联成员，无法删除");
        }
        groupRoleMapper.delete(new LambdaQueryWrapper<SsoGroupRole>().eq(SsoGroupRole::getGroupId, id));
        groupMapper.deleteById(id);
        auditSupport.log(AuditEventType.GROUP_DELETE, group.getCode(), group.getName());
    }

    public void bindUserGroups(Long userId, List<Long> groupIds) {
        userGroupMapper.delete(new LambdaQueryWrapper<SsoUserGroup>().eq(SsoUserGroup::getUserId, userId));
        if (groupIds == null || groupIds.isEmpty()) {
            return;
        }
        for (Long groupId : groupIds) {
            SsoUserGroup ug = new SsoUserGroup();
            ug.setUserId(userId);
            ug.setGroupId(groupId);
            userGroupMapper.insert(ug);
        }
    }

    public List<Long> listGroupIdsByUserId(Long userId) {
        return groupMapper.selectByUserId(userId).stream().map(SsoGroup::getId).toList();
    }

    public List<String> listGroupCodesByUserId(Long userId) {
        return groupMapper.selectByUserId(userId).stream().map(SsoGroup::getCode).toList();
    }

    private void bindRoles(Long groupId, List<String> roleCodes) {
        if (roleCodes == null || roleCodes.isEmpty()) {
            return;
        }
        for (String code : roleCodes) {
            SsoRole role = roleMapper.selectOne(new LambdaQueryWrapper<SsoRole>().eq(SsoRole::getCode, code));
            if (role == null) {
                throw new SsoException(ErrorCode.NOT_FOUND, "角色不存在: " + code);
            }
            SsoGroupRole gr = new SsoGroupRole();
            gr.setGroupId(groupId);
            gr.setRoleId(role.getId());
            groupRoleMapper.insert(gr);
        }
    }

    private List<String> listRoleCodes(Long groupId) {
        List<SsoGroupRole> relations = groupRoleMapper.selectList(new LambdaQueryWrapper<SsoGroupRole>()
                .eq(SsoGroupRole::getGroupId, groupId));
        List<String> codes = new ArrayList<>();
        for (SsoGroupRole relation : relations) {
            SsoRole role = roleMapper.selectById(relation.getRoleId());
            if (role != null) {
                codes.add(role.getCode());
            }
        }
        return codes;
    }

    private SsoGroup findGroup(Long id) {
        SsoGroup group = groupMapper.selectById(id);
        if (group == null) {
            throw new SsoException(ErrorCode.NOT_FOUND, "用户组不存在");
        }
        return group;
    }

    private GroupVO toGroupVO(SsoGroup group) {
        Long memberCount = userGroupMapper.selectCount(new LambdaQueryWrapper<SsoUserGroup>()
                .eq(SsoUserGroup::getGroupId, group.getId()));
        return GroupVO.builder()
                .id(group.getId())
                .code(group.getCode())
                .name(group.getName())
                .description(group.getDescription())
                .roleCodes(listRoleCodes(group.getId()))
                .memberCount(memberCount)
                .build();
    }
}
