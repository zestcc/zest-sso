package cn.zest.sso.server.domain.mapper;

import cn.zest.sso.server.domain.entity.SsoRole;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface SsoRoleMapper extends BaseMapper<SsoRole> {

    @Select("""
            SELECT r.* FROM sso_role r
            INNER JOIN sso_user_role ur ON r.id = ur.role_id
            WHERE ur.user_id = #{userId} AND r.deleted = 0
            """)
    List<SsoRole> selectByUserId(@Param("userId") Long userId);

    @Select("""
            SELECT DISTINCT r.* FROM sso_role r
            INNER JOIN sso_group_role gr ON r.id = gr.role_id
            INNER JOIN sso_user_group ug ON gr.group_id = ug.group_id
            WHERE ug.user_id = #{userId} AND r.deleted = 0
            """)
    List<SsoRole> selectByUserGroups(@Param("userId") Long userId);
}
