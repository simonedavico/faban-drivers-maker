import io.minio.MinioClient

//import java.io.{ByteArrayInputStream, StringWriter}
//import javax.xml.parsers.{DocumentBuilderFactory, DocumentBuilder}
//import javax.xml.transform.{TransformerException, OutputKeys, Transformer, TransformerFactory}
//import javax.xml.transform.dom.DOMSource
//import javax.xml.transform.stream.StreamResult
//import javax.xml.xpath._
//
//import com.sun.org.apache.xerces.internal.dom.DocumentImpl
//import org.apache.commons.lang3.StringEscapeUtils
//import org.w3c.dom.{NodeList, Node, Document}
//
//def nodeToString(node: Node, omitDecl: Boolean, pretty: Boolean) = {
//
//  if(node == null) {
//    throw new Exception("")
//  }
//
//  try {
//
//    val xpath: XPath = XPathFactory.newInstance().newXPath()
//    val xpathExpr: XPathExpression = xpath.compile("//text()[normalize-space()='']")
//    val nodeList: NodeList = xpathExpr.evaluate(node, XPathConstants.NODESET).asInstanceOf[NodeList]
//
//    var i = 0
//
//    while(i < nodeList.getLength) {
//      val nd: Node = nodeList.item(i)
//      nd.getParentNode.removeChild(nd)
//      i = i + 1
//    }
//
//    val transformer: Transformer = TransformerFactory.newInstance().newTransformer()
//    transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8")
//    transformer.setOutputProperty(OutputKeys.VERSION, "1.1")
//    if(omitDecl) {
//      transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes")
//    }
//
//    if(pretty) {
//      transformer.setOutputProperty(OutputKeys.INDENT, "yes")
//      transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "4")
//    }
//
//    val writer = new StringWriter()
//    transformer.transform(new DOMSource(node), new StreamResult(writer))
//    writer.toString
//  }
//  catch {
//    case e: TransformerException => ???
//    case e2: XPathExpressionException => ???
//  }
//
//}
//
//val xmlDoc: Document = new DocumentImpl()
//
//val root = xmlDoc.createElement("root")
//
//xmlDoc.appendChild(root)
//root.setTextContent(StringEscapeUtils.escapeXml11("""<ciao name="foobar"></ciao>"""))
//
//val escaped = xmlDoc.getElementsByTagName("root").item(0).getTextContent
//
////println(StringEscapeUtils.unescapeXml(escaped))
//println(nodeToString(root, omitDecl = false, pretty = true))
//
////node to string
//val stringified = nodeToString(root, omitDecl = false, pretty = true)
//
////string to node
//val docBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder()
//val foo: Node = docBuilder.parse(new ByteArrayInputStream(stringified.getBytes("utf-8"))).getDocumentElement
//foo.getTextContent

val mc = new MinioClient("http://localhost:19000", "AKIAIOSFODNN7EXAMPLE", "wJalrXUtnFEMI/K7MDENG/bPxRfiCYEXAMPLEKEY")
val is = mc.getObject("benchmarks", "BenchFlow/WfMSTest/1/benchflow-test.yml")

