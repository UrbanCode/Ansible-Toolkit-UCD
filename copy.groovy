/*
* Licensed Materials - Property of IBM Corp.
* IBM UrbanCode Build
* IBM UrbanCode Deploy
* IBM UrbanCode Release
* IBM AnthillPro
* (c) Copyright IBM Corporation 2002, 2016. All Rights Reserved.
*
* U.S. Government Users Restricted Rights - Use, duplication or disclosure restricted by
* GSA ADP Schedule Contract with IBM Corp.
*/
import com.urbancode.air.AirPluginTool;

def apTool = new AirPluginTool(this.args[0], this.args[1]) //assuming that args[0] is input props file and args[1] is output props file

def props = apTool.getStepProperties();

final def isWindows = apTool.isWindows;

def src = props["src"];
def dest = props["dest"];
def owner = props["owner"];
def group = props["group"];
def mode = props["mode"];
def with_items = props["with_items"].split("\n");

def now = new Date();
def time = now.getTime();
def filename="tmp"+time+".yml";

File file = new File(filename);
file.write "- hosts: localhost\n";
file << "  any_errors_fatal: true\n";
file << "  tasks:\n";

file << "  - name: copy\n";
file << "    copy:\n";
file << "      src="+src+"\n";
file << "      dest="+dest+"\n";
if (owner)
	file << "      owner="+owner+"\n";
if (group)
	file << "      group="+group+"\n";
if (mode)
	file << "      mode="+mode+"\n";
	
if (with_items!=null && with_items.size()>0)
	{
		if (with_items.size()==1 && with_items[0].trim().length()==0){}
		else
		{
			file << "    with_items:\n";
			
			def theline;
			for(Iterator<String> i = with_items.iterator(); i.hasNext();){
				theline=i.next().trim();
				theline="      "+theline+"\n";
				file << theline;
			}
		}
	}

def command="ansible-playbook "+filename+ " -vvvv";
println("command is "+command)
def exec =command.execute();
exec.waitFor();

def error = new StringBuffer()
exec.consumeProcessErrorStream(error)

exec.text.eachLine {println it}
println(error);
def exitValue=exec.exitValue();
if(!exitValue)
	System.exit(exitValue);