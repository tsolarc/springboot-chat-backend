package com.Backend.Chat.service.interfaces;

import com.Backend.User.entity.User;

import java.util.List;
import java.util.Set;

public interface ILoadParticipantsService {
    Set<User> call(List<Long> usersId);
}
