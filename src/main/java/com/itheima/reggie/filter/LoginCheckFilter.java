package com.itheima.reggie.filter;

import com.alibaba.fastjson.JSON;
import com.itheima.reggie.common.BaseContext;
import com.itheima.reggie.common.R;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.AntPathMatcher;

import javax.servlet.*;
import javax.servlet.annotation.WebFilter;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * 检查登录状态
 */
@WebFilter(filterName = "loginCheckFilter", urlPatterns = "/*")
@Slf4j
public class LoginCheckFilter implements Filter {
    // 路径匹配器，支持通配符
    public static final AntPathMatcher PATH_MATCHER = new AntPathMatcher();

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws IOException, ServletException {
        HttpServletRequest httpServletRequest = (HttpServletRequest)servletRequest;
        HttpServletResponse response = (HttpServletResponse) servletResponse;
        // 获取uri
        String requestURI = httpServletRequest.getRequestURI();
        // 不需要处理的请求路径
        String[] urls = new String[]{
                "/employee/login",
                "/employee/logout",
                "/backend/**",
                "/front/**",
                "/common/**",
                "/user/sendMsg",
                "/user/login"
        };
        // 判断请求是否处理
        boolean check = check(urls, requestURI);
        if(check){

            filterChain.doFilter(httpServletRequest, response);
            return;
        }
        // 判断登录状态，已经登陆则放行
        if(httpServletRequest.getSession().getAttribute("employee") != null){
            Long empId = (Long)httpServletRequest.getSession().getAttribute("employee");
            BaseContext.setCurrentId(empId);
            filterChain.doFilter(httpServletRequest, response);
            return;
        }
        // 判断登录状态，已经登陆则放行
        if(httpServletRequest.getSession().getAttribute("user") != null){
            Long userId = (Long)httpServletRequest.getSession().getAttribute("user");
            BaseContext.setCurrentId(userId);
            filterChain.doFilter(httpServletRequest, response);
            return;
        }
        // 未登录拦截
        response.getWriter().write(JSON.toJSONString(R.error("NOTLOGIN")));
        return;
    }

    /**
     *  路径匹配，检查是否放行
     * @param urls
     * @param requestURI
     * @return
     */
    public boolean check(String[] urls, String requestURI){
        for(String url : urls){
            boolean match = PATH_MATCHER.match(url, requestURI);
            if(match)
                return true;
        }
        return false;
    }
}
