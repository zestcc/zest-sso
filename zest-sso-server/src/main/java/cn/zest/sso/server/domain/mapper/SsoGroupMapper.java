package cn.zest.sso.server.domain.mapper;

import cn.zest.sso.server.domain.entity.SsoGroup;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface SsoGroupMapper extends BaseMapper<SsoGroup> {

    @Select("""
            SELECT g.* FROM sso_group g
            INNER JOIN sso_user_group ug ON g.id = ug.group_id
            WHERE ug.user_id = #{userId} AND g.deleted = 0
            ORDER BY g.code
            """)
    List<SsoGroup> selectByUserId(@Param("userId") Long userId);
}
