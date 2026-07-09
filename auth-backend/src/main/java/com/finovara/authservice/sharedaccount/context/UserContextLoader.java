package com.finovara.authservice.sharedaccount.context;

import com.finovara.authservice.sharedaccount.dto.SharedAccountDetailsDto;
import com.finovara.authservice.user.model.User;
import com.finovara.authservice.user.repository.UserRepository;
import com.finovara.contracts.exception.notfound.RequestedEntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class UserContextLoader {

    private final UserRepository userRepository;

    public SharedAccountUsers loadUsersContext(SharedAccountDetailsDto details) {

        Map<Long, User> usersById = userRepository.findAllById(List.of(details.ownerId(), details.memberId()))
                .stream()
                .collect(Collectors.toMap(User::getId, Function.identity()));

        User owner = Optional.ofNullable(usersById.get(details.ownerId()))
                .orElseThrow(() -> new RequestedEntityNotFoundException("User not found, userId=" + details.ownerId()));
        User member = Optional.ofNullable(usersById.get(details.memberId()))
                .orElseThrow(() -> new RequestedEntityNotFoundException("User not found, userId=" + details.memberId()));

        return new SharedAccountUsers(owner, member);
    }
}
