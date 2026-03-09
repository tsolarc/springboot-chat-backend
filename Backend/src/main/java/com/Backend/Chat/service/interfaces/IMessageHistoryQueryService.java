package com.Backend.Chat.service.interfaces;

import org.springframework.data.domain.PageRequest;

public interface IMessageHistoryQueryService {
    PageRequest initialize(int page, int size, String sortBy, String direction);
}
