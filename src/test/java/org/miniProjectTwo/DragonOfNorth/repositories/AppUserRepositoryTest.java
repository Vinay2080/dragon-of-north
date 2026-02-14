package org.miniProjectTwo.DragonOfNorth.repositories;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.miniProjectTwo.DragonOfNorth.enums.AppUserStatus;
import org.miniProjectTwo.DragonOfNorth.enums.RoleName;
import org.miniProjectTwo.DragonOfNorth.model.AppUser;
import org.miniProjectTwo.DragonOfNorth.model.Role;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AppUserRepositoryTest {

    @Mock
    private AppUserRepository appUserRepository;

    @Test
    void findById_shouldReturnUser_whenUserExists() {
        // arrange
        UUID userId = UUID.randomUUID();
        AppUser expectedUser = createTestUser();
        expectedUser.setId(userId);

        when(appUserRepository.findById(userId)).thenReturn(Optional.of(expectedUser));

        // act
        Optional<AppUser> result = appUserRepository.findById(userId);

        // assert
        assertTrue(result.isPresent());
        assertEquals(userId, result.get().getId());
        assertEquals("test@example.com", result.get().getEmail());
        verify(appUserRepository).findById(userId);
    }

    private AppUser createTestUser() {
        AppUser user = new AppUser();
        user.setId(UUID.randomUUID());
        user.setEmail("test@example.com");
        user.setPassword("hashedPassword");
        user.setAppUserStatus(AppUserStatus.CREATED);
        user.setCreatedAt(Instant.now());
        user.setUpdatedAt(Instant.now());
        return user;
    }

    @Test
    void findById_shouldReturnEmpty_whenUserDoesNotExist() {
        // arrange
        UUID userId = UUID.randomUUID();
        when(appUserRepository.findById(userId)).thenReturn(Optional.empty());

        // act
        Optional<AppUser> result = appUserRepository.findById(userId);

        // assert
        assertFalse(result.isPresent());
        verify(appUserRepository).findById(userId);
    }

    @Test
    void findByEmail_shouldReturnUser_whenEmailExists() {
        // arrange
        AppUser expectedUser = createTestUser();
        when(appUserRepository.findByEmail("test@example.com")).thenReturn(Optional.of(expectedUser));

        // act
        Optional<AppUser> result = appUserRepository.findByEmail("test@example.com");

        // assert
        assertTrue(result.isPresent());
        assertEquals("test@example.com", result.get().getEmail());
        verify(appUserRepository).findByEmail("test@example.com");
    }

    @Test
    void findByEmail_shouldReturnEmpty_whenEmailDoesNotExist() {
        // arrange
        when(appUserRepository.findByEmail("nonexistent@example.com")).thenReturn(Optional.empty());

        // act
        Optional<AppUser> result = appUserRepository.findByEmail("nonexistent@example.com");

        // assert
        assertFalse(result.isPresent());
        verify(appUserRepository).findByEmail("nonexistent@example.com");
    }

    @Test
    void findByPhone_shouldReturnUser_whenPhoneExists() {
        // arrange
        AppUser expectedUser = createTestUser();
        expectedUser.setPhone("+1234567890");
        when(appUserRepository.findByPhone("+1234567890")).thenReturn(Optional.of(expectedUser));

        // act
        Optional<AppUser> result = appUserRepository.findByPhone("+1234567890");

        // assert
        assertTrue(result.isPresent());
        assertEquals("+1234567890", result.get().getPhone());
        verify(appUserRepository).findByPhone("+1234567890");
    }

    @Test
    void findByPhone_shouldReturnEmpty_whenPhoneDoesNotExist() {
        // arrange
        when(appUserRepository.findByPhone("+9999999999")).thenReturn(Optional.empty());

        // act
        Optional<AppUser> result = appUserRepository.findByPhone("+9999999999");

        // assert
        assertFalse(result.isPresent());
        verify(appUserRepository).findByPhone("+9999999999");
    }

    @Test
    void findAppUserStatusByEmail_shouldReturnStatus_whenEmailExists() {
        // arrange
        when(appUserRepository.findAppUserStatusByEmail("test@example.com"))
                .thenReturn(Optional.of(AppUserStatus.VERIFIED));

        // act
        Optional<AppUserStatus> result = appUserRepository.findAppUserStatusByEmail("test@example.com");

        // assert
        assertTrue(result.isPresent());
        assertEquals(AppUserStatus.VERIFIED, result.get());
        verify(appUserRepository).findAppUserStatusByEmail("test@example.com");
    }

    @Test
    void findAppUserStatusByEmail_shouldReturnEmpty_whenEmailDoesNotExist() {
        // arrange
        when(appUserRepository.findAppUserStatusByEmail("nonexistent@example.com"))
                .thenReturn(Optional.empty());

        // act
        Optional<AppUserStatus> result = appUserRepository.findAppUserStatusByEmail("nonexistent@example.com");

        // assert
        assertFalse(result.isPresent());
        verify(appUserRepository).findAppUserStatusByEmail("nonexistent@example.com");
    }

    @Test
    void findAppUserStatusByPhone_shouldReturnStatus_whenPhoneExists() {
        // arrange
        when(appUserRepository.findAppUserStatusByPhone("+1234567890"))
                .thenReturn(Optional.of(AppUserStatus.CREATED));

        // act
        Optional<AppUserStatus> result = appUserRepository.findAppUserStatusByPhone("+1234567890");

        // assert
        assertTrue(result.isPresent());
        assertEquals(AppUserStatus.CREATED, result.get());
        verify(appUserRepository).findAppUserStatusByPhone("+1234567890");
    }

    @Test
    void findAppUserStatusByPhone_shouldReturnEmpty_whenPhoneDoesNotExist() {
        // arrange
        when(appUserRepository.findAppUserStatusByPhone("+9999999999"))
                .thenReturn(Optional.empty());

        // act
        Optional<AppUserStatus> result = appUserRepository.findAppUserStatusByPhone("+9999999999");

        // assert
        assertFalse(result.isPresent());
        verify(appUserRepository).findAppUserStatusByPhone("+9999999999");
    }

    @Test
    void findRolesById_shouldReturnRoles_whenUserExists() {
        // arrange
        UUID userId = UUID.randomUUID();
        Role userRole = new Role();
        userRole.setRoleName(RoleName.USER);
        userRole.setSystemRole(true);

        Set<Role> expectedRoles = Set.of(userRole);
        when(appUserRepository.findRolesById(userId)).thenReturn(expectedRoles);

        // act
        Set<Role> result = appUserRepository.findRolesById(userId);

        // assert
        assertNotNull(result);
        assertEquals(1, result.size());
        assertTrue(result.stream().anyMatch(role -> role.getRoleName() == RoleName.USER));
        verify(appUserRepository).findRolesById(userId);
    }

    @Test
    void findRolesById_shouldReturnEmptySet_whenUserHasNoRoles() {
        // arrange
        UUID userId = UUID.randomUUID();
        when(appUserRepository.findRolesById(userId)).thenReturn(Set.of());

        // act
        Set<Role> result = appUserRepository.findRolesById(userId);

        // assert
        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(appUserRepository).findRolesById(userId);
    }

    @Test
    void deleteByAppUserStatusAndCreatedAtBefore_shouldCallDeleteMethod() {
        // arrange
        AppUserStatus status = AppUserStatus.CREATED;
        Instant cutoff = Instant.now().minusSeconds(1800);

        // act
        appUserRepository.deleteByAppUserStatusAndCreatedAtBefore(status, cutoff);

        // assert
        verify(appUserRepository).deleteByAppUserStatusAndCreatedAtBefore(status, cutoff);
    }
}
