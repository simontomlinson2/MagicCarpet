package uk.co.agware.carpet

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.xml.XmlMapper
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.fasterxml.jackson.module.kotlin.readValue
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import uk.co.agware.carpet.change.Change
import uk.co.agware.carpet.change.tasks.FileTask
import uk.co.agware.carpet.database.DatabaseConnector
import uk.co.agware.carpet.database.use
import uk.co.agware.carpet.exception.MagicCarpetDatabaseException
import uk.co.agware.carpet.exception.MagicCarpetException
import uk.co.agware.carpet.exception.MagicCarpetParseException
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.util.regex.Pattern
import java.util.stream.Stream

/**
 * Executes a set of changes on a database from a specified change set.
 * Accepts changes in JSON, XML or as a directory structure with numbered files
 * Changes are executed through the assigned database connection
 * Changes are tracked through an additional database table
 * Table will automatically be added or updated when run
 * If a change has already been made on the database it is ignored
 * Change statements are hashed in the database table
 * Changes can be Scripts, files or files on the classpath
 *
 * @param databaseConnector connection to the database to update.
 * @param devMode when set changes will not be executed on the database
 * @constructor Sets the database connection and dev mode.
 * @see uk.co.agware.carpet.database.DatabaseConnector
 * Created by Simon on 29/12/2016.
 */
// TODO Needs debug level logging in places it makes sense to have it so that it is easier to see whats going on
// TODO for someone with this library in their application if they need to actually do some debugging

// TODO Should ideally filter the directory contents by .sql when checking for the task files
class MagicCarpet(val databaseConnector: DatabaseConnector, val devMode: Boolean = false, basePath: Path?) {

    private val logger: Logger = LoggerFactory.getLogger(this.javaClass)

    var changes: List<Change> = listOf()
    val createTable = true

    // Sets the base path to either the supplied value or searches for a ChangeSet.xml or ChangeSet.json
    val path = basePath ?: getJsonOrXmlPath(Paths.get("."))

    private val jsonMapper = ObjectMapper().registerModule(KotlinModule())
    private val xmlMapper = XmlMapper()

    /* Returns the JSON or XML file at a given Path */
    private fun getJsonOrXmlPath(originalPath: Path) : Path {
        val path = originalPath.resolve("ChangeSet.json")
        return if(Files.exists(path)) path else originalPath.resolve("ChangeSet.xml")
    }

    /**
     * If devMode is true return
     * If Path contains ChangeSet.json or ChangeSet.xml then add changes to changes
     * Else add the tasks from the directory structure
     */
    fun parseChanges() {
        if(this.devMode) return

        if(Files.notExists(this.path)) {
            throw MagicCarpetParseException("Unable to find file at ${this.path}")
        }

        // If the path is a directory then we check for a ChangeSet file
        if(Files.isDirectory(this.path)) {

            // Try and find the file
            val path = getJsonOrXmlPath(this.path)
            // If there is a file then use that
            if (Files.exists(path)) {
                logger.debug("Building changes from {} ", path)
                this.changes = buildChanges(path)
            }
            else { // Otherwise use this file as the base directory to search for files and folders
                logger.debug("Adding tasks from directory structure at: {} ", path)
                this.changes = addTasksFromDirectory(this.path)
            }
        }
        else { // Otherwise we build from the file
            logger.debug("Adding tasks from {} ", this.path)
            this.changes = buildChanges(this.path)
        }
    }

    /*
     * Iterates through all folders within the supplied path and builds up a set
     * of changes to be applied. It will only check one level deep to avoid checking
     * the same file multiple times and building an incorrect graph
     */
    private fun addTasksFromDirectory(path: Path): List<Change> {
        val directoryNameCheck = Regex("""\d\.\d(\.\d)+""")
        return Files.walk(path, 1)
          .toList()
          .filter { directoryNameCheck.matches(it.fileName.toString()) } // Get only paths matching x.x.x
          .flatMap { p -> pathToChanges(p) }
    }

    /*
     * Checks the current path for a ChangeSet file, if it exists then it is used to build up the
     * changes from this folder, otherwise the folder itself will be used as the base and its contents
     * will become the tasks
     */
    private fun pathToChanges(path: Path): List<Change> {
        val dirName = path.fileName.toString()
        val file = getJsonOrXmlPath(path)

        if(Files.exists(file)) {
            return buildChanges(file)
        }

        val tasks = pathToTasks(path)
        val change = Change(dirName, tasks)
        return listOf(change)
    }

    /* Reads all files in the directory and creates a task list from them */
    private fun pathToTasks(path: Path): List<FileTask> {
        // Will match three groups, the first being potentially a number, the second being potentially
        // some set of separating characters and the third being everything else, this is for matching
        // file names such as "12 - Create new Table" and "1.Add some things" and extracting both the
        // number and the name without worrying if the number and/or separating characters do not exist
        // The only edge case on this is going to be if someone starts a Task name with a number.
        val filenameMatch = Regex("""(\d?)([ -:.]?)(.?)""").toPattern()

        return Files.walk(path)
          .toList()
          .map { p -> createFileTask(p, filenameMatch) }
    }

    /* Creates a FileTask from a given path */
    private fun createFileTask(filePath: Path, pattern: Pattern): FileTask {
        val fileName = filePath.fileName.toString()
        val matcher = pattern.matcher(fileName)
        matcher.find()
        val order = matcher.group(1) ?: "1000000" // If no number is supplied then set a high number
        val taskName = matcher.group(3)

        return FileTask(taskName, order.toInt(), filePath.toString())
    }

    /* Converts the file at the supplied path into a list of Changes */
    private fun buildChanges(path: Path): List<Change> {
        if(!Files.exists(path)) throw MagicCarpetParseException("File does not exist: $path")

        try {
            val contents = Files.readAllBytes(path)

            if (path.endsWith(".json")) {
                return jsonMapper.readValue(contents)
            } else {
                return xmlMapper.readValue(contents)
            }
        }
        catch (e: IOException) {
            throw MagicCarpetParseException("Unable to read file at $path. ${e.message}")
        }
    }

    /**
     * Perform each task on the database
     * No changes are implemented if devMode is set
     *
     * @return boolean tasks all executed successfully
     */
    fun executeChanges() {
        if(this.devMode) {
            this.logger.info("MagicCarpet set to Dev Mode, changes not being implemented")
            return
        }

        // Will close the database connector after completion
        this.databaseConnector.use { connector ->
            connector.checkChangeSetTable(this.createTable)

            this.changes.sorted()
              .forEach { change ->
                  // Check if the change already exists
                    // If so then check the Hash
                    // If it doesn't exist then create it
                    // If it does exist and doesn't match then throw an error, rollback the transaction

                  // If the change doesn't exist then attempt to create it
                  // If error then rollback the transaction and throw the error
              }
            connector.commit()
        }

        // TODO This will all be deleted and there will be some changes to the DatabaseConnector
        Sequence {  this.changes.iterator() }
          .forEach { c ->
              Sequence { c.tasks.sorted().iterator() }
                .forEach { t ->
                    if(!this.databaseConnector.changeExists(c.version, t.taskName, t.query)){
                        this.logger.info("Applying Version {} Task {}", c.version, t.taskName)
                        try {
                            t.performTask(this.databaseConnector)
                            this.databaseConnector.insertChange(c.version, t.taskName, t.query)
                            this.logger.info("Database update complete for task: {}", t.taskName)
                            this.databaseConnector.commit()
                        }
                        catch (e: MagicCarpetException){
                            this.databaseConnector.rollBack()
                            throw MagicCarpetDatabaseException("Error while inserting Task ${t.taskName}" +
                              " for Change Version ${c.version}," +
                              " see the log for additional details")
                        }
                        finally {
                            this.databaseConnector.close()
                        }
                    }
                    else {
                        this.databaseConnector.updateChange(c.version, t.taskName, t.query)
                    }
                } }
    }

    /**
     * Parse Changes and Execute
     * When *devMode* is set return
     * @throws MagicCarpetException on task fail
     * @return boolean tasks all executed successfully
     */
    fun run() {
        if(this.devMode) {
            this.logger.info("MagicCarpet set to Dev Mode, changes not being implemented")
            return
        }
        parseChanges()
        executeChanges()
    }
}

/* I needed it a few times... */
fun <T> Stream<T>.toList(): List<T> {
    return Sequence { this.iterator() }.toList()
}
