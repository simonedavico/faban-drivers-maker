package cloud.benchflow.driversmaker.generation.utils;

import com.sun.faban.harness.ParamRepository;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * @author Simone D'Avico (simonedavico@gmail.com)
 *
 * Created on 26/07/16.
 */
public class RunXml {

    private ParamRepository params;

    public RunXml(ParamRepository params) {
        this.params = params;
    }

    public Node getNode(String xPathExpression) {
        return params.getNode(xPathExpression);
    }

    public Node getNode(String xPathExpression, Element top) {
        return params.getNode(xPathExpression, top);
    }

    public NodeList getNodes(String xPathExpression) {
        return params.getNodes(xPathExpression);
    }

    public String getXPathValue(String xPathExpression) throws RuntimeException {
        try {
            return params.getParameter(xPathExpression);
        } catch(Exception e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    public Element addConfigurationNode(String baseXPath, String nodeName, String value) throws Exception {
        Element node = params.addParameter(baseXPath, null, null, nodeName);
        params.setParameter(node, value);
        params.save();
        return node;
    }

    public Element addConfigurationNode(Element parent, String nodeName, String value) throws Exception {
        Element node = params.addParameter(parent, null, null, nodeName);
        params.setParameter(node, value);
        params.save();
        return node;
    }

    public void save() throws Exception {
        params.save();
    }

    public void addProperty(Element properties, String name, String value) throws Exception {
        // Document runDoc = params.getNode("benchFlowBenchmark").getOwnerDocument();
        Document runDoc = params.getTopLevelElements().item(0).getOwnerDocument();
        Element prop = addConfigurationNode(properties,"property","");
        prop.setAttribute("name",name);
        prop.appendChild(runDoc.createTextNode(value));
        properties.appendChild(prop);
        params.save();
    }

}
