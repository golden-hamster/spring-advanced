package org.example.expert.aop;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.After;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Slf4j
@Aspect
@Component
public class AdminAccessAop {

    private final HttpServletRequest request;

    public AdminAccessAop(HttpServletRequest request) {
        this.request = request;
    }

    @Pointcut("execution(public * com.example.demo.controller.CommentAdminController.deleteComment(..))")
    public void logDeleteCommentAccess() {}

    @Pointcut("execution(public * com.example.demo.controller.UserAdminController.changeUserRole(..))")
    public void logChangeUserRole() {}

    @After("logDeleteCommentAccess() || logChangeUserRole()")
    public void logAdminAccess() {
        String requestURI = request.getRequestURI();
        String userId = (String) request.getAttribute("userId");
        LocalDateTime requestTime = LocalDateTime.now();

        log.info("Admin accessed API | User ID: {} | Time: {} | URL: {}",
                userId, requestTime, requestURI);
    }
}
