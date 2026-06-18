package com.lazybeartoby.btcmarket.common.security;

import com.lazybeartoby.btcmarket.common.constant.AppConstants;
import com.lazybeartoby.btcmarket.common.exception.BizException;
import org.springframework.stereotype.Component;

@Component
public class AuthChecker {

    public UserContext.CurrentUser requireLogin() {
        UserContext.CurrentUser u = UserContext.get();
        if (u == null) {
            throw new BizException(401, "请先登录");
        }
        return u;
    }

    public UserContext.CurrentUser requireAdmin() {
        UserContext.CurrentUser u = requireLogin();
        if (!AppConstants.ROLE_ADMIN.equals(u.role())) {
            throw new BizException(403, "无管理员权限");
        }
        return u;
    }
}
