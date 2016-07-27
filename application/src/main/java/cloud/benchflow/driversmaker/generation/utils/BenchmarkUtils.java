package cloud.benchflow.driversmaker.generation.utils;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.methods.GetMethod;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.*;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.xpath.*;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;

/**
 * @author Simone D'Avico (simonedavico@gmail.com)
 *
 * Created on 24/07/16.
 */
public class BenchmarkUtils {

    /**
     * Retrieves the address for a benchflow service from the deployment manager
     */
    public static String benchFlowServiceAddress(
            String deploymentManagerAddress,
            String privatePort,
            String benchFlowServiceId,
            String trialId,
            HttpClient http
            ) throws Exception {

        try {

            String deploymentManagerPortsApi = deploymentManagerAddress + "/projects/" +
                    trialId + "/port/" + benchFlowServiceId + "/" + privatePort;

            HttpMethod getServiceAddress = new GetMethod(deploymentManagerPortsApi);
            http.executeMethod(getServiceAddress);
            String serviceAddress = new String(getServiceAddress.getResponseBody(), "UTF-8");
            getServiceAddress.releaseConnection();
            return serviceAddress;

        } catch (Exception e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    /**
     * Takes a node and serializes it to string
     */
    public static String nodeToString(Node node, boolean omitDeclaration, boolean prettyPrint) {

        try {

            XPath xpath = XPathFactory.newInstance().newXPath();
            XPathExpression xpathExpr = xpath.compile("//text()[normalize-space()='']");
            NodeList nodeList = (NodeList) xpathExpr.evaluate(node, XPathConstants.NODESET);

            for(int i = 0; i < nodeList.getLength(); i++) {
                Node nd = nodeList.item(i);
                nd.getParentNode().removeChild(nd);
            }

            Transformer transformer = TransformerFactory.newInstance().newTransformer();
            transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
            transformer.setOutputProperty(OutputKeys.VERSION, "1.1");

            if(omitDeclaration) {
                transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
            }

            Writer writer = new StringWriter();
            transformer.transform(new DOMSource(node), new StreamResult(writer));
            return writer.toString();

        } catch (XPathExpressionException e) {
            throw new RuntimeException("An error occurred while serializing the xml node");
        } catch (TransformerException e) {
            throw new RuntimeException("An error occurred while serializing the xml node");
        }

    }

    /**
     * Takes an xml string (with declaration) and return the parsed node
     */
    public static Node stringToNode(String nodeString) {

        try {

            return DocumentBuilderFactory.newInstance()
                    .newDocumentBuilder()
                    .parse(new ByteArrayInputStream(nodeString.getBytes("utf-8")))
                    .getDocumentElement();

        } catch (SAXException e) {
            throw new RuntimeException("An error occurred while parsing the xml node");
        } catch (IOException e) {
            throw new RuntimeException("An error occurred while parsing the xml node");
        } catch (ParserConfigurationException e) {
            throw new RuntimeException("An error occurred while parsing the xml node");
        }

    }

}
