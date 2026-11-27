package com.finovara.contracts.logging;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

@Aspect
@Component
@Slf4j
@RequiredArgsConstructor
public class InvocationTimeAspect {

    private static final String METHOD_MDC_KEY = "methodName";
    private static final String DURATION_MDC_KEY = "methodDuration";
    private static final String METRIC_NAME = "finovara.method.execution";

    private final MeterRegistry meterRegistry;

    @Around("@annotation(com.finovara.contracts.logging.InvocationTimeLogger)")
    public Object logExecutionTime(ProceedingJoinPoint joinPoint) throws Throwable {
        String className = joinPoint.getSignature().getDeclaringType().getSimpleName();
        String methodName = joinPoint.getSignature().getName();
        String method = className + "." + methodName;
        Timer timer = meterRegistry.timer(METRIC_NAME, "method", method);
        Timer.Sample sample = Timer.start(meterRegistry);

        try {
            log.info("Invoking method {}.{}", className, methodName);
            return joinPoint.proceed();
        } finally {
            long durationNanos = sample.stop(timer);
            String previousMethod = MDC.get(METHOD_MDC_KEY);
            String previousDuration = MDC.get(DURATION_MDC_KEY);
            MDC.put(METHOD_MDC_KEY, method);
            MDC.put(DURATION_MDC_KEY, Long.toString(TimeUnit.NANOSECONDS.toMillis(durationNanos)));

            try {
                log.info("Invoked method {}.{} in {} ms (average {} ms over {} calls)", className, methodName, TimeUnit.NANOSECONDS.toMillis(durationNanos), timer.mean(TimeUnit.MILLISECONDS), timer.count());
            } finally {
                restoreMdcValue(METHOD_MDC_KEY, previousMethod);
                restoreMdcValue(DURATION_MDC_KEY, previousDuration);
            }
        }
    }

    private void restoreMdcValue(String key, String previousValue) {
        if (previousValue == null) {
            MDC.remove(key);
        } else {
            MDC.put(key, previousValue);
        }
    }
}
