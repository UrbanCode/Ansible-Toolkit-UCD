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

def queryKey = props["queryKey"];
def resultKey = props["resultKey"];

def command="ansible localhost -m setup -a filter="+queryKey;
println("command is "+command)
def exec =command.execute();
exec.waitFor();


println("command completed")

def output = new StringBuffer()
def error = new StringBuffer()
//exec.consumeProcessOutputStream(output)
exec.consumeProcessErrorStream(error)

println(error);
println(output);
println("==-------------------------==")
def exitValue=exec.exitValue();
def oneline

exec.text.eachLine {
	println it
	oneline=it.trim()

	if (oneline.startsWith("\""+resultKey+"\""))
	{
		def pos="\"resultKey\":".length();
		//println("pos="+pos)
		def pos2=oneline.indexOf("\"",pos+1);


		println("AnsibleVariable="+oneline.substring(pos+1,pos2))
	}
}

if(!exitValue)
	System.exit(exitValue);
