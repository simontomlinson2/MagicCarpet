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
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths


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


    private fun getJsonOrXmlPath(originalPath: Path) : Path {
        return if(Files.exists(originalPath.resolve("ChangeSet.json")) )
            originalPath.resolve("ChangeSet.json")
        else originalPath.resolve("ChangeSet.xml")
    }

    /*
     * Get the Changes from a file structure
     * If ChangeSet.json or ChangeSet.xml exist in the folder the changes are added to *changes*
     * Else each directory is walked and files added to change list using the directory name as the task name.
     *
     * @param path the path of the root file structure.
    */
    private fun addTasksFromDirectory(path: Path): List<Change> {
        var changes: List<Change> = listOf()
        Files.walk(path).forEach {
            p ->
            if (p != path) {
                val filePath = getJsonOrXmlPath(p)
                if (Files.exists(filePath)) {
                    buildChanges(filePath)
                } else if (Files.isDirectory(p)) {
                    //For Each File in directory. Sort it. Filter out the parent folder and create a FileTask from the file name and path
                    val tasks = Files.walk(p)
                            .sorted()
                            .filter { f -> f != p }
                            .toArray()
                            .asList()
                            .map { it as Path }
                            .mapIndexed { i, f ->
                                val name = f.fileName.toString().split(Regex("-|\\."))[1]
                                logger.debug("Creating Task for file: {}", name)
                                return@mapIndexed FileTask(name, i, f.toString(), null)
                            }
                    logger.debug("Creating change for : {}", p.fileName.toString())
                    changes += Change(p.fileName.toString(), tasks)
                }
            }
        }
        return changes
    }

    /**
     * If devMode is true return
     * If Path contains ChangeSet.json or ChangeSet.xml then add changes to changes
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
                logger.debug("Building changes for: {} ", path)
                buildChanges(path)
            }
            else{
                logger.debug("Adding tasks from directory structure at: {} ", path)
                addTasksFromDirectory(this.path)
            }
        }
        else{
            logger.debug("Adding tasks from directory structure at: {} ", this.path)
            buildChanges(this.path)
        }

    }

    /*
     * Build the changes into changes from the path
     * Detects if file is JSON or XML
     */
    private fun buildChanges(path: Path){
        FileInputStream(path.toString()).use { inputStream ->
            if (!Files.exists(path))
                throw MagicCarpetException("File does not exist: $path")
            if(path.fileName.toString().endsWith(".json")){
                this.changes = jsonMapper.readValue(inputStream)
            }
            else {
                this.changes = xmlMapper.readValue(inputStream)
            }
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
        this.databaseConnector.checkChangeSetTable(this.createTable)
        if(!this.changes.isEmpty()) {
            Sequence {  this.changes.iterator() }
                    .forEach { c ->
                        Sequence { c.tasks!!.sorted().iterator() }
                                .forEach { t ->
                                    if(!this.databaseConnector.changeExists(c.version, t.taskName, t.query)){
                                        this.logger.info("Applying Version {} Task {}", c.version, t.taskName)
                                        try {
                                            t.performTask(this.databaseConnector)
                                            this.databaseConnector.insertChange(c.version, t.taskName, t.query)
                                            this.logger.info("Database updates complete")
                                            this.databaseConnector.commit()
                                        }
                                        catch (e: MagicCarpetException){
                                            this.databaseConnector.rollBack()
                                            throw MagicCarpetException("Error while inserting Task ${t.taskName}" +
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
