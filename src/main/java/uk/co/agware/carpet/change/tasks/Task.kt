package uk.co.agware.carpet.change.tasks

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonSubTypes.Type
import com.fasterxml.jackson.annotation.JsonTypeInfo
import uk.co.agware.carpet.database.DatabaseConnector

/**
 * Created by Simon on 29/12/2016.
 */
@JsonTypeInfo(
    use = JsonTypeInfo.Id.NAME,
    include = JsonTypeInfo.As.PROPERTY,
    property = "type")
@JsonSubTypes(
    Type(value = FileTask::class, name = "FileTask"),
    Type(value = ScriptTask::class, name = "ScriptTask")
)
interface Task : Comparable<Task> {
    var databaseConnector: DatabaseConnector?
    var taskName: String
    var taskOrder: Int
    fun performTask(): Boolean

    @Override
    override fun compareTo(o: Task): Int {
        return this.taskOrder.compareTo(o.taskOrder)
    }
}