package com.finovara.finovarabackend.piggybank.service;

import com.finovara.finovarabackend.accountactivity.piggybank.model.PiggyBankActivityType;
import com.finovara.finovarabackend.accountactivity.piggybank.service.PiggyBankActivityService;
import com.finovara.finovarabackend.exception.badrequest.InvalidInputException;
import com.finovara.finovarabackend.exception.conflict.NameAlreadyExistsException;
import com.finovara.finovarabackend.piggybank.dto.PiggyBankDto;
import com.finovara.finovarabackend.piggybank.mapper.PiggyBankMapper;
import com.finovara.finovarabackend.piggybank.model.PiggyBank;
import com.finovara.finovarabackend.piggybank.model.PiggyBankGoalType;
import com.finovara.finovarabackend.piggybank.repository.PiggyBankRepository;
import com.finovara.finovarabackend.user.exception.notfound.UserNotFoundException;
import com.finovara.finovarabackend.user.model.User;
import com.finovara.finovarabackend.usersetting.factory.SettingsFactory;
import com.finovara.finovarabackend.usersetting.piggybank.model.PiggyBankSettings;
import com.finovara.finovarabackend.usersetting.piggybank.repository.PiggyBankSettingsRepository;
import com.finovara.finovarabackend.util.piggybank.exception.notfound.PiggyBankNotFoundException;
import com.finovara.finovarabackend.util.piggybank.manager.PiggyBankManagerService;
import com.finovara.finovarabackend.util.user.service.UserManagerService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PiggyBankManagementServiceTest {

    @InjectMocks
    private PiggyBankManagementService service;

    @Mock
    private UserManagerService userManagerService;
    @Mock
    private PiggyBankRepository piggyBankRepository;
    @Mock
    private PiggyBankManagerService piggyBankManagerService;
    @Mock
    private PiggyBankActivityService piggyBankActivityService;
    @Mock
    private PiggyBankSettingsRepository piggyBankSettingsRepository;
    @Mock
    private SettingsFactory settingsFactory;
    @Mock
    private PiggyBankMapper piggyBankMapper;

    private User user;
    private Long userId;
    private Long piggyBankId;

    @BeforeEach
    void setUp() {
        userId = 1L;
        piggyBankId = 1L;
        user = new User();
        user.setId(userId);
    }

    @Nested
    class AddPiggyBankTests {
        @Test
        void shouldAddPiggyBankSuccessfully() {
            PiggyBankDto dto = new PiggyBankDto(null, null, "Piggy", BigDecimal.valueOf(100), null, PiggyBankGoalType.GIFTS, BigDecimal.valueOf(230), null, null);

            PiggyBank piggyBank = new PiggyBank();
            piggyBank.setId(piggyBankId);
            piggyBank.setUserAssigned(user);

            PiggyBankSettings settings = new PiggyBankSettings();

            when(userManagerService.getUserByIdOrThrow(1L)).thenReturn(user);
            when(piggyBankRepository.countPiggyBanksByUserId(1L)).thenReturn(0L);
            when(piggyBankRepository.existsByNameAndUserAssignedId(dto.name(), 1L)).thenReturn(false);
            when(piggyBankRepository.save(any(PiggyBank.class))).thenReturn(piggyBank);
            when(settingsFactory.createDefaultPiggyBankSettings(any())).thenReturn(settings);

            Long result = service.addPiggyBank(dto, 1L);

            assertEquals(1L, result);

            verify(piggyBankRepository).save(any(PiggyBank.class));
            verify(piggyBankActivityService).createSimplePiggyBankActivity(eq(1L), any(PiggyBank.class), eq(PiggyBankActivityType.ADDED_PIGGY_BANK));
            verify(settingsFactory).createDefaultPiggyBankSettings(any());
            verify(piggyBankSettingsRepository).save(settings);
        }

        @Test
        void shouldThrowWhenMaxPiggyBanksReached() {
            PiggyBankDto dto = new PiggyBankDto(null, null, "Piggy", BigDecimal.valueOf(100), null, PiggyBankGoalType.GIFTS, BigDecimal.valueOf(230), null, null);

            when(userManagerService.getUserByIdOrThrow(1L)).thenReturn(user);
            when(piggyBankRepository.countPiggyBanksByUserId(1L)).thenReturn(5L);

            assertThrows(InvalidInputException.class, () -> service.addPiggyBank(dto, 1L));

            verify(piggyBankRepository, never()).save(any());
        }

        @Test
        void shouldThrowWhenNameAlreadyExists() {
            PiggyBankDto dto = new PiggyBankDto(null, null, "Piggy", BigDecimal.valueOf(100), null, PiggyBankGoalType.GIFTS, BigDecimal.valueOf(230), null, null);

            when(userManagerService.getUserByIdOrThrow(1L)).thenReturn(user);
            when(piggyBankRepository.countPiggyBanksByUserId(1L)).thenReturn(0L);
            when(piggyBankRepository.existsByNameAndUserAssignedId(dto.name(), 1L)).thenReturn(true);

            assertThrows(NameAlreadyExistsException.class, () -> service.addPiggyBank(dto, 1L));

            verify(piggyBankRepository, never()).save(any());
        }

        @Test
        void shouldThrowWhenUserNotFound() {
            PiggyBankDto dto = new PiggyBankDto(null, null, "Piggy", BigDecimal.valueOf(100), null, PiggyBankGoalType.GIFTS, BigDecimal.valueOf(230), null, null);

            when(userManagerService.getUserByIdOrThrow(1L)).thenThrow(new UserNotFoundException("x"));

            assertThrows(UserNotFoundException.class, () -> service.addPiggyBank(dto, 1L));

            verify(piggyBankRepository, never()).save(any());
        }
    }
    @Nested
    class EditPiggyBankTests {
        @Test
        void shouldEditPiggyBankSuccessfully() {
            PiggyBank piggyBank = new PiggyBank();
            piggyBank.setId(piggyBankId);
            piggyBank.setName("Old name");
            piggyBank.setGoalAmount(BigDecimal.valueOf(200));
            piggyBank.setGoalType(PiggyBankGoalType.GIFTS);
            piggyBank.setUserAssigned(user);

            PiggyBankDto dto = new PiggyBankDto(piggyBankId, userId, "New name", BigDecimal.valueOf(100), null, PiggyBankGoalType.GIFTS,
                    BigDecimal.valueOf(200), null, null);

            when(userManagerService.getUserByIdOrThrow(userId)).thenReturn(user);
            when(piggyBankManagerService.getPiggyBankByUserId(piggyBankId, userId)).thenReturn(piggyBank);
            when(piggyBankRepository.existsByNameAndUserAssignedId(dto.name(), userId)).thenReturn(false);
            when(piggyBankRepository.save(any(PiggyBank.class))).thenAnswer(i -> i.getArgument(0));

            Long result = service.editPiggyBank(userId, dto, piggyBankId);

            assertEquals(piggyBankId, result);

            verify(piggyBankRepository).save(piggyBank);

            assertEquals("New name", piggyBank.getName());
            assertEquals(BigDecimal.valueOf(200), piggyBank.getGoalAmount());
            assertEquals(PiggyBankGoalType.GIFTS, piggyBank.getGoalType());

            verify(piggyBankActivityService).createEditPiggyBankActivity(eq(userId), eq(piggyBank), eq(PiggyBankActivityType.EDITED_PIGGY_BANK),
                    any(), any(), eq("Old name"));
        }

        @Test
        void shouldThrowExceptionWhenPiggyBankNotFound() {
            PiggyBankDto dto = new PiggyBankDto(piggyBankId, userId, "New name", BigDecimal.valueOf(200), null, PiggyBankGoalType.GIFTS, null, null, null);

            when(userManagerService.getUserByIdOrThrow(userId)).thenReturn(new User());
            when(piggyBankManagerService.getPiggyBankByUserId(piggyBankId, userId)).thenThrow(new PiggyBankNotFoundException("Piggy bank not found"));

            assertThrows(PiggyBankNotFoundException.class, () -> service.editPiggyBank(userId, dto, piggyBankId));

            verify(piggyBankRepository, never()).save(any());
        }

        @Test
        void shouldThrowExceptionWhenNameAlreadyExists() {
            PiggyBank existing = new PiggyBank();
            existing.setId(piggyBankId);
            existing.setName("Old name");
            existing.setUserAssigned(user);

            PiggyBankDto dto = new PiggyBankDto(piggyBankId, userId, "Another existing name", BigDecimal.valueOf(200), null,
                    PiggyBankGoalType.GIFTS, null, null, null);

            when(userManagerService.getUserByIdOrThrow(userId)).thenReturn(user);
            when(piggyBankManagerService.getPiggyBankByUserId(piggyBankId, userId)).thenReturn(existing);
            when(piggyBankRepository.existsByNameAndUserAssignedId(dto.name(), userId)).thenReturn(true);

            assertThrows(NameAlreadyExistsException.class, () -> service.editPiggyBank(userId, dto, piggyBankId));

            verify(piggyBankRepository, never()).save(any());
        }

        @Test
        void shouldThrowWhenPiggyBankBelongsToAnotherUsers() {
            User otherUser = new User();
            otherUser.setId(999L);

            PiggyBank piggyBank = new PiggyBank();
            piggyBank.setId(piggyBankId);
            piggyBank.setUserAssigned(otherUser);

            PiggyBankDto dto = new PiggyBankDto(piggyBankId, userId, "New name", BigDecimal.valueOf(200),
                    null, PiggyBankGoalType.GIFTS, null, null, null);

            when(userManagerService.getUserByIdOrThrow(userId)).thenReturn(user);
            when(piggyBankManagerService.getPiggyBankByUserId(piggyBankId, userId)).thenReturn(piggyBank);

            assertThrows(PiggyBankNotFoundException.class, () -> service.editPiggyBank(userId, dto, piggyBankId));

            verify(piggyBankRepository, never()).save(any());
        }
    }

    @Nested
    class GetPiggyBankTests {
        @Test
        void shouldReturnAllPiggyBanks() {
            PiggyBank piggyBank = new PiggyBank();
            piggyBank.setId(piggyBankId);
            piggyBank.setAmount(BigDecimal.TEN);

            PiggyBankDto dto = new PiggyBankDto(1L, 1L, "Piggy", BigDecimal.TEN, null, null, null, 0.5, false);

            when(userManagerService.getUserByIdOrThrow(1L)).thenReturn(user);
            when(piggyBankRepository.findAllByUserAssignedId(1L)).thenReturn(List.of(piggyBank));
            when(piggyBankMapper.mapToPiggyBankDto(any(), eq(user), anyDouble(), anyBoolean())).thenReturn(dto);

            List<PiggyBankDto> result = service.getAllPiggyBanks(1L);

            assertEquals(1, result.size());
            assertEquals("Piggy", result.get(0).name());

            verify(piggyBankRepository).findAllByUserAssignedId(1L);
        }

        @Test
        void shouldReturnEmptyList() {
            when(userManagerService.getUserByIdOrThrow(1L)).thenReturn(user);
            when(piggyBankRepository.findAllByUserAssignedId(1L)).thenReturn(List.of());

            List<PiggyBankDto> result = service.getAllPiggyBanks(1L);

            assertTrue(result.isEmpty());

            verifyNoInteractions(piggyBankMapper);
        }

        @Test
        void shouldThrowExceptionWhenUserNotFound() {
            when(userManagerService.getUserByIdOrThrow(1L)).thenThrow(new UserNotFoundException("x"));

            assertThrows(UserNotFoundException.class, () -> service.getAllPiggyBanks(1L));

            verifyNoInteractions(piggyBankRepository);
        }
    }

    @Nested
    class DeletePiggyBankTests {
        @Test
        void shouldDeletePiggyBank() {
            PiggyBank piggy = new PiggyBank();
            piggy.setAmount(BigDecimal.ZERO);

            when(piggyBankManagerService.getPiggyBankByUserId(1L, 1L)).thenReturn(piggy);

            service.deletePiggyBank(1L, 1L);

            verify(piggyBankActivityService).createSimplePiggyBankActivity(eq(1L), eq(piggy), eq(PiggyBankActivityType.DELETED_PIGGY_BANK));

            verify(piggyBankRepository).delete(piggy);
        }

        @Test
        void shouldThrowWhenBalanceNotZero() {
            PiggyBank piggy = new PiggyBank();
            piggy.setAmount(BigDecimal.TEN);

            when(piggyBankManagerService.getPiggyBankByUserId(1L, 1L)).thenReturn(piggy);

            assertThrows(InvalidInputException.class, () -> service.deletePiggyBank(1L, 1L));

            verify(piggyBankRepository, never()).delete(any());
        }

        @Test
        void shouldThrowWhenPiggyBankNull() {
            when(piggyBankManagerService.getPiggyBankByUserId(1L, 1L)).thenReturn(null);

            assertThrows(InvalidInputException.class, () -> service.deletePiggyBank(1L, 1L));

            verifyNoInteractions(piggyBankActivityService);
        }
    }
}