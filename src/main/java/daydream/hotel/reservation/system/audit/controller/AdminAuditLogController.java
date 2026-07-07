package daydream.hotel.reservation.system.audit.controller;

import daydream.hotel.reservation.system.audit.dto.AuditLogVO;
import daydream.hotel.reservation.system.audit.service.AuditLogService;
import daydream.hotel.reservation.system.common.result.PageResult;
import daydream.hotel.reservation.system.common.result.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "操作日志-平台端")
@RestController
@RequestMapping("/admin/audit-logs")
@PreAuthorize("hasRole('ADMIN')")
public class AdminAuditLogController {

    private final AuditLogService auditLogService;

    public AdminAuditLogController(AuditLogService auditLogService) {
        this.auditLogService = auditLogService;
    }

    @Operation(summary = "操作日志列表")
    @GetMapping
    public Result<PageResult<AuditLogVO>> list(
            @RequestParam(required = false) String action,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "10") long size) {
        return Result.ok(auditLogService.listLogs(action, keyword, page, size));
    }
}
