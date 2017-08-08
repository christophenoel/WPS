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
package org.n52.wps.server.transactional.module;

import java.util.ArrayList;
import java.util.List;

import org.n52.wps.webapp.api.AlgorithmEntry;
import org.n52.wps.webapp.api.ClassKnowingModule;
import org.n52.wps.webapp.api.ConfigurationKey;
import org.n52.wps.webapp.api.FormatEntry;
import org.n52.wps.webapp.api.types.ConfigurationEntry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author cnl
 */
public abstract class TransactionalAlgorithmRepositoryCMBase extends ClassKnowingModule {

    protected static Logger LOGGER = LoggerFactory.getLogger(
            TransactionalAlgorithmRepositoryCMBase.class);
    protected List<? extends ConfigurationEntry<?>> configurationEntries = new ArrayList<>();

    public TransactionalAlgorithmRepositoryCMBase() {
        algorithmEntries = new ArrayList<>();
    }

    public final String KEY_PROFILE = "profile";
    public final String KEY_SCHEMA = "schema";
    public final String KEY_MANAGER = "manager";
    private boolean isActive = true;

    private List<AlgorithmEntry> algorithmEntries;

    private String profile;

    public String getProfile() {
        return profile;
    }

    @ConfigurationKey(key = "profile")
    public void setProfile(String profile) {
        this.profile = profile;
    }
    private String schema;

    public String getSchema() {
        return schema;
    }

    @ConfigurationKey(key = "schema")
    public void setSchema(String schema) {
        this.schema = schema;
    }
    private String manager;

    public String getManager() {
        return manager;
    }

    @ConfigurationKey(key = "manager")
    public void setManager(String manager) {
        this.manager = manager;
    }

    @Override
    public boolean isActive() {
        return isActive;
    }

    @Override
    public void setActive(boolean active) {
        this.isActive = active;
    }

    @Override
    public List<AlgorithmEntry> getAlgorithmEntries() {
        LOGGER.debug("Getting algorithm entries");
        return algorithmEntries;
    }

    @Override
    public List<FormatEntry> getFormatEntries() {
        return null;
    }

    @Override
    public List<? extends ConfigurationEntry<?>> getConfigurationEntries() {
        return configurationEntries;
    }

}
