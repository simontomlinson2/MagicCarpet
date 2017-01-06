package uk.co.agware.carpet.change.tasks;

import com.fasterxml.jackson.annotation.JsonIgnore
import uk.co.agware.carpet.database.DatabaseConnector

/**
 * Created by Simon on 29/12/2016.
 */
class ScriptTask(@JsonIgnore override var databaseConnector: DatabaseConnector?, override var taskName: String, override var taskOrder: Int, val script: String, delimiter: String?): Task {

    val delimiter: String
    val inputList: List<String>

    init {
        this.delimiter = if (delimiter == null || "" == delimiter) ";" else delimiter
        this.inputList = this.script.split(this.delimiter.orEmpty())
    }

    @Override
    override fun performTask(): Boolean {
        this.inputList.forEach { s -> if(!this.databaseConnector!!.executeStatement(s.trim())) return false }
        return true
    }
}
