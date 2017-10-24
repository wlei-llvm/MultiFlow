package soot.jimple.multiinfoflow;

import soot.jimple.infoflow.InfoflowConfiguration;
import soot.jimple.infoflow.source.DefaultSourceSinkManager;
import soot.jimple.infoflow.source.ISourceSinkManager;
import soot.jimple.infoflow.taintWrappers.EasyTaintWrapper;
import soot.jimple.infoflow.taintWrappers.ITaintPropagationWrapper;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;

/**
 * main function of the project
 * use this to test small java code
 *
 * @author wanglei
 */
public class CmdMultiInfoflow {

    public static void main(String[] args) {
        //ArgParser aparser = new ArgParser();


        try {
            // predefine sources and sinks
            Collection<String> sources = new ArrayList<>();
            Collection<String> sinks = new ArrayList<>();
            // sources.add("<TaintTest: java.lang.String parsePwd(java.lang.String[])>");
            // sinks.add("<TaintTest: void sink(java.lang.String)>");
            ISourceSinkManager sourcesSinks = new DefaultSourceSinkManager(sources, sinks);

            // the set of classes that shall be checked for taint analysis
            Collection<String> classes = new ArrayList<>();
            classes.add("TaintTest");

            //lib path
            String libpath = "";
            // process dir path
            //next test:
            String appPath = "";

            final ITaintPropagationWrapper taintWrapper;
            final EasyTaintWrapper easyTaintWrapper;
            File twSourceFile = new File("../soot-infoflow/EasyTaintWrapperSource.txt");
            if (twSourceFile.exists())

                easyTaintWrapper = new EasyTaintWrapper(twSourceFile);

            else {
                twSourceFile = new File("EasyTaintWrapperSource.txt");
                if (twSourceFile.exists())
                    easyTaintWrapper = new EasyTaintWrapper(twSourceFile);
                else {
                    System.err.println("Taint wrapper definition file not found at "
                            + twSourceFile.getAbsolutePath());
                    return ;
                }
            }
            boolean aggressiveTaintWrapper = false;
            easyTaintWrapper.setAggressiveMode(aggressiveTaintWrapper);
            taintWrapper = easyTaintWrapper;


            MultiInfoflow minfoflow = new MultiInfoflow();
            minfoflow.setTaintWrapper(taintWrapper);
            minfoflow.getConfig().setMultiTaintMode(InfoflowConfiguration.MultiTaintMode.AllCombination);

            minfoflow.computMultiInfoflowByCmdForTest(appPath, libpath, classes, sourcesSinks);
        } catch (IOException e) {
            e.printStackTrace();
        }


    }
}
