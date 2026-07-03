package com.finovara.authservice.user.mapper;

import com.finovara.authservice.sharedaccount.dto.SharedAccountMemberDto;
import com.finovara.authservice.sharedaccount.model.SharedAccountMember;
import com.finovara.authservice.user.dto.UserDataDto;
import com.finovara.authservice.user.model.User;
import com.finovara.authservice.util.profile.ProfileImageUrlBuilder;
import org.springframework.stereotype.Component;

@Component
public class UserDataMapper {

    public UserDataDto mapToUserData(UserDataDto dto) {
        return new UserDataDto(
                dto.id(),
                dto.username(),
                dto.email(),
                ProfileImageUrlBuilder.buildProfileImageUrl(dto.profileImagePath()));
    }


    public SharedAccountMemberDto toSharedAccountMemberDto(SharedAccountMember member, User user) {
        return new SharedAccountMemberDto(
                member.getUserId(),
                user.getUsername(),
                user.getProfileImageUrl(),
                member.getRole(),
                member.getJoinedAt());
    }
}
