package com.Backend.Chat.config.Messaging;

import com.Backend.Chat.outbox.OutboxProperties;
import com.Backend.Chat.service.implementation.OutboxScheduledTask;
import com.github.kagkarlsson.scheduler.Clock;
import com.github.kagkarlsson.scheduler.SchedulerClient;
import com.github.kagkarlsson.scheduler.task.CompletionHandler;
import com.github.kagkarlsson.scheduler.task.ExecutionContext;
import com.github.kagkarlsson.scheduler.task.TaskInstance;
import com.github.kagkarlsson.scheduler.task.helper.RecurringTask;
import com.github.kagkarlsson.scheduler.task.schedule.FixedDelay;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(OutboxProperties.class)
public class OutboxSchedulerConfig {
    @Bean
    RecurringTask<Void> publishMessageOutbox(OutboxScheduledTask task, OutboxProperties settings) {
        return new RecurringTask<Void>("publish-message-outbox", FixedDelay.of(settings.getPollInterval()), Void.class) {
            @Override
            public void onStartup(SchedulerClient scheduler, Clock clock) {
                super.onStartup(scheduler, clock); // Existing 'recurring' instance is worker zero.
                for (int worker = 1; worker < settings.getWorkers(); worker++) {
                    scheduler.scheduleIfNotExists(instance("worker-" + worker), clock.now());
                }
            }

            @Override
            public CompletionHandler<Void> execute(TaskInstance<Void> instance, ExecutionContext context) {
                if (!enabled(instance.getId())) return new CompletionHandler.OnCompleteRemove<>();
                return super.execute(instance, context);
            }

            @Override
            public void executeRecurringly(TaskInstance<Void> instance, ExecutionContext context) {
                task.run();
            }

            private boolean enabled(String id) {
                if (RecurringTask.INSTANCE.equals(id)) return true;
                for (int worker = 1; worker < settings.getWorkers(); worker++) {
                    if (("worker-" + worker).equals(id)) return true;
                }
                return false;
            }
        };
    }
}
