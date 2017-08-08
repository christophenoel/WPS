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
/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.n52.wps.server.transactional.util;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;

/**
 *
 * @author cnl
 */
public class DownloadUtil {

    public static byte[] downloadHTTP(String url) throws Exception {
        URL u = new URL(url);
        URLConnection uc = u.openConnection();
        String contentType = uc.getContentType();
        int contentLength = uc.getContentLength();
        InputStream raw = uc.getInputStream();
        InputStream in = new BufferedInputStream(raw);
        byte[] data = new byte[contentLength];
        int bytesRead = 0;
        int offset = 0;
        while (offset < contentLength) {
            System.out.print(".");
            bytesRead = in.read(data, offset, data.length - offset);
            if (bytesRead == -1) {
                break;
            }
            offset += bytesRead;
        }
        in.close();
        if (offset != contentLength) {
            throw new IOException("Only read " + offset + " bytes; Expected "
                    + contentLength + " bytes");
        }
        return data;
    }

    /**
     * public static byte[] downloadFTP(String url) throws Exception { URL u =
     * new URL(url); FTPClient client = new FTPClient(); ByteArrayOutputStream
     * fos = null; client.connect(u.getHost(), u.getPort()); // hardcoded
     * MutableDateTime.Property[] properties = WPSConfig.getInstance()
     * .getPropertiesForServer(); MutableDateTime.Property ftpUserProp =
     * WPSConfig.getInstance().getPropertyForKey( properties, "portalFTPUser");
     * MutableDateTime.Property ftpPassProp =
     * WPSConfig.getInstance().getPropertyForKey( properties,
     * "portalFTPPassword"); client.login(ftpUserProp.getStringValue(),
     * ftpPassProp.getStringValue()); int reply = client.getReplyCode(); if
     * (!FTPReply.isPositiveCompletion(reply)) { client.disconnect(); } String
     * filename = u.getFile(); fos = new ByteArrayOutputStream();
     * client.retrieveFile(filename, fos); byte[] data = fos.toByteArray();
     * fos.close(); client.disconnect(); return data; }
     */
}
