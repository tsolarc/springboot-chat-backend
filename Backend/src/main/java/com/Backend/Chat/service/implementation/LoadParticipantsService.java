package com.Backend.Chat.service.implementation;

import com.Backend.Chat.service.interfaces.ILoadParticipantsService;
import com.Backend.User.entity.User;
import com.Backend.User.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

@Service
public class LoadParticipantsService implements ILoadParticipantsService {

    private final UserRepository userRepository;

    public LoadParticipantsService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public Set<User> call(List<Long> usersIds) {
        if (usersIds == null || usersIds.isEmpty()) {
            throw new IllegalArgumentException("Participant IDs cannot be null or empty");
        }

        List<User> users = userRepository.findAllById(usersIds);

        if (users.size() != usersIds.size()) {
            Set<Long> foundIds = users.stream().map(User::getId).collect(Collectors.toSet());
            List<Long> missingIds = usersIds.stream()
                    .filter(id -> !foundIds.contains(id))
                    .toList();
            throw new EntityNotFoundException("Users not found: " + missingIds);
        }

        return new HashSet<>(users);
    }
}
