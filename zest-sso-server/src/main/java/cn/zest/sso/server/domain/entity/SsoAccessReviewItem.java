package cn.zest.sso.server.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("sso_access_review_item")
public class SsoAccessReviewItem {

    public static final String DECISION_PENDING = "PENDING";
    public static final String DECISION_APPROVED = "APPROVED";
    public static final String DECISION_REVOKED = "REVOKED";

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long campaignId;
    private Long userId;
    private String username;
    private String roleCode;
    private String decision;
    private String reviewer;
    private LocalDateTime reviewedAt;
    private LocalDateTime createTime;
}
