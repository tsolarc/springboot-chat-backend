package com.Backend.Chat.config.Messaging;

import com.Backend.Chat.service.implementation.OutboxScheduledTask;
import com.github.kagkarlsson.scheduler.task.helper.RecurringTask;
import com.github.kagkarlsson.scheduler.task.helper.Tasks;
import com.github.kagkarlsson.scheduler.task.schedule.FixedDelay;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;


@Configuration
public class OutboxSchedulerConfig {

    @Bean
    RecurringTask<Void> publishMessageOutbox(OutboxScheduledTask task) {
        return Tasks.recurring("publish-message-outbox", FixedDelay.ofSeconds(1))
                .execute((instance, context) -> task.run());
    }
}
