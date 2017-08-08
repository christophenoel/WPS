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
package org.n52.wps.server.transactional.algorithm;

import java.util.List;
import java.util.Map;
import net.opengis.wps.x100.ExecuteDocument;
import net.opengis.wps.x20.ProcessOfferingDocument;
import org.n52.wps.commons.WPSConfig;
import org.n52.wps.io.data.IData;
import org.n52.wps.server.AbstractTransactionalAlgorithm;
import org.n52.wps.server.ExceptionReport;
import org.n52.wps.server.ProcessDescription;
import org.n52.wps.server.transactional.repository.TransactionalAlgorithmRepository;
import org.n52.wps.server.transactional.repository.TransactionalRepositoryManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author cnl
 */
public class DefaultTransactionalAlgorithm extends AbstractTransactionalAlgorithm{

      private static Logger LOGGER = LoggerFactory
            .getLogger(DefaultTransactionalAlgorithm.class);
    private ProcessDescription description;

    public DefaultTransactionalAlgorithm(String algorithmClassName) {
         super(algorithmClassName);
         this.description = initializeDescription();
    }

   

    @Override
    public Map<String, IData> run(ExecuteDocument document) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public Map<String, IData> run(Map<String, List<IData>> inputData) throws ExceptionReport {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public List<String> getErrors() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }



    @Override
    public String getWellKnownName() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public boolean processDescriptionIsValid(String version) {
        return true;
    }

    @Override
    public Class<?> getInputDataType(String id) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public Class<?> getOutputDataType(String id) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    
    public ProcessOfferingDocument.ProcessOffering getProcessOffering() {
        LOGGER.debug("getProcessOffering called for transactional process:"+algorithmID);
        return TransactionalRepositoryManager.getDescription(algorithmID).getProcessOffering();
    }
    
    private ProcessDescription initializeDescription() {
        LOGGER.debug("initializing description");
          ProcessDescription processDescription = new ProcessDescription();
            processDescription.addProcessDescriptionForVersion(TransactionalRepositoryManager.getDescription(algorithmID).getProcessOffering(), WPSConfig.VERSION_200);
            return processDescription;
    }
    
    public ProcessDescription getDescription() {
         return description;
    
    }
}
