package cn.zest.sso.server.service;

import cn.zest.sso.common.enums.AuditEventType;
import cn.zest.sso.common.exception.ErrorCode;
import cn.zest.sso.common.exception.SsoException;
import cn.zest.sso.server.domain.dto.CreateRoleRequest;
import cn.zest.sso.server.domain.dto.UpdateRoleRequest;
import cn.zest.sso.server.domain.entity.SsoRole;
import cn.zest.sso.server.domain.entity.SsoUserRole;
import cn.zest.sso.server.domain.mapper.SsoRoleMapper;
import cn.zest.sso.server.domain.mapper.SsoUserRoleMapper;
import cn.zest.sso.server.domain.vo.RoleVO;
import cn.zest.sso.server.support.AdminAuditSupport;
import cn.zest.sso.server.support.AdminRbacSupport;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class RoleService {

    private final SsoRoleMapper roleMapper;
    private final SsoUserRoleMapper userRoleMapper;
    private final AdminAuditSupport auditSupport;

    public List<RoleVO> listRoles() {
        return roleMapper.selectList(new LambdaQueryWrapper<SsoRole>()
                        .orderByAsc(SsoRole::getCode))
                .stream()
                .map(this::toRoleVO)
                .toList();
    }

    public RoleVO getById(Long id) {
        return toRoleVO(findRole(id));
    }

    @Transactional(rollbackFor = Exception.class)
    public RoleVO createRole(CreateRoleRequest request) {
        if (AdminRbacSupport.isSystemRole(request.getCode())) {
            throw new SsoException(ErrorCode.CONFLICT, "角色编码与系统角色冲突");
        }
        Long count = roleMapper.selectCount(new LambdaQueryWrapper<SsoRole>()
                .eq(SsoRole::getCode, request.getCode()));
        if (count > 0) {
            throw new SsoException(ErrorCode.CONFLICT, "角色编码已存在");
        }
        SsoRole role = new SsoRole();
        role.setCode(request.getCode());
        role.setName(request.getName());
        role.setDescription(request.getDescription());
        roleMapper.insert(role);
        auditSupport.log(AuditEventType.ROLE_CREATE, role.getCode(), role.getName());
        return toRoleVO(role);
    }

    @Transactional(rollbackFor = Exception.class)
    public RoleVO updateRole(Long id, UpdateRoleRequest request) {
        SsoRole role = findRole(id);
        assertMutable(role);
        if (request.getName() != null) {
            role.setName(request.getName());
        }
        if (request.getDescription() != null) {
            role.setDescription(request.getDescription());
        }
        roleMapper.updateById(role);
        auditSupport.log(AuditEventType.ROLE_UPDATE, role.getCode(), role.getName());
        return toRoleVO(role);
    }

    @Transactional(rollbackFor = Exception.class)
    public void deleteRole(Long id) {
        SsoRole role = findRole(id);
        assertMutable(role);
        Long userCount = userRoleMapper.selectCount(new LambdaQueryWrapper<SsoUserRole>()
                .eq(SsoUserRole::getRoleId, id));
        if (userCount > 0) {
            throw new SsoException(ErrorCode.CONFLICT, "角色仍有关联用户，无法删除");
        }
        roleMapper.deleteById(id);
        auditSupport.log(AuditEventType.ROLE_DELETE, role.getCode(), role.getName());
    }

    private void assertMutable(SsoRole role) {
        if (AdminRbacSupport.isSystemRole(role.getCode())) {
            throw new SsoException(ErrorCode.FORBIDDEN, "系统内置角色不可修改或删除");
        }
    }

    private SsoRole findRole(Long id) {
        SsoRole role = roleMapper.selectById(id);
        if (role == null) {
            throw new SsoException(ErrorCode.NOT_FOUND, "角色不存在");
        }
        return role;
    }

    private RoleVO toRoleVO(SsoRole role) {
        return RoleVO.builder()
                .id(role.getId())
                .code(role.getCode())
                .name(role.getName())
                .description(role.getDescription())
                .system(AdminRbacSupport.isSystemRole(role.getCode()))
                .build();
    }
}
