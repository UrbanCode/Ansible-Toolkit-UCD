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

def aptName = props["aptName"];
def pkg = props["pkg"];
def state = props["state"];
def deb = props["deb"];
def update_cache = props["update_cache"];
def with_items = props["with_items"].split("\n");

def now = new Date();
def time = now.getTime();
def filename="tmp"+time+".yml";

File file = new File(filename);
file.write "- hosts: localhost\n";
file << "  any_errors_fatal: true\n";
file << "  tasks:\n";

file << "  - name: apt\n";
file << "    apt:\n";
if (aptName)
file << "      name="+aptName+"\n";
if (pkg)
file << "      pkg="+pkg+"\n";
if (state)
file << "      state="+state+"\n";
if (deb)
file << "      deb="+deb+"\n";

if (update_cache)
file << "      update_cache="+update_cache+"\n";

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
