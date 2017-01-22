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
import uk.co.agware.carpet.exception.MagicCarpetException
import java.io.FileInputStream
import java.io.FileNotFoundException
import java.io.InputStream
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths


/**
 * // TODO "s" file path?
 * Executes a set of changes on a database from s file path.
 *
 * // TODO This is the main entry point to the library and basically the only class the user will directly
 * // TODO interact with for the majority of configurations, this needs to have a bit more detail still
 * Changes can be JSON, XML or numbered files
 *
 * @param databaseConnector connection to the database to update.
 * @param devMode when set changes will not be executed on the database
 * @constructor Sets the database connection and dev mode.
 * @see uk.co.agware.carpet.database.DatabaseConnector
 * Created by Simon on 29/12/2016.
 */
// TODO public methods use JavaDoc comments, private use block comments, inline use single line comments
// TODO there is a nice white line down the right of IntelliJ, use that to show you how long a line of code should roughly be
// TODO i.e. That line should have just gone over it

// TODO Needs debug level logging in places it makes sense to have it so that it is easier to see whats going on
// TODO for someone with this library in their application if they need to actually do some debugging
class MagicCarpet(val databaseConnector: DatabaseConnector, var devMode: Boolean = false)   {

    var changes: List<Change> = listOf()
    val createTable = true
    var path: Path = Paths.get(javaClass.classLoader.getResource("ChangeSet.xml").toURI())
        set (value){
            if(Files.notExists(value))
                throw MagicCarpetException("Unable to find file: ".plus(path.toString()))
            else field = value
        }
    private val logger: Logger = LoggerFactory.getLogger(this.javaClass)
    private val jsonMapper = ObjectMapper().registerModule(KotlinModule())
    private val xmlMapper = XmlMapper()

    /**
     * // TODO "ot"
     * // TODO Description is pointless, it is exactly what the method name says
     * Get the ChangeSet.json ot ChangeSet.xml
     *
     * @param originalPath the path to the Json or Xml File.
     * @return Path path of the ChangeSet.json or ChangeSet.xml
     *
     */
    // TODO That is a pointlessly long statement, format it so that its actually readable
    private fun getJsonOrXmlPath(originalPath: Path) : Path {
        return if(Files.exists(originalPath.resolve("ChangeSet.json")) ) originalPath.resolve("ChangeSet.json") else originalPath.resolve("ChangeSet.xml")
    }

    /**
     * Get the Changes from a file structure
     * If ChangeSet.json or ChangeSet.xml exist in the folder the changes are added to *changes*
     * Else each directory is walked and files added to change list using the directory name as the task name.
     *
     * @param path the path of the root file structure.
    */
    // TODO Format the code better, this should also return a collection instead of adding to the existing one from within
    // TODO Each call in the chain should be on a new line and aligned with the previous (Have to do it manually for now)
    // TODO Avoid multi-line blocks inside the lambdas where possible as it makes the code messy and annoying
    private fun addTasksFromDirectory(path: Path) {
        Files.walk(path).forEach {
            p ->
            if (p != path) {
                val filePath = getJsonOrXmlPath(p)
                if (Files.exists(filePath)) {
                    buildChanges(filePath)
                } else if (Files.isDirectory(p)) {
                    //For Each File in directory. Sort it. Filter out the parent folder and create a FileTask from the file name and path
                    val tasks = Files.walk(p).sorted().filter { f -> f != p }.toArray().asList().map { it as Path }.mapIndexed { i, f ->
                                val name = f.fileName.toString().split(Regex("-|\\."))[1]
                                return@mapIndexed FileTask(name, i, f.toString(), null)
                             }
                    this.changes += Change(p.fileName.toString(), tasks)
                }
            }
        }
    }

    /**
     * // TODO That is not how to write a comment that includes @throws, check the docs again
     * If devMode is true return
     * @throws MagicCarpetException if path does not exist
     * If Path contains ChangeSet.json or ChangeSet.xml then add changes to *changes*
     * Else add the tasks from the directory structure
     */
    fun parseChanges() {
        if(this.devMode) return
        //File Does Not Exist
        if(Files.notExists(this.path)) {
            throw MagicCarpetException("Path not found")
        }
        val path = getJsonOrXmlPath(this.path)
        if(Files.isDirectory(this.path)){
            //Contains ChangeSet.xml or ChangeSet.json
            if (Files.exists(path)) {
                buildChanges(path)
            }
            else{
                addTasksFromDirectory(this.path)
            }
        }
        else{
            buildChanges(this.path)
        }

    }

    /**
     * Build the changes into *changes* from the path
     * Detects if file is JSON or XML
     *
     * @param path the path of the changes
     * @throws MagicCarpetException if file does not exist
     */
    // TODO Use string templating in your exception messages instead of what you have done there
    // TODO Also shouldn't need to call .toString() on an object, toString gets called implicitly most of the time
    private fun buildChanges(path: Path){
        val inputStream: InputStream

        // TODO Use a try/finally or try with resources pattern here, you wont be closing inputStream when you get errors
        if(Files.exists(path)){
            try {
                inputStream = FileInputStream(path.toString())
            } catch (e: FileNotFoundException) { // TODO Should go on the next line, I hate catch here, it makes no sense
                throw MagicCarpetException("Unable to find file: ".plus(path.toString()))
            }
        }
        else {
            throw MagicCarpetException("Unable to find file: ".plus(path.toString()))
        }
        if(path.fileName.toString().endsWith(".json")){
            this.changes = jsonMapper.readValue(inputStream)
        }
        else {
            this.changes = xmlMapper.readValue(inputStream)
        }
        inputStream.close()
    }

    /**
     * Perform each task on the database
     * No changes are implemented if *devMode* is set // TODO Should be a space between the description and the extra info
     * @throws MagicCarpetException on task fail
     * @return boolean tasks all executed successfully
     */
    // TODO Returning a boolean from this method doesn't really work, should be throwing an error when something goes wrong
    // TODO And just not returning anything if things are OK / not needed
    fun executeChanges(): Boolean {
        if(this.devMode) {
            this.logger.info("MagicCarpet set to Dev Mode, changes not being implemented")
            return false
        }
        this.databaseConnector.checkChangeSetTable(this.createTable)
        if(!this.changes.isEmpty()) {
            // TODO Need to format this better, see above comments on how, that's practically unreadable right now
            Sequence {  this.changes.iterator() }.forEach { c -> Sequence { c.tasks!!.sorted().iterator() }.forEach { t ->
                if(!this.databaseConnector.changeExists(c.version, t.taskName)){
                    this.logger.info("Applying Version {} Task {}", c.version, t.taskName)
                    if(t.performTask(this.databaseConnector)){
                        this.databaseConnector.insertChange(c.version, t.taskName)
                    }
                    else {
                        this.databaseConnector.rollBack()
                        this.databaseConnector.close()
                        // TODO String templating
                        throw MagicCarpetException(String.format("Error while inserting Task %s for Change Version %s, see the log for additional details", t.taskName, c.version))
                    }
                }
            } }
            // TODO The connection should always be closed, no matter what happens, try/finally or some variation of
            this.logger.info("Database updates complete")
            this.databaseConnector.commit()
            this.databaseConnector.close()
        }
        return true
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
