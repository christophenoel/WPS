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
package org.n52.wps.server.transactional.repository;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import javax.xml.transform.TransformerFactoryConfigurationError;
import net.opengis.wps.x20.ProcessOfferingDocument;
import org.apache.commons.lang.NotImplementedException;
import org.apache.xmlbeans.XmlException;
import org.apache.xmlbeans.XmlOptions;
import org.n52.wps.commons.WPSConfig;
import org.n52.wps.server.AbstractTransactionalAlgorithm;
import org.n52.wps.server.ExceptionReport;
import org.n52.wps.server.IAlgorithm;
import org.n52.wps.server.ITransactionalAlgorithmRepository;
import org.n52.wps.server.ProcessDescription;
import org.n52.wps.server.transactional.algorithm.DefaultTransactionalAlgorithm;
import org.n52.wps.server.transactional.module.TransactionalAlgorithmRepositoryCMBase;
import org.n52.wps.server.transactional.profiles.DeploymentProfile;
import org.n52.wps.server.transactional.request.DeployProcessRequest;
import org.n52.wps.webapp.api.AlgorithmEntry;
import org.n52.wps.webapp.api.ConfigurationCategory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A static repository to retrieve the available algorithms.
 *
 * @author foerster
 *
 */
public abstract class TransactionalAlgorithmRepository implements
        ITransactionalAlgorithmRepository {

    private static Logger LOGGER = LoggerFactory
            .getLogger(TransactionalAlgorithmRepository.class);
    private Map<String, ProcessDescription> processDescriptionMap;
    private Map<String, IAlgorithm> algorithmMap;
    private final TransactionalAlgorithmRepositoryCMBase transactionalAlgorithmRepoConfigModule;
    private String schema;

    /**
     * Retrieve the corresponding configuration module and instantiate the
     * algorithm entries
     */
    public TransactionalAlgorithmRepository() {
        LOGGER.info("Creating TransactionalAlgorithmRepository");
        processDescriptionMap = new HashMap<String, ProcessDescription>();
        algorithmMap = new HashMap<String, IAlgorithm>();
        LOGGER.info("Getting Configuration Module");
        // Retrieve the related configuration module (DB config)
        transactionalAlgorithmRepoConfigModule = (TransactionalAlgorithmRepositoryCMBase) WPSConfig.getInstance()
                .getConfigurationModuleForClass(this.getClass().getName(),
                        ConfigurationCategory.REPOSITORY);
        // From the configuration module, retrieve the related schema
        this.schema = transactionalAlgorithmRepoConfigModule.getSchema();
        // check if the repository is active
        if (transactionalAlgorithmRepoConfigModule.isActive()) {
            LOGGER.info(
                    transactionalAlgorithmRepoConfigModule.getModuleName() + "is active getting algorithms");
            // Get algorithm entries
            List<AlgorithmEntry> algorithmEntries = transactionalAlgorithmRepoConfigModule
                    .getAlgorithmEntries();
            if (algorithmEntries != null && algorithmEntries.size() > 0) {
                LOGGER.debug("entries: " + algorithmEntries.size());
                for (AlgorithmEntry algorithmEntry : algorithmEntries) {
                    LOGGER.debug("loop");
                    LOGGER.debug(
                            "adding transact algorithm " + algorithmEntry.getAlgorithm());
                    if (algorithmEntry.isActive()) {
                        addAlgorithm(algorithmEntry.getAlgorithm());
                    }
                }
            }
        } else {
            LOGGER.debug("Transactional Algorithm Repository is inactive.");
        }

    }

    public boolean addAlgorithms(String[] algorithms) {
        throw new NotImplementedException();
    }

    /**
     * Return the requested algorithm
     *
     * @param className
     * @return
     */
    public IAlgorithm getAlgorithm(String className) {
        if (getAlgorithmNames().contains(className)) {
            return algorithmMap.get(className);
        }
        return null;
    }

    /**
     * Return the list of algorithm names
     *
     * @return
     */
    public Collection<String> getAlgorithmNames() {

        Collection<String> algorithmNames = new ArrayList<>();

        List<AlgorithmEntry> algorithmEntries = transactionalAlgorithmRepoConfigModule
                .getAlgorithmEntries();

        for (AlgorithmEntry algorithmEntry : algorithmEntries) {
            LOGGER.debug(
                    "Getting algorithm names found entry:" + algorithmEntry.getAlgorithm());
            if (algorithmEntry.isActive()) {
                algorithmNames.add(algorithmEntry.getAlgorithm());
            }
        }

        return algorithmNames;
    }

    /**
     * Check if an algorithm exists
     *
     * @param className
     * @return
     */
    public boolean containsAlgorithm(String className) {
        DefaultTransactionalAlgorithm algo = (DefaultTransactionalAlgorithm) getAlgorithm(
                className);
        return getAlgorithmNames().contains(className);
    }

    /**
     * Load the algorithm (which is always an instance of
     * DefaultTransactionalAlgorithm)
     *
     * @param algorithmClassName
     * @return
     * @throws Exception
     */
    private IAlgorithm loadAlgorithm(String processId)
            throws Exception {

        IAlgorithm algorithm = null;
        LOGGER.debug(
                "loading default transactional algorithm for transactional process:" + processId);
        String manager = transactionalAlgorithmRepoConfigModule.getManager();
        LOGGER.debug(
                "manager loaded - attentio !!!!! loaded at startup !!! even when manager is modified");
        LOGGER.debug("new default transactional algo");
        algorithm = new DefaultTransactionalAlgorithm(processId, manager);

        boolean isNoProcessDescriptionValid = false;

        for (String supportedVersion : WPSConfig.SUPPORTED_VERSIONS) {
            isNoProcessDescriptionValid = isNoProcessDescriptionValid
                    && !algorithm.processDescriptionIsValid(supportedVersion);
        }

        if (isNoProcessDescriptionValid) {
            LOGGER.warn("Algorithm description is not valid: "
                    + processId);// TODO add version to exception/log
            throw new Exception("Could not load algorithm "
                    + processId + ". ProcessDescription Not Valid.");
        }

        return algorithm;
    }

    public boolean undeployAlgorithm(String identifier) {
        DefaultTransactionalAlgorithm algo = (DefaultTransactionalAlgorithm) getAlgorithm(
                identifier);
        removeAlgorithm(identifier);
        WPSConfig.getInstance().getConfigurationManager().getConfigurationServices().deleteAlgorithmEntry(
                transactionalAlgorithmRepoConfigModule.getClass().getName(),
                identifier);
        return true;
    }

    public boolean deployAlgorithm(DeploymentProfile deployProfile) throws ExceptionReport {

        String processID = deployProfile.getProcessID();
        LOGGER.debug("deploy algorithm " + processID);
        //TransactionalRepositoryManagerSingletonWrapper.getInstance().deployProcess(deployProfile);
        LOGGER.debug("deploying process...");
        // Get the ConfigurationModule

        try {
            LOGGER.debug("adding algorithm entry process...");
            WPSConfig.getInstance().getConfigurationManager().getConfigurationServices().addAlgorithmEntry(
                    transactionalAlgorithmRepoConfigModule.getClass().getName(),
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
        LOGGER.debug("adding algorithm entry in repository");
        // Add algorithm entry in repository
        addAlgorithm(deployProfile.getProcessID());
        DefaultTransactionalAlgorithm algo = (DefaultTransactionalAlgorithm) getAlgorithm(
                deployProfile.getProcessID());
        try {
            algo.getManager().deployProcess(deployProfile);
        } catch (Exception ex) {
            throw new ExceptionReport(
                    "Error: Process cannot be deployed on the backend",
                    ExceptionReport.INVALID_PARAMETER_VALUE, ex);
        }
        return true;
    }

    /**
     * When an algorithm is discovered (or deploy) it is instantiated, added to
     * the algorithm map, and process description is also added. added to the
     * algorithms map
     *
     * @return
     */
    public boolean addAlgorithm(Object processIDObject) {
        // TODO check if contains
        LOGGER.debug("adding algorithm :" + processIDObject);
        if (!(processIDObject instanceof String)) {
            LOGGER.warn("processId not a String");
            return false;
        }
        String processID = (String) processIDObject;

        try {
            LOGGER.debug("add algo : loading algorithm :" + processID);
            IAlgorithm algorithm = loadAlgorithm(processID);
            LOGGER.debug("putting algorithm to map:" + processID);
            processDescriptionMap.put(processID,
                    algorithm.getDescription());
            LOGGER.debug(
                    "Added description:" + algorithm.getDescription().toString());
            algorithmMap.put(processID, algorithm);
            LOGGER.info("Algorithm class registered: " + processID);

            return true;
        } catch (Exception e) {
            e.printStackTrace();
            LOGGER.error("Exception while trying to add algorithm {}",
                    processID);
            LOGGER.error(e.getMessage());

        }

        return false;

    }

    public boolean removeAlgorithm(Object processID) {
        throw new NotImplementedException();
    }

    @Override
    public ProcessDescription getProcessDescription(String processID) {
        LOGGER.debug("Getting process description of " + processID);
        if (getAlgorithmNames().contains(processID)) {
            LOGGER.debug("contains process:" + processID);
            ProcessDescription desc = processDescriptionMap.get(processID);
            if (desc == null) {
                LOGGER.debug("Description is null - listing map");
                for (IAlgorithm algo : algorithmMap.values()) {
                    LOGGER.debug("found:" + algo.getWellKnownName());
                }
                return null;

            }
            return desc;
        }
        LOGGER.debug("does not contain process:" + processID + " - listing map:");
        for (IAlgorithm algo : algorithmMap.values()) {
            LOGGER.debug("found:" + algo.getWellKnownName());
        }
        return null;
    }

    @Override
    public void shutdown() {
    }

    public String getSchema() {
        return schema;
    }

    public void setSchema(String schema) {
        this.schema = schema;
    }

    /**
     * Writes the ProcessDescription in the appropriate directory. Additional
     * Note the directory (location) is agnostic of the repository / config
     * module.
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
        LOGGER.debug("Get description file  of " + processId);
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
        LOGGER.info("Path for description is " + path);
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
            ex.printStackTrace();
            java.util.logging.Logger.getLogger(
                    TransactionalRepositoryManager.class.getName()).log(
                    Level.SEVERE,
                    null, ex);

        } catch (IOException ex) {
            ex.printStackTrace();
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

}
