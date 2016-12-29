package uk.co.agware.carpet.util

import org.w3c.dom.Document
import org.xml.sax.InputSource
import java.io.StringReader
import java.io.StringWriter
import java.nio.charset.Charset
import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.transform.OutputKeys
import javax.xml.transform.TransformerFactory
import javax.xml.transform.dom.DOMSource
import javax.xml.transform.stream.StreamResult

/**
 * Created by Simon on 29/12/2016.
 */
class FileUtil {

    fun  byteArrayToDocument(source: ByteArray): Document {
        var factory = DocumentBuilderFactory.newInstance()
        var builder = factory.newDocumentBuilder()
        return builder.parse(InputSource(StringReader(String(source, Charset.forName("UTF8")))))
    }

    fun documentToString(doc: Document): String {
        try {
            var sw = StringWriter()
            var tf = TransformerFactory.newInstance()
            var transformer = tf.newTransformer()
            transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes")
            transformer.setOutputProperty(OutputKeys.METHOD, "xml")
            transformer.setOutputProperty(OutputKeys.INDENT, "yes")
            transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8")

            transformer.transform(DOMSource(doc), StreamResult(sw))
            return sw.toString()
        } catch (ex: Exception) {
            throw  RuntimeException("Error converting to String", ex)
        }
    }
}
