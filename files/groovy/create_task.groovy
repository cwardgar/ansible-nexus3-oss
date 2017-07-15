import groovy.json.JsonSlurper
import org.sonatype.nexus.scheduling.TaskConfiguration
import org.sonatype.nexus.scheduling.TaskInfo
import org.sonatype.nexus.scheduling.TaskScheduler
import org.sonatype.nexus.scheduling.schedule.Schedule

parsed_args = new JsonSlurper().parseText(args)

TaskScheduler taskScheduler = container.lookup(TaskScheduler.class.getName())

TaskInfo existingTask = taskScheduler.listsTasks().find { TaskInfo taskInfo ->
    taskInfo.name == parsed_args.name
}

if (existingTask && !existingTask.remove()) {
    throw new RuntimeException("Could not remove currently running task : " + parsed_args.name)
}

TaskConfiguration taskConfiguration = taskScheduler.createTaskConfigurationInstance(parsed_args.typeId)
taskConfiguration.name = parsed_args.name
taskConfiguration.alertEmail = parsed_args.alertEmail  // Properly handles null or empty alertEmail

parsed_args.taskProperties.each { key, value ->
    // Cast to String to avoid MissingMethodException when value isn't already a String.
    // TaskConfiguration stores all KV pairs in a Map<String, String>, so we lose nothing by casting now.
    taskConfiguration.setString(key, value as String)
}

Schedule schedule = taskScheduler.scheduleFactory.cron(new Date(), parsed_args.cron)

taskScheduler.scheduleTask(taskConfiguration, schedule)
