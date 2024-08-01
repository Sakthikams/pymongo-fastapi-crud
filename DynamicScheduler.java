import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.scheduling.support.CronTrigger;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ScheduledFuture;

@Component
public class DynamicScheduler {

    @Autowired
    private CronRepository cronRepository;

    private TaskScheduler taskScheduler = new ThreadPoolTaskScheduler();
    private Map<Long, ScheduledFuture<?>> scheduledTasks = new HashMap<>();

    @PostConstruct
    public void scheduleTasks() {
        rescheduleTasks();
    }

    @Scheduled(fixedRate = 60000) // Check for updates every minute
    public void rescheduleTasks() {
        List<CronExpression> cronExpressions = cronRepository.findAll();

        // Cancel tasks that are no longer in the database
        scheduledTasks.keySet().removeIf(id -> {
            boolean exists = cronExpressions.stream().anyMatch(cron -> cron.getId().equals(id));
            if (!exists) {
                scheduledTasks.get(id).cancel(false);
            }
            return !exists;
        });

        // Schedule new or updated tasks
        for (CronExpression cronExpression : cronExpressions) {
            if (scheduledTasks.containsKey(cronExpression.getId())) {
                scheduledTasks.get(cronExpression.getId()).cancel(false);
            }
            ScheduledFuture<?> scheduledFuture = taskScheduler.schedule(
                new RunnableTask(cronExpression.getTaskName()), new CronTrigger(cronExpression.getExpression()));
            scheduledTasks.put(cronExpression.getId(), scheduledFuture);
        }
    }

    private static class RunnableTask implements Runnable {
        private final String taskName;

        public RunnableTask(String taskName) {
            this.taskName = taskName;
        }

        @Override
        public void run() {
            System.out.println("Running scheduled task: " + taskName);
            // Task logic here
        }
    }
}
