
/**
 * Copyright (C) 2007-2017 52°North Initiative for Geospatial Open Source
 * Software GmbH
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 as published
 * by the Free Software Foundation.
 *
 * If the program is linked with libraries which are licensed under one of
 * the following licenses, the combination of the program with the linked
 * library is not considered a "derivative work" of the program:
 *
 *       • Apache License, version 2.0
 *       • Apache Software License, version 1.0
 *       • GNU Lesser General Public License, version 3
 *       • Mozilla Public License, versions 1.0, 1.1 and 2.0
 *       • Common Development and Distribution License (CDDL), version 1.0
 *
 * Therefore the distribution of the program linked with libraries licensed
 * under the aforementioned licenses, is permitted by the copyright holders
 * if the distribution is compliant with both the GNU General Public
 * License version 2 and the aforementioned licenses.
 *
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
 * Public License for more details.
 */
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.xml.namespace.NamespaceContext;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import net.opengis.ows.x20.MetadataType;
import net.opengis.wps.x20.ProcessOfferingDocument;
import net.opengis.wps.x20.UndeployProcessDocument;
import net.opengis.wps.x20.UndeployResultDocument;
import net.opengis.wps.x20.UndeployResultType;
import net.opengis.wps.x20.profile.tb13.eoc.ApplicationContextDocument;
import org.apache.xmlbeans.XmlException;
import org.apache.xmlbeans.XmlObject;
import org.n52.wps.io.data.IData;
import org.n52.wps.io.data.binding.complex.GenericXMLDataBinding;
import org.n52.wps.io.data.binding.literal.LiteralStringBinding;
import org.n52.wps.server.AbstractSelfDescribingAlgorithm;
import org.n52.wps.server.transactional.request.UndeployProcessRequest;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
/**
 *
 * @author cnl
 */
public class UndeployProcess extends AbstractSelfDescribingAlgorithm {

    private static org.slf4j.Logger log = LoggerFactory
            .getLogger(UndeployProcess.class);

    @Override
    public List<String> getInputIdentifiers() {
        List<String> identifierList = new ArrayList<String>();
        identifierList.add("processIdentifier");
        return identifierList;
    }

    @Override
    public List<String> getOutputIdentifiers() {
        List<String> identifierList = new ArrayList<String>();
        identifierList.add("undeployResult");
        return identifierList;
    }

    public static ProcessOfferingDocument convertOWCtoWPS(XmlObject packageXml) throws XPathExpressionException, XmlException {
        XPath xPath = XPathFactory.newInstance().newXPath();
        NamespaceContext nc = new PackageNamespaceContext();
        xPath.setNamespaceContext(nc);
        // Retrieving process description
        Node processOfferingNode = (Node) xPath.evaluate(
                "//atom:entry/descendant::owc:offering[@code='http://www.opengis.net/tb13/eoc/wpsProcessOffering']/owc:content/wps:ProcessOfferings/wps:ProcessOffering",
                packageXml.copy().getDomNode(), XPathConstants.NODE);
        ProcessOfferingDocument processOffering = ProcessOfferingDocument.Factory.parse(
                processOfferingNode);
        Node packageDom = packageXml.getDomNode();
        processOfferingNode = (Node) xPath.evaluate(
                "//atom:entry/descendant::owc:offering[@code='http://www.opengis.net/tb13/eoc/wpsProcessOffering']",
                packageDom, XPathConstants.NODE);
        // Remove the wpsProcessOfferingNode
        processOfferingNode.getParentNode().removeChild(processOfferingNode);
        XmlObject packageTarget = XmlObject.Factory.parse(packageDom);
        MetadataType metadata = processOffering.getProcessOffering().getProcess().addNewMetadata();
        metadata.setRole("http://www.opengis.net//tb13/eoc/applicationContext");
        ApplicationContextDocument appContext = ApplicationContextDocument.Factory.newInstance();
        appContext.addNewApplicationContext().set(packageTarget);
        metadata.set(appContext);
        return processOffering;
    }

    @Override
    public Map<String, IData> run(Map<String, List<IData>> inputData) {
      
            /**
             * ProcessOfferingDocument.ProcessOffering desc =
             * (ProcessOfferingDocument.ProcessOffering)
             * this.getDescription().getProcessDescriptionType(
             * WPSConfig.VERSION_200);
             *
             */
            log.debug("Run for IncrementConversion");
            // Get application pacakge input
            String identifier = ((LiteralStringBinding) inputData.get(
                    "processIdentifier").get(0)).getPayload(); // Creating a XPath

            HashMap<String, IData> results = new HashMap<String, IData>();

            UndeployProcessDocument undeploy = UndeployProcessDocument.Factory.newInstance();
            UndeployProcessDocument.UndeployProcess undp = undeploy.addNewUndeployProcess();
            undp.setService("WPS");
            undp.setVersion("2.0.0");
            undp.addNewIdentifier().setStringValue(identifier);

            UndeployResultDocument response = UndeployResultDocument.Factory.newInstance();
            response.addNewUndeployResult();
            response.getUndeployResult().setUndeploymentDone(true);
            try {
                UndeployProcessRequest undeployRequest = new UndeployProcessRequest(
                        (Document) undeploy.getDomNode());
                undeployRequest.call();
            } catch (Exception ex) {
                Logger.getLogger(DeployProcess.class.getName()).log(Level.SEVERE,
                        null, ex);
                response = UndeployResultDocument.Factory.newInstance();
                UndeployResultType result = response.addNewUndeployResult();
                result.setUndeploymentDone(false);
                result.setFailureReason(ex.getMessage());
            }
            // Create DeployResult document

            GenericXMLDataBinding binding = new GenericXMLDataBinding(
                    response);
            results.put("undeployResult", binding);

            return results;
    }

    @Override
    public Class<?> getInputDataType(String id) {

        return LiteralStringBinding.class;
    }

    @Override
    public Class<?> getOutputDataType(String id) {
        return GenericXMLDataBinding.class;
    }

}
