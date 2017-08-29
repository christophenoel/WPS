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
package org.n52.wps.server.transactional.profiles.docker.managers;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.CreateContainerResponse;
import com.github.dockerjava.core.DefaultDockerClientConfig;
import com.github.dockerjava.core.DockerClientBuilder;
import com.github.dockerjava.core.DockerClientConfig;
import java.util.Collection;
import net.opengis.wps.x20.ExecuteDocument;
import org.n52.wps.commons.WPSConfig;
import org.n52.wps.server.request.ExecuteRequest;
import org.n52.wps.server.transactional.manager.AbstractTransactionalProcessManager;
import org.n52.wps.server.transactional.profiles.DeploymentProfile;
import org.n52.wps.webapp.entities.RemoteDockerHostBackend;
import org.w3c.dom.Document;

/**
 *
 * @author cnl
 */
public class RemoteDockerProcessManager extends AbstractTransactionalProcessManager {

    private DockerClient docker;

    public RemoteDockerProcessManager(DeploymentProfile profile) throws Exception {
        super(profile);
        RemoteDockerHostBackend db = getBackendConfig();
        DockerClientConfig config = DefaultDockerClientConfig.createDefaultConfigBuilder()
                .withDockerHost(db.getDockerHost())
                .withDockerTlsVerify(true)
                .withDockerCertPath(db.getDockerCertPath())
                .withDockerConfig(db.getDockerConfig())
                .withApiVersion(db.getApiVersion())
                .withRegistryUrl("") /*TODO*/
                .withRegistryUsername(db.getRegistryUserName())
                .withRegistryPassword(db.getRegistryPassword())
                .withRegistryEmail(db.getRegistryEmail())
                .build();
        this.docker = DockerClientBuilder.getInstance(config).build();
    }

    @Override
    public boolean unDeployProcess(String processID) throws Exception {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public boolean containsProcess(String processID) throws Exception {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public Collection<String> getAllProcesses() throws Exception {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public Document invoke(ExecuteDocument payload, String algorithmID) throws Exception {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public Document invoke(ExecuteRequest request, String algorithmID) throws Exception {
        /** TODO parsing of inputs , mouting of volumes, collecting of results**/
        
        CreateContainerResponse container = docker.createContainerCmd("busybox")
   .withCmd("touch", "/test").exec();
        docker.startContainerCmd(container.getId()).exec();
        return null;
   
    }

    @Override
    public boolean deployProcess(DeploymentProfile request) throws Exception {
        // get a context with docker that offers the portable ComputeService api

// release resources
        return true;
    }

    @Override
    public RemoteDockerHostBackend getBackendConfig() throws Exception {
        return ((RemoteDockerHostBackend) WPSConfig.getInstance().getConfigurationManager().getConfigurationServices().getConfigurationModule(
                RemoteDockerHostBackend.class.getName()));

    }

    public DockerClient getDocker() {
        return docker;
    }

    public void setDocker(DockerClient docker) {
        this.docker = docker;
    }
}
