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
package org.n52.wps.server.transactional.repository;

import java.util.List;
import java.util.Map;

import org.n52.wps.commons.WPSConfig;
import org.n52.wps.server.RepositoryManager;
import org.n52.wps.server.RepositoryManagerSingletonWrapper;
import org.n52.wps.server.transactional.util.TransactionalRepositoryManagerSingletonWrapper;
import org.n52.wps.webapp.api.ClassKnowingModule;
import org.n52.wps.webapp.api.ConfigurationCategory;
import org.n52.wps.webapp.api.ConfigurationModule;
import org.n52.wps.webapp.api.types.ConfigurationEntry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * General transactional tasks
 *
 * @author cnl
 */
public class TransactionalRepositoryManager extends RepositoryManager {

       public final String KEY_PROFILE = "profile";
    public final String KEY_SCHEMA = "schema";
    public final String KEY_MANAGER = "manager";
    
    private RepositoryManager repositoryManager;

    public void init() {
           TransactionalRepositoryManagerSingletonWrapper.init(this);
        this.repositoryManager = RepositoryManagerSingletonWrapper.getInstance();
        this.repositories = repositoryManager.repositories;
        this.updateThread = repositoryManager.updateThread;
    }

   
    private static Logger LOGGER = LoggerFactory
            .getLogger(TransactionalRepositoryManager.class.getName());

    /**
     * Retrieve the deployment profile corresponding to a schema reference by
     * looking into the existing configuration modules.
     *
     * @param schema
     * @return
     */
    public String getDeploymentProfileForSchema(String schema) {
        LOGGER.debug("getDeploymentProfileForSchema:" + schema);
        ConfigurationModule module = getConfigurationModuleForSchema(schema);
        if (module == null) {
            return null;
        }
        List<? extends ConfigurationEntry<?>> entries = module.getConfigurationEntries();
        for (ConfigurationEntry<?> entry : entries) {
            String deployementProfileClass;
            if (entry.getKey().equals(KEY_PROFILE)) {
                deployementProfileClass = (String) entry.getValue();
                LOGGER.debug(
                        "deployementProfileClass:" + deployementProfileClass);
                return deployementProfileClass;

            }
        }
        return null;
    }

    public ConfigurationModule getConfigurationModuleForSchema(String schema) {
        LOGGER.debug("getConfigurationModuleForSchema:" + schema);
        Map<String, ConfigurationModule> activeModules = WPSConfig.getInstance().getActiveConfigurationModules(
                ConfigurationCategory.REPOSITORY);
        for (ConfigurationModule module : activeModules.values()) {
            LOGGER.debug("checking for module:" + module.getModuleName());
            if (!(module instanceof ClassKnowingModule)) {
                LOGGER.debug("this module is not classknowingmodule");
                continue;
            }
            List<? extends ConfigurationEntry<?>> entries = module.getConfigurationEntries();
            for (ConfigurationEntry<?> entry : entries) {
                LOGGER.debug(
                        "checking entry " + entry.getKey() + "value " + entry.getValue().toString());
                String s;
                if (entry.getKey().equals(KEY_SCHEMA)) {
                    s = (String) entry.getValue();
                    if (s.equals(schema)) {
                        LOGGER.debug("module name:" + module.getModuleName());
                        return module;
                    }
                }
            }
        }
        /**
         * not found
         */
        LOGGER.warn("No configuration module found");
        return null;
    }


    /**
     * Retrieve a repository for a given schema. Configuration module is
     * searched first, then repository class is retrieve for the matching CM.
     *
     * @param schema
     * @return
     */
    public TransactionalAlgorithmRepository getRepositoryForSchema(String schema) {
        LOGGER.debug("getRepository for schema:" + schema);
        String repositoryClass = ((ClassKnowingModule) getConfigurationModuleForSchema(
                schema)).getClassName();
        LOGGER.debug("found repository class:" + repositoryClass);
        if (repositoryClass == null) {

            return null;
        }
        return (TransactionalAlgorithmRepository) getRepositoryForClassName(
                repositoryClass);

    }

}
