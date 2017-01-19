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

public class MQScriptHelper {

    public static MQScriptHelper newInstance(Properties props) {
        File scriptFile = new File(props['scriptFile']);
        File cmdDir = new File(props['commandDir']);
        String qMgrName = props['queueManagerName'];
        boolean isBatch = Boolean.valueOf(props['isBatch']);
        boolean deleteOnExe = Boolean.valueOf(props['deleteOnExecute'])
        return new MQScriptHelper(scriptFile, cmdDir, qMgrName, isBatch, deleteOnExe);
    }

    def workDir = new File(".").canonicalFile
    final def cmdHelper = new CommandHelper(workDir)
    def out = System.out
    File scriptFile;
    File commandFile;
    boolean isBatch;
    boolean deleteOnExecute
    String queueManager;

    volatile StringBuilder curCommand = null;

    public MQScriptHelper(File scriptFile, File commandDir, String queueManager,boolean isBatch, boolean deleteOnExe) {
        this.isBatch = isBatch;
        this.scriptFile = scriptFile;
        if (!this.isBatch && scriptFile.exists()) {
            throw new IllegalStateException("Not in batch mode and script already exists at location : " + scriptFile.absolutePath);
        }
        this.deleteOnExecute = deleteOnExe;
        this.queueManager = queueManager;
        if (!commandDir.isDirectory()) {
            throw new IllegalArgumentException("Specified command directory is not a directory, does not exist, or is not readable : " + commandDir.absolutePath);
        }

        if (new File(commandDir, "runmqsc").isFile()) {
            commandFile = new File(commandDir, "runmqsc");
        }
        else if (new File(commandDir, "runmqsc.exe").isFile()) {
            commandFile = new File(commandDir, "runmqsc.exe");
        }
        else if (new File(commandDir, "runmqsc.sh").isFile()) {
            commandFile = new File(commandDir, "runmqsc.sh");
        }
        else {
            throw new IllegalArgumentException("Specified command directory does not seem to contain runmqsc executable : " + commandDir.absolutePath);
        }
    }


    private void checkNotStarted(String errMessage) {
        if (curCommand != null) {
            throw new IllegalStateException(errMessage);
        }
    }

    private void checkStarted(String errMessage) {
        if (curCommand == null) {
            throw new IllegalStateException(errMessage);
        }
    }

    synchronized public void startCommand(String commandStart) {
        checkNotStarted("Cannot start another command until the current is written");
        curCommand = new StringBuilder().append(commandStart.trim());
    }

    synchronized public void addSingletonElementToCommand(String element) {
        checkStarted("Cannot add element before starting command.");
        curCommand.append(" ").append(element.trim());
    }

    synchronized public void addKeyValueToCommand(String key, String value) {
        checkStarted("Cannot add key value before starting command.");
        value = escapeSingleQuotes(value);
        curCommand.append(" ").append(key.trim()).append("('").append(value?.trim()?value.trim():"").append("')");
    }

    synchronized public void addUnquotedKeyValueToCommand(String key, String value) {
        checkStarted("Cannot add key value before starting command.");
        curCommand.append(" ").append(key.trim()).append("(").append(value?.trim()?value.trim():"").append(")");
    }

    synchronized public void writeCommand() {
        checkStarted("Cannot write command before starting command.");
        curCommand.append("\n");
        scriptFile.append(curCommand.toString());
        curCommand = null;
    }

    synchronized public void execute() {
        if (!isBatch) {
            executeNow();
        }
    }

    synchronized public void executeNow() {
        executeNow(null);
    }

    synchronized public void executeNow(Closure clos) {
        try {
            checkNotStarted("Cannot execute until the current command is written");
            def cmdArgs = [commandFile.absolutePath, queueManager]
            cmdHelper.runCommand("Executing final mqsc script " +scriptFile.absolutePath, cmdArgs) { proc ->
                proc.withWriter{it << scriptFile.text};
                if (clos == null) {
                    proc.consumeProcessOutput(out, out);
                }
                else {
                    clos(proc);
                }
            }

       }
       finally {
           try {
               if (deleteOnExecute) {
                   scriptFile.delete();
               }
           }
           catch (Throwable e) {
               System.out.println("Error deleting script file!");
           }
       }
    }

    private String escapeSingleQuotes(String input) {
        if (!input) {
            input = "";
        }
        String result = input;
        if (input && input.contains("\'")) {
            // Confirm even number of single quotes, simple check
            // Note: Will still continue if values == 'a'''
            if (((input.length() - input.replace("\'", "").length()) % 2) != 0) {
                throw new RuntimeException("[Error] Uneven number of single quotes for (${input}) value.");
            }

            result = input.replaceAll("\'\'", "'").replaceAll("\'", "''");

            if (input != result) {
                println "[Warning] Made dynamic updates to the value (${input}) to escape single quotes.";
            }
        }
        return result;
    }
}
