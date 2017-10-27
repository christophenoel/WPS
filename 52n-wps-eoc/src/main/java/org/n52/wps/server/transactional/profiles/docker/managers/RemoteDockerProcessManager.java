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

import com.spotify.docker.client.DefaultDockerClient;
import com.spotify.docker.client.DockerCertificates;
import com.spotify.docker.client.DockerClient;
import com.spotify.docker.client.LogStream;
import com.spotify.docker.client.exceptions.DockerCertificateException;
import com.spotify.docker.client.exceptions.DockerException;
import com.spotify.docker.client.messages.ContainerConfig;
import com.spotify.docker.client.messages.ContainerCreation;
import com.spotify.docker.client.messages.ContainerExit;
import com.spotify.docker.client.messages.ContainerInfo;
import com.spotify.docker.client.messages.HostConfig;
import com.spotify.docker.client.messages.HostConfig.Bind;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import javax.xml.namespace.NamespaceContext;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import net.opengis.ows.x20.MetadataType;
import net.opengis.wps.x20.ComplexDataType;
import net.opengis.wps.x20.DataInputType;
import net.opengis.wps.x20.ExecuteDocument;
import net.opengis.wps.x20.InputDescriptionType;
import net.opengis.wps.x20.LiteralDataType;
import net.opengis.wps.x20.LiteralValueDocument;
import net.opengis.wps.x20.OutputDescriptionType;
import net.opengis.wps.x20.ProcessOfferingDocument;
import net.schmizz.sshj.SSHClient;
import net.schmizz.sshj.sftp.SFTPClient;
import net.schmizz.sshj.xfer.FileSystemFile;
import net.schmizz.sshj.xfer.LocalDestFile;
import org.apache.commons.compress.utils.IOUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.filefilter.TrueFileFilter;
import org.apache.commons.lang.StringUtils;
import org.apache.xmlbeans.XmlException;
import org.n52.wps.commons.WPSConfig;
import org.n52.wps.io.data.IData;
import org.n52.wps.io.data.binding.complex.GenericFileDataBinding;
import org.n52.wps.io.datahandler.parser.GenericFileParser;
import org.n52.wps.server.ExceptionReport;
import org.n52.wps.server.request.ExecuteRequest;
import org.n52.wps.server.transactional.manager.AbstractTransactionalProcessManager;
import org.n52.wps.server.transactional.profiles.DeploymentProfile;
import org.n52.wps.server.transactional.util.MimeUtil;
import org.n52.wps.webapp.entities.RemoteDockerHostBackend;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Node;

/**
 *
 * @author cnl
 */
public class RemoteDockerProcessManager extends AbstractTransactionalProcessManager {

    private static Logger log = LoggerFactory
            .getLogger(RemoteDockerProcessManager.class);
    private DockerClient docker;

    // list of environmnent variables to provide to the appplication
    private List<String> env = new ArrayList<String>();
    private List<String> dirToMount = new ArrayList<String>();
    private RemoteDockerHostBackend db = getBackendConfig();

    private ProcessOfferingDocument.ProcessOffering description;
    private String instanceId;
    private boolean mountEOStore;
    private SSHClient ssh;
    private Map<String, String> outputs = new HashMap<>();

    public RemoteDockerProcessManager(String processID) throws Exception {
        super(processID);

        /* note: see doc for connection pooling and authentication to private registries */
        /**
         * old docker-java DockerClientConfig config =
         * DefaultDockerClientConfig.createDefaultConfigBuilder()
         * .withDockerHost(db.getDockerHost()) .withDockerTlsVerify(true)
         * .withDockerCertPath(db.getDockerCertPath())
         * .withDockerConfig(db.getDockerConfig())
         * .withApiVersion(db.getApiVersion()) .withRegistryUrl("")
         * .withRegistryUsername(db.getRegistryUserName())
         * .withRegistryPassword(db.getRegistryPassword())
         * .withRegistryEmail(db.getRegistryEmail()) .build(); this.docker =
         * DockerClientBuilder.getInstance(config).build();
         */
    }

    @Override
    public boolean unDeployProcess(String processID) throws Exception {
        return true;
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
    public Map<String, IData> invoke(Map<String, List<IData>> inputData, String processId, ProcessOfferingDocument.ProcessOffering description, ExecuteRequest request) throws ExceptionReport {
        ExecuteDocument execDoc = (ExecuteDocument) request.getDocument();
        env = new ArrayList<String>();
        dirToMount = new ArrayList<String>();
        outputs = new HashMap<>();
        log.debug("Starting invoke in RemoteDocker manager");
        this.instanceId = request.getUniqueId().toString();
        this.description = description;
        try {
        this.ssh = DockerUtil.getSSHClient(db.getSshhost(), db.getUser(),
                db.getPassword());
        }
        catch(Exception e) {
            throw new ExceptionReport(
                                "Error when connecting to SSH remote Docker host",
                                ExceptionReport.REMOTE_COMPUTATION_ERROR,e);
        }

        log.debug("Parse docker image");
        String dockerImageReference;
        try {
            dockerImageReference = parseDockerImage();
        } catch (XPathExpressionException ex) {
             throw new ExceptionReport(
                                "Error when parsing DockerImage metadata",
                                ExceptionReport.REMOTE_COMPUTATION_ERROR,ex);
        } catch (XmlException ex) {
            throw new ExceptionReport(
                                "Error when parsing DockerImage metadata",
                                ExceptionReport.REMOTE_COMPUTATION_ERROR,ex);
        }
        try {
            docker = getDockerConnection();
        } catch (DockerCertificateException ex) {
            throw new ExceptionReport(
                                "Certificate error when connecting to Docker engine",
                                ExceptionReport.REMOTE_COMPUTATION_ERROR,ex);
        }
        // get the ssh client connected to the host with NFS access
        log.debug("Getting SSH connection to the host connected to NFS stores");

        /**
         * TODO parsing of inputs , mouting of volumes, collecting of results*
         */
        /**
         * Inputs handling supports 3 types of inputs: simple parameters
         * (literal data), files to copy in the shared directory, fileReferences
         * (local NFS reference of files)
         */
        log.debug("Handling Inputs");
        try {
            // Handling inputs means prepare the environmnent variable and stores input file on configured NFS store
            handleInputs(execDoc, inputData, description);
        } catch (IOException ex) {
             throw new ExceptionReport(
                                "Error when handling inputs",
                                ExceptionReport.REMOTE_COMPUTATION_ERROR,ex);
        }
        // Handling outputs consists of providing the output file locations in environmnent variables
        log.debug("Handling Outputs");
        try {
            handleOutputs(description); // TODO CHANGE
        } catch (IOException ex) {
            throw new ExceptionReport(
                                "Error when handling outputs",
                                ExceptionReport.REMOTE_COMPUTATION_ERROR,ex);
        }

        // Get the image URL
        String imageName = dockerImageReference;
        log.debug("pulling image:" + imageName);
        try {
            docker.pull(imageName);
            //   ImageInfo infoTest = docker.inspectImage("test");
        } catch (DockerException ex) {
           throw new ExceptionReport(
                                "Error when pulling image",
                                ExceptionReport.REMOTE_COMPUTATION_ERROR,ex);
        } catch (InterruptedException ex) {
            throw new ExceptionReport(
                                "Error interrupted exception",
                                ExceptionReport.REMOTE_COMPUTATION_ERROR,ex);
        }

        try {
            writeEnvPropertyFile(env);
        } catch (IOException ex) {
            throw new ExceptionReport(
                                "Error writing environmnent variables",
                                ExceptionReport.REMOTE_COMPUTATION_ERROR,ex);
        }
        log.debug("Env variables passed to the containers");
        ContainerConfig.Builder build = ContainerConfig.builder().image(
                imageName).env(env);
        // Add all EO NFS directories to mount

        HostConfig.Builder hostConfigBuild = HostConfig.builder();

        for (String d : dirToMount) {
            log.debug("adding mounted directory:" + d);
            hostConfigBuild.appendBinds(Bind.from(d)
                    .to(d)
                    .readOnly(false).build());

        }
        // Mount WPS NFS directory (with inputDir/outputDir)
        hostConfigBuild.appendBinds(Bind.from(db.getNfsWPSPath())
                .to(db.getNfsWPSPath())
                .readOnly(false).build());

        // DEMO hack TODO remove
        hostConfigBuild.appendBinds(Bind.from("/data/landcover")
                .to("/home/worker/workDir")
                .readOnly(false).build());
        final HostConfig hostConfig = hostConfigBuild.build();
        // attach stdout and stderr
        build.attachStdout(true);
        build.attachStderr(true);
        build.hostConfig(hostConfig);
        // Not sure auto remove is already supported!
        hostConfig.autoRemove();

        // create container request
        ContainerConfig container = build.build();

        // create container
        ContainerCreation creation;
        try {
            creation = docker.createContainer(container);
        } catch (DockerException ex) {
            throw new ExceptionReport(
                                "Error when creating Docker container",
                                ExceptionReport.REMOTE_COMPUTATION_ERROR,ex);
        } catch (InterruptedException ex) {
            throw new ExceptionReport(
                                "Error interrupted exception",
                                ExceptionReport.REMOTE_COMPUTATION_ERROR,ex);
        }

        // get container id
        String id = creation.id();
        // logs declarations
        String stdOutLog;
        String stdErrLog;
        try {
            // start container
            docker.startContainer(id);
        } catch (DockerException ex) {
           throw new ExceptionReport(
                                "Error when starting docker container",
                                ExceptionReport.REMOTE_COMPUTATION_ERROR,ex);
        } catch (InterruptedException ex) {
            throw new ExceptionReport(
                                "Error interupted exception when starting container",
                                ExceptionReport.REMOTE_COMPUTATION_ERROR,ex);
        }

        ContainerInfo info;
        try {
            info = docker.inspectContainer(id);
        } catch (DockerException ex) {
           throw new ExceptionReport(
                                "Error when starting inspectContainer",
                                ExceptionReport.REMOTE_COMPUTATION_ERROR,ex);
        } catch (InterruptedException ex) {
            throw new ExceptionReport(
                                "Error interupted exception when inspectContainer",
                                ExceptionReport.REMOTE_COMPUTATION_ERROR,ex);
        }
        log.debug("Current status: " + info.state().status());

        //DockerUtil.logAndWait(docker, id);
        final ContainerExit exit;
        try {
            exit = docker.waitContainer(id);
        } catch (DockerException ex) {
           throw new ExceptionReport(
                                "Error when waiting for Docker container",
                                ExceptionReport.REMOTE_COMPUTATION_ERROR,ex);
        } catch (InterruptedException ex) {
           throw new ExceptionReport(
                                "Error when waiting for container interrupted exception",
                                ExceptionReport.REMOTE_COMPUTATION_ERROR,ex);
        }
        LogStream stream;
        LogStream streamErr;
        
        try {
            stream = docker.logs(id, DockerClient.LogsParam.stdout());
            streamErr = docker.logs(id, DockerClient.LogsParam.stderr());
        } catch (DockerException ex) {
            throw new ExceptionReport(
                                "Error when logging docker",
                                ExceptionReport.REMOTE_COMPUTATION_ERROR,ex);

        } catch (InterruptedException ex) {
            throw new ExceptionReport(
                                "Error when logging docker interrupted exception",
                                ExceptionReport.REMOTE_COMPUTATION_ERROR,ex);

        }
        String logOut = stream.readFully();
        String logErr = streamErr.readFully();
        log.debug(logOut);
        log.debug(logErr);
        log.debug("DOCKER Exit code is "+exit.statusCode());
        
        if(exit.statusCode()!=0) {
            throw new ExceptionReport(
                                "Docker Image execution error",
                                ExceptionReport.REMOTE_COMPUTATION_ERROR,logErr);
        }
        try {
            //ContainerConfig.builder().image(imageName).cmd("sh", "-c", "while
            /**
             * ServiceCreateResponse serviceResponse = docker.createService(null);
             * Service service = docker.inspectService(serviceResponse.id());
             * service.spec().
             *
             */
            //   CreateContainerResponse container = docker.createContainerCmd("busybox")
            //      .withCmd("touch", "/test").exec();
            //        docker.startContainerCmd(container.getId()).exec();
            // Remove container
            docker.removeContainer(id);
        } catch (DockerException ex) {
            throw new ExceptionReport(
                                "Error when removing container",
                                ExceptionReport.REMOTE_COMPUTATION_ERROR,ex);

        } catch (InterruptedException ex) {
            throw new ExceptionReport(
                                "Error when removing container interrupted exception ",
                                ExceptionReport.REMOTE_COMPUTATION_ERROR,ex);
        }

        HashMap<String, IData> resultHash = new HashMap<String, IData>();
        for (String k
                : outputs.keySet()) {
            String outputPath = outputs.get(k);
            String extension = "";
            // define file Path
            ComplexDataType complexData = (ComplexDataType) DockerUtil.getOutputDesc(
                    k, description).getDataDescription();
            if (complexData.getFormatArray(0) != null && complexData.getFormatArray(
                    0).getMimeType() != null) {
                extension = "." + MimeUtil.getExtension(
                        complexData.getFormatArray(0).getMimeType());
            }
            String tempPath = Paths.get(System.getProperty("java.io.tmpdir"),
                    k + this.instanceId).toString() + extension;
            SFTPClient ftpClient;
            try {
                ftpClient = ssh.newSFTPClient();
            } catch (IOException ex) {
                throw new ExceptionReport(
                                "Error when creating SSH client for remote Docker host",
                                ExceptionReport.REMOTE_COMPUTATION_ERROR,ex);
            }
            LocalDestFile tempLocalFile = new FileSystemFile(tempPath);
            try {
                ftpClient.get(outputPath, tempLocalFile);
            } catch (IOException ex) {
                throw new ExceptionReport(
                                "Error when retrieving by SSH output "+outputPath,
                                ExceptionReport.REMOTE_COMPUTATION_ERROR,ex);
            }

            File temp = new File(tempPath);
            GenericFileParser parser = new GenericFileParser();
            GenericFileDataBinding parsedOutput;
            try {
                parsedOutput = parser.parse(
                        FileUtils.openInputStream(temp), DockerUtil.getOutputDesc(k,
                                description).getDataDescription().getFormatArray()[0].getMimeType(),
                        null);
            } catch (IOException ex) {
               throw new ExceptionReport(
                                "Error when reading output temp file "+temp,
                                ExceptionReport.REMOTE_COMPUTATION_ERROR,ex);
            }
            log.debug("Putting output " + k);
            resultHash.put(k, parsedOutput);
        }

        //resultHash.put(key,OutputParser.handleLiteralValue(ioElement));
        //resultHash.put(key, OutputParser.handleComplexValue(			ioElement, getDescription()));
        log.debug(
                "Returning debug");
        return resultHash;

    }

 

    @Override
    public boolean deployProcess(DeploymentProfile request) throws Exception {
        // get a context with docker that offers the portable ComputeService api

// release resources
        return true;
    }

    @Override
    public RemoteDockerHostBackend
            getBackendConfig() throws Exception {
        return ((RemoteDockerHostBackend) WPSConfig.getInstance().getConfigurationManager().getConfigurationServices().getConfigurationModule(
                RemoteDockerHostBackend.class
                        .getName()));

    }

    public DockerClient getDocker() {
        return docker;
    }

    /**
     * Read the execute request and process description in order to classify
     * inputs and parse the values
     *
     * @param execute
     * @param description
     * @param db
     * @throws ExceptionReport
     */
    private void handleInputs(ExecuteDocument execute,Map<String, List<IData>> inputData,
            ProcessOfferingDocument.ProcessOffering description) throws ExceptionReport, IOException {
        HashMap<String, byte[]> files = new HashMap<String, byte[]>();
        HashMap<String, byte[]> zipfiles = new HashMap<String, byte[]>();
        for (DataInputType input : execute.getExecute().getInputArray()) {
            String id = input.getId();
            log.debug("handling input:" + id);
            InputDescriptionType inputDesc = DockerUtil.getInputDesc(id,
                    description);
            if (inputDesc.getDataDescription() instanceof ComplexDataType) {
                log.debug("complex data");
                ComplexDataType complexData = (ComplexDataType) inputDesc.getDataDescription();
                if (input.isSetReference()) {
                    log.debug("input by reference");
                    // reference File
                    // Check if starting with prefix
                    String ref = input.getReference().getHref();
                    if (ref.startsWith(db.getEODataConversionPrefix())) {
                        log.debug(
                                "Reference starts with the configured prefix " + db.getEODataConversionPrefix());
                        //Hack for Spacebel
                        log.debug("hack for spacebel?");
                        if (ref.startsWith("file:///nas-data")) {
                            log.debug("adding SAFE directory");
                            String productid = StringUtils.substringAfterLast(
                                    ref, "/");
                            ref = ref.concat("/").concat(productid).concat(
                                    ".SAFE");
                        }
                        String convertedRef = ref.replaceFirst(
                                db.getEODataConversionPrefix(),
                                db.getNfsEODataPath());

                        log.debug("Converted reference to " + convertedRef);
                        // indicate the EO store must be mounted to container for execution
                        dirToMount.add(convertedRef);
                        // add location to the environmnent variables list
                        env.add("WPS_INPUT_" + id + "=" + convertedRef);
                    } else {
                        log.debug("input not local reference");
                        //simple reference file
                        String extension = "";
                        String pe = StringUtils.substringAfterLast(ref, ".");
                        if (pe != null && pe.length() < 5 && ref.endsWith(
                                pe)) {
                            extension = pe;
                        }
                        // TODO get Value from inputHandler
                        log.debug("extension is " + extension);
                        URL refURL = new URL(ref);
                        byte[] binary = IOUtils.toByteArray(refURL.openStream());
                        String mimeType="";
                        if(input.getReference().getMimeType()!=null) {
                            mimeType = input.getReference().getMimeType();
                        }
                        log.debug("Found mimeType " + mimeType);
                        if (extension.equalsIgnoreCase("zip") || mimeType.equalsIgnoreCase(
                                "application/zip") || mimeType.contains(
                                        "zip")) {
                            log.debug("added to zipfiles");
                            zipfiles.put(id, binary);
                        } else {
                            log.debug("added to files");
                            files.put(id, binary);
                        }
                    }
                } else if (input.isSetData()) {
                    log.debug("input by data");
                    // TODO parse this non base64 if encoding normal
                    byte[] binary = Base64.getDecoder().decode(
                            input.getData().getDomNode().getFirstChild().getNodeValue());
                    String testString = new String(binary);
                    //log.debug("decoded:" + testString);
                    log.debug("putting file " + id);
                     String mimeType="";
                        if(input.getData().getMimeType()!=null) {
                            mimeType = input.getData().getMimeType();
                        }
                    if (mimeType.equalsIgnoreCase(
                            "application/zip") || mimeType.contains(
                                    "zip")) {
                        log.debug("added to zipfiles");
                        zipfiles.put(id, binary);
                    } else {
                        log.debug("added to files");
                        files.put(id, binary);
                    }
                } else {
                    if (inputDesc.getMinOccurs().intValue() >= 1) {
                        throw new ExceptionReport(
                                id + " input is missing (mandatory)",
                                ExceptionReport.INVALID_PARAMETER_VALUE);
                    }
                }
            } else if (inputDesc.getDataDescription() instanceof LiteralDataType) {
                LiteralDataType literalData = (LiteralDataType) inputDesc.getDataDescription();
                String value = null;
                // Try parsing literalValue
                try {
                    LiteralValueDocument literalValue = LiteralValueDocument.Factory.parse(
                            input.getData().getDomNode().getFirstChild());
                    value = literalValue.getLiteralValue().getStringValue();
                    // Else this is plain/text
                } catch (Exception literalEx) {
                    value = input.getData().getDomNode().getFirstChild().getNodeValue();
                }
                // add to the environmnent variable
                env.add("WPS_INPUT_" + id + "=" + value);
            } else {
                throw new ExceptionReport(
                        id + " input is neither ComplexData nor LiteralData!",
                        ExceptionReport.INVALID_PARAMETER_VALUE);
            }
        }
        // write the input files to NFS
        log.debug("write input files to NFS store");
        writeFilesToNFS(ssh, files, zipfiles,
                getInputDirectory(this.getProcessID(), instanceId.toString())
        );
    }

    /**
     * Handle Outputs
     *
     * @param execute
     * @param description
     * @param db
     * @throws ExceptionReport
     * @throws IOException
     */
    private void handleOutputs(
            ProcessOfferingDocument.ProcessOffering description
    ) throws ExceptionReport, IOException {
        for ( OutputDescriptionType output : description.getProcess().getOutputArray()) {
            String id = output.getIdentifier().getStringValue();
            log.debug("id:" + id);
            OutputDescriptionType outputDesc = DockerUtil.getOutputDesc(id,
                    description);
            log.debug("output desc " + outputDesc.getIdentifier());
            if (outputDesc.getDataDescription() instanceof ComplexDataType) {
                log.debug("getting data description");
                ComplexDataType complexData = (ComplexDataType) outputDesc.getDataDescription();
                String pathDir = FilenameUtils.separatorsToUnix(Paths.get(
                        getOutputDirectory(processID, instanceId).toString()).toString());
                String extension = "";
                if (complexData.getFormatArray(0) != null && complexData.getFormatArray(
                        0).getMimeType() != null) {
                    extension = "." + MimeUtil.getExtension(
                            complexData.getFormatArray(0).getMimeType());
                }
                String path = FilenameUtils.separatorsToUnix(Paths.get(
                        getOutputDirectory(processID, instanceId).toString(), id).toString()) + extension;
                SFTPClient ftpClient = ssh.newSFTPClient();
                ftpClient.mkdirs(pathDir);
                env.add("WPS_OUTPUT_" + id + "=" + path);
                outputs.put(id, path);

            }
        }
    }

    /**
     * Write input files into the NFS directory
     *
     * @param ssh
     * @param files
     * @param path
     * @throws IOException
     */
    private void writeFilesToNFS(SSHClient ssh, HashMap<String, byte[]> files,
            HashMap<String, byte[]> zipfiles,
            Path path) throws IOException {
        for (String i : zipfiles.keySet()) {
            log.debug("handling file store of ZIP " + i);
            byte[] fileBytes = zipfiles.get(i);
            InputDescriptionType inputDesc = DockerUtil.getInputDesc(i,
                    description);
            // TODO implement if image location is overriden in description
            // create TMP file
            File temp = File.createTempFile("wps_inp", ".tmp");
            // write bytes in temp file
            FileUtils.writeByteArrayToFile(temp, fileBytes);
            Path tempDir = Files.createTempDirectory("wps_zip");
            //tempDirFile = new File(tempDir)
            DockerUtil.unZipIt(temp.getAbsolutePath(), tempDir.toString());
            // write bytes in temp file
            String targetDirLocation = FilenameUtils.separatorsToUnix(Paths.get(
                    getInputDirectory(processID,
                            instanceId).toString()).toString());
            String targetFileLocation = FilenameUtils.separatorsToUnix(
                    Paths.get(getInputDirectory(processID,
                            instanceId).toString(), i).toString());
            SFTPClient ftpClient = ssh.newSFTPClient();
            ftpClient.mkdirs(targetFileLocation);

            for (File f : FileUtils.listFiles(new File(tempDir.toString()),
                    TrueFileFilter.INSTANCE,
                    TrueFileFilter.INSTANCE)) {
                log.debug("writing " + f.getAbsolutePath());
                ftpClient.put(f.getAbsolutePath(),
                        targetFileLocation);

            }
            //temp.delete();
            //FileUtils.deleteDirectory(new File(tempDir.toString()));
            // add input location to env variables
            log.debug("adding " + i + " =" + targetFileLocation);
            env.add("WPS_INPUT_" + i + "=" + targetFileLocation);
        }
        for (String i : files.keySet()) {
            log.debug("handling file store of non-zip" + i);
            byte[] fileBytes = files.get(i);
            InputDescriptionType inputDesc = DockerUtil.getInputDesc(i,
                    description);
            // TODO implement if image location is overriden in description
            // create TMP file
            File temp = File.createTempFile("wps_inp", ".tmp");
            // write bytes in temp file
            FileUtils.writeByteArrayToFile(temp, fileBytes);
            String targetDirLocation = FilenameUtils.separatorsToUnix(Paths.get(
                    getInputDirectory(processID,
                            instanceId).toString()).toString());
            String targetFileLocation = FilenameUtils.separatorsToUnix(
                    Paths.get(getInputDirectory(processID,
                            instanceId).toString(), i).toString());
            SFTPClient ftpClient = ssh.newSFTPClient();
            ftpClient.mkdirs(targetDirLocation);
            ftpClient.put(temp.getAbsolutePath(),
                    targetFileLocation);
            temp.delete();
            // add input location to env variables
            log.debug("adding " + i + " =" + targetFileLocation);
            env.add("WPS_INPUT_" + i + "=" + targetFileLocation);
        }

        // ssh.disconnect();
    }

    /**
     *  // String inPath = FilenameUtils.separatorsToUnix( Paths.get(
     * getInputDirectory(processID, instanceId).toString(), id).toString()) +
     * extension; if (extension.equalsIgnoreCase("zip")) { inPath =
     * FilenameUtils.separatorsToUnix( Paths.get( getInputDirectory(processID,
     * instanceId).toString(), id).toString());
     *
     * }
     * File dir = new File(inPath); dir.mkdirs(); FileUtils.copyURLToFile(new
     * URL(ref), inPath);
     */
    /**
     * Return the Working Directory Path on NFS
     *
     * @param processId
     * @param instanceId
     * @return
     */
    private Path getWorkingDirectory(String processId, String instanceId) {
        log.debug("getWorkingdirctory for " + processId + " " + instanceId);
        return Paths.get(db.getNfsWPSPath(), processId, instanceId);
    }

    private Path getInputDirectory(String processId, String instanceId) {
        return Paths.get(getWorkingDirectory(processId, instanceId).toString(),
                db.getInputDir());
    }

    private Path getOutputDirectory(String processId, String instanceId) {
        return Paths.get(getWorkingDirectory(processId, instanceId).toString(),
                db.getOutputDir());
    }

    private DockerClient getDockerConnection() throws DockerCertificateException {
        DockerClient docker = DefaultDockerClient.builder().uri(URI.create(
                db.getDockerURL())).dockerCertificates(new DockerCertificates(
                Paths.get("D:\\vm1"))).build();
        return docker;
    }

    public static void main(String[] args) {
        try {
            MetadataType appContextMetadata = MetadataType.Factory.parse(
                    new File("D:/app.txt"));
            log.debug("Parsing appContext " + appContextMetadata.toString());
            XPath xPath = XPathFactory.newInstance().newXPath();
            NamespaceContext context = DockerUtil.getTB13NamespaceContext();
            log.debug("Context:" + context.getNamespaceURI("eoc"));
            xPath.setNamespaceContext(context);
            Node node = (Node) xPath.evaluate(
                    "//eoc:ApplicationContext/descendant::owc:offering[@code='http://www.opengis.net/tb13/eoc/DockerImage']/ows:AdditionalParameters/ows:AdditionalParameter/ows:Name[text()='eoc.reference']/../ows:Value/text()",
                    appContextMetadata.copy().getDomNode(), XPathConstants.NODE);
            if (node == null) {
                log.warn("null node");
                return;
            }
            log.debug("Node content is :" + node.toString());
            log.debug("Node content is :" + node.getNodeValue());
            log.debug("Node content is :" + node.getTextContent());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private String parseDockerImage() throws XPathExpressionException, XmlException {
        MetadataType appContextMetadata = DockerUtil.getMetadataContentByKey(
                "http://www.opengis.net//tb13/eoc/applicationContext",
                description);
        log.debug("Parsing appContext " + appContextMetadata.toString());
        XPath xPath = XPathFactory.newInstance().newXPath();
        NamespaceContext context = DockerUtil.getTB13NamespaceContext();
        log.debug("Context:" + context.getNamespaceURI("eoc"));
        xPath.setNamespaceContext(context);
        Node node = (Node) xPath.evaluate(
                "//eoc:ApplicationContext/descendant::owc:offering[@code='http://www.opengis.net/tb13/eoc/docker']/owc:content/text()",
                appContextMetadata.copy().getDomNode(), XPathConstants.NODE);
        log.debug("Node content is :" + node.getNodeValue());
        return node.getNodeValue();
    }

    /**
     * Write the environmnent variables property file
     *
     * @param env
     */
    private void writeEnvPropertyFile(List<String> env) throws FileNotFoundException, IOException {
        Properties prop = new Properties();
        OutputStream output = null;
        File temp = File.createTempFile("wps_env", ".tmp");
        log.debug("envDir :" + db.getEnvDir());
        String envDir = FilenameUtils.separatorsToUnix(Paths.get(
                getWorkingDirectory(this.processID, this.instanceId).toString(),
                db.getEnvDir()).toString());
        String envFile = FilenameUtils.separatorsToUnix(Paths.get(
                getWorkingDirectory(this.getProcessID(), this.instanceId).toString(),
                db.getEnvDir(), "env.properties").toString());
        output = new FileOutputStream(temp.getAbsolutePath());
        for (String e : env) {
            log.debug(e);
            String[] split = e.split("=");
            prop.setProperty(split[0], split[1]);
        }
        // Store property file
        prop.store(output, null);
        SFTPClient ftpClient = ssh.newSFTPClient();
        ftpClient.mkdirs(envDir);
        ftpClient.put(temp.getAbsolutePath(),
                envFile);
    }



}
