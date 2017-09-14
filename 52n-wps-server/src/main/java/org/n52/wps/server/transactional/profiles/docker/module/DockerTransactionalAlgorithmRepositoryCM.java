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
package org.n52.wps.server.transactional.profiles.docker.module;

import java.util.Arrays;
import java.util.List;
import org.n52.wps.server.transactional.module.TransactionalAlgorithmRepositoryCMBase;
import org.n52.wps.server.transactional.profiles.docker.repository.DockerTransactionalAlgorithmRepository;

import org.n52.wps.webapp.api.ConfigurationCategory;
import org.n52.wps.webapp.api.types.ConfigurationEntry;
import org.n52.wps.webapp.api.types.StringConfigurationEntry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DockerTransactionalAlgorithmRepositoryCM extends TransactionalAlgorithmRepositoryCMBase {

    public DockerTransactionalAlgorithmRepositoryCM() {

        super();
    }

    protected static Logger LOGGER = LoggerFactory.getLogger(
            DockerTransactionalAlgorithmRepositoryCM.class);

    @Override
    public String getClassName() {
        return DockerTransactionalAlgorithmRepository.class.getName();
    }

    @Override
    public String getModuleName() {
        return "DockerTransactionalAlgorithmRepositoryCM Configuration Module";
    }

    @Override
    public ConfigurationCategory getCategory() {
        return ConfigurationCategory.REPOSITORY;
    }
    private ConfigurationEntry<String> schemaEntry = new StringConfigurationEntry(
            "schema", "schema Class Name", "schema description to be done",
            true, "http://spacebel.be/profile/docker.xsd");
    private ConfigurationEntry<String> profileEntry = new StringConfigurationEntry(
            "profile", "Profile Class Name", "Description to be done",
            true,
            "org.n52.wps.server.transactional.profiles.docker.DockerDeploymentProfile");

    private ConfigurationEntry<String> managerEntry = new StringConfigurationEntry(
            "manager", "manager Class Name", "Manager Description to be done",
            true,
            "org.n52.wps.server.transactional.profiles.docker.managers.RemoteDockerProcessManager");

    protected List<? extends ConfigurationEntry<?>> configurationEntries = Arrays.asList(
            schemaEntry, profileEntry, managerEntry);

    @Override
    public List<? extends ConfigurationEntry<?>> getConfigurationEntries() {
        return configurationEntries;
    }
}
