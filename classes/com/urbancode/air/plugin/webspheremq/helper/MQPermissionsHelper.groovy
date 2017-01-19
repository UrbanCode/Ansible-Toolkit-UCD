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

public class MQPermissionsHelper {

    public static MQPermissionsHelper newInstance(Properties props) {
        File cmdDir = new File(props['commandDir']);
        String objectType = props['objectType'];
        String objectName = props['objectName'];
        String queueMan = props['queueManagerName'];
        List<String> groups = new ArrayList<String>();
        props['groups'].splitEachLine(",") { list ->
            list.each { 
                if (it?.trim()) {
                    groups << it.trim();
                }
            }
        }
        List<String> principals = new ArrayList<String>();
        props['principals'].splitEachLine(",") { list ->
            list.each { 
                if (it?.trim()) {
                    principals << it.trim();
                }
            }
        }
        List<String> authorities = new ArrayList<String>();
        props['authorities'].splitEachLine(",") { list ->
            list.each { 
                if (it?.trim()) {
                    authorities << it.trim();
                }
            }
        }
       
        return new MQPermissionsHelper(cmdDir, queueMan, objectType, objectName, principals, groups, authorities);
    }

    def workDir = new File(".").canonicalFile
    final def cmdHelper = new CommandHelper(workDir)
    def out = System.out
    File commandFile;
    String queueManager;
    String objectType;
    String objectName;
    List<String> authorities = new ArrayList<String>();
    List<String> groups = new ArrayList<String>();
    List<String> principals = new ArrayList<String>();
    
    public MQPermissionsHelper(File commandDir, String queueManager, String objType, String objName, List<String> principals, List<String> curgroups, authorities) {
        this.queueManager = queueManager;
        this.objectType = objType;
        this.objectName = objName;
        this.groups.addAll(curgroups);
        this.principals.addAll(principals);

        authorities.each {
            def val = it?.trim();
            if (val) {
                if (! it.startsWith("+") && ! it.startsWith("-")) {
                    System.out.println("Ignoring malformed authority specification " + val + ".");
                }
                else {
                    this.authorities << val;
                }
            }
        }
        if (!commandDir.isDirectory()) {
            throw new IllegalArgumentException("Specified command directory is not a directory, does not exist, or is not readable : " + commandDir.absolutePath);
        }

        if (new File(commandDir, "setmqaut").isFile()) {
            commandFile = new File(commandDir, "setmqaut");
        }
        else if (new File(commandDir, "setmqaut.exe").isFile()) {
            commandFile = new File(commandDir, "setmqaut.exe");
        }
        else if (new File(commandDir, "setmqaut.sh").isFile()) {
            commandFile = new File(commandDir, "setmqaut.sh");
        }
        else {
            throw new IllegalArgumentException("Specified command directory does not seem to contain setmqaut executable : " + commandDir.absolutePath);
        }
    }
    
    public void execute() {
        execute(null);
    }

    public void execute(Closure clos) {
        def cmdArgs = [commandFile.absolutePath, "-m", queueManager, "-t", objectType, '-n', objectName]
        groups.each { groupName ->
            cmdArgs << "-g"
            cmdArgs << groupName;
        }
        principals.each { principalName ->
            cmdArgs << "-p"
            cmdArgs << principalName;
        }
        authorities.each {
            cmdArgs << it;
        }
        cmdHelper.runCommand("Granting permissions", cmdArgs, clos);
    }
}
