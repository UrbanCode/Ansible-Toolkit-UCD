import com.urbancode.air.plugin.tool.AirPluginTool

/**
 *  © Copyright IBM Corporation 2016, 2024.
 *  This is licensed under the following license.
 *  The Eclipse Public 1.0 License (http://www.eclipse.org/legal/epl-v10.html)
 *  U.S. Government Users Restricted Rights:  Use, duplication or disclosure restricted by GSA ADP Schedule Contract with IBM Corp.
 */


def apTool = new AirPluginTool(this.args[0], this.args[1]) //assuming that args[0] is input props file and args[1] is output props file

def props = apTool.getStepProperties();

final def isWindows = apTool.isWindows;

def taskcommand = props["taskcommand"];
def creates = props["creates"];
def environment = props["environment"].split("\n");
def chdir = props["chdir"];
def become = props["become"];

def now = new Date();
def time = now.getTime();
def filename="tmp"+time+".yml";

File file = new File(filename);
file.write "- hosts: localhost\n";
file << "  any_errors_fatal: true\n";
file << "  tasks:\n";

file << "  - name: command\n";
file << "    command: "+taskcommand+"\n";
if (creates)
file << "      creates="+creates+"\n";
if (chdir)
file << "      chdir="+chdir+"\n";
if (environment!=null && environment.size()>0)
{
	if (environment.size()==1 && environment[0].trim().length()==0){}
	else
	{
		file << "    environment:\n";

		def theline;
		for(Iterator<String> i = environment.iterator(); i.hasNext();){
			theline=i.next().trim();
			theline="      "+theline+"\n";
			file << theline;
		}
	}
}

if (become)
file << "    become: "+become+"\n";

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
