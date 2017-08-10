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

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Constructor;
import net.opengis.wps.x20.AbstractDeploymentProfileType;
import net.opengis.wps.x20.DeployProcessDocument;
import net.opengis.wps.x20.DescriptionType;
import net.opengis.wps.x20.ProcessOfferingDocument;
import org.apache.commons.collections.map.CaseInsensitiveMap;
import org.apache.commons.io.FileUtils;
import org.apache.xmlbeans.XmlException;
import org.apache.xmlbeans.XmlOptions;
import org.n52.wps.server.ExceptionReport;
import org.n52.wps.server.RepositoryManagerSingletonWrapper;
import org.n52.wps.server.request.Request;
import org.n52.wps.server.response.Response;
import org.n52.wps.server.transactional.profiles.DeploymentProfile;
import org.n52.wps.server.transactional.repository.TransactionalAlgorithmRepository;
import org.n52.wps.server.transactional.response.DeployProcessResponse;
import org.n52.wps.server.transactional.util.TransactionalRepositoryManagerSingletonWrapper;
import org.slf4j.LoggerFactory;
import org.springframework.web.multipart.MultipartFile;
import org.w3c.dom.Document;

/**
 * Handles DeployProcess operation
 *
 * @author cnl
 */
public class DeployProcessRequest extends Request {

    private static org.slf4j.Logger log = LoggerFactory.getLogger(
            DeployProcessRequest.class.getName());

    private DeployProcessDocument deployDom;
    private DeploymentProfile deploymentProfile;

    public DeployProcessRequest(CaseInsensitiveMap map) throws ExceptionReport {
        super(map);
    }

    public DeployProcessRequest(Document doc) throws ExceptionReport {
        super(doc);
        LOGGER.info("DeployProcessRequest constructor");
        /**
         * DeployProcess request is parsed from XML to XMLbeans bindings
         */
        try {
            XmlOptions option = new XmlOptions();
            option.setLoadTrimTextBuffer();
            this.deployDom = DeployProcessDocument.Factory.parse(doc, option);
            if (this.deployDom == null) {
                LOGGER.warn("DeployProcessDocument is null");
                throw new ExceptionReport("Error while parsing post data",
                        ExceptionReport.MISSING_PARAMETER_VALUE);
            }
        } catch (XmlException e) {
            throw new ExceptionReport("Error while parsing post data",
                    ExceptionReport.MISSING_PARAMETER_VALUE, e);
        }
        LOGGER.debug("DeployProcessRequest parsing done");
        // validate the client input (TODO)
        validate();
        LOGGER.debug("DeployProcessRequest validation done");
        // Extract deployment profile
        AbstractDeploymentProfileType deploymentProfile = this.deployDom.getDeployProcess().getDeploymentProfile();
        // Extract process description (offering)
        ProcessOfferingDocument.ProcessOffering processOffering = this.deployDom.getDeployProcess().getProcessOffering();
        // Get schema reference of the deployment profile.
        String schema = deploymentProfile.getSchema().getReference();
        DescriptionType descriptionType = processOffering.getProcess();
        LOGGER.debug("DeployProcessRequest looking deployment profile");
        String deployementProfileClass;
        try {
            // Question TransactionRepositoryManager to get the deployment profile class (defined in DB configuration module)
            deployementProfileClass = TransactionalRepositoryManagerSingletonWrapper.getInstance().getDeploymentProfileForSchema(
                    schema);
            Constructor<?> constructor;
            constructor = Class.forName(deployementProfileClass).getConstructor(
                    DeployProcessDocument.class, String.class);
            // Instantiate the deployment profile (constructor is called with deploy request and process id)
            DeploymentProfile profile = (DeploymentProfile) constructor.newInstance(
                    this.deployDom,
                    processOffering.getProcess().getIdentifier().getStringValue());
            LOGGER.debug("DeployProcessRequest found deployment profile");
            // Instantiated profile can be set.
            this.deploymentProfile = profile;

        } catch (Exception e) {
            throw new ExceptionReport("Error ",
                    ExceptionReport.MISSING_PARAMETER_VALUE, e);
        }
    }

    public DeployProcessDocument getDeployDom() {
        return deployDom;
    }

    public void setDeployDom(DeployProcessDocument deployDom) {
        this.deployDom = deployDom;
    }

    public DeploymentProfile getDeploymentProfile() {
        return deploymentProfile;
    }

    public void setDeploymentProfile(DeploymentProfile deploymentProfile) {
        this.deploymentProfile = deploymentProfile;
    }

    @Override
    public Object getAttachedResult() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    /**
     * Handle the deployment process
     */
    public Response call() throws ExceptionReport {
        LOGGER.info("Starting the deployement...");
       

      
        // add process to repository map
        /// IAlgorithmRepository repository = RepositoryManager.getRepositoryForSchema(this.getDeploymentProfile().getProcessID());
        //TransactionalAlgorithmRepository repository = (TransactionalAlgorithmRepository) TransactionalRepositoryManagerSingletonWrapper.getInstance().getRepositoryForSchema(
                //this.getDeploymentProfile().getSchema());
                LOGGER.debug("Getting transactional algorithm repository");
                TransactionalAlgorithmRepository repository = (TransactionalAlgorithmRepository) TransactionalRepositoryManagerSingletonWrapper.getInstance().getRepositoryForSchema(
                this.getDeploymentProfile().getSchema());
                
        
        repository.deployAlgorithm(this.getDeploymentProfile());
        
        
        DeployProcessResponse response = new DeployProcessResponse(this);
        response.setSuccess(true);

        return response;
    }

    @Override
    public boolean validate() throws ExceptionReport {
        return true;
    }

    private void saveXml(MultipartFile xml, String savePath) throws IOException {
        if (xml != null) {
            LOGGER.debug("Trying to upload '{}'.", xml.getOriginalFilename());
            String xmlPath = savePath + "/" + xml.getOriginalFilename();
            if (xml != null) {
                FileUtils.writeByteArrayToFile(new File(xmlPath), xml.getBytes());
            }
            LOGGER.info("Uploaded file saved in '{}'.", xmlPath);
        }
    }

}
