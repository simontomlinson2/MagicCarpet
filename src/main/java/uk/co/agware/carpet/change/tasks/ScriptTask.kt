package uk.co.agware.carpet.change.tasks;

import uk.co.agware.carpet.database.DatabaseConnector

/**
 * Created by Simon on 29/12/2016.
 */
class ScriptTask(databaseConnector: DatabaseConnector, taskName: String, taskOrder: Int, script: String, delimiter: String?): Task {

    var databaseConnector = databaseConnector
    val delimiter: String
    var inputList: List<String>
    init {
        this.delimiter = if (delimiter == null || "" == delimiter) ";" else delimiter
        this.inputList = script.split(this.delimiter.orEmpty())
    }
    override var taskName = taskName
    override var taskOrder = taskOrder
    @Override
    override fun performTask(): Boolean {
        inputList.forEach { s -> if(!databaseConnector.executeStatement(s.trim())) return false }
        return true
    }
}
