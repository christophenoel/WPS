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
package org.n52.wps.server.transactional.profiles.docker.managers;

import com.spotify.docker.client.DockerClient;
import com.spotify.docker.client.LogMessage;
import com.spotify.docker.client.LogStream;
import com.spotify.docker.client.exceptions.DockerException;
import com.spotify.docker.client.messages.ContainerInfo;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.security.PublicKey;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import javax.xml.namespace.NamespaceContext;
import net.opengis.ows.x20.MetadataType;
import net.opengis.wps.x20.InputDescriptionType;
import net.opengis.wps.x20.OutputDescriptionType;
import net.opengis.wps.x20.ProcessOfferingDocument;
import net.schmizz.sshj.SSHClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DockerUtil {

    private static Logger log = LoggerFactory
            .getLogger(DockerUtil.class);

    /**
     * Return an input description
     *
     * @param id
     * @param description
     */
    public static InputDescriptionType getInputDesc(String id,
            ProcessOfferingDocument.ProcessOffering description) {
        //System.out.println("GetInputDesc for "+id);
        for (InputDescriptionType input : description.getProcess().getInputArray()) {

            if (input.getIdentifier().getStringValue().equals(id)) {
                return input;
            }
        }
        return null;
    }

    /**
     * Return an output description
     *
     * @param id
     * @param description
     */
    public static OutputDescriptionType getOutputDesc(String id,
            ProcessOfferingDocument.ProcessOffering description) {
        //System.out.println("GetInputDesc for "+id);
        for (OutputDescriptionType output : description.getProcess().getOutputArray()) {

            if (output.getIdentifier().getStringValue().equals(id)) {
                return output;
            }
        }
        return null;
    }

    /**
     * Get a Metadata block according to role key
     *
     * @param role
     * @param description
     * @return
     */
    public static String getMetadataRefByKey(String role,
            ProcessOfferingDocument.ProcessOffering description) {
        for (MetadataType m : description.getProcess().getMetadataArray()) {
            if (m.getRole().equalsIgnoreCase(role)) {
                return m.getHref();
            }
        }
        return null;
    }

    /**
     * Get a Metadata block according to role key
     *
     * @param role
     * @param description
     * @return
     */
    public static MetadataType getMetadataContentByKey(String role,
            ProcessOfferingDocument.ProcessOffering description) {
        //log.debug("Retrieving MetadataContent from description:" + description);
        for (MetadataType m : description.getProcess().getMetadataArray()) {
            if (m.getRole().equalsIgnoreCase(role)) {
                log.debug("found role");
                return m;
            }
        }
        log.debug("not found role " + role);
        return null;
    }

    public static SSHClient getSSHClient(String sshhost, String user,
            String pass) throws IOException {
        SSHClient ssh = new SSHClient();
        ssh.loadKnownHosts();
        // TODO improvement: add host to the file known hosts file instead of accepting all connection (?)
        ssh.addHostKeyVerifier((String fingerprint, int p, PublicKey k) -> true);
        ssh.connect(sshhost);
        ssh.authPassword(user, pass);
        return ssh;

    }

    public static NamespaceContext getTB13NamespaceContext() {
        NamespaceContext nc = new DockerNamespaceContext();
        return nc;
    }

    /**
     * DO NOT WORK - continuous logging
     */
    static void logAndWait(DockerClient docker, String id) throws DockerException, InterruptedException {

        LogStream stream = docker.attachContainer(id,
                DockerClient.AttachParameter.STDOUT,
                DockerClient.AttachParameter.STREAM);
        LogStream streamErr = docker.attachContainer(id,
                DockerClient.AttachParameter.STDERR,
                DockerClient.AttachParameter.STREAM);
        ContainerInfo info = docker.inspectContainer(id);
        while (info.state().running()) {

            while (stream.hasNext()) {
                LogMessage logMessage = stream.next();
                ByteBuffer buffer = logMessage.content();
                byte[] bytes = new byte[buffer.remaining()];
                buffer.get(bytes);
                log.debug(new String(bytes));
            }
            while (streamErr.hasNext()) {
                LogMessage logMessage = streamErr.next();
                ByteBuffer buffer = logMessage.content();
                byte[] bytes = new byte[buffer.remaining()];
                buffer.get(bytes);
                log.debug(new String(bytes));
            }
            Thread.sleep(2000);
        }
    }

    /**
     * Unzip it
     *
     * @param zipFile input zip file
     * @param output zip file output folder
     */
    public static void unZipIt(String zipFile, String outputFolder) {

        byte[] buffer = new byte[1024];
        try {
            //create output directory is not exists
            File folder = new File(outputFolder);
            if (!folder.exists()) {
                folder.mkdir();
            }
            //get the zip file content
            ZipInputStream zis
                    = new ZipInputStream(new FileInputStream(zipFile));
            //get the zipped file list entry
            ZipEntry ze = zis.getNextEntry();
            while (ze != null) {
                String fileName = ze.getName();
                File newFile = new File(outputFolder + File.separator + fileName);

                System.out.println("file unzip : " + newFile.getAbsoluteFile());

                //create all non exists folders
                //else you will hit FileNotFoundException for compressed folder
                new File(newFile.getParent()).mkdirs();
                FileOutputStream fos = new FileOutputStream(newFile);
                int len;
                while ((len = zis.read(buffer)) > 0) {
                    fos.write(buffer, 0, len);
                }
                fos.close();
                ze = zis.getNextEntry();
            }

            zis.closeEntry();
            zis.close();

            System.out.println("Done");

        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

}
