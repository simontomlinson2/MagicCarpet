package uk.co.agware.carpet.change.tasks

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonProperty
import org.apache.commons.io.IOUtils
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import uk.co.agware.carpet.database.DatabaseConnector
import uk.co.agware.carpet.exception.MagicCarpetException
import java.io.IOException
import java.io.InputStream
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
/**
 * Created by Simon on 29/12/2016.
 */
class FileTask @JsonCreator constructor(@JsonProperty("taskName") override var taskName: String, @JsonProperty("taskOrder") override var taskOrder: Int, @JsonProperty("filePath") val filePath: String, @JsonProperty("delimiter") delimiter: String?) : Task {

    private val LOGGER: Logger = LoggerFactory.getLogger(this.javaClass)
    val delimiter: String
    init {
        this.delimiter = if (delimiter == null || "" == delimiter) ";" else delimiter
    }

    constructor(): this("", 0, "", null) {

    }

    @Override
    override fun performTask(databaseConnector: DatabaseConnector?): Boolean {
        try {
            val contents: String = String(getFileContents())
            val statements: List<String> = contents.split(this.delimiter.orEmpty())
            statements.forEach { s -> if(!databaseConnector!!.executeStatement(s.trim())) return false }
        } catch (e: Exception) {
            when(e){
                is MagicCarpetException , is IOException -> {
                    this.LOGGER.error(e.message, e)
                }
                else -> throw e
            }
            return false
        }

        return true
    }

    fun getFileContents(): ByteArray {
        if(this.filePath.toLowerCase().startsWith("classpath:")){
            val filename: String = this.filePath.replace("classpath:", "")
            var input : InputStream = javaClass.getClassLoader().getResourceAsStream(filename)
            if(input != null) {
                return IOUtils.toByteArray(input)
            }
        }
        else {
            var path: Path = Paths.get(this.filePath)
            if(Files.exists(path)){
                return Files.readAllBytes(path)
            }
        }
        throw MagicCarpetException("Unable to find file " +this.filePath)
    }
}
