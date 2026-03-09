package com.Backend.Common.Redis;

import com.Backend.User.dto.UserDTO;
import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.function.Supplier;

@Service
@Profile("!test")
public class RedisService {

    private final RedisTemplate<String,Object> redis;
    private static final String ALL_USERS_CACHE_KEY = "ALL_USERS_LIST";

    public RedisService(RedisTemplate<String,Object> redis) {
        this.redis = redis;
    }

    @SuppressWarnings("unchecked")
    public List<UserDTO> getAllUsersCached(Supplier<List<UserDTO>> dbQuery) {
        List<UserDTO> cached = (List<UserDTO>) redis.opsForValue().get(ALL_USERS_CACHE_KEY);
        if (cached != null) {
            return cached;
        }
        List<UserDTO> users = dbQuery.get();
        redis.opsForValue().set(ALL_USERS_CACHE_KEY, users);
        return users;
    }
}
