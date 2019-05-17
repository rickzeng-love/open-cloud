package com.opencloud.zuul.filter;

import com.opencloud.common.constants.CommonConstants;
import com.opencloud.common.constants.ResultEnum;
import com.opencloud.common.utils.WebUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * IP访问限制过滤器
 * @author liuyadu
 */
@Slf4j
public class IpCheckFilter extends OncePerRequestFilter {

    private AccessDeniedHandler accessDeniedHandler;

    private ApiAuthorizationManager apiAccessManager;

    public IpCheckFilter(ApiAuthorizationManager apiAccessManager, AccessDeniedHandler accessDeniedHandler) {
        this.apiAccessManager = apiAccessManager;
        this.accessDeniedHandler = accessDeniedHandler;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        String requestPath = apiAccessManager.getRequestPath(request);
        String remoteIpAddress = WebUtils.getIpAddr(request);
        try {
            // 1.ip黑名单检测
            boolean deny = apiAccessManager.matchIpBlacklist(requestPath, remoteIpAddress);
            if (deny) {
                // 拒绝
                throw new AccessDeniedException(ResultEnum.ACCESS_DENIED_BLACK_IP_LIMITED.getMessage());
            }

            // 3.ip白名单检测
            boolean[] matchIpWhiteListResult = apiAccessManager.matchIpWhiteList(requestPath, remoteIpAddress);
            boolean hasWhiteList = matchIpWhiteListResult[0];
            boolean allow = matchIpWhiteListResult[1];

            if (hasWhiteList) {
                // 接口存在白名单限制
                if (!allow) {
                    // IP白名单检测通过,拒绝
                    request.setAttribute(CommonConstants.X_ACCESS_DENIED, ResultEnum.ACCESS_DENIED_WHITE_IP_LIMITED);
                    throw new AccessDeniedException(ResultEnum.ACCESS_DENIED_WHITE_IP_LIMITED.getMessage());
                }
            }
        } catch (AccessDeniedException e) {
            accessDeniedHandler.handle(request, response, e);
            return;
        }
        filterChain.doFilter(request, response);

    }
}