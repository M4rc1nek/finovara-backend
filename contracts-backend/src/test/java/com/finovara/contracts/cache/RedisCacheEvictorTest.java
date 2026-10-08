package com.finovara.contracts.cache;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RedisCacheEvictorTest {

    @Mock
    private StringRedisTemplate stringRedisTemplate;

    @Mock
    private Cursor<String> cursor;

    @InjectMocks
    private RedisCacheEvictor redisCacheEvictor;

    private void stubCursorWith(List<String> keys) {
        Iterator<String> iterator = keys.iterator();
        when(cursor.hasNext()).thenAnswer(inv -> iterator.hasNext());
        lenient().when(cursor.next()).thenAnswer(inv -> iterator.next());
    }

    /** Zapamiętuje kopię zawartości przy każdym delete(Collection), bo evictor czyści batch. */
    private List<List<String>> captureDeleteCalls() {
        List<List<String>> calls = new ArrayList<>();
        doAnswer(inv -> {
            Collection<String> arg = inv.getArgument(0);
            calls.add(new ArrayList<>(arg));
            return (long) arg.size();
        }).when(stringRedisTemplate).delete(anyCollection());
        return calls;
    }

    @Nested
    class EvictByPattern {

        @BeforeEach
        void setUp() {
            when(stringRedisTemplate.scan(any(ScanOptions.class))).thenReturn(cursor);
        }

        @Test
        void shouldDeleteAllMatchingKeysInSingleBatch() {
            List<String> keys = List.of("expense::123:1", "expense::123:2");
            stubCursorWith(keys);
            List<List<String>> calls = captureDeleteCalls();

            redisCacheEvictor.evictByPattern("expense::*123:*");

            assertEquals(1, calls.size());
            assertEquals(keys, calls.get(0));
        }

        @Test
        void shouldNotDeleteAnythingWhenNoKeysMatch() {
            stubCursorWith(List.of());

            redisCacheEvictor.evictByPattern("nonexistent:*");

            verify(stringRedisTemplate, never()).delete(anyCollection());
        }

        @Test
        void shouldSplitKeysIntoBatchesOf500() {
            List<String> keys = IntStream.range(0, 1200).mapToObj(i -> "key:" + i).toList();
            stubCursorWith(keys);
            List<List<String>> calls = captureDeleteCalls();

            redisCacheEvictor.evictByPattern("key:*");

            assertEquals(3, calls.size());
            assertEquals(500, calls.get(0).size());
            assertEquals(500, calls.get(1).size());
            assertEquals(200, calls.get(2).size());
            assertEquals(keys, calls.stream().flatMap(List::stream).toList());
        }

        @Test
        void shouldNotSendEmptyBatchWhenKeyCountIsExactMultipleOfBatchSize() {
            List<String> keys = IntStream.range(0, 500).mapToObj(i -> "key:" + i).toList();
            stubCursorWith(keys);
            List<List<String>> calls = captureDeleteCalls();

            redisCacheEvictor.evictByPattern("key:*");

            assertEquals(1, calls.size());
            assertEquals(500, calls.get(0).size());
        }

        @Test
        void shouldCloseCursorAfterSuccessfulScan() {
            stubCursorWith(List.of());

            redisCacheEvictor.evictByPattern("some:pattern:*");

            verify(cursor).close();
        }

        @Test
        void shouldCloseCursorEvenWhenExceptionIsThrown() {
            when(cursor.hasNext()).thenThrow(new RuntimeException("Redis down"));

            assertThrows(IllegalStateException.class,
                    () -> redisCacheEvictor.evictByPattern("some:pattern:*"));

            verify(cursor).close();
        }

        @Test
        void shouldThrowExceptionWithPatternInMessageAndCauseWhenScanFails() {
            String pattern = "report:*::99";
            RuntimeException cause = new RuntimeException("Redis down");
            when(cursor.hasNext()).thenThrow(cause);

            IllegalStateException exception = assertThrows(IllegalStateException.class,
                    () -> redisCacheEvictor.evictByPattern(pattern));

            assertTrue(exception.getMessage().contains(pattern));
            assertSame(cause, exception.getCause());
        }

        @Test
        void shouldThrowExceptionWhenDeleteFails() {
            stubCursorWith(List.of("key:1"));
            doThrow(new RuntimeException("Redis down")).when(stringRedisTemplate).delete(anyCollection());

            IllegalStateException exception = assertThrows(IllegalStateException.class,
                    () -> redisCacheEvictor.evictByPattern("key:*"));

            assertTrue(exception.getMessage().contains("key:*"));
            verify(cursor).close();
        }
    }

    @Nested
    class EvictByPatterns {

        @BeforeEach
        void setUp() {
            when(stringRedisTemplate.scan(any(ScanOptions.class))).thenReturn(cursor);
        }

        @Test
        void shouldCallScanForEachPattern() {
            when(cursor.hasNext()).thenReturn(false);
            List<String> patterns = List.of("report:*:123*", "report:*::123");

            redisCacheEvictor.evictByPatterns(patterns);

            verify(stringRedisTemplate, times(patterns.size())).scan(any(ScanOptions.class));
        }

        @Test
        void shouldStopProcessingWhenOnePatternFails() {
            when(cursor.hasNext()).thenThrow(new RuntimeException("Redis down"));
            List<String> patterns = List.of("report:*:123*", "report:*::123");

            assertThrows(IllegalStateException.class,
                    () -> redisCacheEvictor.evictByPatterns(patterns));

            verify(stringRedisTemplate, times(1)).scan(any(ScanOptions.class));
        }
    }

    @Nested
    class EvictKeys {

        @Test
        void shouldDeleteAllGivenKeysInSingleCall() {
            List<String> keys = List.of("shared-account:activity::1", "shared-account:activity::2");

            redisCacheEvictor.evictKeys(keys);

            verify(stringRedisTemplate, times(1)).delete(keys);
            verify(stringRedisTemplate, never()).scan(any(ScanOptions.class));
        }

        @Test
        void shouldFilterOutNullKeys() {
            List<String> keys = new ArrayList<>();
            keys.add("shared-account:activity::1");
            keys.add(null);

            redisCacheEvictor.evictKeys(keys);

            verify(stringRedisTemplate).delete(List.of("shared-account:activity::1"));
        }

        @Test
        void shouldDoNothingWhenKeysAreEmpty() {
            redisCacheEvictor.evictKeys(List.of());

            verify(stringRedisTemplate, never()).delete(anyCollection());
        }

        @Test
        void shouldDoNothingWhenAllKeysAreNull() {
            List<String> keys = new ArrayList<>();
            keys.add(null);
            keys.add(null);

            redisCacheEvictor.evictKeys(keys);

            verify(stringRedisTemplate, never()).delete(anyCollection());
        }

        @Test
        void shouldThrowExceptionWithKeysAndCauseWhenDeleteFails() {
            List<String> keys = List.of("shared-account:activity::1");
            RuntimeException cause = new RuntimeException("Redis down");
            doThrow(cause).when(stringRedisTemplate).delete(anyCollection());

            IllegalStateException exception = assertThrows(IllegalStateException.class,
                    () -> redisCacheEvictor.evictKeys(keys));

            assertTrue(exception.getMessage().contains("shared-account:activity::1"));
            assertSame(cause, exception.getCause());
        }
    }
}