package cn.zest.sso.server.controller.admin;

import cn.zest.sso.common.api.ApiResponse;
import cn.zest.sso.common.api.PageResult;
import cn.zest.sso.server.domain.entity.SsoAuditLog;
import cn.zest.sso.server.service.AuditService;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.List;

@RestController
@RequestMapping("/api/admin/audit-logs")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('SSO_ADMIN', 'SSO_OPERATOR')")
public class AdminAuditController {

    private final AuditService auditService;

    @GetMapping
    public ApiResponse<PageResult<SsoAuditLog>> listAuditLogs(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String eventType,
            @RequestParam(required = false) String actor,
            @RequestParam(required = false) String startTime,
            @RequestParam(required = false) String endTime) {
        Page<SsoAuditLog> result = auditService.page(page, size, eventType, actor, startTime, endTime);
        return ApiResponse.success(PageResult.of(result.getRecords(), result.getTotal(), page, size));
    }

    @GetMapping("/export")
    public void exportCsv(
            @RequestParam(required = false) String eventType,
            @RequestParam(required = false) String actor,
            @RequestParam(required = false) String startTime,
            @RequestParam(required = false) String endTime,
            HttpServletResponse response) throws IOException {
        List<SsoAuditLog> logs = auditService.export(eventType, actor, startTime, endTime, 5000);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType("text/csv; charset=UTF-8");
        response.setHeader(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=audit-logs.csv");
        try (PrintWriter writer = response.getWriter()) {
            writer.write('\ufeff');
            writer.println("id,eventType,actor,target,clientId,ipAddress,detail,createTime");
            for (SsoAuditLog log : logs) {
                writer.printf("%d,%s,%s,%s,%s,%s,%s,%s%n",
                        log.getId(),
                        csv(log.getEventType()),
                        csv(log.getActor()),
                        csv(log.getTarget()),
                        csv(log.getClientId()),
                        csv(log.getIpAddress()),
                        csv(log.getDetail()),
                        csv(String.valueOf(log.getCreateTime())));
            }
        }
    }

    private String csv(String value) {
        if (value == null) {
            return "";
        }
        String escaped = value.replace("\"", "\"\"");
        if (escaped.contains(",") || escaped.contains("\"") || escaped.contains("\n")) {
            return "\"" + escaped + "\"";
        }
        return escaped;
    }
}
