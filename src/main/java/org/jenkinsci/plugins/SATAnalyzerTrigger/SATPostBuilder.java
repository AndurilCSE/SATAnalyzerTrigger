package org.jenkinsci.plugins.SATAnalyzerTrigger;
import hudson.Launcher;
import hudson.Extension;
import hudson.FilePath;
import hudson.model.*;
import hudson.tasks.*;
import hudson.util.FormValidation;
import jenkins.tasks.SimpleBuildStep;
import net.sf.json.JSONObject;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.QueryParameter;

import javax.servlet.ServletException;
import java.io.IOException;

/**
 * Sample {@link Builder}.
 *
 * <p>
 * When the user configures the project and enables this builder,
 * {@link DescriptorImpl#newInstance(StaplerRequest)} is invoked
 * and a new {@link SATPostBuilder} is created. The created
 * instance is persisted to the project configuration XML by using
 * XStream, so this allows you to use instance fields (like {@link #script})
 * to remember the configuration.
 *
 * <p>
 * When a build is performed, the {@link #perform} method will be invoked. 
 *
 * @author Kohsuke Kawaguchi
 */
public class SATPostBuilder extends Recorder implements BuildStep {

    //private final String name;
    private String script;
    private boolean RunIfJobCodeBaseChanged;

    // Fields in config.jelly must match the parameter names in the "DataBoundConstructor"
    @DataBoundConstructor
    public SATPostBuilder(String script, boolean RunIfJobCodeBaseChanged) {
        this.script=script;
        this.RunIfJobCodeBaseChanged=RunIfJobCodeBaseChanged;
    }

    /**
     * We'll use this from the <tt>config.jelly</tt>.
     */
    /*
    public String getName() {
        return name;
    }
    */
    public String getScript(){
        return script;
    }
    public boolean isRunIfJobCodeBaseChanged(){
        return RunIfJobCodeBaseChanged;
    }

    @Override
    public boolean perform(AbstractBuild build, Launcher launcher, BuildListener listener) throws InterruptedException, IOException {
        String buildLog = build.getLog();
        listener.getLogger().println("Performing Post build task...:)");

        Result pr = build.getResult();
        listener.getLogger().println("Performing Post build task..."+pr.toString());

        try {
            listener.getLogger().println("Entering try block... script : "+script+" Checkbox checked: "+RunIfJobCodeBaseChanged);

                if (pr!=null && pr.isWorseThan(Result.UNSTABLE)) {
                    listener.getLogger().println("Skipping post build task - job status is worse than unstable : "+build.getResult());
                    return true;
                }
                if(isRunIfJobCodeBaseChanged()) {
                    listener.getLogger().println("Code changes between previous build and current build are checked ");
                    if (!checkLogForCodeChange(buildLog, listener)) {
                        listener.getLogger().println("There is no code changes between previous build and current build, so skipping script");
                        return true;
                    }
                }
                listener.getLogger().println("Running script  : " + script);
                CommandInterpreter runner = getCommandInterpreter(launcher,script);
                Result result = runner.perform(build, launcher, listener) ? Result.SUCCESS: Result.FAILURE;
                listener.getLogger().println("POST BUILD TASK : " + result.toString());
                listener.getLogger().println( "END OF POST BUILD TASK ");

            } catch (Exception e) {
                listener.getLogger().println("Exception when executing the batch command : "+ e.getMessage());
            return false;
        }
        return true;
    }


    /*
    *Check the log for the information about the code changes between the previous build and current build
    *
     */
    public boolean checkLogForCodeChange(String buildLog,  BuildListener listener){
        boolean isCodeChanged=false;
        String checkout;
        String revList;
        if(buildLog!=null && !buildLog.equals("") && buildLog.contains(">")) {
            String[] logSents = buildLog.split(">");
            String checkoutSent = logSents[logSents.length - 2].trim();
            String revListSent = logSents[logSents.length - 1].trim();
            String[] checkoutWords = checkoutSent.split(" ");
            String[] revListWords = revListSent.split(" ");
            if(checkoutWords.length>3 && revListWords.length>2) {
                checkout = checkoutWords[3];
                revList = revListWords[2];
                isCodeChanged = !checkout.equals(revList);
            }
        }

        return isCodeChanged;
    }

    /**
     * This method will return the command intercepter as per the node OS
     *
     * @param launcher
     * @param script
     * @return CommandInterpreter
     */
    private CommandInterpreter getCommandInterpreter(Launcher launcher,String script) {
        if (launcher.isUnix())
            return new Shell(script);
        else
            return new BatchFile(script);
    }


    // Overridden for better type safety.
    // If your plugin doesn't really define any property on Descriptor,
    // you don't have to do this.
    @Override
    public DescriptorImpl getDescriptor() {
        return (DescriptorImpl)super.getDescriptor();
    }

    @Override
    public BuildStepMonitor getRequiredMonitorService() {
        return BuildStepMonitor.NONE;
    }

    /**
     * Descriptor for {@link SATPostBuilder}. Used as a singleton.
     * The class is marked as public so that it can be accessed from views.
     *
     * <p>
     * See <tt>src/main/resources/hudson/plugins/hello_world/SATPostBuilder/*.jelly</tt>
     * for the actual HTML fragment for the configuration screen.
     */
    @Extension // This indicates to Jenkins that this is an implementation of an extension point.
    public static final class DescriptorImpl extends BuildStepDescriptor<Publisher> {
        /**
         * To persist global configuration information,
         * simply store it in a field and call save().
         *
         * <p>
         * If you don't want fields to be persisted, use <tt>transient</tt>.
         */
        //private boolean useFrench;

        /**
         * In order to load the persisted global configuration, you have to 
         * call load() in the constructor.
         */
        public DescriptorImpl() {
            super(SATPostBuilder.class);
            load();
        }



        public boolean isApplicable(Class<? extends AbstractProject> aClass) {
            // Indicates that this builder can be used with all kinds of project types 
            return true;
        }

        /**
         * This human readable name is used in the configuration screen.
         */
        public String getDisplayName() {
            return "SAT Analyzer Trigger";
        }

        @Override
        public boolean configure(StaplerRequest req, JSONObject formData) throws FormException {
            // To persist global configuration information,
            // set that to properties and call save().
            //useFrench = formData.getBoolean("useFrench");
            req.bindJSON(this, formData);
            //  (easier when there are many fields; need set* methods for this, like setUseFrench)
            save();
            return super.configure(req,formData);
        }

        /**
         * This method returns true if the global configuration says we should speak French.
         *
         * The method name is bit awkward because global.jelly calls this method to determine
         * the initial state of the checkbox by the naming convention.
         */
        /*
        public boolean getUseFrench() {

            return useFrench;
        }
         */
    }
}

