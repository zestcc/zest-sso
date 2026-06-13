package cn.zest.sso.server.domain.mapper;

import cn.zest.sso.server.domain.entity.SsoPasswordPolicy;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface SsoPasswordPolicyMapper extends BaseMapper<SsoPasswordPolicy> {
}
