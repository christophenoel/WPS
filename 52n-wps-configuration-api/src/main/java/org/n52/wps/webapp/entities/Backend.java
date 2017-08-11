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
import org.hibernate.validator.constraints.NotBlank;
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
public class Backend implements ConfigurationModule {

    private static final String blankErrorMessage = "Field cannot be blank.";
    private ConfigurationEntry<String> dockerHostnameEntry = new StringConfigurationEntry(
            "dockerhost", "Docker Server Host Name", "",
            true, "localhost");

    private String dockerHost;
    private String user;

    private String password;

    public String getDockerHost() {
        return dockerHost;
    }

    @ConfigurationKey(key = "docker_host")
    public void setDockerHost(String dockerHost) {
        this.dockerHost = dockerHost;
    }

    private List<? extends ConfigurationEntry<?>> configurationEntries = Arrays.asList(
            dockerHostnameEntry);

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
@ConfigurationKey(key = "ssh_user")
    public void setUser(String user) {
        this.user = user;
    }

    public String getPassword() {
        return password;
    }
@ConfigurationKey(key = "ssh_password")
    public void setPassword(String password) {
        this.password = password;
    }
}
