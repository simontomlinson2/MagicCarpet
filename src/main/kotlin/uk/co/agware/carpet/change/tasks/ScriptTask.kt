package uk.co.agware.carpet.change.tasks;

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonProperty
import uk.co.agware.carpet.database.DatabaseConnector
import uk.co.agware.carpet.exception.MagicCarpetException

/**
 * Task that parses a script embedded in the change set to
 * execute statements on the database
 *
 * @param taskName name of the task
 * @param taskOrder order to execute the task on the database
 * @param script the script to execute on the database as statements
 * @param delimiter the delimiter used to split the script. Default ; *
 * @constructor Set task name, order and scripts with delimiter
 * Created by Simon on 29/12/2016.
 */
class ScriptTask @JsonCreator constructor(@JsonProperty("taskName") override var taskName: String,
                                          @JsonProperty("taskOrder") override var taskOrder: Int,
                                          @JsonProperty("script") val script: String,
                                          @JsonProperty("delimiter") delimiter: String?): Task {

    val delimiter = if (delimiter == null || "" == delimiter) ";" else delimiter
    val inputList = this.script.split(this.delimiter)
    override val query = this.script

    @Override
    override fun performTask(databaseConnector: DatabaseConnector?) {
        this.inputList.forEach { s ->
            try {
                databaseConnector!!.executeStatement(s.trim())
            }
            catch (e: MagicCarpetException){
                throw MagicCarpetException("Failed to execute statement: $s")
            }
        }

    }

}
