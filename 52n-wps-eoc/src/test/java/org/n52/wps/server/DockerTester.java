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
/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.n52.wps.server;

import com.spotify.docker.client.DefaultDockerClient;
import com.spotify.docker.client.DockerCertificates;
import com.spotify.docker.client.DockerClient;
import com.spotify.docker.client.messages.ImageInfo;
import com.spotify.docker.client.messages.Info;
import java.io.File;
import java.net.URI;
import java.nio.file.Paths;
import java.util.Base64;
import java.util.HashMap;
import net.opengis.ows.x20.MetadataType;
import net.opengis.wps.x20.ComplexDataType;
import net.opengis.wps.x20.DataInputType;
import net.opengis.wps.x20.ExecuteDocument;
import net.opengis.wps.x20.InputDescriptionType;
import net.opengis.wps.x20.LiteralDataType;
import net.opengis.wps.x20.ProcessOfferingDocument;
import net.opengis.wps.x20.ProcessOfferingsDocument;
import net.schmizz.sshj.SSHClient;
import net.schmizz.sshj.sftp.SFTPClient;
import net.schmizz.sshj.xfer.FileSystemFile;
import org.junit.Test;
import org.n52.wps.server.transactional.profiles.docker.managers.DockerUtil;

/**
 *
 * @author cnl
 */
public class DockerTester {

    //@Test
    public void parseExecute() throws Exception {
        ExecuteDocument execute = ExecuteDocument.Factory.parse(new File(
                "D:\\execute.xml"));
        System.out.println(
                "Parsed execute request for " + execute.getExecute().getIdentifier().getStringValue());
        ProcessOfferingDocument.ProcessOffering description = ProcessOfferingsDocument.Factory.parse(
                new File("D:\\landcover.xml")).getProcessOfferings().getProcessOfferingArray()[0];
        System.out.println(
                "Parsed description of " + description.getProcess().getIdentifier().getStringValue());
        MetadataType appContextMetadata = DockerUtil.getMetadataContentByKey(
                "http://www.opengis.net//tb13/eoc/applicationContext",
                description);
        System.out.println("---------------------------------");
        System.out.println("Metadata:"+appContextMetadata.toString());
/**
        XPath xPath = XPathFactory.newInstance().newXPath();
        xPath.setNamespaceContext(DockerUtil.getTB13NamespaceContext());
        NodeList nodes = (NodeList) xPath.evaluate("//eoc:DockerImage",
                appContextMetadata.getDomNode(), XPathConstants.NODESET);
        DockerImageDocument dockerImage = DockerImageDocument.Factory.parse(
                nodes.item(0));
        System.out.println("found:" + dockerImage.toString());
        // Cursor Selection
        XmlCursor selectionCursor = appContextMetadata.newCursor();
        String namespaceDecl = "declare namespace eoc='http://www.opengis.net/wps/2.0/profile/tb13/eoc'; ";
        selectionCursor.selectPath(namespaceDecl
                + "$this//descendant::eoc:DockerImage");
        selectionCursor.toNextSelection();
        XmlObject[] appContext = appContextMetadata.selectChildren(new QName(
                "http://www.opengis.net/wps/2.0/profile/tb13/eoc",
                "ApplicationContext"));

        XmlObject test1 = appContext[0].selectChildren(new QName(
                "http://www.opengis.net/owc/1.0",
                "offering"))[0].selectChildren(new QName(
                "http://www.opengis.net/owc/1.0", "content"))[0].
                selectChildren(new QName(
                        "http://www.opengis.net/wps/2.0/profile/tb13/eoc",
                        "DockerImage"))[0];
        System.out.println(test1.getDomNode().getNodeName());
        System.out.println(test1.toString());

        Element element = (Element) appContextMetadata.getDomNode();
        NodeList alist = element.getElementsByTagName("ApplicationContext");

        System.out.println(alist.getLength());
        //System.out.println(context.toString());
        //ApplicationContextType appContext = ApplicationContextType.Factory.parse(appContextMetadata.getDomNode().getFirstChild());        //System.out.println(context.toString());
        //ApplicationContextType appContext = ApplicationContextType.Factory.parse(appContextMetadata.getDomNode().getFirstChild());
        */
        /**
         * for (int i = 0; i < childNodes.getLength(); i++) { Node owcOffering =
         * childNodes.item(i); System.out.println("type:" +
         * owcOffering.getNodeType()); System.out.println("name:" +
         * owcOffering.getNodeName()); System.out.println("localname:" +
         * owcOffering.getLocalName());
         * //System.out.println("textcontent:"+owcOffering.getTextContent());
         *
         * //
         * System.out.println(owcOffering.getAttributes().getNamedItem("role"));
        }
         */
        HashMap<String, String> params = new HashMap<String, String>();
        HashMap<String, String> files = new HashMap<String, String>();
        HashMap<String, String> localFiles = new HashMap<String, String>();
        for (DataInputType input : execute.getExecute().getInputArray()) {
            String id = input.getId();
            System.out.println("Looking for metadata for " + id);
            InputDescriptionType inputDesc = DockerUtil.getInputDesc(id,
                    description);
            if (inputDesc == null) {
                throw new ExceptionReport(
                        "No metadata defined for this input " + id,
                        ExceptionReport.INVALID_PARAMETER_VALUE);
            }
            if (inputDesc.getDataDescription() instanceof ComplexDataType) {
                ComplexDataType complexData = (ComplexDataType) inputDesc.getDataDescription();
                if (input.isSetReference()) {
                    System.out.println("Found reference");
                    // reference File
                    // Check if starting with prefix
                    //db.getNfsEODataPath();
                } else if (input.isSetData()) {
                    System.out.println(
                            "Found file - parsing " + input.getData().getDomNode().getFirstChild().getNodeValue());
                    // TODO parse this using stream of course...
                    byte[] binary = Base64.getDecoder().decode(
                            input.getData().getDomNode().getFirstChild().getNodeValue());
                    String binaryString = new String(binary);
                    System.out.println("Decoded:" + binaryString);
                } else {
                    if (inputDesc.getMinOccurs().intValue() >= 1) {
                        throw new ExceptionReport(
                                id + " input is missing (mandatory)",
                                ExceptionReport.INVALID_PARAMETER_VALUE);
                    }
                }
            } else if (inputDesc.getDataDescription() instanceof LiteralDataType) {
                LiteralDataType literalData = (LiteralDataType) inputDesc.getDataDescription();
                System.out.println("Found param:"+input.getData().getDomNode().getFirstChild().getNodeValue());
                // Add the literal data values to the list of parameters
                params.put(id, input.getData().toString());
            } else {
                throw new ExceptionReport(
                        id + " input is neither ComplexData nor LiteralData!",
                        ExceptionReport.INVALID_PARAMETER_VALUE);
            }
        }

    }

    //@Test
    public void ssh() throws Exception {

        String INPUT_shapefileAttribute = "Class";
        String INPUT_crs = "EPSG:32615";
        String INPUT_inputfiles = "S2A_MSIL1C_20170309T163321_N0204_R083_T15PWT_20170309T164737.zip";
        String INPUT_refDataShapefile = "F-TEPchiapasLandCover.zip";
        String INPUT_aoi = "POLYGON((-92.906633 16.190411,-92.066559 16.188383,-92.070266 15.376645,-92.907004 15.378567,-92.906633 16.190411))";
        String INPUT_targetResolution = "20";
        String INPUT_image_url = "file:///eodata/Sentinel-2/MSI/L1C/2016/11/07/S2A_OPER_PRD_MSIL1C_PDMC_20161107T222004_R050_V20161107T095032/20161107T095032.SAFE";

        String CONFIG_sshhost = "ld-ogc-tb13-vm1";
        String CONFIG_sshuser = "s_admin";
        String CONFIG_sshpass = "Startup1$";
        // SSH Connexion
        SSHClient ssh = new SSHClient();
        ssh.loadKnownHosts();
        // TODO add host to the file known hosts
        ssh.addHostKeyVerifier("45:77:55:cc:1b:9f:b4:4b:55:05:06:05:56:f4:46:15");
        ssh.connect(CONFIG_sshhost);
        try {
            ssh.authPassword(CONFIG_sshuser, CONFIG_sshpass);
            SFTPClient ftpClient = ssh.newSFTPClient();
            ftpClient.mkdirs("/nfs-wps/LandCoverMapping/Pouet");
            ftpClient.put(new FileSystemFile("C:\\test.txt"),                 "/nfs-wps/LandCoverMapping/Pouet/Test");
        } finally {
            ssh.disconnect();
        }

    }
    @Test

    public void docker() throws Exception {
        final String imageName = "registry.hub.docker.com/cnlspacebel/landcover";
        System.out.println("Starting test Docker");
        DockerClient docker = DefaultDockerClient.builder().uri(
                URI.create("https://172.17.2.165:2376/")).dockerCertificates(
                new DockerCertificates(Paths.get(
                        "D:\\vm1"))).build();
        Info myinfo = docker.info();
        ImageInfo iinfo = docker.inspectImage(imageName);
        System.out.println("INFO IMAGE OS:"+iinfo.os());
        /**
        System.out.println(myinfo.toString());
        // List<ImageSearchResult> searchResult = docker.searchImages("busybox");
        List<Image> list = docker.listImages(
                DockerClient.ListImagesParam.byName(
                        imageName));

        if (list.size() > 0) {
            System.out.println("already downloaded");
        } else {
            System.out.println("not available");
            docker.pull(imageName);
        }

        //docker.pull(imageName);
        ContainerConfig containerConfig = ContainerConfig.builder().image(
                imageName).attachStdout(true)
                .cmd("sh", "-c", "ls")
                //.cmd("sh", "-c", "while :; do sleep 1; done")
                .build();

        ContainerCreation creation = docker.createContainer(containerConfig);
        String id = creation.id();
        final ContainerInfo info = docker.inspectContainer(id);

// Start container
        System.out.println("PROCESS STATE:" + info.state().status());
        docker.startContainer(id);
        final ContainerExit exit = docker.waitContainer("containerID");
        final String stdOutLog;
        try (LogStream stream = docker.logs(id, DockerClient.LogsParam.stdout())) {
            stdOutLog = stream.readFully();
        }
        System.out.println("log stdout:" + stdOutLog);
        System.out.println("PROCESS STATE:" + info.state().status());
        /* Exec command inside running container with attached STDOUT and STDERR*/
        /**
         * final String[] command = {"sh", "-c", "ls"}; final ExecCreation
         * execCreation = docker.execCreate(id, command,
         * DockerClient.ExecCreateParam.attachStdout(),
         * DockerClient.ExecCreateParam.attachStderr()); final LogStream output
         * = docker.execStart(execCreation.id());
         *
         * final String execOutput = output.readFully();
         */
        System.out.println("Output:");
        //System.out.println(execOutput);

    }
}
