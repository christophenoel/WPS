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
package org.n52.wps.server.transactional.response;

import java.io.InputStream;
import net.opengis.wps.x20.DeployProcessResponseDocument;
import org.n52.wps.server.ExceptionReport;
import org.n52.wps.server.request.Request;
import org.n52.wps.server.response.ExecuteResponse;
import org.n52.wps.server.response.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author cnl
 */
public class DeployProcessResponse extends Response {

    private static Logger LOGGER = LoggerFactory.getLogger(ExecuteResponse.class);
    private boolean success;

    public DeployProcessResponse(Request request) {
        super(request);
    }

    public InputStream getAsStream() throws ExceptionReport {
        try {
            //TODO change to Request.getMapValue
            DeployProcessResponseDocument response = DeployProcessResponseDocument.Factory.newInstance();
            response.setDeployProcessResponse( DeployProcessResponseDocument.DeployProcessResponse.Factory.newInstance());
            
            response.getDeployProcessResponse().setResult(DeployProcessResponseDocument.DeployProcessResponse.Result.Factory.newInstance());
            response.getDeployProcessResponse().getResult().setSuccess(this.success);
            return response.newInputStream();

        } catch (Exception e) {
            throw new ExceptionReport(
                    "Exception occured while writing response document",
                    ExceptionReport.NO_APPLICABLE_CODE, e);

        }

    }

    public void setSuccess(boolean b) {
        this.success=b;
    }

    public boolean isSuccess() {
        return success;
    }

}
