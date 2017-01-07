package uk.co.agware.carpet.change.tasks;

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonProperty
import uk.co.agware.carpet.database.DatabaseConnector

/**
 * Created by Simon on 29/12/2016.
 */
class ScriptTask @JsonCreator constructor(@JsonProperty("taskName") override var taskName: String, @JsonProperty("taskOrder") override var taskOrder: Int, @JsonProperty("script") val script: String, @JsonProperty("delimiter") delimiter: String?): Task {

    val delimiter: String
    val inputList: List<String>

    init {
        this.delimiter = if (delimiter == null || "" == delimiter) ";" else delimiter
        this.inputList = this.script.split(this.delimiter.orEmpty())
    }


    @Override
    override fun performTask(databaseConnector: DatabaseConnector?): Boolean {
        this.inputList.forEach { s -> if(!databaseConnector!!.executeStatement(s.trim())) return false }
        return true
    }
}
