package com.finovara.notificationservice.notification;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.util.ReflectionTestUtils.setField;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.finovara.contracts.util.SortType;
import com.finovara.notificationservice.notification.dto.NotificationResponse;
import com.finovara.notificationservice.notification.model.Notification;
import com.finovara.notificationservice.notification.repository.NotificationRepository;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;

@ExtendWith(MockitoExtension.class)
class NotificationPersistenceServiceTest {

    private static final int PAGE_SIZE = 20;
    private static final int PAGE = 2;
    private static final Long USER_ID = 1L;
    private static final Long NOTIFICATION_ID = 100L;
    private static final String KEY = "key-1";
    private static final String OTHER_KEY = "key-2";
    private static final String PAYLOAD = "{\"id\":1}";
    private static final String OTHER_PAYLOAD = "{\"id\":2}";
    private static final String BLANK_PAYLOAD = "   ";
    private static final String EMPTY_PAYLOAD = "";

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private ObjectMapper objectMapper;

    @Mock
    private NotificationResponse firstDto;

    @Mock
    private NotificationResponse secondDto;

    @Mock
    private Notification firstNotification;

    @Mock
    private Notification secondNotification;

    @Mock
    private Notification savedNotification;

    @Mock
    private JsonProcessingException jsonProcessingException;

    @Captor
    private ArgumentCaptor<Notification> notificationCaptor;

    @InjectMocks
    private NotificationPersistenceService service;

    private PageRequest pageRequest;

    @BeforeEach
    void setUp() {
        setField(service, "pageSize", PAGE_SIZE);
        pageRequest = PageRequest.of(PAGE, PAGE_SIZE);
    }

    @Nested
    class Save {

        @Test
        void shouldPersistNotificationWhenDtoIsNew() throws JsonProcessingException {
            when(firstDto.deduplicationKey()).thenReturn(KEY);
            when(notificationRepository.findExistingDeduplicationKeys(USER_ID, List.of(KEY))).thenReturn(List.of());
            when(objectMapper.writeValueAsString(firstDto)).thenReturn(PAYLOAD);

            service.save(USER_ID, firstDto);

            verify(notificationRepository).save(notificationCaptor.capture());
            Notification saved = notificationCaptor.getValue();
            assertEquals(USER_ID, saved.getUserId());
            assertEquals(KEY, saved.getDeduplicationKey());
            assertEquals(PAYLOAD, saved.getPayload());
            assertEquals(firstDto.type(), saved.getType());
            assertEquals(firstDto.createdAt(), saved.getCreatedAt());
        }

        @Test
        void shouldNotPersistNotificationWhenDeduplicationKeyAlreadyExists() {
            when(firstDto.deduplicationKey()).thenReturn(KEY);
            when(notificationRepository.findExistingDeduplicationKeys(USER_ID, List.of(KEY))).thenReturn(List.of(KEY));

            service.save(USER_ID, firstDto);

            verify(notificationRepository, never()).save(any(Notification.class));
            verifyNoInteractions(objectMapper);
        }

        @Test
        void shouldThrowExceptionWhenDtoIsNull() {
            assertThrows(NullPointerException.class, () -> service.save(USER_ID, null));

            verifyNoInteractions(notificationRepository, objectMapper);
        }
    }

    @Nested
    class SaveAll {

        @Test
        void shouldDoNothingWhenDtoListIsEmpty() {
            service.saveAll(USER_ID, List.of());

            verifyNoInteractions(notificationRepository, objectMapper);
        }

        @Test
        void shouldThrowExceptionWhenDtoListIsNull() {
            assertThrows(NullPointerException.class, () -> service.saveAll(USER_ID, null));

            verifyNoInteractions(notificationRepository, objectMapper);
        }

        @Test
        void shouldPersistAllNotificationsWhenAllKeysAreNew() throws JsonProcessingException {
            when(firstDto.deduplicationKey()).thenReturn(KEY);
            when(secondDto.deduplicationKey()).thenReturn(OTHER_KEY);
            when(notificationRepository.findExistingDeduplicationKeys(USER_ID, List.of(KEY, OTHER_KEY)))
                    .thenReturn(List.of());
            when(objectMapper.writeValueAsString(firstDto)).thenReturn(PAYLOAD);
            when(objectMapper.writeValueAsString(secondDto)).thenReturn(OTHER_PAYLOAD);

            service.saveAll(USER_ID, List.of(firstDto, secondDto));

            verify(notificationRepository, times(2)).save(notificationCaptor.capture());
            List<Notification> saved = notificationCaptor.getAllValues();
            assertEquals(KEY, saved.get(0).getDeduplicationKey());
            assertEquals(PAYLOAD, saved.get(0).getPayload());
            assertEquals(OTHER_KEY, saved.get(1).getDeduplicationKey());
            assertEquals(OTHER_PAYLOAD, saved.get(1).getPayload());
        }

        @Test
        void shouldSkipOnlyExistingNotificationsWhenSomeKeysAlreadyExist() throws JsonProcessingException {
            when(firstDto.deduplicationKey()).thenReturn(KEY);
            when(secondDto.deduplicationKey()).thenReturn(OTHER_KEY);
            when(notificationRepository.findExistingDeduplicationKeys(USER_ID, List.of(KEY, OTHER_KEY)))
                    .thenReturn(List.of(KEY));
            when(objectMapper.writeValueAsString(secondDto)).thenReturn(OTHER_PAYLOAD);

            service.saveAll(USER_ID, List.of(firstDto, secondDto));

            verify(notificationRepository, times(1)).save(notificationCaptor.capture());
            assertEquals(OTHER_KEY, notificationCaptor.getValue().getDeduplicationKey());
            verify(objectMapper, never()).writeValueAsString(firstDto);
        }

        @Test
        void shouldPersistOnlyFirstNotificationWhenDtosShareSameDeduplicationKey() throws JsonProcessingException {
            when(firstDto.deduplicationKey()).thenReturn(KEY);
            when(secondDto.deduplicationKey()).thenReturn(KEY);
            when(notificationRepository.findExistingDeduplicationKeys(USER_ID, List.of(KEY))).thenReturn(List.of());
            when(objectMapper.writeValueAsString(firstDto)).thenReturn(PAYLOAD);

            service.saveAll(USER_ID, List.of(firstDto, secondDto));

            verify(notificationRepository, times(1)).save(any(Notification.class));
            verify(objectMapper, never()).writeValueAsString(secondDto);
        }

        @Test
        void shouldRequestExistingKeysOnlyOnceWhenMultipleDtosAreProvided() {
            when(firstDto.deduplicationKey()).thenReturn(KEY);
            when(secondDto.deduplicationKey()).thenReturn(OTHER_KEY);
            when(notificationRepository.findExistingDeduplicationKeys(USER_ID, List.of(KEY, OTHER_KEY)))
                    .thenReturn(List.of(KEY, OTHER_KEY));

            service.saveAll(USER_ID, List.of(firstDto, secondDto));

            verify(notificationRepository, times(1)).findExistingDeduplicationKeys(USER_ID, List.of(KEY, OTHER_KEY));
            verifyNoMoreInteractions(notificationRepository);
        }

        @Test
        void shouldContinueSavingWhenDuplicateIsDetectedDuringInsert() throws JsonProcessingException {
            when(firstDto.deduplicationKey()).thenReturn(KEY);
            when(secondDto.deduplicationKey()).thenReturn(OTHER_KEY);
            when(notificationRepository.findExistingDeduplicationKeys(USER_ID, List.of(KEY, OTHER_KEY)))
                    .thenReturn(List.of());
            when(objectMapper.writeValueAsString(firstDto)).thenReturn(PAYLOAD);
            when(objectMapper.writeValueAsString(secondDto)).thenReturn(OTHER_PAYLOAD);
            when(notificationRepository.save(any(Notification.class)))
                    .thenThrow(new DataIntegrityViolationException("duplicate"))
                    .thenReturn(savedNotification);

            service.saveAll(USER_ID, List.of(firstDto, secondDto));

            verify(notificationRepository, times(2)).save(notificationCaptor.capture());
            assertEquals(OTHER_KEY, notificationCaptor.getAllValues().get(1).getDeduplicationKey());
        }

        @Test
        void shouldThrowExceptionWhenSerializationFails() throws JsonProcessingException {
            when(firstDto.deduplicationKey()).thenReturn(KEY);
            when(notificationRepository.findExistingDeduplicationKeys(USER_ID, List.of(KEY))).thenReturn(List.of());
            when(objectMapper.writeValueAsString(firstDto)).thenThrow(jsonProcessingException);

            assertThrows(IllegalStateException.class, () -> service.saveAll(USER_ID, List.of(firstDto)));

            verify(notificationRepository, never()).save(any(Notification.class));
        }

        @Test
        void shouldThrowExceptionWhenInsertFailsWithNonDuplicateException() throws JsonProcessingException {
            when(firstDto.deduplicationKey()).thenReturn(KEY);
            when(notificationRepository.findExistingDeduplicationKeys(USER_ID, List.of(KEY))).thenReturn(List.of());
            when(objectMapper.writeValueAsString(firstDto)).thenReturn(PAYLOAD);
            when(notificationRepository.save(any(Notification.class))).thenThrow(new IllegalStateException());

            assertThrows(IllegalStateException.class, () -> service.saveAll(USER_ID, List.of(firstDto)));
        }

        @Test
        void shouldThrowExceptionWhenFindingExistingKeysFails() {
            when(firstDto.deduplicationKey()).thenReturn(KEY);
            when(notificationRepository.findExistingDeduplicationKeys(USER_ID, List.of(KEY)))
                    .thenThrow(new IllegalStateException());

            assertThrows(IllegalStateException.class, () -> service.saveAll(USER_ID, List.of(firstDto)));

            verifyNoInteractions(objectMapper);
        }
    }

    @Nested
    class GetUserNotifications {

        @Test
        void shouldReturnNewestNotificationsWhenSortTypeIsNewest() throws JsonProcessingException {
            when(notificationRepository.findNewestByUserId(USER_ID, pageRequest)).thenReturn(List.of(firstNotification));
            when(firstNotification.getPayload()).thenReturn(PAYLOAD);
            when(objectMapper.readValue(PAYLOAD, NotificationResponse.class)).thenReturn(firstDto);

            List<NotificationResponse> result = service.getUserNotifications(USER_ID, SortType.NEWEST, PAGE);

            assertEquals(1, result.size());
            assertSame(firstDto, result.get(0));
            verify(notificationRepository, never()).findOldestByUserId(any(Long.class), any(PageRequest.class));
        }

        @Test
        void shouldReturnOldestNotificationsWhenSortTypeIsOldest() throws JsonProcessingException {
            when(notificationRepository.findOldestByUserId(USER_ID, pageRequest)).thenReturn(List.of(firstNotification));
            when(firstNotification.getPayload()).thenReturn(PAYLOAD);
            when(objectMapper.readValue(PAYLOAD, NotificationResponse.class)).thenReturn(firstDto);

            List<NotificationResponse> result = service.getUserNotifications(USER_ID, SortType.OLDEST, PAGE);

            assertEquals(1, result.size());
            assertSame(firstDto, result.get(0));
            verify(notificationRepository, never()).findNewestByUserId(any(Long.class), any(PageRequest.class));
        }

        @Test
        void shouldReturnEmptyListWhenNoNotificationsFound() {
            when(notificationRepository.findNewestByUserId(USER_ID, pageRequest)).thenReturn(List.of());

            List<NotificationResponse> result = service.getUserNotifications(USER_ID, SortType.NEWEST, PAGE);

            assertTrue(result.isEmpty());
            verifyNoInteractions(objectMapper);
        }

        @Test
        void shouldRequestPageWithConfiguredPageSizeWhenFetchingNotifications() {
            when(notificationRepository.findNewestByUserId(USER_ID, pageRequest)).thenReturn(List.of());

            service.getUserNotifications(USER_ID, SortType.NEWEST, PAGE);

            verify(notificationRepository).findNewestByUserId(USER_ID, PageRequest.of(PAGE, PAGE_SIZE));
        }

        @Test
        void shouldPreserveRepositoryOrderWhenMappingNotifications() throws JsonProcessingException {
            when(notificationRepository.findNewestByUserId(USER_ID, pageRequest))
                    .thenReturn(List.of(secondNotification, firstNotification));
            when(firstNotification.getPayload()).thenReturn(PAYLOAD);
            when(secondNotification.getPayload()).thenReturn(OTHER_PAYLOAD);
            when(objectMapper.readValue(PAYLOAD, NotificationResponse.class)).thenReturn(firstDto);
            when(objectMapper.readValue(OTHER_PAYLOAD, NotificationResponse.class)).thenReturn(secondDto);

            List<NotificationResponse> result = service.getUserNotifications(USER_ID, SortType.NEWEST, PAGE);

            assertEquals(List.of(secondDto, firstDto), result);
        }

        @Test
        void shouldSkipNotificationWhenPayloadIsNull() {
            when(notificationRepository.findNewestByUserId(USER_ID, pageRequest)).thenReturn(List.of(firstNotification));
            when(firstNotification.getPayload()).thenReturn(null);

            List<NotificationResponse> result = service.getUserNotifications(USER_ID, SortType.NEWEST, PAGE);

            assertTrue(result.isEmpty());
            verifyNoInteractions(objectMapper);
        }

        @Test
        void shouldSkipNotificationWhenPayloadIsEmpty() {
            when(notificationRepository.findNewestByUserId(USER_ID, pageRequest)).thenReturn(List.of(firstNotification));
            when(firstNotification.getPayload()).thenReturn(EMPTY_PAYLOAD);

            List<NotificationResponse> result = service.getUserNotifications(USER_ID, SortType.NEWEST, PAGE);

            assertTrue(result.isEmpty());
            verifyNoInteractions(objectMapper);
        }

        @Test
        void shouldSkipNotificationWhenPayloadIsBlank() {
            when(notificationRepository.findNewestByUserId(USER_ID, pageRequest)).thenReturn(List.of(firstNotification));
            when(firstNotification.getPayload()).thenReturn(BLANK_PAYLOAD);

            List<NotificationResponse> result = service.getUserNotifications(USER_ID, SortType.NEWEST, PAGE);

            assertTrue(result.isEmpty());
            verifyNoInteractions(objectMapper);
        }

        @Test
        void shouldReturnOnlyValidNotificationsWhenSomePayloadsAreEmpty() throws JsonProcessingException {
            when(notificationRepository.findNewestByUserId(USER_ID, pageRequest))
                    .thenReturn(List.of(firstNotification, secondNotification));
            when(firstNotification.getPayload()).thenReturn(null);
            when(secondNotification.getPayload()).thenReturn(PAYLOAD);
            when(objectMapper.readValue(PAYLOAD, NotificationResponse.class)).thenReturn(secondDto);

            List<NotificationResponse> result = service.getUserNotifications(USER_ID, SortType.NEWEST, PAGE);

            assertEquals(1, result.size());
            assertSame(secondDto, result.get(0));
        }

        @Test
        void shouldDeserializeEachPayloadExactlyOnceWhenNotificationsExist() throws JsonProcessingException {
            when(notificationRepository.findNewestByUserId(USER_ID, pageRequest))
                    .thenReturn(List.of(firstNotification, secondNotification));
            when(firstNotification.getPayload()).thenReturn(PAYLOAD);
            when(secondNotification.getPayload()).thenReturn(OTHER_PAYLOAD);
            when(objectMapper.readValue(PAYLOAD, NotificationResponse.class)).thenReturn(firstDto);
            when(objectMapper.readValue(OTHER_PAYLOAD, NotificationResponse.class)).thenReturn(secondDto);

            service.getUserNotifications(USER_ID, SortType.NEWEST, PAGE);

            verify(objectMapper, times(1)).readValue(PAYLOAD, NotificationResponse.class);
            verify(objectMapper, times(1)).readValue(OTHER_PAYLOAD, NotificationResponse.class);
            verifyNoMoreInteractions(objectMapper);
        }

        @Test
        void shouldThrowExceptionWhenDeserializationFails() throws JsonProcessingException {
            when(notificationRepository.findNewestByUserId(USER_ID, pageRequest)).thenReturn(List.of(firstNotification));
            when(firstNotification.getId()).thenReturn(NOTIFICATION_ID);
            when(firstNotification.getPayload()).thenReturn(PAYLOAD);
            when(objectMapper.readValue(PAYLOAD, NotificationResponse.class)).thenThrow(jsonProcessingException);

            assertThrows(IllegalStateException.class,
                    () -> service.getUserNotifications(USER_ID, SortType.NEWEST, PAGE));
        }

        @Test
        void shouldThrowExceptionWhenRepositoryFailsForNewest() {
            when(notificationRepository.findNewestByUserId(USER_ID, pageRequest)).thenThrow(new IllegalStateException());

            assertThrows(IllegalStateException.class,
                    () -> service.getUserNotifications(USER_ID, SortType.NEWEST, PAGE));

            verifyNoInteractions(objectMapper);
        }

        @Test
        void shouldThrowExceptionWhenRepositoryFailsForOldest() {
            when(notificationRepository.findOldestByUserId(USER_ID, pageRequest)).thenThrow(new IllegalStateException());

            assertThrows(IllegalStateException.class,
                    () -> service.getUserNotifications(USER_ID, SortType.OLDEST, PAGE));

            verifyNoInteractions(objectMapper);
        }

        @Test
        void shouldThrowExceptionWhenSortTypeIsNull() {
            assertThrows(NullPointerException.class, () -> service.getUserNotifications(USER_ID, null, PAGE));

            verifyNoInteractions(notificationRepository, objectMapper);
        }
    }

    @Nested
    class DeleteAllNotifications {

        @Test
        void shouldDeleteNotificationsInBatchWhenUserHasNotifications() {
            List<Long> ids = List.of(1L, 2L, 3L);
            when(notificationRepository.findIdsByUserId(USER_ID)).thenReturn(ids);

            service.deleteAllNotifications(USER_ID);

            verify(notificationRepository).deleteAllByIdInBatch(ids);
        }

        @Test
        void shouldDelegateEmptyIdListWhenUserHasNoNotifications() {
            when(notificationRepository.findIdsByUserId(USER_ID)).thenReturn(List.of());

            service.deleteAllNotifications(USER_ID);

            verify(notificationRepository).deleteAllByIdInBatch(List.of());
        }

        @Test
        void shouldDelegateWithNullWhenUserIdIsNull() {
            when(notificationRepository.findIdsByUserId(null)).thenReturn(List.of());

            service.deleteAllNotifications(null);

            verify(notificationRepository).findIdsByUserId(null);
            verify(notificationRepository).deleteAllByIdInBatch(List.of());
        }

        @Test
        void shouldFindIdsAndDeleteExactlyOnceWhenUserHasNotifications() {
            List<Long> ids = List.of(1L);
            when(notificationRepository.findIdsByUserId(USER_ID)).thenReturn(ids);

            service.deleteAllNotifications(USER_ID);

            verify(notificationRepository, times(1)).findIdsByUserId(USER_ID);
            verify(notificationRepository, times(1)).deleteAllByIdInBatch(ids);
            verifyNoMoreInteractions(notificationRepository);
        }

        @Test
        void shouldThrowExceptionWhenFindingIdsFails() {
            when(notificationRepository.findIdsByUserId(USER_ID)).thenThrow(new IllegalStateException());

            assertThrows(IllegalStateException.class, () -> service.deleteAllNotifications(USER_ID));

            verifyNoMoreInteractions(notificationRepository);
        }

        @Test
        void shouldThrowExceptionWhenBatchDeleteFails() {
            List<Long> ids = List.of(1L);
            when(notificationRepository.findIdsByUserId(USER_ID)).thenReturn(ids);
            doThrow(new IllegalStateException()).when(notificationRepository).deleteAllByIdInBatch(ids);

            assertThrows(IllegalStateException.class, () -> service.deleteAllNotifications(USER_ID));
        }
    }
}