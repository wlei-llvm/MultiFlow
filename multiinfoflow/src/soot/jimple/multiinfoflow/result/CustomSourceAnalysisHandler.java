package soot.jimple.multiinfoflow.result;

import heros.InterproceduralCFG;
import heros.solver.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import soot.SootMethod;
import soot.Unit;
import soot.jimple.Stmt;
import soot.jimple.infoflow.android.SetupApplication;
import soot.jimple.infoflow.android.source.AndroidSourceSinkManager;
import soot.jimple.infoflow.data.SourceContext;
import soot.jimple.infoflow.problems.TaintPropagationResults;
import soot.jimple.multiinfoflow.MultiInfoflow;
import soot.jimple.multiinfoflow.data.CustomSourceStmtInfo;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;

/**
 * @author wanglei
 */
public class CustomSourceAnalysisHandler extends AbstractMultiAnalysisHandler {

    TaintPropagationResults firstRunResults = null;

    InterproceduralCFG<Unit, SootMethod> cfg = null;

    List<List<String>> allPaseSourceSinkList = new ArrayList<>();

    private final Logger logger = LoggerFactory.getLogger(getClass());
    public CustomSourceAnalysisHandler(InterproceduralCFG<Unit, SootMethod> cfg, TaintPropagationResults firstRunResult, MultiInfoflow multiInfoflow, AndroidSourceSinkManager sourcesSinks){
        super(multiInfoflow, sourcesSinks);
        this.cfg = cfg;
        this.firstRunResults = firstRunResult;
    }


    @Override
    public void run() {

        String cusFileName = SetupApplication.CUSTOM_SOURCE_FILE;


        readFile(cusFileName);

        for(int k = 0; k < this.allPaseSourceSinkList.size(); k++) {

            logger.info("--Test (" + (k+1) + ")  ------------");

            List<String> sourceSinkList = allPaseSourceSinkList.get(k);

            CustomSourceStmtInfo validSourceStmtInfo = new CustomSourceStmtInfo();

            validSourceStmtInfo.calculateSourcesSinks(sourceSinkList, this.firstRunResults.getResults(),cfg);

            MultiInfoflow.validSourceStmtInfo = validSourceStmtInfo;


            MultiTaintPropagationResults propagationResults = runMultiAnalysis();
            Map<Integer, Set<Integer>> correlationRes = propagationResults.computeResults();
            Map<Pair<SourceContext,Stmt>, Integer> resIndex = propagationResults.getResultToIndex();

            logger.info("index to multiple sources and sinks!");

            List<Map.Entry<Pair<SourceContext,Stmt>, Integer>> sortedbyIndexRes =
                    new ArrayList<Map.Entry<Pair<SourceContext,Stmt>, Integer>>(resIndex.entrySet());


            Collections.sort(sortedbyIndexRes, new Comparator<Map.Entry<Pair<SourceContext,Stmt>, Integer>>() {
                public int compare(Map.Entry<Pair<SourceContext,Stmt>, Integer> o1, Map.Entry<Pair<SourceContext,Stmt>, Integer> o2) {
                    return (o1.getValue() - o2.getValue());
                    //return (o1.getKey()).toString().compareTo(o2.getKey());
                }
            });


            for(int i = 0; i < sortedbyIndexRes.size(); i++) {
                Map.Entry<Pair<SourceContext, Stmt>, Integer> entry = sortedbyIndexRes.get(i);
                int index = entry.getValue();
                Pair<SourceContext, Stmt> srcSink = entry.getKey();
                String tmp = "result:[" + index + "]:\n" + srcSink.getO1() +" TO \n" + srcSink.getO2();
                logger.info(tmp);
            }

            for(Map.Entry<Integer, Set<Integer>> entry : correlationRes.entrySet()) {
                String tmp = entry.getKey().toString() + ": [";
                for(int j : entry.getValue()) {
                    tmp += j + " ";
                }
                tmp += "]";
                logger.info(tmp);
            }
            if(correlationRes.size() > 0)
                logger.info("Multi Pattern " + (k + 1) + " :" + correlationRes.size());
            logger.info("=====================================================");

        }


    }

    private void readFile(String fileName)  {
        String line;
        List<String> onePaseList = new ArrayList<String>();
        FileReader fr = null;
        BufferedReader br = null;
        try {
            fr = new FileReader(fileName);
            br = new BufferedReader(fr);
            while((line = br.readLine()) != null) {
                if(line.startsWith("%%%")) {
                    allPaseSourceSinkList.add(onePaseList);
                    onePaseList = new ArrayList<String>();
                } else {
                    onePaseList.add(line);
                }
            }
            if(!onePaseList.isEmpty()) {
                allPaseSourceSinkList.add(onePaseList);
            }

        }
        catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


}
