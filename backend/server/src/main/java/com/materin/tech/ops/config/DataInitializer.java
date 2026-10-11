package com.materin.tech.ops.config;

import cn.dev33.satoken.secure.BCrypt;
import com.materin.tech.ops.module.system.entity.SysUser;
import com.materin.tech.ops.module.system.mapper.SysUserMapper;
import com.mybatisflex.core.query.QueryColumn;
import com.mybatisflex.core.query.QueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

/**
 * 数据初始化器：admin 用户在 V2 种子数据中为占位密码，
 * 首次启动时使用 BCrypt 覆盖为真实密码，避免在 SQL 中硬编码哈希。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    /** V2 种子数据中的密码占位串 */
    private static final String INIT_PLACEHOLDER = "INIT_NEED_RESET";

    /** 默认管理员初始密码 */
    private static final String DEFAULT_ADMIN_PASSWORD = "123456";

    private final SysUserMapper sysUserMapper;

    @Override
    public void run(String... args) {
        SysUser admin = sysUserMapper.selectOneByQuery(
                QueryWrapper.create().where(new QueryColumn("username").eq("admin")));
        if (admin != null && INIT_PLACEHOLDER.equals(admin.getPassword())) {
            admin.setPassword(BCrypt.hashpw(DEFAULT_ADMIN_PASSWORD, BCrypt.gensalt()));
            sysUserMapper.update(admin);
            log.info("默认管理员账号已初始化: admin / 123456");
        }
    }
}
