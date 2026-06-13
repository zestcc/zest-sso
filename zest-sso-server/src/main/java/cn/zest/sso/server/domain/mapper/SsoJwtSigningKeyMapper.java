package cn.zest.sso.server.domain.mapper;

import cn.zest.sso.server.domain.entity.SsoJwtSigningKey;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface SsoJwtSigningKeyMapper extends BaseMapper<SsoJwtSigningKey> {
}
