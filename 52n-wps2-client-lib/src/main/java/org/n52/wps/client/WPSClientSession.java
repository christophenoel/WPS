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
package org.n52.wps.client;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.GZIPInputStream;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import net.opengis.ows.x20.CodeType;
import net.opengis.ows.x20.ExceptionReportDocument;
import net.opengis.ows.x20.OperationDocument;
import net.opengis.wps.x20.CapabilitiesDocument;
import net.opengis.wps.x20.DeployProcessDocument;
import net.opengis.wps.x20.DeployResultDocument;
import net.opengis.wps.x20.ExecuteDocument;
import net.opengis.wps.x20.ExecuteRequestType;
import net.opengis.wps.x20.GetResultDocument;
import net.opengis.wps.x20.GetStatusDocument;
import net.opengis.wps.x20.ProcessOfferingDocument;
import net.opengis.wps.x20.ProcessOfferingsDocument;
import net.opengis.wps.x20.ProcessSummaryType;
import net.opengis.wps.x20.ResultDocument;
import net.opengis.wps.x20.StatusInfoDocument;
import net.opengis.wps.x20.UndeployProcessDocument;
import net.opengis.wps.x20.UndeployResultDocument;
import org.apache.xmlbeans.XmlException;
import org.apache.xmlbeans.XmlObject;
import org.apache.xmlbeans.XmlOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

/**
 * Contains some convenient methods to access and manage WebProcessingSerivces
 * in a very generic way.
 *
 * This is implemented as a singleton.
 *
 * @author foerster
 */
public class WPSClientSession {

    private static Logger LOGGER = LoggerFactory.getLogger(
            WPSClientSession.class);
    private static final String OGC_OWS_URI = "http://www.opengis.net/ows/2.0";
    private static String SUPPORTED_VERSION = "2.0.0";

    private static WPSClientSession session;
    private Map<String, CapabilitiesDocument> loggedServices;
    private XmlOptions options = null;

    // a Map of <url, all available process descriptions>
    private Map<String, ProcessOfferingsDocument> processDescriptions;

    /**
     * Initializes a WPS client session.
     *
     */
    private WPSClientSession() {
        options = new XmlOptions();
        options.setLoadStripWhitespace();
        options.setLoadTrimTextBuffer();
        loggedServices = new HashMap<String, CapabilitiesDocument>();
        processDescriptions = new HashMap<String, ProcessOfferingsDocument>();
    }

    /*
     * @result An instance of a WPS Client session.
     */
    public static WPSClientSession getInstance() {
        if (session == null) {
            session = new WPSClientSession();
        }
        return session;
    }

    /**
     * This resets the WPSClientSession. This might be necessary, to get rid of
     * old service entries/descriptions. However, the session has to be
     * repopulated afterwards.
     */
    public static void reset() {
        session = new WPSClientSession();
    }

    /**
     * Connects to a WPS and retrieves Capabilities plus puts all available
     * Descriptions into cache.
     *
     * @param url the entry point for the service. This is used as id for
     * further identification of the service.
     * @return true, if connect succeeded, false else.
     * @throws WPSClientException if an exception occurred while trying to
     * connect
     */
    public boolean connect(String url) throws WPSClientException {
        LOGGER.info("CONNECT");
        if (loggedServices.containsKey(url)) {
            LOGGER.info("Service already registered: " + url);
            return false;
        }
        CapabilitiesDocument capsDoc = retrieveCapsViaGET(url);
        if (capsDoc != null) {
            loggedServices.put(url, retrieveCapsViaGET(url));
        }
        ProcessOfferingsDocument processDescs = describeAllProcesses(url);
        if (processDescs != null && capsDoc != null) {
            processDescriptions.put(url, processDescs);
            return true;
        }
        LOGGER.warn("retrieving caps failed, caps are null");
        return false;
    }

    /**
     * removes a service from the session
     *
     * @param url the url of the service that should be disconnected
     */
    public void disconnect(String url) {
        if (loggedServices.containsKey(url)) {
            loggedServices.remove(url);
            processDescriptions.remove(url);
            LOGGER.info("service removed successfully: " + url);
        }
    }

    /**
     * returns the serverIDs of all loggedServices
     *
     * @return a list of logged service URLs
     */
    public List<String> getLoggedServices() {
        return new ArrayList<String>(loggedServices.keySet());
    }

    /**
     * informs you if the descriptions for the specified service is already in
     * the session. in normal case it should return true :)
     *
     * @param serverID the URL of the WPS server
     * @return success if process descriptions are cached for the WPS server
     */
    public boolean descriptionsAvailableInCache(String serverID) {
        return processDescriptions.containsKey(serverID);
    }

    /**
     * returns the cached processdescriptions of a service.
     *
     * @param serverID the URL of the WPS server
     * @return success if process descriptions are cached for the WPS server
     * @throws IOException if an exception occurred while trying to connect to
     * the WPS
     */
    private ProcessOfferingsDocument getProcessDescriptionsFromCache(
            String wpsUrl) throws IOException {
        if (!descriptionsAvailableInCache(wpsUrl)) {
            try {
                connect(wpsUrl);
            } catch (WPSClientException e) {
                throw new IOException("Could not initialize WPS " + wpsUrl);
            }
        }
        return processDescriptions.get(wpsUrl);
    }

    /**
     * return the processDescription for a specific process from Cache.
     *
     * @param serverID the URL of the WPS server
     * @param processID the id of the process
     * @return a ProcessDescription for a specific process from Cache.
     * @throws IOException if an exception occurred while trying to connect
     */
    public ProcessOfferingDocument.ProcessOffering getProcessDescription(
            String serverID, String processID) throws IOException {
        ProcessOfferingDocument.ProcessOffering[] processes = getProcessDescriptionsFromCache(
                serverID).getProcessOfferings().getProcessOfferingArray();
        for (ProcessOfferingDocument.ProcessOffering process : processes) {
            if (process.getProcess().getIdentifier().getStringValue().equals(
                    processID)) {
                return process;
            }
        }
        return null;
    }

    /**
     * Delivers all ProcessDescriptions from a WPS
     *
     * @param wpsUrl the URL of the WPS
     * @return An Array of ProcessDescriptions
     * @throws IOException if an exception occurred while trying to connect
     */
    public ProcessOfferingDocument.ProcessOffering[] getAllProcessDescriptions(
            String wpsUrl) throws IOException {
        return getProcessDescriptionsFromCache(wpsUrl).getProcessOfferings().getProcessOfferingArray();
    }

    /**
     * looks up, if the service exists already in session.
     *
     * @param serverID the URL of the WPS
     * @return true if the WPS was already connected
     */
    public boolean serviceAlreadyRegistered(String serverID) {
        return loggedServices.containsKey(serverID);
    }

    /**
     * provides you the cached capabilities for a specified service.
     *
     * @param url the URL of the WPS
     * @return the <code>CapabilitiesDocument</code> of the WPS
     */
    public CapabilitiesDocument getWPSCaps(String url) {
        return loggedServices.get(url);
    }

    /**
     * retrieves all current available ProcessDescriptions of a WPS. Mention: to
     * get the current list of all processes, which will be requested, the
     * cached capabilities will be used. Please keep that in mind. the retrieved
     * descriptions will not be cached, so only transient information!
     *
     * @param url the URL of the WPS
     * @return a process descriptions document containing all process
     * descriptions of this WPS
     * @throws WPSClientException if an exception occurred while trying to
     * connect
     */
    public ProcessOfferingsDocument describeAllProcesses(String url) throws WPSClientException {
        CapabilitiesDocument doc = loggedServices.get(url);
        if (doc == null) {
            LOGGER.warn("serviceCaps are null, perhaps server does not exist");
            return null;
        }
        ProcessSummaryType[] processes = doc.getCapabilities().getContents().getProcessSummaryArray();
        String[] processIDs = new String[processes.length];
        for (int i = 0; i < processIDs.length; i++) {
            processIDs[i] = processes[i].getIdentifier().getStringValue();
        }
        return describeProcess(processIDs, url);

    }

    /**
     * retrieves the desired description for a service. the retrieved
     * information will not be held in cache!
     *
     * @param processIDs one or more processIDs
     * @param serverID the URL of the WPS
     * @return a process descriptions document containing the process
     * descriptions for the ids
     * @throws WPSClientException if an exception occurred while trying to
     * connect
     */
    public ProcessOfferingsDocument describeProcess(String[] processIDs,
            String serverID) throws WPSClientException {
        CapabilitiesDocument caps = this.loggedServices.get(serverID);
        OperationDocument.Operation[] operations = caps.getCapabilities().getOperationsMetadata().getOperationArray();
        String url = null;
        for (OperationDocument.Operation operation : operations) {
            if (operation.getName().equals("DescribeProcess")) {
                url = operation.getDCPArray()[0].getHTTP().getGetArray()[0].getHref();
            }
        }
        if (url == null) {
            throw new WPSClientException(
                    "Missing DescribeOperation in Capabilities");
        }
        return retrieveDescriptionViaGET(processIDs, url);
    }

    public Object deploy(String serverURL, DeployProcessDocument doc) throws WPSClientException, XmlException {
        return retrieveDeployResponseViaPOST(serverURL, doc);
    }

    public Object undeploy(String serverURL, String processId) throws WPSClientException {
        UndeployProcessDocument undeploy = UndeployProcessDocument.Factory.newInstance();
        undeploy.addNewUndeployProcess();
        undeploy.getUndeployProcess().setService("WPS");
        undeploy.getUndeployProcess().setVersion(SUPPORTED_VERSION);
        CodeType identifier = undeploy.getUndeployProcess().addNewIdentifier();
        identifier.setStringValue(processId);
        return retrieveUndeployResponseViaPOST(serverURL, undeploy);
    }

    public Object getStatus(String serverURL, String id) throws WPSClientException, XmlException {
        GetStatusDocument doc = GetStatusDocument.Factory.newInstance();
        doc.addNewGetStatus();
        doc.getGetStatus().setJobID(id);
        doc.getGetStatus().setService("WPS");
        doc.getGetStatus().setVersion(SUPPORTED_VERSION);
        return retrieveGetStatusResponseViaPOST(serverURL, doc);
    }

    public Object getResult(String serverURL, String id) throws WPSClientException, XmlException {
        GetResultDocument doc = GetResultDocument.Factory.newInstance();

        doc.addNewGetResult();
        doc.getGetResult().setJobID(id);
        doc.getGetResult().setService("WPS");
        doc.getGetResult().setVersion(SUPPORTED_VERSION);
        return retrieveGetResultResponseViaPOST(serverURL, doc);
    }

    /**
     * Executes a process at a WPS
     *
     * @param serverID url of server not the entry additionally defined in the
     * caps.
     * @param execute Execute document
     * @param rawData indicates whether a output should be requested as raw data
     * (works only if just one output is requested)
     * @return either an ExecuteResponseDocument or an InputStream if asked for
     * RawData or an Exception Report
     */
    private Object execute(String serverID, ExecuteDocument execute,
            boolean rawData) throws WPSClientException, XmlException, XmlException, XmlException {
        CapabilitiesDocument caps = loggedServices.get(serverID);
        OperationDocument.Operation[] operations = caps.getCapabilities().getOperationsMetadata().getOperationArray();
        String url = null;
        for (OperationDocument.Operation operation : operations) {
            if (operation.getName().equals("Execute")) {
                url = operation.getDCPArray()[0].getHTTP().getPostArray()[0].getHref();
            }
        }
        if (url == null) {
            throw new WPSClientException(
                    "Caps does not contain any information about the entry point for process execution");
        }
        execute.getExecute().setVersion(SUPPORTED_VERSION);
        return retrieveExecuteResponseViaPOST(url, execute, rawData);
    }

    /**
     * Executes a process at a WPS
     *
     * @param serverID url of server not the entry additionally defined in the
     * caps.
     * @param execute Execute document
     * @return either an ExecuteResponseDocument or an InputStream if asked for
     * RawData or an Exception Report
     * @throws WPSClientException if an exception occurred during execute
     */
    public Object execute(String serverID, ExecuteDocument execute) throws WPSClientException, XmlException {
        ExecuteRequestType.Response.Enum xx = execute.getExecute().getResponse();
        if (execute.getExecute().getResponse() != null && execute.getExecute().getResponse() == ExecuteRequestType.Response.RAW) {
            return execute(serverID, execute, true);
        } else {
            return execute(serverID, execute, false);
        }

    }

    private CapabilitiesDocument retrieveCapsViaGET(String url) throws WPSClientException {
        ClientCapabiltiesRequest req = new ClientCapabiltiesRequest();
        url = req.getRequest(url);
        try {
            URL urlObj = new URL(url);
            urlObj.getContent();
            InputStream is = urlObj.openStream();
            Document doc = checkInputStream(is);
            return CapabilitiesDocument.Factory.parse(doc, options);
        } catch (MalformedURLException e) {
            throw new WPSClientException(
                    "Capabilities URL seems to be unvalid: " + url, e);
        } catch (IOException e) {
            throw new WPSClientException(
                    "Error occured while retrieving capabilities from url: " + url,
                    e);
        } catch (XmlException e) {
            throw new WPSClientException("Error occured while parsing XML", e);
        }
    }

    private ProcessOfferingsDocument retrieveDescriptionViaGET(
            String[] processIDs, String url) throws WPSClientException {

        ClientDescribeProcessRequest req = new ClientDescribeProcessRequest();
        req.setIdentifier(processIDs);

        String requestURL = req.getRequest(url);
        try {
            URL urlObj = new URL(requestURL);
            InputStream is = urlObj.openStream();
            Document doc = checkInputStream(is);
            return ProcessOfferingsDocument.Factory.parse(doc, options);
        } catch (MalformedURLException e) {

            e.printStackTrace();
            throw new WPSClientException("URL seems not to be valid", e);

        } catch (IOException e) {

            e.printStackTrace();
            throw new WPSClientException("Error occured while receiving data", e);
        } catch (XmlException e) {
            e.printStackTrace();

            throw new WPSClientException(
                    "Error occured while parsing ProcessDescription document", e);
        }
    }

    private InputStream retrieveDataViaPOST(XmlObject obj, String urlString) throws WPSClientException {
        try {
            URL url = new URL(urlString);
            URLConnection conn = url.openConnection();
            conn.setRequestProperty("Accept-Encoding", "gzip");
            conn.setRequestProperty("Content-Type", "text/xml");
            conn.setDoOutput(true);
            obj.save(conn.getOutputStream());
            InputStream input = null;
            String encoding = conn.getContentEncoding();
            if (encoding != null && encoding.equalsIgnoreCase("gzip")) {
                input = new GZIPInputStream(conn.getInputStream());
            } else {
                input = conn.getInputStream();
            }
            return input;
        } catch (MalformedURLException e) {
            throw new WPSClientException("URL seems to be unvalid", e);
        } catch (IOException e) {
            throw new WPSClientException("Error while transmission", e);
        }
    }

    private Document checkInputStream(InputStream is) throws WPSClientException {
        DocumentBuilderFactory fac = DocumentBuilderFactory.newInstance();
        fac.setNamespaceAware(true);
        try {
            Document doc = fac.newDocumentBuilder().parse(is);
            if (getFirstElementNode(doc.getFirstChild()).getLocalName().equals(
                    "ExceptionReport") && getFirstElementNode(
                            doc.getFirstChild()).getNamespaceURI().equals(
                            OGC_OWS_URI)) {
                try {
                    ExceptionReportDocument exceptionDoc = ExceptionReportDocument.Factory.parse(
                            doc);
                    LOGGER.debug(exceptionDoc.xmlText(options));
                    throw new WPSClientException(
                            "Error occured while executing query", exceptionDoc);
                } catch (XmlException e) {
                    e.printStackTrace();
                    throw new WPSClientException(
                            "Error while parsing ExceptionReport retrieved from server",
                            e);
                }
            }
            return doc;
        } catch (SAXException e) {
            throw new WPSClientException("Error while parsing input.", e);
        } catch (IOException e) {
            throw new WPSClientException("Error occured while transfer", e);
        } catch (ParserConfigurationException e) {
            throw new WPSClientException(
                    "Error occured, parser is not correctly configured", e);
        }
    }

    private Node getFirstElementNode(Node node) {
        if (node == null) {
            return null;
        }
        if (node.getNodeType() == Node.ELEMENT_NODE) {
            return node;
        } else {
            return getFirstElementNode(node.getNextSibling());
        }

    }

    private Object retrieveGetStatusResponseViaPOST(String url,
            GetStatusDocument doc) throws WPSClientException, XmlException {
         ExceptionReportDocument erDoc = null;
        Document documentObj=null;
        try {
            LOGGER.debug(doc.xmlText());
            InputStream is = retrieveDataViaPOST(doc, url);

             documentObj = checkInputStream(is);
             erDoc = null;

            return StatusInfoDocument.Factory.parse(documentObj);
        } catch (WPSClientException e) {
            LOGGER.debug(e.getServerException().xmlText());
            return e.getServerException();
        }
    }

    private Object retrieveGetResultResponseViaPOST(String url,
            GetResultDocument doc) throws WPSClientException, XmlException {
        ExceptionReportDocument erDoc = null;
        Document documentObj=null;
        try {
            LOGGER.debug(doc.xmlText());
            InputStream is = retrieveDataViaPOST(doc, url);
             documentObj = checkInputStream(is);
            return ResultDocument.Factory.parse(documentObj);
        } catch (WPSClientException e) {
            return e.getServerException();
        }
    }

    private Object retrieveDeployResponseViaPOST(String url,
            DeployProcessDocument doc) throws WPSClientException, XmlException {
        ExceptionReportDocument erDoc = null;
        Document documentObj=null;
        try {
            LOGGER.debug(doc.xmlText());
            InputStream is = retrieveDataViaPOST(doc, url);
             documentObj = checkInputStream(is);
            erDoc = null;
            return DeployResultDocument.Factory.parse(documentObj);
        } catch (WPSClientException e) {
            LOGGER.debug(e.getServerException().xmlText());
            return e.getServerException();
        }
    }

    private Object retrieveUndeployResponseViaPOST(String url,
            UndeployProcessDocument doc) throws WPSClientException {
            ExceptionReportDocument erDoc = null;
        Document documentObj=null;
   try {
        LOGGER.debug(doc.xmlText());
        InputStream is = retrieveDataViaPOST(doc, url);

         documentObj = checkInputStream(is);
         erDoc = null;
        

            return UndeployResultDocument.Factory.parse(documentObj);
        } catch (XmlException e) {
            try {
                erDoc = ExceptionReportDocument.Factory.parse(documentObj);
                LOGGER.warn(erDoc.xmlText());
            } catch (XmlException e1) {
                throw new WPSClientException(
                        "Error occured while parsing executeResponse", e);
            }
            return erDoc;
        }
    }

    /**
     * either an ExecuteResponseDocument or an InputStream if asked for RawData
     * or an Exception Report
     *
     * @param url the URL of the WPS server
     * @param doc the <code>ExecuteDocument</code> that should be send to the
     * server
     * @param rawData indicates whether a output should be requested as raw data
     * (works only if just one output is requested)
     * @return either an ExecuteResponseDocument or an InputStream if asked for
     * RawData or an Exception Report
     * @throws WPSClientException if an exception occurred during execute
     */
    private Object retrieveExecuteResponseViaPOST(String url,
            ExecuteDocument doc, boolean rawData) throws WPSClientException, XmlException {
        InputStream is = retrieveDataViaPOST(doc, url);
        if (rawData) {
            return is;
        }
            ExceptionReportDocument erDoc = null;
        Document documentObj=null;
   try {
        
         documentObj = checkInputStream(is);
         erDoc = null;
        
            if (doc.getExecute().getMode() == ExecuteRequestType.Mode.ASYNC) {
                return StatusInfoDocument.Factory.parse(documentObj);
            } else {
                return ResultDocument.Factory.parse(documentObj);
            }

        } catch (WPSClientException e) {
            LOGGER.debug(e.getServerException().xmlText());
            return e.getServerException();
        }
    }

    public String[] getProcessNames(String url) throws IOException {
        ProcessOfferingDocument.ProcessOffering[] processes = getProcessDescriptionsFromCache(
                url).getProcessOfferings().getProcessOfferingArray();
        String[] processNames = new String[processes.length];
        for (int i = 0; i < processNames.length; i++) {
            processNames[i] = processes[i].getProcess().getIdentifier().getStringValue();
        }
        return processNames;
    }

    /**
     * Executes a process at a WPS
     *
     * @param url url of server not the entry additionally defined in the caps.
     * @param executeAsGETString KVP Execute request
     * @return either an ExecuteResponseDocument or an InputStream if asked for
     * RawData or an Exception Report
     * @throws WPSClientException if an exception occurred during execute
     *
     * public Object executeViaGET(String url, String executeAsGETString) throws
     * WPSClientException { url = url + executeAsGETString; try { URL urlObj =
     * new URL(url); InputStream is = urlObj.openStream();
     *
     * if (executeAsGETString.toUpperCase().contains("RAWDATA")) { return is; }
     * Document doc = checkInputStream(is); ExceptionReportDocument erDoc =
     * null; try { return ExecuteResponseDocument.Factory.parse(doc); } catch
     * (XmlException e) { try { erDoc =
     * ExceptionReportDocument.Factory.parse(doc); } catch (XmlException e1) {
     * throw new WPSClientException( "Error occured while parsing
     * executeResponse", e); } throw new WPSClientException( "Error occured
     * while parsing executeResponse", erDoc); } } catch (MalformedURLException
     * e) { throw new WPSClientException( "Capabilities URL seems to be unvalid:
     * " + url, e); } catch (IOException e) { throw new WPSClientException(
     * "Error occured while retrieving capabilities from url: " + url, e); }
     *
     * }
     */
}
