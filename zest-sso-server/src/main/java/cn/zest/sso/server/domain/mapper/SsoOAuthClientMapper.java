package cn.zest.sso.server.domain.mapper;

import cn.zest.sso.server.domain.entity.SsoOAuthClient;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface SsoOAuthClientMapper extends BaseMapper<SsoOAuthClient> {
}
