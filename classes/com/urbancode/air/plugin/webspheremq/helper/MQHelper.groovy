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

public class MQHelper {
    def workDir = new File(".").canonicalFile
    final def cmdHelper = new CommandHelper(workDir)
    def cmdArgs = []
    def scriptData
    def out = System.out
    
    public MQHelper(def cmdArgsIn) {
        cmdArgs = cmdArgsIn;
    }
    
    public MQHelper(def cmdArgsIn, def scriptDataIn) {
        cmdArgs = cmdArgsIn
        scriptData = scriptDataIn
    }
    
    def runCommand(def message) {
        cmdHelper.runCommand(message, cmdArgs);
    }
    
    def runCommandScript(def message) {
        cmdHelper.runCommand(message, cmdArgs) {proc ->
            proc.withWriter{it << scriptData};
            proc.consumeProcessOutput(out, out);
        }
    }
}