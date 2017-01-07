package uk.co.agware.carpet

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.xml.XmlMapper
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.fasterxml.jackson.module.kotlin.readValue
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import uk.co.agware.carpet.change.Change
import uk.co.agware.carpet.change.tasks.FileTask
import uk.co.agware.carpet.change.tasks.Task
import uk.co.agware.carpet.database.DatabaseConnector
import uk.co.agware.carpet.exception.MagicCarpetException
import java.io.FileInputStream
import java.io.FileNotFoundException
import java.io.InputStream
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.util.*



/**
 * Created by Simon on 29/12/2016.
 */
class MagicCarpet(val databaseConnector: DatabaseConnector)   {

    var changes = mutableListOf<Change>()
    var devMode = false
    val createTable = true
    var path: Path = Paths.get(javaClass.classLoader.getResource("ChangeSet.xml").toURI())
        set (value){
            if(Files.notExists(value))
                throw MagicCarpetException("Unable to find file: ".plus(path.toString()))
            else field = value
        }
    private val logger: Logger = LoggerFactory.getLogger(this.javaClass)

    constructor (databaseConnector: DatabaseConnector, devMode: Boolean): this(databaseConnector) {
        this.devMode = devMode
    }
    constructor (databaseConnector: DatabaseConnector, path: Path): this(databaseConnector) {
        this.path = path
    }

    fun parseChanges() {

        if(this.devMode) return

        //File Does Not Exist
        if(Files.notExists(this.path)) {
            this.logger.error("No ChangeSet found")
            throw MagicCarpetException("No ChangeSet found")
            return
        }

        if(Files.isDirectory(this.path)){
            //Contains ChangeSet.xml or ChangeSet.json
            if (Files.exists(Paths.get(this.path.toString(),"ChangeSet.xml"))){
                buildChanges(Paths.get(this.path.toString(),"ChangeSet.xml"))
            }
            else if (Files.exists(Paths.get(this.path.toString(),"ChangeSet.json"))){
                buildChanges(Paths.get(this.path.toString(),"ChangeSet.json"))
            }
            else{
                var paths =  Files.walk(this.path)
                paths.forEach {
                    p ->
                    if(p != this.path){
                        if (Files.exists(Paths.get(p.toString(),"ChangeSet.xml"))){
                            buildChanges(Paths.get(p.toString(),"ChangeSet.xml"))
                        }
                        else if (Files.exists(Paths.get(p.toString(),"ChangeSet.json"))){
                            buildChanges(Paths.get(p.toString(),"ChangeSet.json"))
                        }
                        else if (Files.isDirectory(p)){
                            val tasks = mutableListOf<Task>()
                            var index = 0
                            Files.walk(p)
                                    .sorted()
                                    .forEach {
                                        f ->
                                        if(f != p)
                                            tasks.add(FileTask(f.fileName.toString().split(Regex("-|\\."))[1], index++, f.toString(), null))
                                    }
                            this.changes.add(Change(p.fileName.toString(),tasks))
                        }
                    }
                }
            }
        }
        else{
            buildChanges(this.path)
        }


    }

    fun buildXML(inputStream: InputStream){
        val mapper = XmlMapper()
        this.changes = mapper.readValue(inputStream)
    }


    fun buildJSON(inputStream: InputStream){
        val mapper = ObjectMapper().registerModule(KotlinModule())
        this.changes = mapper.readValue(inputStream)
    }

    fun buildChanges(path: Path){
        var inputStream: InputStream

        if(Files.exists(path)){
            try {
                inputStream = FileInputStream(path.toString())
            } catch (e: FileNotFoundException) {
                this.logger.error(e.message, e)
                throw MagicCarpetException("Unable to find file: ".plus(path.toString()))
            }
        }
        else {
            this.logger.error("File {} does not exist", path.toString())
            throw MagicCarpetException("Unable to find file: ".plus(path.toString()))
        }
        if(path.fileName.toString().endsWith(".json")){
            buildJSON(inputStream)
        }
        else {
            buildXML(inputStream)
        }
        inputStream.close()
    }

    fun executeChanges(): Boolean {
        if(this.devMode) {
            this.logger.info("MagicCarpet set to Dev Mode, changes not being implemented")
            return false
        }
        this.databaseConnector.checkChangeSetTable(this.createTable)
        if(!this.changes.isEmpty()) {
            Collections.sort(this.changes)
            for(c: Change in this.changes){
                Collections.sort(c.tasks)
                for(t in c.tasks.orEmpty()){
                    if(!this.databaseConnector.changeExists(c.version, t.taskName)){
                        this.logger.info("Applying Version {} Task {}", c.version, t.taskName)
                        if(t.performTask(this.databaseConnector)){
                            this.databaseConnector.insertChange(c.version, t.taskName)
                        }
                        else {
                            this.databaseConnector.rollBack()
                            this.databaseConnector.close()
                            throw MagicCarpetException(String.format("Error while inserting Task %s for Change Version %s, see the log for additional details", t.taskName, c.version))
                        }
                    }
                }
            }
            this.logger.info("Database updates complete")
            this.databaseConnector.commit()
            this.databaseConnector.close()
        }
        return true
    }

    fun run() {
        if(this.devMode) {
            this.logger.info("MagicCarpet set to Dev Mode, changes not being implemented")
            return
        }
        parseChanges()
        executeChanges()
    }
}
