package org.miniProjectTwo.DragonOfNorth.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.miniProjectTwo.DragonOfNorth.dto.auth.request.IdentifierEmail;
import org.miniProjectTwo.DragonOfNorth.model.AppUser;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface IdentifierEmailMapper {
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "appUserStatus", ignore = true)
    @Mapping(target = "roles", ignore = true)
    @Mapping(target = "phoneNumberVerified", ignore = true)
    @Mapping(target = "phoneNumber", ignore = true)
    @Mapping(target = "lockedAt", ignore = true)
    @Mapping(target = "lastLoginAt", ignore = true)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "failedLoginAttempts", ignore = true)
    @Mapping(target = "emailVerified", ignore = true)
    @Mapping(target = "deleted", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "accountLocked", ignore = true)
    AppUser toEntity(IdentifierEmail identifierEmail);

}