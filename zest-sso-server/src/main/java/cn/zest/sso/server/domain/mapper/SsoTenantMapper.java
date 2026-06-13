package cn.zest.sso.server.domain.mapper;

import cn.zest.sso.server.domain.entity.SsoTenant;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface SsoTenantMapper extends BaseMapper<SsoTenant> {

    @Select("""
            SELECT t.* FROM sso_tenant t
            INNER JOIN sso_user_tenant ut ON t.id = ut.tenant_id
            WHERE ut.user_id = #{userId} AND t.deleted = 0 AND t.status = 1
            ORDER BY ut.is_default DESC
            """)
    List<SsoTenant> selectByUserId(@Param("userId") Long userId);

    @Select("""
            SELECT t.* FROM sso_tenant t
            INNER JOIN sso_user_tenant ut ON t.id = ut.tenant_id
            WHERE ut.user_id = #{userId} AND ut.is_default = 1 AND t.deleted = 0
            LIMIT 1
            """)
    SsoTenant selectDefaultByUserId(@Param("userId") Long userId);
}
