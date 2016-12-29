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
class MagicCarpet(databaseConnector: DatabaseConnector) {

    companion object {
        private val LOGGER: Logger = LoggerFactory.getLogger(MagicCarpet.javaClass)
        private val fileUtil: FileUtil = FileUtil()
    }

    var  changes = mutableListOf<Change>()
    var  databaseConnector: DatabaseConnector = databaseConnector
    var  inputStream: InputStream? = null
    var  devMode: Boolean = false
    var  createTable: Boolean = true
    var  xPath: XPath? = null

    constructor (databaseConnector: DatabaseConnector, devMode: Boolean): this(databaseConnector) {
        this.databaseConnector = databaseConnector
        this.devMode = devMode
    }
    fun setChangeSetFile(fileStream: FileInputStream){
        inputStream = fileStream
    }
   
    fun setChangeSetFile(filePath: Path){
        if(Files.exists(filePath)){
            try {
                inputStream = FileInputStream(filePath.toString())
            } catch (e: FileNotFoundException) {
                LOGGER.error(e.message, e)
                throw MagicCarpetException("Unable to find file: ".plus(filePath.toString()))
            }
        }
        else {
            LOGGER.error("File {} does not exist", filePath.toString())
            throw MagicCarpetException("Unable to find file: ".plus(filePath.toString()))
        }
    }

    fun parseChanges() {
        if(devMode) return

        if(inputStream == null) {
            inputStream = javaClass.getClassLoader().getResourceAsStream("ChangeSet.xml")
        }

        if(inputStream == null) {
            LOGGER.error("No ChangeSet.xml found")
            throw MagicCarpetException("No ChangeSet.xml found")
        }

        try {
            var changeSetDoc = fileUtil.byteArrayToDocument(IOUtils.toByteArray(inputStream))
            inputStream!!.close()
            xPath = XPathFactory.newInstance().newXPath()
            val changeNodes: NodeList = xPath!!.compile("changeList/change").evaluate(changeSetDoc, XPathConstants.NODESET) as NodeList
            for (i in 0 until changeNodes.length -1){
                val version: String = xPath!!.compile("@version").evaluate(changeNodes.item(i), XPathConstants.STRING) as String
                val taskNodes:NodeList  = xPath!!.compile("task").evaluate(changeNodes.item(i), XPathConstants.NODESET) as NodeList
                val tasks = buildVersionTasks(taskNodes)
                changes!!.add(Change(version, tasks))
            }

        } catch (e: Exception) {
            when(e){
                is SAXException, is ParserConfigurationException, is IOException, is XPathExpressionException -> {
                    LOGGER.error(e.message, e)
                }
                else -> throw e
            }
            LOGGER.error(e.message, e)
            throw MagicCarpetException(e.message, e)

        }
    }

    fun buildVersionTasks(taskNodes: NodeList): List<Task> {
        var tasks  = mutableListOf<Task>()
        try {
            for (i in 0 until taskNodes.length -1){
                val name: String = xPath!!.compile("@name").evaluate(taskNodes.item(i), XPathConstants.STRING) as String
                val orderString: String = xPath!!.compile("@order").evaluate(taskNodes.item(i), XPathConstants.STRING) as String
                val order: Int = if(orderString == null || "".equals(orderString)) Integer.MAX_VALUE else Integer.parseInt(orderString)
                val script: String = xPath!!.compile("script").evaluate(taskNodes.item(i), XPathConstants.STRING) as String
                val file:String = xPath!!.compile("file").evaluate(taskNodes.item(i), XPathConstants.STRING) as String
                if (file != null && !file.equals("")) {
                    val delimiter: String =  xPath!!.compile("file/@delimiter").evaluate(taskNodes.item(i), XPathConstants.STRING) as String
                    tasks.add(FileTask(databaseConnector, name, order, file, delimiter))
                } else {
                    val delimiter: String = xPath!!.compile("script/@delimiter").evaluate(taskNodes.item(i), XPathConstants.STRING) as String
                    tasks.add(ScriptTask(databaseConnector, name, order, script, delimiter))
                }
            }

        } catch (e: XPathExpressionException){
            LOGGER.error(e.message, e)
            throw MagicCarpetException(e.message, e)
        }
        return tasks
    }

    fun executeChanges(): Boolean {
        if(devMode) {
            LOGGER.info("MagicCarpet set to Dev Mode, changes not being implemented")
            return false
        }
        databaseConnector.checkChangeSetTable(createTable)
        if(changes != null && !changes!!.isEmpty()) {
            Collections.sort(changes!!)
            for(c: Change in changes!!){
                Collections.sort(c.tasks)
                for(t in c.tasks.orEmpty()){
                    if(!databaseConnector.changeExists(c.version, t.taskName)){
                        LOGGER.info("Applying Version {} Task {}", c.version, t.taskName)
                        if(t.performTask()){
                            databaseConnector.insertChange(c.version, t.taskName)
                        }
                        else {
                            databaseConnector.rollBack()
                            databaseConnector.close()
                            throw MagicCarpetException(String.format("Error while inserting Task %s for Change Version %s, see the log for additional details", t.taskName, c.version))
                        }
                    }
                }
            }
            LOGGER.info("Database updates complete")
            databaseConnector.commit()
            databaseConnector.close()
        }
        return true
    }

    fun run() {
        if(devMode) {
            LOGGER.info("MagicCarpet set to Dev Mode, changes not being implemented")
            return
        }
        parseChanges()
        executeChanges()
    }
}
