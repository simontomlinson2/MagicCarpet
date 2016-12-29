package uk.co.agware.carpet.change.tasks

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
class FileTask(databaseConnector: DatabaseConnector, taskName: String, taskOrder: Int, filePath: String, delimiter: String?) : Task {

    var databaseConnector = databaseConnector
    override var taskName = taskName
    override var taskOrder = taskOrder

    val delimiter: String
    init {
        this.delimiter = if (delimiter == null || "" == delimiter) ";" else delimiter
    }
    val filePath = filePath

    @Override
    override fun performTask(): Boolean {
        try {
            val contents: String = String(getFileContents())
            val statements: List<String> = contents.split(delimiter.orEmpty())
            statements.forEach { s -> if(!databaseConnector.executeStatement(s.trim())) return false }
        } catch (e: Exception) {
            when(e){
                is MagicCarpetException , is IOException -> {
                    LOGGER.error(e.message, e)
                }
                else -> throw e
            }
            return false
        }

        return true
    }

    fun getFileContents(): ByteArray {
        if(filePath.toLowerCase().startsWith("classpath:")){
            val filename: String = filePath.replace("classpath:", "")
            var input : InputStream = javaClass.getClassLoader().getResourceAsStream(filename)
            if(input != null) {
                return IOUtils.toByteArray(input)
            }
        }
        else {
            var path: Path = Paths.get(filePath)
            if(Files.exists(path)){
                return Files.readAllBytes(path)
            }
        }
        throw MagicCarpetException("Unable to find file " +filePath)
    }
    companion object {
        private val LOGGER: Logger = LoggerFactory.getLogger(FileTask.javaClass)
    }


}
