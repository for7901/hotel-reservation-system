package daydream.hotel.reservation.system.auth.security;

/**
 * Token 版本号存储：让已签发的 JWT <b>立即失效</b>。
 *
 * <p>每个用户在 Redis 里保存一个自增计数（{@code auth:token:version:<userId>}）。 签发 Token 时把当前版本写进 claim {@code
 * ver}，每个请求再比对 claim 与存储值是否一致。 改密码、禁用账号时调用 {@link #revokeAll}，版本号 +1， 旧 Token 立刻对不上，不必等它自然过期。
 *
 * <p>这是"无状态 JWT + 少量有状态"的常见折中：读一份 Redis 换掉一次数据库查询。
 */
public interface TokenVersionStore {

    /**
     * 读取当前版本号。
     *
     * @param userId 用户 id
     * @return 版本号（从未吊销过则为 0）；<b>返回 {@code null} 表示存储不可用</b>（如 Redis 未配置/连接失败），
     *     调用方应按"无法校验、放行"处理，而不是当成 0
     */
    Long currentVersion(Long userId);

    /** 吊销该用户已签发的全部 Token（版本号 +1）。 */
    void revokeAll(Long userId);
}
