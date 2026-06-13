package cn.zest.sso.server.service;

import cn.zest.sso.common.constant.SsoConstants;
import cn.zest.sso.common.enums.AuditEventType;
import cn.zest.sso.common.exception.ErrorCode;
import cn.zest.sso.common.exception.SsoException;
import cn.zest.sso.server.domain.dto.CreateTenantRequest;
import cn.zest.sso.server.domain.dto.UpdateTenantRequest;
import cn.zest.sso.server.domain.entity.SsoTenant;
import cn.zest.sso.server.domain.entity.SsoUserTenant;
import cn.zest.sso.server.domain.mapper.SsoTenantMapper;
import cn.zest.sso.server.domain.mapper.SsoUserTenantMapper;
import cn.zest.sso.server.domain.vo.TenantVO;
import cn.zest.sso.server.support.AdminAuditSupport;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class TenantService {

    private final SsoTenantMapper tenantMapper;
    private final SsoUserTenantMapper userTenantMapper;
    private final AdminAuditSupport auditSupport;

    public Page<TenantVO> pageTenants(int page, int size) {
        Page<SsoTenant> tenantPage = tenantMapper.selectPage(
                new Page<>(page, size),
                new LambdaQueryWrapper<SsoTenant>().orderByDesc(SsoTenant::getCreateTime));
        Page<TenantVO> result = new Page<>(page, size, tenantPage.getTotal());
        result.setRecords(tenantPage.getRecords().stream().map(this::toTenantVO).toList());
        return result;
    }

    public TenantVO getById(Long id) {
        SsoTenant tenant = findTenant(id);
        return toTenantVO(tenant);
    }

    @Transactional(rollbackFor = Exception.class)
    public TenantVO createTenant(CreateTenantRequest request) {
        Long count = tenantMapper.selectCount(new LambdaQueryWrapper<SsoTenant>()
                .eq(SsoTenant::getCode, request.getCode()));
        if (count > 0) {
            throw new SsoException(ErrorCode.CONFLICT, "租户编码已存在");
        }
        SsoTenant tenant = new SsoTenant();
        tenant.setCode(request.getCode());
        tenant.setName(request.getName());
        tenant.setStatus(1);
        tenantMapper.insert(tenant);
        auditSupport.log(AuditEventType.TENANT_CREATE, tenant.getCode(), tenant.getName());
        return toTenantVO(tenant);
    }

    @Transactional(rollbackFor = Exception.class)
    public TenantVO updateTenant(Long id, UpdateTenantRequest request) {
        SsoTenant tenant = findTenant(id);
        if (request.getName() != null) {
            tenant.setName(request.getName());
        }
        if (request.getStatus() != null) {
            tenant.setStatus(request.getStatus());
        }
        tenantMapper.updateById(tenant);
        auditSupport.log(AuditEventType.TENANT_UPDATE, tenant.getCode(), tenant.getName());
        return toTenantVO(tenant);
    }

    @Transactional(rollbackFor = Exception.class)
    public void enableTenant(Long id) {
        updateStatus(id, 1, AuditEventType.TENANT_ENABLE);
    }

    @Transactional(rollbackFor = Exception.class)
    public void disableTenant(Long id) {
        SsoTenant tenant = findTenant(id);
        if ("default".equals(tenant.getCode())) {
            throw new SsoException(ErrorCode.BAD_REQUEST, "默认租户不可禁用");
        }
        updateStatus(id, 0, AuditEventType.TENANT_DISABLE);
    }

    @Transactional(rollbackFor = Exception.class)
    public void deleteTenant(Long id) {
        SsoTenant tenant = findTenant(id);
        if ("default".equals(tenant.getCode())) {
            throw new SsoException(ErrorCode.BAD_REQUEST, "默认租户不可删除");
        }
        Long memberCount = userTenantMapper.selectCount(new LambdaQueryWrapper<SsoUserTenant>()
                .eq(SsoUserTenant::getTenantId, id));
        if (memberCount > 0) {
            throw new SsoException(ErrorCode.CONFLICT, "租户下仍有用户，无法删除");
        }
        tenantMapper.deleteById(id);
        auditSupport.log(AuditEventType.TENANT_DELETE, tenant.getCode(), tenant.getName());
    }

    private void updateStatus(Long id, int status, AuditEventType eventType) {
        SsoTenant tenant = findTenant(id);
        tenant.setStatus(status);
        tenantMapper.updateById(tenant);
        auditSupport.log(eventType, tenant.getCode(), tenant.getName());
    }

    private SsoTenant findTenant(Long id) {
        SsoTenant tenant = tenantMapper.selectById(id);
        if (tenant == null) {
            throw new SsoException(ErrorCode.TENANT_NOT_FOUND, "租户不存在");
        }
        return tenant;
    }

    private TenantVO toTenantVO(SsoTenant tenant) {
        return TenantVO.builder()
                .id(tenant.getId())
                .code(tenant.getCode())
                .name(tenant.getName())
                .status(tenant.getStatus())
                .isDefault("default".equals(tenant.getCode()))
                .system("default".equals(tenant.getCode()))
                .build();
    }
}
