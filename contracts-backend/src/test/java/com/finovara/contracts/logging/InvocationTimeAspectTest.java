package com.finovara.contracts.logging;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.Signature;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class InvocationTimeAspectTest {

    @Test
    void recordsExecutionTime() throws Throwable {
        SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();
        InvocationTimeAspect aspect = new InvocationTimeAspect(meterRegistry);
        ProceedingJoinPoint joinPoint = mock(ProceedingJoinPoint.class);
        Signature signature = mock(Signature.class);
        when(joinPoint.getSignature()).thenReturn(signature);
        when(signature.getDeclaringType()).thenReturn(ProbeService.class);
        when(signature.getName()).thenReturn("execute");
        when(joinPoint.proceed()).thenReturn("result");

        assertEquals("result", aspect.logExecutionTime(joinPoint));
        assertEquals(1, meterRegistry.get("finovara.method.execution")
                .tag("method", "ProbeService.execute")
                .timer()
                .count());
    }

    private static class ProbeService {
    }
}
