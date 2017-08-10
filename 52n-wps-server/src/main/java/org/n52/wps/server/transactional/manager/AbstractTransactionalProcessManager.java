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
package org.n52.wps.server.transactional.manager;

import java.util.Collection;
import net.opengis.wps.x20.ExecuteDocument;
import org.n52.wps.server.request.ExecuteRequest;
import org.n52.wps.server.transactional.profiles.DeploymentProfile;
import org.n52.wps.server.transactional.request.UndeployProcessRequest;
import org.w3c.dom.Document;

/**
 *
 * @author cnl
 */
public abstract class AbstractTransactionalProcessManager implements IProcessManager{

    private  DeploymentProfile profile;
    private  String processID;

    public AbstractTransactionalProcessManager(DeploymentProfile profile) {
        this.profile=profile;
        this.processID=profile.getProcessID();
    }

   

    
    @Override
    public abstract  boolean unDeployProcess(UndeployProcessRequest request) throws Exception;
    @Override
        public abstract boolean containsProcess(String processID) throws Exception;
    @Override
	public abstract Collection<String> getAllProcesses() throws Exception;
    @Override
	public abstract Document invoke(ExecuteDocument payload, String algorithmID) throws Exception;
    @Override
	public abstract Document invoke(ExecuteRequest request, String algorithmID) throws Exception;
    @Override
	public abstract boolean deployProcess(DeploymentProfileOrice request) throws Exception;
	

    /**
     * @return the profile
     */
    public DeploymentProfile getProfile() {
        return profile;
    }

    /**
     * @param profile the profile to set
     */
    public void setProfile(
            DeploymentProfile profile) {
        this.profile = profile;
    }

    /**
     * @return the processID
     */
    public String getProcessID() {
        return processID;
    }

    /**
     * @param processID the processID to set
     */
    public void setProcessID(String processID) {
        this.processID = processID;
    }
    
}
