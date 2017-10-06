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
package int_.esa.mep.wps.algorithm;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.n52.wps.io.data.IData;
import org.n52.wps.io.data.binding.literal.LiteralBooleanBinding;
import org.n52.wps.io.data.binding.literal.LiteralStringBinding;
import org.n52.wps.server.AbstractSelfDescribingAlgorithm;

/**
 *
 * @author cnl
 */
public class DecrementConversion extends AbstractSelfDescribingAlgorithm {

    @Override
    public List<String> getInputIdentifiers() {
        List<String> identifierList = new ArrayList<String>();
        identifierList.add("CollectionId");
        identifierList.add("SourceStoreType");
        identifierList.add("SourceStoreRoot");
        identifierList.add("TargetStoreType");
        identifierList.add("TargetStoreRoot");
        identifierList.add("Inputs");
        identifierList.add("Outputs");
        return identifierList;
    }

    @Override
    public List<String> getOutputIdentifiers() {
        List<String> identifierList = new ArrayList<String>();
        identifierList.add("Success");
        return identifierList;
    }

    @Override
    public Map<String, IData> run(Map<String, List<IData>> inputData) {
        String inputFile = ((LiteralStringBinding) inputData.get("Inputs").get(0)).getPayload();
        String outputFile = ((LiteralStringBinding) inputData.get("Outputs").get(
                0)).getPayload();
        System.out.println(
                "MNG: dummyConvert (inputFile = " + inputFile + ", outputFile = " + outputFile + ")");
        File file = new File(outputFile);
        if (!file.exists()) {
            try {
                file.createNewFile();
            } catch (IOException ex) {
                Logger.getLogger(DecrementConversion.class.getName()).log(
                        Level.SEVERE,
                        null, ex);
                return null;
            }
        }
        /*
         change permission to 777 for all the users
         */
        file.setExecutable(true, false);
        file.setReadable(true, false);
        file.setWritable(true, false);
        try {
            BufferedReader reader = new BufferedReader(new FileReader(inputFile));
            BufferedWriter writer = new BufferedWriter(new FileWriter(outputFile));
            String input = reader.readLine();
            System.out.println("MNG: input = " + input);

            while (input != null && input.trim().length() > 0) {
                File checkInputFile = new File(input);

                if (!checkInputFile.exists()) {
                    throw new IOException(
                            "The store file " + input + " does not exist.");
                }

                Path inputPath = Paths.get(input);
                String fileName = inputPath.getFileName().toString();
                System.out.println("MNG: fileName = " + fileName);

                String parentPath = inputPath.getParent().toString();
                System.out.println("MNG: parentPath = " + parentPath);

                String output = parentPath + "/converted_" + fileName;
                System.out.println("MNG: output = " + output);

                if (input.endsWith(".txt")) {
                    System.out.println(
                            "MNG: This is a text file. Replace 5555 by 5554");
                    try (BufferedReader dataReader = new BufferedReader(
                            new FileReader(input));
                            BufferedWriter dataWriter = new BufferedWriter(
                                    new FileWriter(output));) {
                        String line = dataReader.readLine();
                        System.out.println("Line = " + line);

                        /*Pattern p = Pattern.compile("-?\\d+");
                         Matcher m = p.matcher(line);
                         while (m.find()) {
                         System.out.println(m.group());
                         }*/
                        line = line.replaceFirst("5555", "5554");

                        while (line != null && line.trim().length() > 0) {
                            dataWriter.write(line);
                            dataWriter.newLine();
                            line = dataReader.readLine();
                        }
                    }
                } else {
                    System.out.println(
                            "MNG: This is not a text file. Duplicate it.");
                    Files.copy(inputPath, Paths.get(output),
                            StandardCopyOption.REPLACE_EXISTING);
                }

                writer.write(output);
                writer.newLine();

                input = reader.readLine();

                System.out.println("MNG: input = " + input);
            }
        } catch (IOException ex) {
            Logger.getLogger(DecrementConversion.class.getName()).log(
                    Level.SEVERE,
                    null, ex);
            return null;
        }
        HashMap<String, IData> results = new HashMap<String, IData>();
        results.put("Success", new LiteralBooleanBinding(Boolean.TRUE));

        return results;
    }

    @Override
    public Class<?> getInputDataType(String id) {

        return LiteralStringBinding.class;
    }

    @Override
    public Class<?> getOutputDataType(String id) {
        return LiteralBooleanBinding.class;
    }

}
