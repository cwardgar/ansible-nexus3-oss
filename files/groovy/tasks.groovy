import com.google.common.collect.ImmutableMap
import groovy.json.JsonBuilder
import groovy.json.JsonSlurper
import groovy.transform.Field
import org.sonatype.nexus.coreui.TaskComponent
import org.sonatype.nexus.coreui.TaskXO

@Field final TaskComponent taskComponent = container.lookup(TaskComponent.name)

Map<String, Object> parsedArgs = new JsonSlurper().parseText(args)

switch (parsedArgs.methodName) {
    case "read":
        return read()
    case "readTypes":
        return readTypes()
    case "create":
        return create(parsedArgs.methodArgs)
    case "update":
        return update(parsedArgs.methodArgs)
    case "remove":
        return remove(parsedArgs.methodArgs)
    case "run":
        return run(parsedArgs.methodArgs)
    case "stop":
        return stop(parsedArgs.methodArgs)
    default:
        throw new IllegalArgumentException("There is no method named '${parsedArgs.methodName}'.")
}

///////////////////////////////////////////////////// Public API /////////////////////////////////////////////////////
// This API mirrors org.sonatype.nexus.coreui.TaskComponent

String read() {
    makeJsonResponse("OK: Scheduled tasks retrieved.",
                     *taskComponent.read())  // Convert list to vararg. See https://stackoverflow.com/questions/14453449
}

String readTypes() {
    JsonBuilder builder = new JsonBuilder()
    builder.response {
        outcome "OK: Task types retrieved."
        types taskComponent.readTypes()
    }
    
    return builder.toString()
}

String create(Map<String, Object> argsMap) {
    TaskXO existingTask = getTaskWithName argsMap.name
    
    if (existingTask) {
        update argsMap
    } else {
        TaskXO task = taskComponent.create createTaskXO(argsMap)
        makeJsonResponse "CHANGED: Created task '${task.name}'.", task
    }
}

String update(Map<String, Object> argsMap) {
    TaskXO existingTask = getTaskWithNameOrFail argsMap.name
    boolean changed = updateTaskXO existingTask, argsMap
    
    if (changed) {
        TaskXO updatedTask = taskComponent.update existingTask
        makeJsonResponse "CHANGED: Updated task '${existingTask.name}'.", updatedTask
    } else {
        makeJsonResponse "OK: Existing task '${existingTask.name}' already has updated values.", existingTask
    }
}

String remove(Map<String, Object> argsMap) {
    TaskXO existingTask = getTaskWithName argsMap.name
    if (!existingTask) {
        makeJsonResponse "OK: Task is already removed."
    }
    
    // TaskComponent.update() doesn't return a value, even though it easily could.
    // So, we're extracting the code from that method and running it here so that we can get an outcome.
    boolean ret = taskComponent.scheduler.getTaskById(existingTask.id).remove()
    
    if (ret) {
        makeJsonResponse "CHANGED: Task was removed from the scheduler, and no future executions of it will happen.",
                         existingTask
    } else {
        makeJsonResponse "FAILED: Task is running and is not cancelable.", existingTask
    }
}

String run(Map<String, Object> argsMap) {
    TaskXO existingTask = getTaskWithNameOrFail argsMap.name
    
    // It's possible to get a TaskInfo object if we run the task directly via taskComponent.scheduler,
    // but TaskInfo.getCurrentState() won't have been updated yet, so it's not much use to us.
    taskComponent.run existingTask.id
    
    makeJsonResponse "CHANGED: Ran task '${argsMap.name}'.", existingTask
}

String stop(Map<String, Object> argsMap) {
    TaskXO existingTask = getTaskWithName argsMap.name
    if (!existingTask) {
        makeJsonResponse "OK: No task name '${argsMap.name}' was found, so it didn't need to be stopped."
    }
    
    // See note in remove().
    def ret = taskComponent.scheduler.getTaskById(existingTask.id)?.currentState?.future?.cancel(false)
    
    if (ret) {
        makeJsonResponse "CHANGED: Task was successfully cancelled.", existingTask
    } else {
        makeJsonResponse "FAILED: Task could not be cancelled, typically because it has already completed normally.",
                         existingTask
    }
}

/////////////////////////////////////////////////// Utility methods ///////////////////////////////////////////////////

String makeJsonResponse(String outcomeMsg, TaskXO... taskXOs) {
    JsonBuilder builder = new JsonBuilder()
    builder.response {
        outcome outcomeMsg
        tasks taskXOs
    }
    return builder.toString()
}

TaskXO getTaskWithName(String taskName) {
    for (TaskXO existingTask : taskComponent.read()) {
        if (existingTask.name == taskName) {
            return existingTask
        }
    }
    null
}

TaskXO getTaskWithNameOrFail(String taskName) throws IllegalArgumentException {
    def ret = getTaskWithName(taskName)
    if (ret) {
        ret
    } else {
        throw new IllegalArgumentException("No task named '$taskName' was found.")
    }
}

TaskXO createTaskXO(Map<String, Object> argsMap) {
    TaskXO task = new TaskXO(
            id: UUID.randomUUID().toString(),
            enabled: true,
            properties: new HashMap<String, String>()
    )
    
    // We're creating a brand new task here, not updating an existing one, so don't log updates.
    updateTaskXO task, argsMap, false
    
    task
}

boolean updateTaskXO(TaskXO task, Map<String, Object> argsMap, boolean logUpdates = true) {
    boolean changed = false
    
    argsMap.each { key, argsValue ->
        def taskValue = task."$key"
        
        if (argsValue instanceof Map) {
            assert taskValue instanceof Map :
                    "Expected comparable property in TaskXO ('$key') to be a map as well."
            
            if (taskValue instanceof ImmutableMap) {
                // TaskComponent creates TaskXOs with immutable 'properties' maps. We need a mutable map.
                task."$key" = new HashMap<String, String>(taskValue)
            }
            
            changed |= updateMap task."$key", argsValue, logUpdates
        } else {
            if (taskValue != argsValue) {
                task."$key" = argsValue
                changed = true
                if (logUpdates) {
                    log.debug "Updated value of field '{}' from '{}' to '{}'", key, taskValue, argsValue
                }
            }
        }
    }
    changed
}

boolean updateMap(Map<String, String> origMap, Map<String, String> newMap, boolean logUpdates = true) {
    boolean changed = false
    
    newMap.each { key, newValue ->
        // Convert all property values to strings, which org.sonatype.nexus.scheduling.TaskConfiguration requires.
        String origValStr = origMap[key] as String
        String newValueStr = newValue as String
        
        if (origValStr != newValueStr) {
            origMap[key] = newValueStr
            changed = true
            if (logUpdates) {
                log.debug "Updated value of property '{}' from '{}' to '{}'", key, origValStr, newValueStr
            }
        }
    }
    changed
}
