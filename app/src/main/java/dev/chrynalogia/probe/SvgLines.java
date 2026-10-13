package dev.chrynalogia.probe;

import java.io.StringReader;
import java.io.StringWriter;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

/** Geometry stays intact; the app owns line paint, including inline/exported CSS styles. */
final class SvgLines {
    static String normalize(String xml,float width,float height) throws Exception {
        if(xml.startsWith("\uFEFF"))xml=xml.substring(1);
        if(xml.contains("<!DOCTYPE")||xml.contains("<!ENTITY"))throw new java.io.IOException("외부 문서 선언 없는 SVG 선 파일을 사용해 주세요.");
        DocumentBuilderFactory factory=DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);factory.setExpandEntityReferences(false);
        var document=factory.newDocumentBuilder().parse(new InputSource(new StringReader(xml)));
        Element root=document.getDocumentElement();
        if(width>0&&height>0&&!root.hasAttribute("viewBox"))root.setAttribute("viewBox","0 0 "+width+" "+height);
        NodeList elements=document.getElementsByTagName("*");
        for(int i=0;i<elements.getLength();i++) {
            Element element=(Element)elements.item(i);
            for(String property:new String[]{"stroke-width","stroke","fill"})element.removeAttribute(property);
            if(element.hasAttribute("style"))element.setAttribute("style",stripPaint(element.getAttribute("style")));
            if("style".equals(element.getLocalName()))element.setTextContent(stripPaint(element.getTextContent()));
        }
        var transformer=TransformerFactory.newInstance().newTransformer();
        transformer.setOutputProperty(javax.xml.transform.OutputKeys.OMIT_XML_DECLARATION,"yes");
        StringWriter output=new StringWriter();transformer.transform(new DOMSource(document),new StreamResult(output));return output.toString();
    }
    private static String stripPaint(String css) {
        return css.replaceAll("(?i)(?<![a-z0-9_-])(?:stroke-width|stroke|fill)\\s*:\\s*[^;{}]+;?","");
    }
}
