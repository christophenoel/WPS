/***************************************************************
 This implementation provides a framework to publish processes to the
 web through the  OGC Web Processing Service interface. The framework 
 is extensible in terms of processes and data handlers. It is compliant 
 to the WPS version 0.4.0 (OGC 05-007r4). 

 Copyright (C) 2006 by con terra GmbH

 Authors: 
 Theodor Foerster, ITC, Enschede, the Netherlands
 Carsten Priess, Institute for geoinformatics, University of
 Muenster, Germany
 Timon Ter Braak, University of Twente, the Netherlands
 Bastian Schaeffer, Institute for geoinformatics, University of Muenster, Germany


 Contact: Albert Remke, con terra GmbH, Martin-Luther-King-Weg 24,
 48155 Muenster, Germany, 52n@conterra.de

 This program is free software; you can redistribute it and/or
 modify it under the terms of the GNU General Public License
 version 2 as published by the Free Software Foundation.

 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 GNU General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with this program (see gnu-gpl v2.txt); if not, write to
 the Free Software Foundation, Inc., 59 Temple Place - Suite 330,
 Boston, MA  02111-1307, USA or visit the web page of the Free
 Software Foundation, http://www.fsf.org.

 Created on: 13.06.2006
 ***************************************************************/
package org.n52.wps.server.handler;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.StringWriter;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.RejectedExecutionException;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import net.opengis.wps.x100.ProcessFailedType;
import net.opengis.wps.x100.StatusType;

import org.apache.commons.collections.map.CaseInsensitiveMap;
import org.apache.log4j.Logger;
import org.n52.wps.commons.WPSConfig;
import org.n52.wps.server.ExceptionReport;
import org.n52.wps.server.WebProcessingService;
import org.n52.wps.server.request.CancelRequest;
import org.n52.wps.server.request.CapabilitiesRequest;
import org.n52.wps.server.request.DeployDataRequest;
import org.n52.wps.server.request.DeployProcessRequest;
import org.n52.wps.server.request.DescribeDataRequest;
import org.n52.wps.server.request.DescribeProcessRequest;
import org.n52.wps.server.request.ExecuteCallback;
import org.n52.wps.server.request.ExecuteRequest;
import org.n52.wps.server.request.GetAuditRequest;
import org.n52.wps.server.request.GetStatusRequest;
import org.n52.wps.server.request.Request;
import org.n52.wps.server.request.RetrieveResultRequest;
import org.n52.wps.server.request.UndeployDataRequest;
import org.n52.wps.server.request.UndeployProcessRequest;
import org.n52.wps.server.response.ExecuteResponse;
import org.n52.wps.server.response.Response;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

/**
 * This class accepts client requests, determines its type and then schedules
 * the {@link ExecuteRequest}'s for execution. The request is executed for a
 * short time, within the client will be served with an immediate result. If the
 * time runs out, the client will be served with a reference to the future
 * result. The client can come back later to retrieve the result. Uses
 * "computation_timeout_seconds" from wps.properties
 * 
 * @author Timon ter Braak
 */
public class RequestHandler {

	/** Computation timeout in seconds */
	protected static RequestExecutor pool = new RequestExecutor();

	protected OutputStream os;

	private static Logger LOGGER = Logger.getLogger(RequestHandler.class);

	protected String responseMimeType;

	protected Request req;

	// Empty constructor due to classes which extend the RequestHandler
	protected RequestHandler() {

	}

	/**
	 * Handles requests of type HTTP_GET (currently capabilities and
	 * describeProcess). A Map is used to represent the client input.
	 * 
	 * @param params
	 *            The client input
	 * @param os
	 *            The OutputStream to write the response to.
	 * @throws ExceptionReport
	 *             If the requested operation is not supported
	 */
	public RequestHandler(Map<String, String[]> params, OutputStream os)
			throws ExceptionReport {
		this.os = os;
		// sleepingTime is 0, by default.
		/*
		 * if(WPSConfiguration.getInstance().exists(
		 * PROPERTY_NAME_COMPUTATION_TIMEOUT)) { this.sleepingTime =
		 * Integer.parseInt(WPSConfiguration.getInstance().getProperty(
		 * PROPERTY_NAME_COMPUTATION_TIMEOUT)); } String sleepTime =
		 * WPSConfig.getInstance
		 * ().getWPSConfig().getServer().getComputationTimeoutMilliSeconds();
		 */

		Request req;
		CaseInsensitiveMap ciMap = new CaseInsensitiveMap(params);

		// get the request type
		String requestType = Request.getMapValue("request", ciMap, true);
		if (requestType.equalsIgnoreCase("GetCapabilities")) {
			req = new CapabilitiesRequest(ciMap);
		} else if (requestType.equalsIgnoreCase("DescribeProcess")) {
			req = new DescribeProcessRequest(ciMap);
		} else if (requestType.equalsIgnoreCase("Execute")) {
			req = new ExecuteRequest(ciMap);
		} else if (requestType.equalsIgnoreCase("RetrieveResult")) {
			req = new RetrieveResultRequest(ciMap);
		} else {
			throw new ExceptionReport(
					"The requested Operation is for HTTP GET not supported or not applicable to the specification: "
							+ requestType,
					ExceptionReport.OPERATION_NOT_SUPPORTED);
		}

		this.req = req;
	}

	/**
	 * Handles requests of type HTTP_POST (currently executeProcess). A Document
	 * is used to represent the client input. This Document must first be parsed
	 * from an InputStream.
	 * 
	 * @param is
	 *            The client input
	 * @param os
	 *            The OutputStream to write the response to.
	 * @throws ExceptionReport
	 */
	public RequestHandler(InputStream is, OutputStream os)
			throws ExceptionReport {
		String nodeName, localName, nodeURI, version;
		Document doc;
		this.os = os;

		String sleepTime = WPSConfig.getInstance().getWPSConfig().getServer()
				.getComputationTimeoutMilliSeconds();
		if (sleepTime == null || sleepTime.equals("")) {
			sleepTime = "5";
		}

		try {
			System.setProperty("javax.xml.parsers.DocumentBuilderFactory",
					"org.apache.xerces.jaxp.DocumentBuilderFactoryImpl");

			DocumentBuilderFactory fac = DocumentBuilderFactory.newInstance();
			fac.setNamespaceAware(true);

			// parse the InputStream to create a Document
			doc = fac.newDocumentBuilder().parse(is);

			// Get the first non-comment child.
			Node child = doc.getFirstChild();
			while (child.getNodeName().compareTo("#comment") == 0) {
				child = child.getNextSibling();
			}
			nodeName = child.getNodeName();
			localName = child.getLocalName();
			nodeURI = child.getNamespaceURI();
			if(!localName.equals("GetCapabilities")) {
			Node versionNode = child.getAttributes().getNamedItem("version");
			if (versionNode == null) {
				throw new ExceptionReport("No version parameter supplied.",
						ExceptionReport.MISSING_PARAMETER_VALUE);
			}
			version = child.getAttributes().getNamedItem("version")
					.getNodeValue();
			if (!version.equals(Request.SUPPORTED_VERSION)) {
				throw new ExceptionReport("version is null: ",
						ExceptionReport.INVALID_PARAMETER_VALUE);
			}
			}
		} catch (SAXException e) {
			throw new ExceptionReport(
					"There went something wrong with parsing the POST data: "
							+ e.getMessage(),
					ExceptionReport.NO_APPLICABLE_CODE, e);
		} catch (IOException e) {
			throw new ExceptionReport(
					"There went something wrong with the network connection.",
					ExceptionReport.NO_APPLICABLE_CODE, e);
		} catch (ParserConfigurationException e) {
			throw new ExceptionReport(
					"There is a internal parser configuration error",
					ExceptionReport.NO_APPLICABLE_CODE, e);
		}
		
		
		// get the request type
		if (nodeURI.equals(WebProcessingService.WPS_NAMESPACE)
				&& localName.equals("Execute")) {
			req = new ExecuteRequest(doc);
			if (req instanceof ExecuteRequest) {
				setResponseMimeType((ExecuteRequest) req);
			} else {
				this.responseMimeType = "text/xml";
			}
		} else if (nodeName.equals("DeployProcess")) {
			req = new DeployProcessRequest(doc);
		} else if (nodeName.equals("Cancel")) {
			req = new CancelRequest(doc);
		} else if (nodeName.equals("UndeployProcess")) {
			req = new UndeployProcessRequest(doc);
		} else if (nodeName.equals("DeployData")) {
			req = new DeployDataRequest(doc);
		} else if (nodeName.equals("DescribeData")) {
			req = new DescribeDataRequest(doc);
		} else if (nodeName.equals("GetAudit")) {
			req = new GetAuditRequest(doc);
		} else if (nodeName.equals("GetStatus")) {
			req = new GetStatusRequest(doc);
		
		} else if (nodeName.equals("UndeployData")) {
			req = new UndeployDataRequest(doc);
		//} else if (nodeName.equals("DescribeData")) {
			//req = new DescribeDataRequest(doc);
			
		} else if (nodeName.equals("GetCapabilities")) {
			req = new CapabilitiesRequest(doc);
		} else if (nodeName.equals("DescribeProcess")) {
			req= new DescribeDataRequest(doc);
		} else if (!localName.equals("Execute")) {
			throw new ExceptionReport("specified operation is not supported: "
					+ nodeName, ExceptionReport.OPERATION_NOT_SUPPORTED);
		} else if (nodeURI.equals(WebProcessingService.WPS_NAMESPACE)) {
			throw new ExceptionReport("specified namespace is not supported: "
					+ nodeURI, ExceptionReport.INVALID_PARAMETER_VALUE);
		}
	}

	/**
	 * Handle a request after its type is determined. The request is scheduled
	 * for execution. If the server has enough free resources, the client will
	 * be served immediately. If time runs out, the client will be asked to come
	 * back later with a reference to the result.
	 * 
	 * @param req
	 *            The request of the client.
	 * @throws ExceptionReport
	 */
	public void handle() throws ExceptionReport {
		LOGGER.info("handle");
		Response resp = null;
		if (req == null) {
			throw new ExceptionReport("Internal Error", "");
		}
		if (req instanceof ExecuteRequest) {
			// cast the request to an executerequest
			ExecuteRequest execReq = (ExecuteRequest) req;
			// get the statustype of this request
			StatusType status = StatusType.Factory.newInstance();
			// ??
			execReq.getExecuteResponseBuilder().setStatus(status);
			// create a task for this request
			WPSTask<Response> task = new WPSTask<Response>(req);

			// add process status before execution to enables clients to see the
			// status
			// status.addNewProcessStarted();
			
			ExceptionReport exceptionReport = null;
			try {
				LOGGER.debug("submit the task for execution");
				// submit the task for execution
				status.setProcessAccepted("Request is queued for execution.");
				task.getRequest().getExecuteResponseBuilder().setStatus(status);

				pool.addTask(task);
				// set status to accepted
				
				if (((ExecuteRequest) req).isStoreResponse()) {
					resp = new ExecuteResponse(execReq);
					resp.save(os);
					return;
				}
				try {
					// retrieve status with timeout enabled
					try {
						LOGGER.debug("Wait for finished");
						resp = task.get();
						LOGGER.debug("Succeeded");
						// Thread.sleep(this.sleepingTime);
						status.setProcessSucceeded("Process has succeeded");
						status.unsetProcessAccepted();
						task.getRequest().getExecuteResponseBuilder()
								.setStatus(status);
					} catch (ExecutionException ee) {
						// the computation threw an error
						// probably the client input is not valid
						LOGGER.debug("Exception of execution catched");
						if (ee.getCause() instanceof ExceptionReport) {
							exceptionReport = (ExceptionReport) ee.getCause();
						} else {
							exceptionReport = new ExceptionReport(
									"An error occurred in the computation: "
											+ ee.getMessage(),
									ExceptionReport.NO_APPLICABLE_CODE);
						}
					} catch (InterruptedException ie) {
						// interrupted while waiting in the queue
						exceptionReport = new ExceptionReport(
								"The computation in the process was interrupted.",
								ExceptionReport.NO_APPLICABLE_CODE);
					}
				} finally {
					if (exceptionReport != null) {
						LOGGER.debug("ExceptionReport not null: "
								+ exceptionReport.getMessage());
						ProcessFailedType statusFailed = ProcessFailedType.Factory
								.newInstance();
						statusFailed.setExceptionReport(exceptionReport
								.getExceptionDocument().getExceptionReport());
						status.setProcessFailed(statusFailed);
						// NOT SURE, if this exceptionReport is also written to
						// the DB, if required... test please!
						throw exceptionReport;
					}
					// send the result to the outputstream of the client.
					/*
					 * if(((ExecuteRequest) req).isQuickStatus()) { resp = new
					 * ExecuteResponse(execReq); }
					 */
					else if (resp == null) {
						LOGGER.debug("repsonse object is null");
						throw new ExceptionReport(
								"Problem with handling threads in RequestHandler",
								ExceptionReport.NO_APPLICABLE_CODE);
					}
					if (!((ExecuteRequest) req).isStoreResponse()) {
						resp.save(this.os);
						LOGGER.info("Served ExecuteRequest.");
					}
				}
			} catch (RejectedExecutionException ree) {
				// server too busy?
				throw new ExceptionReport(
						"The requested process was rejected. Maybe the server is flooded with requests.",
						ExceptionReport.SERVER_BUSY);
			}
		} else if (req instanceof ExecuteCallback) {
			ExceptionReport exceptionReport = null;
			try {
				
				WPSTask<Response> task = pool.getTask(((ExecuteCallback) req).getRelatesTo());
				resp = ((ExecuteCallback) req).call(task);
			} catch (Exception e) {
				if (e.getCause() instanceof ExceptionReport) {
					exceptionReport = (ExceptionReport) e.getCause();
				} else {
					exceptionReport = new ExceptionReport(
							"An error occurred in the computation: "
									+ e.getMessage(),
							ExceptionReport.NO_APPLICABLE_CODE);
				}
				throw exceptionReport;
			}
			//resp.save(os);
		} else if (req instanceof CancelRequest) {
			ExceptionReport exceptionReport = null;
			try {
				// CancelRequest is called with the WPSTask retrieved from the
				// tasks registry
				
				String taskId = ((CancelRequest) req).getCancelDom()
						.getCancel().getProcessInstanceIdentifier()
						.getInstanceId();
				LOGGER.debug("loading Task with PID "+taskId);
				WPSTask<Response> task = pool.getTask(taskId);
				resp = ((CancelRequest) req).call(task);
			} catch (Exception e) {
				e.printStackTrace();
				if (e.getCause() instanceof ExceptionReport) {
					exceptionReport = (ExceptionReport) e.getCause();
				} else {
					exceptionReport = new ExceptionReport(
							"An error occurred in the computation: "
									+ e.getMessage(),
							ExceptionReport.NO_APPLICABLE_CODE);
				}
				throw exceptionReport;
			}
			resp.save(os);
		} else if (req instanceof GetAuditRequest) {
			ExceptionReport exceptionReport = null;
			try {
				// CancelRequest is called with the WPSTask retrieved from the
				// tasks registry
				String taskId = ((GetAuditRequest) req).getGetAuditDom()
						.getGetAudit().getProcessInstanceIdentifier()
						.getInstanceId();
				WPSTask<Response> task = pool.getTask(taskId);
				resp = ((GetAuditRequest) req).call(task);
			} catch (Exception e) {
				if (e.getCause() instanceof ExceptionReport) {
					exceptionReport = (ExceptionReport) e.getCause();
				} else {
					exceptionReport = new ExceptionReport(
							"An error occurred in the computation: "
									+ e.getMessage(),
							ExceptionReport.NO_APPLICABLE_CODE);
				}
				e.printStackTrace();
				throw exceptionReport;
			}
			resp.save(os);
		
		} else {
			// for GetCapabilities and DescribeProcess, and GetStatus:

			resp = req.call();
			resp.save(os);
		}
	}

	protected void setResponseMimeType(ExecuteRequest req) {
		if (req.isRawData()) {
			responseMimeType = req.getExecuteResponseBuilder().getMimeType();
		} else {
			responseMimeType = "text/xml";
		}

	}

	public String getResponseMimeType() {
		if (responseMimeType == null) {
			return "text/xml";
		}
		return responseMimeType.toLowerCase();
	}

}
