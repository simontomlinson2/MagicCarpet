package uk.co.agware.carpet.change.tasks

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonProperty
import org.apache.commons.io.IOUtils
import uk.co.agware.carpet.database.DatabaseConnector
import uk.co.agware.carpet.exception.MagicCarpetException
import java.io.InputStream
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

/**
 * Task that uses a file path to specify changes to the database.
 * The statements in the file are split using the delimiter.
 *
 * @param taskName name of the task
 * @param taskOrder order to execute the task on the database.
 * @param filePath the file path to the script to execute on the database
 * @param delimiter the delimiter used to split the script into individual statements. Default ;
 * @constructor Set task name, order and scripts with delimiter
 * Created by Simon on 29/12/2016.
 */
class FileTask @JsonCreator constructor(@JsonProperty("taskName") override var taskName: String,
                                        @JsonProperty("taskOrder") override var taskOrder: Int,
                                        @JsonProperty("filePath") val filePath: String,
                                        @JsonProperty("delimiter") delimiter: String?) : Task {

    val delimiter = if (delimiter == null || "" == delimiter) ";" else delimiter
    override val query = getFileContents()

    @Override
    override fun performTask(databaseConnector: DatabaseConnector?) {
        val contents: String = this.query
        val statements: List<String> = contents.split(this.delimiter.orEmpty())
        statements.forEach { s ->
            try {
                databaseConnector!!.executeStatement(s.trim())
            }
            catch (e: MagicCarpetException){
                throw MagicCarpetException("Failed to execute statement: $s")
            }
        }

    }

    // TODO Tidy this method up, the execution paths are screwed up and the error handling is wrong
    fun getFileContents(): String {
        //check for file path
        val path: Path = Paths.get(this.filePath)
        if(Files.exists(path)){
            return Files.readAllBytes(path).toString()
        }
        //try classpath file
        else if(this.filePath.toLowerCase().startsWith("classpath:")){
            val filename: String = this.filePath.replace("classpath:", "")
            val input : InputStream = javaClass.classLoader.getResourceAsStream(filename)
            return IOUtils.toString(input)
        }
        throw MagicCarpetException("Unable to find file " +this.filePath)
    }

}
