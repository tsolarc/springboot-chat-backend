package com.Backend.Chat.service.implementation;

import com.Backend.Chat.service.interfaces.IMessageHistoryQueryService;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.Set;

@Service
public class MessageHistoryQueryServiceImpl implements IMessageHistoryQueryService {

    private static final Set<String> ALLOWED_SORT_FIELDS = Set.of("sendedAt", "id");
    private static final int MAX_PAGE_SIZE = 100;

    @Override
    public PageRequest initialize(int page, int size, String sortBy, String direction) {
        validatePage(page);
        validateSize(size);
        validateSortBy(sortBy);
        validateDirection(direction);

        Sort sort = direction.equalsIgnoreCase("asc")
                ? Sort.by(sortBy).ascending()
                : Sort.by(sortBy).descending();

        return PageRequest.of(page, size, sort);
    }

    private void validatePage(int page) {
        if (page < 0) {
            throw new IllegalArgumentException("Page must be greater than or equal to 0");
        }
    }

    private void validateSize(int size) {
        if (size <= 0 || size > MAX_PAGE_SIZE) {
            throw new IllegalArgumentException("Size must be between 1 and " + MAX_PAGE_SIZE);
        }
    }

    private void validateSortBy(String sortBy) {
        if (!ALLOWED_SORT_FIELDS.contains(sortBy)) {
            throw new IllegalArgumentException("Invalid sort field: " + sortBy);
        }
    }

    private void validateDirection(String direction) {
        if (!direction.equalsIgnoreCase("asc") && !direction.equalsIgnoreCase("desc")) {
            throw new IllegalArgumentException("Invalid sort direction: " + direction);
        }
    }
}
