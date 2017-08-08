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
/** *************************************************************
 * This implementation provides a framework to publish processes to the
 * web through the  OGC Web Processing Service interface. The framework
 * is extensible in terms of processes and data handlers.  *
 * Copyright (C) 2006 by con terra GmbH
 *
 * Authors:
 * Bastian Schaeffer, Institute for Geoinformatics, Muenster, Germany
 *
 * Contact: Albert Remke, con terra GmbH, Martin-Luther-King-Weg 24,
 * 48155 Muenster, Germany, 52n@conterra.de
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * version 2 as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program (see gnu-gpl v2.txt); if not, write to
 * the Free Software Foundation, Inc., 59 Temple Place - Suite 330,
 * Boston, MA  02111-1307, USA or visit the web page of the Free
 * Software Foundation, http://www.fsf.org.
 *
 ************************************************************** */
package org.n52.wps.server.transactional.profiles;


import java.util.logging.Logger;

import net.opengis.wps.x20.DeployProcessDocument;
import net.opengis.wps.x20.ProcessOfferingDocument;


public abstract class DeploymentProfile {

    private static Logger LOGGER = Logger
            .getLogger(DeploymentProfile.class.getName());

    private String processID;
    private ProcessOfferingDocument.ProcessOffering processOffering;
    private String schema;

    public DeploymentProfile(DeployProcessDocument deployDom, String processID) {
        this.processID = processID;
        this.processOffering = deployDom.getDeployProcess().getProcessOffering();
        this.schema = deployDom.getDeployProcess().getDeploymentProfile().getSchema().getReference();
    }

    public String getSchema() {
        return schema;
    }

    public void setSchema(String schema) {
        this.schema = schema;
    }

    public ProcessOfferingDocument.ProcessOffering getProcessOffering() {
        return processOffering;
    }

    public void setProcessOffering(
            ProcessOfferingDocument.ProcessOffering processOffering) {
        this.processOffering = processOffering;
    }

    public String getProcessID() {
        return processID;
    }

}
