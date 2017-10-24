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
package org.n52.wps.webapp.entities;

import java.util.Arrays;
import java.util.List;
import org.n52.wps.webapp.api.AlgorithmEntry;
import org.n52.wps.webapp.api.ConfigurationCategory;
import org.n52.wps.webapp.api.ConfigurationKey;
import org.n52.wps.webapp.api.ConfigurationModule;
import org.n52.wps.webapp.api.FormatEntry;
import org.n52.wps.webapp.api.types.ConfigurationEntry;
import org.n52.wps.webapp.api.types.StringConfigurationEntry;
import org.n52.wps.webapp.dao.CapabilitiesDAO;
import org.n52.wps.webapp.service.CapabilitiesService;

/**
 * Holds parsed backend values.
 *
 * @see CapabilitiesService
 * @see CapabilitiesDAO
 */
public class RemoteDockerHostBackend implements ConfigurationModule {

    private static final String blankErrorMessage = "Field cannot be blank.";

    private List<? extends ConfigurationEntry<?>> configurationEntries = Arrays.asList(
            new StringConfigurationEntry(
                    "dockerurl", "Docker Server URL", "",
                    true, "localhost"),
            new StringConfigurationEntry(
                    "dockerCertPath", "Docker Certificate Path", "",
                    true, ""),
            /**
            new StringConfigurationEntry(
                    "dockerConfig", "Docker Config File Path", "",
                    true, ""),
            new StringConfigurationEntry(
                    "apiVersion", "Docker API Version", "",
                    true, ""),
            new StringConfigurationEntry(
                    "registryUserName", "Docker Registry Username", "",
                    true, ""),
            new StringConfigurationEntry(
                    "registryPassword", "Docker Registry Password", "",
                    true, ""),
            new StringConfigurationEntry(
                    "registryEmail", "Docker Registry Email", "",
                    true, ""),*/
            new StringConfigurationEntry(
                    "sshhost", "Host access by SSH with NFS mounted", "",
                    true, ""),
            new StringConfigurationEntry(
                    "sshuser", "SSH User ", "",
                    true, ""),
            new StringConfigurationEntry(
                    "sshpassword", "SSH Password", "",
                    true, ""),
            new StringConfigurationEntry(
                    "nfsWPSPath", "WPS NFS Path", "",
                    true, ""),
            new StringConfigurationEntry(
                    "nfsEODataPath", "EOData NFS Path", "",
                    true, ""),
            new StringConfigurationEntry(
                    "EODataConversionPrefix", "EOData Prefix for conversion to NFS path", "",
                    true, ""),
            new StringConfigurationEntry(
                    "inputDir", "Inputs Directory Relative Path", "",
                    true, ""),
            new StringConfigurationEntry(
                    "outputDir", "Outputs Directory Relative Path", "",
                    true, ""),
            new StringConfigurationEntry(
                    "envDir", "Environmnent Variables  file location", "",
                    true, "")
    );

    private String dockerURL;
    private String sshhost;
    private String nfsWPSPath;
    private String nfsEODataPath;
    private String user;
    private String password;
    
    private String dockerCertPath;
    /**
    private String dockerConfig;
    private String apiVersion;
    private String registryUserName;
    private String registryPassword;
    private String registryEmail;
    * */
    private String EODataConversionPrefix;

    public String getEODataConversionPrefix() {
        return EODataConversionPrefix;
    }

    @ConfigurationKey(key = "EODataConversionPrefix")
    public void setEODataConversionPrefix(String EODataConversionPrefix) {
        this.EODataConversionPrefix = EODataConversionPrefix;
    }

    public String getInputDir() {
        return inputDir;
    }
@ConfigurationKey(key = "inputDir")
    public void setInputDir(String inputDir) {
        this.inputDir = inputDir;
    }

    public String getOutputDir() {
        return outputDir;
    }
@ConfigurationKey(key = "outputDir")
    public void setOutputDir(String outputDir) {
        this.outputDir = outputDir;
    }

    public String getEnvDir() {
        return envDir;
    }
@ConfigurationKey(key = "envDir")
    public void setEnvDir(String envDir) {
        this.envDir = envDir;
    }
    private String inputDir;
    private String outputDir;
    private String envDir;

    public String getDockerURL() {
        return dockerURL;
    }

    @ConfigurationKey(key = "dockerurl")
    public void setDockerURL(String dockerURL) {
        this.dockerURL = dockerURL;
    }

    @Override
    public List<? extends ConfigurationEntry<?>> getConfigurationEntries() {
        return configurationEntries;
    }

    @Override
    public String getModuleName() {
        return "Backend Platform Configuration (cloud and docker)";
    }

    @Override
    public boolean isActive() {
        return true;
    }

    @Override
    public void setActive(boolean active) {

    }

    @Override
    public ConfigurationCategory getCategory() {
        return ConfigurationCategory.GENERAL;
    }

    @Override
    public List<AlgorithmEntry> getAlgorithmEntries() {
        return null;
    }

    @Override
    public List<FormatEntry> getFormatEntries() {
        return null;
    }

    public String getUser() {
        return user;
    }

    @ConfigurationKey(key = "sshuser")
    public void setUser(String user) {
        this.user = user;
    }

    public String getPassword() {
        return password;
    }

    @ConfigurationKey(key = "sshpassword")
    public void setPassword(String password) {
        this.password = password;
    }

    /**
     * @return the dockerCertPath
     */
    public String getDockerCertPath() {
        return dockerCertPath;
    }

    /**
     * @param dockerCertPath the dockerCertPath to set
     */
    @ConfigurationKey(key = "dockerCertPath")
    public void setDockerCertPath(String dockerCertPath) {
        this.dockerCertPath = dockerCertPath;
    }

    public String getSshhost() {
        return sshhost;
    }

    @ConfigurationKey(key = "sshhost")
    public void setSshhost(String sshhost) {
        this.sshhost = sshhost;
    }

    public String getNfsWPSPath() {
        return nfsWPSPath;
    }

    @ConfigurationKey(key = "nfsWPSPath")
    public void setNfsWPSPath(String nfsWPSPath) {
        this.nfsWPSPath = nfsWPSPath;
    }

    public String getNfsEODataPath() {
        return nfsEODataPath;
    }

    @ConfigurationKey(key = "nfsEODataPath")
    public void setNfsEODataPath(String nfsEODataPath) {
        this.nfsEODataPath = nfsEODataPath;
    }

}
