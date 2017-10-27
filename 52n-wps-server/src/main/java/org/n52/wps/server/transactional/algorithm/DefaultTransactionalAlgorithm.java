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
package org.n52.wps.server.transactional.algorithm;

import java.lang.reflect.Constructor;
import java.util.List;
import java.util.Map;
import net.opengis.wps.x20.ProcessOfferingDocument;
import org.n52.wps.commons.WPSConfig;
import org.n52.wps.io.data.IData;
import org.n52.wps.io.data.binding.complex.GenericFileDataBinding;
import org.n52.wps.server.ExceptionReport;
import org.n52.wps.server.IAlgorithm;
import org.n52.wps.server.ProcessDescription;
import org.n52.wps.server.RepositoryManagerSingletonWrapper;
import org.n52.wps.server.request.ExecuteRequest;
import org.n52.wps.server.transactional.manager.IProcessManager;
import org.n52.wps.server.transactional.repository.TransactionalAlgorithmRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author cnl
 */
public class DefaultTransactionalAlgorithm implements IAlgorithm {

    private static Logger LOGGER = LoggerFactory
            .getLogger(DefaultTransactionalAlgorithm.class);
    private ProcessDescription description;
    protected String algorithmID;
    private IProcessManager manager;

    public IProcessManager getManager() {
        return manager;
    }

    public void setManager(IProcessManager manager) {
        this.manager = manager;
    }


    public DefaultTransactionalAlgorithm(String algorithmID, String  managerClass) throws Exception {
        LOGGER.debug("loading default transactional algorith for "+algorithmID+ " with managerClass "+managerClass);
        this.algorithmID = algorithmID;
        try {
         Constructor<?> constructor;
            constructor = Class.forName(managerClass).getConstructor(
                   String.class);
            // Instantiate the deployment profile (constructor is called with deploy request and process id)
            this.manager = (IProcessManager) constructor.newInstance(algorithmID);
            
        }
        catch(Exception ex) {
            LOGGER.warn("exception when trying to instantiate");
            ex.printStackTrace();
        }
        
    }

    public String getAlgorithmID() {
        return algorithmID;
    }

   

   

    
    public Map<String, IData> run(Map<String, List<IData>> inputData, ExecuteRequest request) throws ExceptionReport {
        ProcessDescription pdesc = RepositoryManagerSingletonWrapper.getInstance().getProcessDescription(this.getAlgorithmID());
            ProcessOfferingDocument.ProcessOffering offering  = (ProcessOfferingDocument.ProcessOffering) pdesc.getProcessDescriptionType(WPSConfig.VERSION_200);
        Map<String, IData> response = manager.invoke(inputData, algorithmID,offering,request);
        return response;
    }

    @Override
    public List<String> getErrors() {
        LOGGER.warn("getErrors returns null (implement if necesary)");
        return null;
    }

    @Override
    public String getWellKnownName() {
        return this.getAlgorithmID();
    }

    @Override
    public boolean processDescriptionIsValid(String version) {
        return true;
    }

    @Override
    public Class<?> getInputDataType(String id) {
        throw new UnsupportedOperationException("Transactional getInputDataType not supported."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public Class<?> getOutputDataType(String id) {
        return GenericFileDataBinding.class;
    }

    public ProcessOfferingDocument.ProcessOffering getProcessOffering() throws Exception {
        LOGGER.debug(
                "getProcessOffering called for transactional process:" + algorithmID);
        TransactionalAlgorithmRepository repository = (TransactionalAlgorithmRepository) RepositoryManagerSingletonWrapper.getInstance().getRepositoryForAlgorithm(
                algorithmID);
        //return TransactionalRepositoryManager.getDescription(algorithmID).getProcessOffering();
        return repository.getDescription(algorithmID).getProcessOffering();

    }

    private ProcessDescription initializeDescription() throws Exception {
        LOGGER.debug("initializing description");
        ProcessDescription processDescription = new ProcessDescription();
        
       LOGGER.debug("adding process description for version 2.0 of "+algorithmID);
        ProcessOfferingDocument.ProcessOffering processOffering = TransactionalAlgorithmRepository.getDescription(algorithmID).getProcessOffering();
        processDescription.addProcessDescriptionForVersion(
                processOffering,
                WPSConfig.VERSION_200);
        this.description=processDescription;
        return processDescription;
    }

    public ProcessDescription getDescription()  {
        LOGGER.debug("initialize description");
        if(description==null) {
            try {
                initializeDescription();
            } catch (Exception ex) {
                ex.printStackTrace();
                LOGGER.warn("description retrieve failed");
            }
        }
        return description;

    }

    @Override
    public Map<String, IData> run(Map<String, List<IData>> inputData) throws ExceptionReport {
        throw new UnsupportedOperationException("Not supported - replaced with unique id-"); //To change body of generated methods, choose Tools | Templates.
    }
}






