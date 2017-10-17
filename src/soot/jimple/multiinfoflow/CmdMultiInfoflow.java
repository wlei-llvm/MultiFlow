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
            sources.add("<TaintTest: java.lang.String parsePwd(java.lang.String[])>");
            sources.add("<TaintTest: java.lang.String parseId(java.lang.String[])>");
            sources.add("<TaintTest: java.lang.String parseId()>");
            sources.add("<TaintTest: java.lang.String parseName(java.lang.String[])>");
            sources.add("<TaintTest: java.lang.String parseName1(java.lang.String[])>");
            sources.add("<TaintTest: java.lang.String parseName2(java.lang.String[])>");
            sinks.add("<TaintTest: void sink(java.lang.String)>");
            ISourceSinkManager sourcesSinks = new DefaultSourceSinkManager(sources, sinks);

            // the set of classes that shall be checked for taint analysis
            Collection<String> classes = new ArrayList<>();
            classes.add("TaintTest");

            //lib path
            String libpath = "";
            // process dir path
            //next test:
            //String appPath = "/Users/wanglei/Desktop/MyProgram/MultiTest/src/main/java/nexttest/test2";
            //String appPath = "/Users/wanglei/Desktop/MyProgram/MultiTest/src/main/java/nexttest/testcall";
            //String appPath = "/Users/wanglei/Desktop/MyProgram/MultiTest/src/main/java/nexttest/testmerge2";
            //String appPath = "/Users/wanglei/Desktop/MyProgram/MultiTest/src/main/java/nexttest/test2sink";

            //normal test
            //String appPath = "/Users/wanglei/Desktop/MyProgram/MultiTest/src/main/java/normaltest/test9";
            //String appPath = "/Users/wanglei/Desktop/MyProgram/MultiTest/src/main/java/normaltest/test1";

            //call test
            //String appPath = "/Users/wanglei/Desktop/MyProgram/MultiTest/src/main/java/calltest/test6";
            //String appPath = "/Users/wanglei/Desktop/MyProgram/MultiTest/src/main/java/calltest/teststatic1";
            //String appPath = "/Users/wanglei/Desktop/MyProgram/MultiTest/src/main/java/calltest/testmerge";
            //String appPath = "/Users/wanglei/Desktop/MyProgram/MultiTest/src/main/java/calltest/multiparmtest3";
            //String appPath = "/Users/wanglei/Desktop/MyProgram/MultiTest/src/main/java/calltest/testex2";
            //String appPath = "/Users/wanglei/Desktop/MyProgram/MultiTest/src/main/java/calltest/testmanycall1";
            //String appPath = "/Users/wanglei/Desktop/MyProgram/MultiTest/src/main/java/calltest/testmanycall2";
            //String appPath = "/Users/wanglei/Desktop/MyProgram/MultiTest/src/main/java/calltest/testrecur";

            //single test
            //String appPath = "/Users/wanglei/Desktop/MyProgram/MultiTest/src/main/java/singletest/calltest";

            //alias test
            //String appPath = "/Users/wanglei/Desktop/MyProgram/MultiTest/src/main/java/aliastest/test2";
            //String appPath = "/Users/wanglei/Desktop/MyProgram/MultiTest/src/main/java/aliastest/testthis1";
            //String appPath = "/Users/wanglei/Desktop/MyProgram/MultiTest/src/main/java/aliastest/testmerge";
            //String appPath = "/Users/wanglei/Desktop/MyProgram/MultiTest/src/main/java/aliastest/testnewobject";
            //String appPath = "/Users/wanglei/Desktop/MyProgram/MultiTest/src/main/java/aliastest/test1if";

            //interalias test
            //String appPath = "/Users/wanglei/Desktop/MyProgram/MultiTest/src/main/java/interaliastest/test5";
            //String appPath = "/Users/wanglei/Desktop/MyProgram/MultiTest/src/main/java/interaliastest/singletest2";
            //String appPath = "/Users/wanglei/Desktop/MyProgram/MultiTest/src/main/java/interaliastest/singletestcall";
            //String appPath = "/Users/wanglei/Desktop/MyProgram/MultiTest/src/main/java/interaliastest/testunb";
            //String appPath = "/Users/wanglei/Desktop/MyProgram/MultiTest/src/main/java/interaliastest/singletestsumopt";

            //String appPath = "/Users/wanglei/Desktop/MyProgram/MultiTest/src/main/java/interaliastest/singletest2";

            // test ubr

            //String appPath = "/Users/wanglei/Desktop/MyProgram/MultiTest/src/main/java/ubrtest/testunb3";
           // String appPath = "/Users/wanglei/Desktop/MyProgram/MultiTest/src/main/java/ubrtest/testunb4";

            //return test:
            //String appPath = "/Users/wanglei/Desktop/MyProgram/MultiTest/src/main/java/returntest/test1";
            //String appPath = "/Users/wanglei/Desktop/MyProgram/MultiTest/src/main/java/returntest/thistest";
            //String appPath = "/Users/wanglei/Desktop/MyProgram/MultiTest/src/main/java/interaliastest/singletestcall";

            //end test:
            //String appPath = "/Users/wanglei/Desktop/MyProgram/MultiTest/src/main/java/endtest/test1";
            //String appPath = "/Users/wanglei/Desktop/MyProgram/MultiTest/src/main/java/endtest/testmulti";
            //wrapper test
            //String appPath = "/Users/wanglei/Desktop/MyProgram/MultiTest/src/main/java/wrappertest/test1";
            //String appPath = "/Users/wanglei/Desktop/MyProgram/MultiTest/src/main/java/wrappertest/test1";
            //array test
            //String appPath = "/Users/wanglei/Desktop/MyProgram/MultiTest/src/main/java/arraytest/singletest";
            //droidbench test :
            //String appPath = "/Users/wanglei/Desktop/MyProgram/MultiTest/src/main/java/droidbench/test1";
            //opt test :
            //String appPath = "/Users/wanglei/Desktop/MyProgram/MultiTest/src/main/java/opttest/singletest";
            //String appPath = "/Users/wanglei/Desktop/MyProgram/MultiTest/src/main/java/opttest/multitest";
            //String appPath = "/Users/wanglei/Desktop/MyProgram/MultiTest/src/main/java/opttest/calltest";
            //String appPath = "/Users/wanglei/Desktop/MyProgram/MultiTest/src/main/java/opttest/calltest1eff";
            //String appPath = "/Users/wanglei/Desktop/MyProgram/MultiTest/src/main/java/opttest/ifcalltest";

            //summary opt
            //String appPath = "/Users/wanglei/Desktop/MyProgram/MultiTest/src/main/java/opttest/sumtest";
            //String appPath = "/Users/wanglei/Desktop/MyProgram/MultiTest/src/main/java/opttest/sumtestif";
            String appPath = "/Users/wanglei/Desktop/MyProgram/MultiTest/src/main/java/sumopttest/test4";


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
