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
package org.n52.wps.server.transactional.request;

import java.util.logging.Level;
import java.util.logging.Logger;
import net.opengis.wps.x20.UndeployProcessDocument;
import org.apache.commons.collections.map.CaseInsensitiveMap;
import org.apache.xmlbeans.XmlException;
import org.apache.xmlbeans.XmlOptions;
import org.n52.wps.server.ExceptionReport;
import org.n52.wps.server.request.Request;
import org.n52.wps.server.response.Response;
import org.n52.wps.server.transactional.repository.TransactionalAlgorithmRepository;
import org.n52.wps.server.transactional.response.UndeployProcessResponse;
import org.n52.wps.server.transactional.util.TransactionalRepositoryManagerSingletonWrapper;
import org.w3c.dom.Document;

/**
 * Handles DeployProcess operation
 * @author cnl
 */
public class UndeployProcessRequest extends Request {

    private String identifier;

    public UndeployProcessRequest(CaseInsensitiveMap map) throws ExceptionReport {
        super(map);
    }

    public UndeployProcessRequest(Document doc) throws ExceptionReport {
         super(doc);
        try {
            XmlOptions option = new XmlOptions();
            option.setLoadTrimTextBuffer();
            UndeployProcessDocument undeploy = UndeployProcessDocument.Factory.parse(doc, option);
            this.identifier = undeploy.getUndeployProcess().getIdentifier().getStringValue();
        } catch (XmlException ex) {
            Logger.getLogger(UndeployProcessRequest.class.getName()).log(Level.SEVERE,
                    null, ex);
        }
        
           }

    @Override
    public Object getAttachedResult() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public Response call() throws ExceptionReport {
        TransactionalAlgorithmRepository repository = (TransactionalAlgorithmRepository) TransactionalRepositoryManagerSingletonWrapper.getInstance().getRepositoryForAlgorithm(
                identifier);
        repository.undeployAlgorithm(identifier);
         UndeployProcessResponse response = new UndeployProcessResponse(this);
         response.setSuccess(true);
        return response;
    }

    @Override
    public boolean validate() throws ExceptionReport {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
    
}
