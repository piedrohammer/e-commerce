package com.hammer.ecommerce.config;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.*;
import org.springframework.stereotype.Component;

import java.util.Arrays;

@Aspect
@Component
@Slf4j
public class LoggingAspect {

    @Pointcut("within(@org.springframework.web.bind.annotation.RestController *)")
    public void springBeanPointcut() {
        // Pointcut para todos os controllers
    }

    @Pointcut("within(com.seudominio.ecommerce.service..*)")
    public void applicationPackagePointcut() {
        // Pointcut para todos os services
    }

    @Before("springBeanPointcut() || applicationPackagePointcut()")
    public void logBefore(JoinPoint joinPoint) {

        log.debug("Executando: {}.{}() com argumentos = {}",
                joinPoint.getSignature().getDeclaringTypeName(),
                joinPoint.getSignature().getName(),
                Arrays.toString(joinPoint.getArgs()));
    }

    @AfterReturning(pointcut = "springBeanPointcut() || applicationPackagePointcut()", returning = "result")
    public void logAfterReturning(JoinPoint joinPoint, Object result) {

        log.debug("Concluído: {}.{}() com resultado = {}",
                joinPoint.getSignature().getDeclaringTypeName(),
                joinPoint.getSignature().getName(),
                result);
    }

    @AfterThrowing(pointcut = "springBeanPointcut() || applicationPackagePointcut()", throwing = "e")
    public void logAfterThrowing(JoinPoint joinPoint, Throwable e) {

        log.error("Exceção em: {}.{}() com causa = {}",
                joinPoint.getSignature().getDeclaringTypeName(),
                joinPoint.getSignature().getName(),
                e.getMessage());
    }

    @Around("springBeanPointcut() || applicationPackagePointcut()")
    public Object logAround(ProceedingJoinPoint joinPoint) throws Throwable {

        long start = System.currentTimeMillis();
        try {
            Object result = joinPoint.proceed();
            long executionTime = System.currentTimeMillis() - start;

            log.debug("Tempo de execução: {}.{}() = {}ms",
                    joinPoint.getSignature().getDeclaringTypeName(),
                    joinPoint.getSignature().getName(),
                    executionTime);

            return result;
        } catch (IllegalArgumentException e) {
            log.error("Argumento ilegal em: {}.{}() com argumentos = {}",
                    joinPoint.getSignature().getDeclaringTypeName(),
                    joinPoint.getSignature().getName(),
                    Arrays.toString(joinPoint.getArgs()));
            throw e;
        }
    }
}