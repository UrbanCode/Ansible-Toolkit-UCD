/*
* Licensed Materials - Property of IBM Corp.
* IBM UrbanCode Build
* IBM UrbanCode Deploy
* IBM UrbanCode Release
* IBM AnthillPro
* (c) Copyright IBM Corporation 2002, 2013. All Rights Reserved.
*
* U.S. Government Users Restricted Rights - Use, duplication or disclosure restricted by
* GSA ADP Schedule Contract with IBM Corp.
*/
package com.urbancode.air.plugin.webspheremq.helper;
import com.urbancode.air.AirPluginTool;
import com.urbancode.air.CommandHelper;

public class MQCertHelper {

    public static MQCertHelper newInstance(Properties props) {
        File cmdDir = new File(props['commandDir']);
       
        return new MQCertHelper(props, cmdDir);
    }

    Properties props;
    def workDir = new File(".").canonicalFile
    final def cmdHelper = new CommandHelper(workDir)
    def out = System.out
    File akmFile;
    File ckmFile;
    
    public MQCertHelper(Properties props, File commandDir) {
        this.props = props;
        if (!commandDir.isDirectory()) {
            throw new IllegalArgumentException("Specified command directory is not a directory, does not exist, or is not readable : " + commandDir.absolutePath);
        }

        if (new File(commandDir, "runmqakm").isFile()) {
            akmFile = new File(commandDir, "runmqakm");
            ckmFile = new File(commandDir, "runmqckm");
        }
        else if (new File(commandDir, "runmqakm.exe").isFile()) {
            akmFile = new File(commandDir, "runmqakm.exe");
            ckmFile = new File(commandDir, "runmqckm.exe");
        }
        else if (new File(commandDir, "runmqakm.sh").isFile()) {
            akmFile = new File(commandDir, "runmqakm.sh");
            ckmFile = new File(commandDir, "runmqckm.sh");
        }
        else {
            throw new IllegalArgumentException("Specified command directory does not seem to contain runmqakm executable : " + commandDir.absolutePath);
        }
    }

    public void receiveCert() {
        boolean deleteIfExists = Boolean.valueOf(props['deleteIfExists']);
        boolean failIfExists = Boolean.valueOf(props['failIfExists']);
        String srcLoc = props['srcLoc'];
        String srcType = props['srcType'];
        String targetType = props['targetType'];
        String targetLoc = props['targetLoc']
        String targetPW = props['targetPW'];
        String newLabel = props['newLabel']
        if (deleteIfExists && certInTarget(targetLoc, targetPW, targetType, newLabel)) {
            _deleteCert(targetLoc, targetPW, targetType, newLabel);
        }
        else if (failIfExists && certInTarget(targetLoc, targetPW, targetType, newLabel)) {
            throw new RuntimeException("Cert with label ${newLabel} already exists in target location.");
        }
        _receiveCert(srcLoc, srcType, targetLoc, targetPW, targetType, newLabel);
    }

    public void importCert() {
        boolean deleteIfExists = Boolean.valueOf(props['deleteIfExists']);
        boolean failIfExists = Boolean.valueOf(props['failIfExists']);
        String srcLoc = props['srcLoc'];
        String srcPW = props['srcPW'];
        String srcType = props['srcType']
        String targetType = props['targetType'];
        String targetLoc = props['targetLoc']
        String targetPW = props['targetPW'];
        String label = props['label']
        String newLabel = props['newLabel']

        if (!(newLabel?.trim())) { 
            newLabel = label;
        }

        if (deleteIfExists && certInTarget(targetLoc, targetPW, targetType, newLabel)) {
            _deleteCert(targetLoc, targetPW, targetType, newLabel);
        }
        else if (failIfExists && certInTarget(targetLoc, targetPW, targetType, newLabel)) {
            throw new RuntimeException("Cert with label ${newLabel} already exists in target location.");
        }
        _importCert(srcLoc, srcPW, srcType, label, targetLoc, targetPW, targetType, newLabel);
    }

    public void deleteCert() {
        String targetType = props['targetType'];
        String targetLoc = props['targetLoc']
        String targetPW = props['targetPW'];
        String newLabel = props['newLabel']
        if (certInTarget(targetLoc, targetPW, targetType, newLabel)) {
            _deleteCert(targetLoc, targetPW, targetType, newLabel);
        }
    }

    private void _deleteCert(String targetLoc, String targetPW, String targetType, String newLabel) {
        boolean isJKS = false;
        if (targetType == "jks" || targetType == "jceks") {
            isJKS = true;
        }
        def args = ['-delete', '-db', targetLoc, '-pw', targetPW, '-type', targetType, '-label', newLabel];
        execute("Deleting Certificate", args, isJKS);
    }

    private void _importCert(String srcLoc,
                             String srcPW,
                             String srcType,
                             String label,
                             String targetLoc,
                             String targetPW,
                             String targetType,
                             String newLabel)
    {
        boolean isJKS = false;
        if (targetType == "jks" || targetType == "jceks" || srcType == "jks" || srcType == "jceks") {
            isJKS = true;
        }

        def args = ['-import', '-file', srcLoc, '-pw', srcPW, '-type', srcType];
        args << '-target' << targetLoc << '-target_pw' << targetPW << '-target_type';
        args << targetType << '-label' << label << '-new_label' << newLabel;
        execute("Importing Certificate", args, isJKS);
    }

    private void _receiveCert(String srcLoc, 
                              String srcType, 
                              String targetLoc, 
                              String targetPW, 
                              String targetType, 
                              String label) 
    {
        boolean isJKS = false;
        if (targetType == "jks" || targetType == "jceks") {
            isJKS = true;
        }
        def args = ['-receive', '-file', srcLoc, '-format', srcType, '-db', targetLoc]
        args << '-pw' << targetPW << '-type' << targetType;
        execute("Recieving Certificate", args, isJKS);
    }

    private boolean certInTarget(String targetLoc, String targetPW, String targetType, String label) {
        boolean result = true;
        boolean isJKS = false;
        if (targetType == "jks" || targetType == "jceks") {
            isJKS = true;
        }
        def args = ['-details', '-db', targetLoc, '-pw', targetPW, '-type', targetType, '-label', label];
        try {
            execute("Checking for certificate", args, isJKS) { proc ->
                proc.getOutputStream().close(); //close proc std in
                proc.consumeProcessErrorStream(out);
                BufferedReader reader = new BufferedReader(new InputStreamReader(proc.getIn()));
                String line;
                while((line = reader.readLine()) != null) {
                      out.println(line);
                     if (line.contains("CTGSK3029W") || line.contains("The database doesn't contain an entry with label")) {
                         result = false;
                     }
                }
                proc.waitForProcessOutput(System.out, System.out);
            }
        }
        catch (com.urbancode.air.ExitCodeException e) {
            if (!result) {
                //the process failed because the key couldn't be found ignore
            }
            else {
                //process failed for unknown reason, rethrow
                throw e;
            }
        }
        return result;
    }
    
    private void execute(String message, List args, boolean isJKS) {
        execute(message, args, isJKS,  null);
    }

    private void execute(String message, List args, boolean isJKS, Closure clos) {
        def cmdArgs = []
        if (isJKS) {
            cmdArgs << ckmFile.absolutePath;
        }
        else {
            cmdArgs << akmFile.absolutePath;
        }
        cmdArgs << '-cert';
        cmdArgs.addAll(args);
        cmdHelper.runCommand(message, cmdArgs, clos);
    }
}
