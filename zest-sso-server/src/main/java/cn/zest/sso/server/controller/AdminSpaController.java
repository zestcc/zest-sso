package cn.zest.sso.server.controller;

import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Admin SPA 入口：显式提供 index.html，避免 ResourceHttpRequestHandler 将目录路径解析为 {@code .}。
 */
@Controller
public class AdminSpaController {

    private static final Resource ADMIN_INDEX = new ClassPathResource("/static/admin/index.html");

    @GetMapping(path = {"/admin", "/admin/"}, produces = MediaType.TEXT_HTML_VALUE)
    public ResponseEntity<Resource> serveIndex() {
        return ResponseEntity.ok().contentType(MediaType.TEXT_HTML).body(ADMIN_INDEX);
    }
}
