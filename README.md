# SATAnalyzerTrigger
SATAnalyzer Trigger is Jenkins post build plugin to invoke SATAnalyzer tool after the Jenkins job build.

This is a plugin for Jenkins continuous integration serve. It invokes SATAnalyzer (Software Artefact Traceability Analyzer) tool after a build of job in the Jenkins. 
This plugin check the log of the jenkins job build for is there any code changes in the job.
Then execut the script given in the script field while the configuration of the job.
The script is do the job to invoke the SATAnalyzer tool for tracing inconsitencies between artefacts. 
