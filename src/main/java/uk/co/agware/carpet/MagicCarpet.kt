package uk.co.agware.carpet

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.fasterxml.jackson.module.kotlin.readValue
import org.apache.commons.io.IOUtils
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.w3c.dom.NodeList
import org.xml.sax.SAXException
import uk.co.agware.carpet.change.Change
import uk.co.agware.carpet.change.tasks.FileTask
import uk.co.agware.carpet.change.tasks.ScriptTask
import uk.co.agware.carpet.change.tasks.Task
import uk.co.agware.carpet.database.DatabaseConnector
import uk.co.agware.carpet.exception.MagicCarpetException
import uk.co.agware.carpet.util.FileUtil
import java.io.FileInputStream
import java.io.FileNotFoundException
import java.io.IOException
import java.io.InputStream
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.util.*
import javax.xml.parsers.ParserConfigurationException
import javax.xml.xpath.XPath
import javax.xml.xpath.XPathConstants
import javax.xml.xpath.XPathExpressionException
import javax.xml.xpath.XPathFactory

/**
 * Created by Simon on 29/12/2016.
 */
class MagicCarpet(val databaseConnector: DatabaseConnector, val fileUtil: FileUtil)   {

    private val LOGGER: Logger = LoggerFactory.getLogger(this.javaClass)
    var changes = mutableListOf<Change>()
    var path: Path = Paths.get(javaClass.getClassLoader().getResource("ChangeSet.xml").toURI())
        set (value){ if(Files.notExists(value)) throw MagicCarpetException("Unable to find file: ".plus(path.toString())) else field = value }
    var devMode = false
    val createTable = true
    val xPath: XPath = XPathFactory.newInstance().newXPath()

    constructor (databaseConnector: DatabaseConnector, fileUtil: FileUtil, devMode: Boolean): this(databaseConnector, fileUtil) {
        this.devMode = devMode
    }
    constructor (databaseConnector: DatabaseConnector, fileUtil: FileUtil, path: Path): this(databaseConnector, fileUtil) {
        this.path = path
    }

    fun parseChanges() {

        if(this.devMode) return

        if(this.path == null || Files.notExists(this.path)) {
            this.LOGGER.error("No ChangeSet found")
            throw MagicCarpetException("No ChangeSet.xml found")
            return
        }

        if(Files.isDirectory(this.path)){
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
                                            tasks.add(FileTask(this.databaseConnector, f.fileName.toString().split(Regex("-|\\."))[1], index++, f.toString(), null))
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
        try {
            var changeSetDoc = this.fileUtil.byteArrayToDocument(IOUtils.toByteArray(inputStream))
            inputStream.close()
            val changeNodes: NodeList = this.xPath.compile("changeList/change").evaluate(changeSetDoc, XPathConstants.NODESET) as NodeList
            for (i in 0 until changeNodes.length){
                val version: String = this.xPath.compile("@version").evaluate(changeNodes.item(i), XPathConstants.STRING) as String
                val taskNodes:NodeList  = this.xPath.compile("task").evaluate(changeNodes.item(i), XPathConstants.NODESET) as NodeList
                val tasks = buildVersionTasks(taskNodes)
                this.changes.add(Change(version, tasks))
            }
        } catch (e: Exception) {
            when(e){
                is SAXException, is ParserConfigurationException, is IOException, is XPathExpressionException -> {
                    this.LOGGER.error(e.message, e)
                }
                else -> throw e
            }
        }
    }

    fun buildJSON(inputStream: InputStream){
        val mapper = ObjectMapper().registerModule(KotlinModule())
        this.changes = mapper.readValue(IOUtils.toByteArray(inputStream))
        this.changes.forEach { c -> c.tasks!!.forEach { t -> t.databaseConnector = this.databaseConnector } }
    }

    fun buildChanges(path: Path){
        var inputStream: InputStream

        if(Files.exists(path)){
            try {
                inputStream = FileInputStream(path.toString())
            } catch (e: FileNotFoundException) {
                this.LOGGER.error(e.message, e)
                throw MagicCarpetException("Unable to find file: ".plus(path.toString()))
            }
        }
        else {
            this.LOGGER.error("File {} does not exist", path.toString())
            throw MagicCarpetException("Unable to find file: ".plus(path.toString()))
        }
        if(path.fileName.toString().endsWith(".json")){
            buildJSON(inputStream)
        }
        else {
            buildXML(inputStream)
        }

    }

    fun buildVersionTasks(taskNodes: NodeList): List<Task> {
        var tasks  = mutableListOf<Task>()
        try {
            for (i in 0 until taskNodes.length){
                val name: String = this.xPath.compile("@name").evaluate(taskNodes.item(i), XPathConstants.STRING) as String
                val orderString: String = this.xPath.compile("@order").evaluate(taskNodes.item(i), XPathConstants.STRING) as String
                val order: Int = if(orderString == null || "".equals(orderString)) Integer.MAX_VALUE else Integer.parseInt(orderString)
                val script: String = this.xPath.compile("script").evaluate(taskNodes.item(i), XPathConstants.STRING) as String
                val file:String = this.xPath.compile("file").evaluate(taskNodes.item(i), XPathConstants.STRING) as String
                if (file != null && !file.equals("")) {
                    val delimiter: String =  this.xPath.compile("file/@delimiter").evaluate(taskNodes.item(i), XPathConstants.STRING) as String
                    tasks.add(FileTask(this.databaseConnector, name, order, file, delimiter))
                } else {
                    val delimiter: String = this.xPath.compile("script/@delimiter").evaluate(taskNodes.item(i), XPathConstants.STRING) as String
                    tasks.add(ScriptTask(this.databaseConnector, name, order, script, delimiter))
                }
            }

        } catch (e: XPathExpressionException){
            this.LOGGER.error(e.message, e)
            throw MagicCarpetException(e.message, e)
        }
        return tasks
    }

    fun executeChanges(): Boolean {
        if(this.devMode) {
            this.LOGGER.info("MagicCarpet set to Dev Mode, changes not being implemented")
            return false
        }
        this.databaseConnector.checkChangeSetTable(this.createTable)
        if(this.changes != null && !this.changes.isEmpty()) {
            Collections.sort(this.changes)
            for(c: Change in this.changes){
                Collections.sort(c.tasks)
                for(t in c.tasks.orEmpty()){
                    if(!this.databaseConnector.changeExists(c.version, t.taskName)){
                        this.LOGGER.info("Applying Version {} Task {}", c.version, t.taskName)
                        if(t.performTask()){
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
            this.LOGGER.info("Database updates complete")
            this.databaseConnector.commit()
            this.databaseConnector.close()
        }
        return true
    }

    fun run() {
        if(this.devMode) {
            this.LOGGER.info("MagicCarpet set to Dev Mode, changes not being implemented")
            return
        }
        parseChanges()
        executeChanges()
    }
}
