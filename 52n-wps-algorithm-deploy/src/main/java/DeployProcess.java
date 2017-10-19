
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
import java.io.File;
import java.io.IOException;
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
import net.opengis.wps.x20.DeployProcessDocument;
import net.opengis.wps.x20.DeployResultDocument;
import net.opengis.wps.x20.DeployResultType;
import net.opengis.wps.x20.DeploymentProfileType;
import net.opengis.wps.x20.ExecutionUnitType;
import net.opengis.wps.x20.ProcessOfferingDocument;
import net.opengis.wps.x20.profile.tb13.eoc.ApplicationContextDocument;
import org.apache.xmlbeans.XmlException;
import org.apache.xmlbeans.XmlObject;
import org.n52.wps.io.data.IData;
import org.n52.wps.io.data.binding.complex.GenericXMLDataBinding;
import org.n52.wps.server.AbstractSelfDescribingAlgorithm;
import org.n52.wps.server.ExceptionReport;
import org.n52.wps.server.transactional.request.DeployProcessRequest;
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
public class DeployProcess extends AbstractSelfDescribingAlgorithm {

    private static org.slf4j.Logger log = LoggerFactory
            .getLogger(DeployProcess.class);

    @Override
    public List<String> getInputIdentifiers() {
        List<String> identifierList = new ArrayList<String>();
        identifierList.add("ApplicationPackage");
        return identifierList;
    }

    @Override
    public List<String> getOutputIdentifiers() {
        List<String> identifierList = new ArrayList<String>();
        identifierList.add("DeployProcessResponse");
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

    public static void main(String[] args) {
        try {

            XmlObject packageXml = XmlObject.Factory.parse(new File(
                    "D:/package.xml"));
            
            
             
            ProcessOfferingDocument processOffering = convertOWCtoWPS(packageXml);
             NamespaceContext nc = new PackageNamespaceContext(); XPath xPath
              = XPathFactory.newInstance().newXPath();
              xPath.setNamespaceContext(nc); Node node = (Node) xPath.evaluate(
              "//owc:offering[@code='http://www.opengis.net/tb13/eoc/docker']/owc:content/text()",
              processOffering.copy().getDomNode(), XPathConstants.NODE);
              System.out.println("Test:"+node.getNodeValue());
             
            
            System.out.println(processOffering.toString());

        } catch (XPathExpressionException ex) {
            Logger.getLogger(DeployProcess.class.getName()).log(Level.SEVERE,
                    null, ex);
        } catch (XmlException ex) {
            Logger.getLogger(DeployProcess.class.getName()).log(Level.SEVERE,
                    null, ex);
        } catch (IOException ex) {
            Logger.getLogger(DeployProcess.class.getName()).log(Level.SEVERE,
                    null, ex);
        }
    }

    @Override
    public Map<String, IData> run(Map<String, List<IData>> inputData) {
        try {
            /**
            ProcessOfferingDocument.ProcessOffering desc = (ProcessOfferingDocument.ProcessOffering) this.getDescription().getProcessDescriptionType(
                    WPSConfig.VERSION_200);
                    * */
            log.debug("Run for IncrementConversion");
            // Get application pacakge input
            XmlObject packageXml = ((GenericXMLDataBinding) inputData.get(
                    "ApplicationPackage").get(0)).getPayload(); // Creating a XPath
            ProcessOfferingDocument processOffering = null;
            // Allow 2 formats
            try {
                processOffering = ProcessOfferingDocument.Factory.parse(packageXml.copy().getDomNode());
            }
            catch(Exception e) {
                // Case of OWC context
                 processOffering =  convertOWCtoWPS(packageXml);
            }
            
            HashMap<String, IData> results = new HashMap<String, IData>();

            DeployProcessDocument deploy = DeployProcessDocument.Factory.newInstance();
            DeployProcessDocument.DeployProcess dp = deploy.addNewDeployProcess();
            dp.setService("WPS");
            dp.setVersion("2.0.0");
            dp.setProcessOffering(processOffering.getProcessOffering());
            DeploymentProfileType prof = dp.addNewDeploymentProfile();
            prof.setDeploymentProfileName(
                    "http://spacebel.be/profile/docker.xsd");
            // Retrieve the docker reference
            NamespaceContext nc = new PackageNamespaceContext();
            XPath xPath = XPathFactory.newInstance().newXPath();
            xPath.setNamespaceContext(nc);
            //log.debug("evaluate in "+processOffering.toString());
            Node node = (Node) xPath.evaluate(
                    "//owc:offering[@code='http://www.opengis.net/tb13/eoc/docker']/owc:content/text()",
                    processOffering.copy().getDomNode(), XPathConstants.NODE);
            log.debug("NodeisNull?"+(node==null));
            ExecutionUnitType unit = prof.addNewExecutionUnit();
            log.debug(node.getNodeValue());
            unit.addNewReference().setHref(node.getNodeValue());

            DeployResultDocument response = DeployResultDocument.Factory.newInstance();
            response.addNewDeployResult();
            response.getDeployResult().setDeploymentDone(true);
            try {
                DeployProcessRequest deployRequest = new DeployProcessRequest(
                        (Document) deploy.getDomNode());
                deployRequest.call();
            } catch (ExceptionReport ex) {
                Logger.getLogger(DeployProcess.class.getName()).log(Level.SEVERE,
                        null, ex);
                response = DeployResultDocument.Factory.newInstance();
                DeployResultType result = response.addNewDeployResult();
                result.setDeploymentDone(false);
            }
            // Create DeployResult document

            GenericXMLDataBinding binding = new GenericXMLDataBinding(
                    response);
            results.put("Success", binding);

            return results;
        } catch (XPathExpressionException ex) {
            Logger.getLogger(DeployProcess.class.getName()).log(Level.SEVERE,
                    null, ex);
        } catch (XmlException ex) {
            Logger.getLogger(DeployProcess.class.getName()).log(Level.SEVERE,
                    null, ex);
        }

        return null;
    }

    @Override
    public Class<?> getInputDataType(String id) {

        return GenericXMLDataBinding.class;
    }

    @Override
    public Class<?> getOutputDataType(String id) {
        return GenericXMLDataBinding.class;
    }

}
