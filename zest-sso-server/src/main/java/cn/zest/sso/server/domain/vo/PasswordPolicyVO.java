package cn.zest.sso.server.domain.vo;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class PasswordPolicyVO {

    private Integer minLength;
    private boolean requireUppercase;
    private boolean requireLowercase;
    private boolean requireDigit;
    private boolean requireSpecial;
    private Integer passwordHistoryCount;
    private Integer maxAgeDays;
}
