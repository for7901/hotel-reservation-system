package daydream.hotel.reservation.system.audit.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import daydream.hotel.reservation.system.audit.dto.AuditLogVO;
import daydream.hotel.reservation.system.audit.entity.SysAuditLog;
import daydream.hotel.reservation.system.audit.mapper.SysAuditLogMapper;
import daydream.hotel.reservation.system.auth.security.LoginUser;
import daydream.hotel.reservation.system.auth.security.SecurityUtils;
import daydream.hotel.reservation.system.common.exception.BusinessException;
import daydream.hotel.reservation.system.common.exception.ErrorCode;
import daydream.hotel.reservation.system.common.result.PageResult;
import daydream.hotel.reservation.system.user.enums.UserRole;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class AuditLogService {

    private final SysAuditLogMapper auditLogMapper;

    public AuditLogService(SysAuditLogMapper auditLogMapper) {
        this.auditLogMapper = auditLogMapper;
    }

    public void record(String action, String targetType, Long targetId, String detail) {
        LoginUser loginUser = SecurityUtils.getLoginUser();
        SysAuditLog log = new SysAuditLog();
        log.setOperatorId(loginUser.getUserId());
        log.setOperatorName(loginUser.getUsername());
        log.setOperatorRole(loginUser.getRole());
        log.setAction(action);
        log.setTargetType(targetType);
        log.setTargetId(targetId);
        log.setDetail(detail);
        auditLogMapper.insert(log);
    }

    public PageResult<AuditLogVO> listLogs(String action, String keyword, long page, long size) {
        if (!UserRole.ADMIN.name().equals(SecurityUtils.getLoginUser().getRole())) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
        Page<SysAuditLog> logPage =
                auditLogMapper.selectPage(
                        new Page<>(page, size),
                        new LambdaQueryWrapper<SysAuditLog>()
                                .eq(StringUtils.hasText(action), SysAuditLog::getAction, action)
                                .and(
                                        StringUtils.hasText(keyword),
                                        w ->
                                                w.like(SysAuditLog::getOperatorName, keyword)
                                                        .or()
                                                        .like(SysAuditLog::getDetail, keyword))
                                .orderByDesc(SysAuditLog::getCreatedAt));
        return new PageResult<>(
                logPage.getRecords().stream().map(this::toVO).toList(),
                logPage.getTotal(),
                logPage.getCurrent(),
                logPage.getSize());
    }

    private AuditLogVO toVO(SysAuditLog log) {
        AuditLogVO vo = new AuditLogVO();
        vo.setId(log.getId());
        vo.setOperatorId(log.getOperatorId());
        vo.setOperatorName(log.getOperatorName());
        vo.setOperatorRole(log.getOperatorRole());
        vo.setAction(log.getAction());
        vo.setTargetType(log.getTargetType());
        vo.setTargetId(log.getTargetId());
        vo.setDetail(log.getDetail());
        vo.setCreatedAt(log.getCreatedAt());
        return vo;
    }
}
