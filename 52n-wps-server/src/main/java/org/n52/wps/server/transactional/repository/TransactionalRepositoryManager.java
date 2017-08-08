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

import java.beans.PropertyChangeEvent;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import javax.xml.transform.TransformerFactoryConfigurationError;
import net.opengis.wps.x20.ProcessOfferingDocument;
import org.apache.xmlbeans.XmlException;
import org.apache.xmlbeans.XmlOptions;

import org.n52.wps.commons.WPSConfig;
import org.n52.wps.server.AbstractTransactionalAlgorithm;
import org.n52.wps.server.ExceptionReport;
import org.n52.wps.server.IAlgorithmRepository;
import org.n52.wps.server.ProcessIDRegistry;
import org.n52.wps.server.RepositoryManager;
import org.n52.wps.server.RepositoryManagerSingletonWrapper;
import org.n52.wps.server.transactional.profiles.DeploymentProfile;
import org.n52.wps.server.transactional.request.DeployProcessRequest;
import org.n52.wps.server.transactional.util.TransactionalRepositoryManagerSingletonWrapper;
import org.n52.wps.webapp.api.ClassKnowingModule;
import org.n52.wps.webapp.api.ConfigurationCategory;
import org.n52.wps.webapp.api.ConfigurationModule;
import org.n52.wps.webapp.api.types.ConfigurationEntry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;

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

    public void deployProcess(DeploymentProfile deployProfile) throws ExceptionReport {
        LOGGER.debug("deploying process...");
        // Get the ConfigurationModule
        ConfigurationModule repositoryModule = this.getConfigurationModuleForSchema(
                deployProfile.getSchema());
        // Add algorithm entry in configuration module
        try {
            LOGGER.debug("adding algorithm entry process...");
            WPSConfig.getInstance().getConfigurationManager().getConfigurationServices().addAlgorithmEntry(
                    repositoryModule.getClass().getName(),
                    deployProfile.getProcessID());
        } catch (Exception e) {
            throw new ExceptionReport("Error: Process already exists",
                    ExceptionReport.INVALID_PARAMETER_VALUE, e);
        }
        /**
         * Save process description. Note that the Process Description is put in
         * an location agnostic of the repository profile. Therefore this is
         * handled by the TransactionalRepositoryManager.
         *
         */
        try {
            LOGGER.debug("writing process description file...");
            ProcessOfferingDocument po = ProcessOfferingDocument.Factory.newInstance();
            po.setProcessOffering(
                    deployProfile.getProcessOffering());
            // Call the transactional repository manager to write process description
            setDescription(deployProfile.getProcessID(), po);
            // Call backend manager to deploy
        } catch (Exception ex) {
            java.util.logging.Logger.getLogger(
                    DeployProcessRequest.class.getName()).log(
                    Level.SEVERE,
                    null, ex);
        }
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
     * Writes the ProcessDescription in the appropriate directory. Additional
     * Note the directory (location) is agnostic of the repository / config module.
     *
     * @param processId
     * @param processDescription
     */
    public static void setDescription(String processId,
            ProcessOfferingDocument processDescription) {
        String fullPath = AbstractTransactionalAlgorithm.class
                .getProtectionDomain().getCodeSource().getLocation().toString();
        int searchIndex = fullPath.indexOf("WEB-INF");
        String subPath = fullPath.substring(0, searchIndex);
        subPath = subPath.replaceFirst("file:", "");
        /**
         * if (subPath.startsWith("/")) { subPath = subPath.substring(1); }
         */
        File directory = new File(subPath + "WEB-INF/ProcessDescriptions/");
        if (!directory.exists()) {
            directory.mkdirs();
        }
        String path = subPath + "WEB-INF/ProcessDescriptions/" + processId
                + ".xml";
        try {
            // TODO handling when exception occurs ...
            LOGGER.info("*************************=========. write " + path);
            processDescription.save(new File(path));

        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    /**
     * Read the Process Description for the given Process Id in the location
     * note the location is agnostic of the repository and config module.
     */
    public static ProcessOfferingDocument getDescription(String processId) {
        String fullPath = AbstractTransactionalAlgorithm.class
                .getProtectionDomain().getCodeSource().getLocation().toString();
        int searchIndex = fullPath.indexOf("WEB-INF");
        String subPath = fullPath.substring(0, searchIndex);
        subPath = subPath.replaceFirst("file:", "");
        /**
         * Cause problem ! if (subPath.startsWith("/")) { subPath =
         * subPath.substring(1); }
         */
        String path = subPath + "WEB-INF/ProcessDescriptions/" + processId
                + ".xml";
        LOGGER.info(path);
        try {
            XmlOptions option = new XmlOptions();
            option.setLoadTrimTextBuffer();
            File descFile = new File(path);
            ProcessOfferingDocument descDom = ProcessOfferingDocument.Factory
                    .parse(descFile, option);
            return descDom;
        } catch (TransformerFactoryConfigurationError e) {
            e.printStackTrace();

        } catch (XmlException ex) {
            java.util.logging.Logger.getLogger(
                    TransactionalRepositoryManager.class.getName()).log(
                    Level.SEVERE,
                    null, ex);
        } catch (IOException ex) {
            java.util.logging.Logger.getLogger(
                    TransactionalRepositoryManager.class.getName()).log(
                    Level.SEVERE,
                    null, ex);
        }
        return null;
    }

    /**
     * Remove description from the transactional repository descriptions
     * location the location is agnostic of the repository / module.
     *
     * @param processId
     */
    public static void removeDescription(String processId) {
        String fullPath = AbstractTransactionalAlgorithm.class
                .getProtectionDomain().getCodeSource().getLocation().toString();
        int searchIndex = fullPath.indexOf("WEB-INF");
        String subPath = fullPath.substring(0, searchIndex);
        subPath = subPath.replaceFirst("file:", "");
        String path = subPath + "WEB-INF/ProcessDescriptions/" + processId
                + ".xml";
        File descFile = new File(path);
        descFile.delete();
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
