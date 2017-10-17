package soot.jimple.multiinfoflow.result;

import heros.InterproceduralCFG;
import heros.solver.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import soot.SootMethod;
import soot.Unit;
import soot.jimple.Stmt;
import soot.jimple.infoflow.android.SetupApplication;
import soot.jimple.infoflow.android.data.parsers.PermissionMethodParser;
import soot.jimple.infoflow.android.source.AndroidSourceSinkManager;
import soot.jimple.infoflow.data.AbstractionAtSink;
import soot.jimple.infoflow.problems.TaintPropagationResults;
import soot.jimple.infoflow.source.data.ISourceSinkDefinitionProvider;
import soot.jimple.infoflow.source.data.SourceSinkDefinition;
import soot.jimple.multiinfoflow.MultiInfoflow;
import soot.jimple.multiinfoflow.source.CustomMultiSourceSinkManager;
import soot.jimple.multiinfoflow.util.NoDisPair;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;

/**
 * @author wanglei
 */
public class CustomPairSourceAnalysisHandler extends AllCombFasterAnalysisHandler {

    List<List<String>> allPaseSourceSinkList = new ArrayList<>();

    private final Logger logger = LoggerFactory.getLogger(getClass());
    public CustomPairSourceAnalysisHandler(InterproceduralCFG<Unit, SootMethod> cfg, TaintPropagationResults firstRunResult, MultiInfoflow multiInfoflow, AndroidSourceSinkManager sourcesSinks){
        super(cfg, firstRunResult, multiInfoflow, sourcesSinks);
    }


    @Override
    public void run() {

        String cusFileName = SetupApplication.CUSTOM_SOURCE_FILE;

        readFile(cusFileName);

        for (int k = 0; k < this.allPaseSourceSinkList.size(); k++) {

            int newPatternCount = 0;
            int multiAnalysisCount = 0;

            List<String> sourceSinkList = allPaseSourceSinkList.get(k);

            calculateSourcesSinksSignature(sourceSinkList, preResults.getResults(), iCfg);

            Pair<Integer, Integer> ret = computeMultiInfo();
            if(ret != null) {
                newPatternCount = ret.getO1();
                multiAnalysisCount = ret.getO2();
            }

            logger.info("-- Pattern (" + (k+1) + ") : new pattern found : " + newPatternCount + " ,multiinfoflow analysis count : "  + multiAnalysisCount );
        }


        String summary = "";
        int resCount = 0;
        for (Map.Entry<NoDisPair<String, String>, Integer> entry : resPairToFlagMap.entrySet()) {
            if (entry.getKey().getO1() != entry.getKey().getO2()) {
                //summary +="\n[ "+ (resCount+1) +"] Pair<< " + entry.getKey().getO1() +", " + entry.getKey().getO2() + ">>, Flag : " + flagToString(entry.getValue()) ;
                summary +="\n[ "+ (resCount+1) +"] Pair<< " + entry.getKey().getO1() +", " + entry.getKey().getO2() + ">>" ;
                resCount++;
            }
        }
        logger.info("===== Multi Pattern Found Result : " + resCount);
        logger.info(summary);
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


    private void calculateSourcesSinksSignature(List<String> sourceSinkFile, Set<AbstractionAtSink> res, InterproceduralCFG<Unit, SootMethod> cfg ) {
        ISourceSinkDefinitionProvider parser = null;

        try {
//            parser = PermissionMethodParser.fromFile(sourceSinkFile);
            parser = PermissionMethodParser.fromStringList(sourceSinkFile);

            CustomMultiSourceSinkManager sourceSinkManager = new CustomMultiSourceSinkManager(parser.getSources(), Collections.<SourceSinkDefinition>emptySet());

            for(AbstractionAtSink sink : res) {

                Stmt sourceStmt = sink.getAbstraction().getSourceContext().getStmt();
                Stmt sinkStmt = sink.getSinkStmt();

                String sourceSignature = sourceSinkManager.getSourceSignature(sourceStmt, cfg);
                if(sourceSignature == null)
                    continue;

                if(!sourceSinkMap.containsKey(sourceSignature))
                    sourceSinkMap.put(sourceSignature, new HashSet<Pair<Stmt, Stmt>>());

                Set<Pair<Stmt, Stmt>> list = sourceSinkMap.get(sourceSignature);
                if(sourceStmt != null && sinkStmt != null)
                    list.add(new Pair<Stmt, Stmt>(sourceStmt, sinkStmt));

            }

        }
        catch (IOException e) {
            System.err.println("CustomSourcesAndSinks.txt file not found ! ");
            e.printStackTrace();
        }

    }



}
