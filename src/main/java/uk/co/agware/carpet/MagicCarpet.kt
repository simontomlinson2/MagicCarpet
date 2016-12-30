package uk.co.agware.carpet

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
    val changes = mutableListOf<Change>()
    var inputStream: InputStream = javaClass.getClassLoader().getResourceAsStream("ChangeSet.xml")
    var devMode = false
    val createTable = true
    val xPath: XPath = XPathFactory.newInstance().newXPath()

    constructor (databaseConnector: DatabaseConnector, fileUtil: FileUtil, devMode: Boolean): this(databaseConnector, fileUtil) {
        this.devMode = devMode
    }

    fun setChangeSetFile(fileStream: FileInputStream){
        this.inputStream = fileStream
    }

    fun setChangeSetFile(filePath: Path){
        if(Files.exists(filePath)){
            try {
                this.inputStream = FileInputStream(filePath.toString())
            } catch (e: FileNotFoundException) {
                this.LOGGER.error(e.message, e)
                throw MagicCarpetException("Unable to find file: ".plus(filePath.toString()))
            }
        }
        else {
            this.LOGGER.error("File {} does not exist", filePath.toString())
            throw MagicCarpetException("Unable to find file: ".plus(filePath.toString()))
        }
    }

    fun parseChanges() {
        if(this.devMode) return

        if(this.inputStream == null) {
            this.LOGGER.error("No ChangeSet.xml found")
            throw MagicCarpetException("No ChangeSet.xml found")
        }

        try {
            var changeSetDoc = this.fileUtil.byteArrayToDocument(IOUtils.toByteArray(this.inputStream))
            this.inputStream.close()
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
