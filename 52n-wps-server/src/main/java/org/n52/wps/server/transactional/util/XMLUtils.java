/*
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
package org.n52.wps.server.transactional.util;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.TransformerFactoryConfigurationError;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Document;
import org.w3c.dom.DocumentType;
import org.w3c.dom.NameList;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class XMLUtils {

	/**
	 * Write an XML file TODO move to Util package ?
	 * 
	 * @param node
	 * @param filename
	 * @throws IOException
	 * @throws TransformerFactoryConfigurationError
	 * @throws TransformerException
	 * @throws ParserConfigurationException
	 */
	public static void writeXmlFile(Node node, String filename)
			throws IOException, TransformerFactoryConfigurationError,
			TransformerException, ParserConfigurationException {
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		DocumentBuilder documentBuilder = factory.newDocumentBuilder();
		Document tempDocument = documentBuilder.newDocument();
		Node importedNode = tempDocument.importNode(node, true);
		tempDocument.appendChild(importedNode);
		// Prepare the DOM document for writing
		Source source = new DOMSource(tempDocument);

		// Prepare the output file
		File file = new File(filename);
		String parent = file.getParent();
		File directory = new File(parent);
		directory.mkdirs();
		// file.createNewFile();
		OutputStream fileOutput = new FileOutputStream(file);
		Result result = new StreamResult(fileOutput);

		// Write the DOM document to the file
		Transformer xformer = TransformerFactory.newInstance().newTransformer();
		xformer.transform(source, result);
	}

	// private static void writeXmlFile(Document doc, String filename) {
	public static void writeXmlFile(Document doc, File file) {
		try {
			// if(filename==null){
			// filename = "C:\\BPEL\\serverside.xml";
			// }
			// Prepare the DOM document for writing
			Source source = new DOMSource(doc);

			// Prepare the output file
			// File file = new File(filename);
			// file.createNewFile();
			// Result result = new StreamResult(file);
			Result result = new StreamResult(file.toURI().getPath());

			// Write the DOM document to the file
			Transformer xformer = TransformerFactory.newInstance()
					.newTransformer();
			xformer.transform(source, result);
		} catch (TransformerConfigurationException e) {
			System.out.println("error");
		} catch (TransformerException e) {
			System.out.println("error");
		} catch (Exception e) {
			System.out.println("error");
		}

	}

	public static void writeXmlFile(Node n, File file) {
		try {
			// if(filename==null){
			// filename = "C:\\BPEL\\serverside.xml";
			// }
			// Prepare the DOM document for writing
			Source source = new DOMSource(n);

			// Prepare the output file
			// File file = new File(filename);
			// file.createNewFile();
			// Result result = new StreamResult(file);
			Result result = new StreamResult(file.toURI().getPath());

			// Write the DOM document to the file
			Transformer xformer = TransformerFactory.newInstance()
					.newTransformer();
			xformer.transform(source, result);
		} catch (TransformerConfigurationException e) {
			System.out.println("error");
		} catch (TransformerException e) {
			System.out.println("error");
		} catch (Exception e) {
			System.out.println("error");
		}

	}

	public void printNode(Node node, String indent) {
		switch (node.getNodeType()) {
		case Node.DOCUMENT_NODE:
			System.out.println(indent + "<?xml version=\"1.0\"?>");
			NodeList nodes = node.getChildNodes();
			if (nodes != null) {
				for (int i = 0; i < nodes.getLength(); i++) {
					printNode(nodes.item(i), "");
				}
			}

			break;

		case Node.ELEMENT_NODE:

			String name = node.getNodeName();
			System.out.print(indent + "<" + name);

			NamedNodeMap attributes = node.getAttributes();

			for (int i = 0; i < attributes.getLength(); i++) {
				Node current = attributes.item(i);
				System.out.print(" " + current.getNodeName() + "=\""
						+ current.getNodeValue() + "\"");
			}

			System.out.println(">");

			NodeList children = node.getChildNodes();

			if (children != null) {
				for (int i = 0; i < children.getLength(); i++) {
					printNode(children.item(i), indent + "  ");
				}
			}

			System.out.println(indent + "</" + name + ">");

			break;

		case Node.TEXT_NODE:
		case Node.CDATA_SECTION_NODE:
			System.out.println(indent + node.getNodeValue());

			break;

		case Node.PROCESSING_INSTRUCTION_NODE:
			System.out.println(indent + "<?" + node.getNodeName() + " "
					+ node.getNodeValue() + " ?>");

			break;

		case Node.ENTITY_REFERENCE_NODE:
			System.out.println("&" + node.getNodeName() + ";");

			break;

		case Node.DOCUMENT_TYPE_NODE:

			DocumentType docType = (DocumentType) node;
			System.out.print("<!DOCTYPE " + docType.getName());

			if (docType.getPublicId() != null) {
				System.out.print("PUBLIC \"" + docType.getPublicId() + "\"");
			} else {
				System.out.print(" SYSTEM ");
			}

			System.out.println("\"" + docType.getSystemId() + "\" >");

			break;
		}
	}

}
