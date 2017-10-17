package soot.jimple.multiinfoflow.result;

import heros.InterproceduralCFG;
import heros.solver.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import soot.SootMethod;
import soot.Unit;
import soot.jimple.Stmt;
import soot.jimple.infoflow.android.source.AndroidSourceSinkManager;
import soot.jimple.infoflow.data.AbstractionAtSink;
import soot.jimple.infoflow.problems.TaintPropagationResults;
import soot.jimple.multiinfoflow.MultiInfoflow;
import soot.jimple.multiinfoflow.data.AbstractionVector;
import soot.jimple.multiinfoflow.data.CustomPairSourceStmtInfo;
import soot.jimple.multiinfoflow.data.MultiAbstractionAtSink;
import soot.jimple.multiinfoflow.util.NoDisPair;

import java.util.*;

/**
 * @author wanglei
 */
public class AllCombFasterAnalysisHandler extends AbstractMultiAnalysisHandler {

    protected TaintPropagationResults preResults = null;

    protected InterproceduralCFG<Unit, SootMethod> iCfg = null;

    private List<Double> timeList = new ArrayList<Double>();

    protected List<String> sourceSignatureList = new ArrayList<>();

    protected Map<String, Set<Pair<Stmt, Stmt>>> sourceSinkMap = new HashMap<>();

    protected Map<NoDisPair<String, String>, Integer> resPairToFlagMap = new HashMap<>();
    protected Set<NoDisPair<Stmt, Stmt>> visited = new HashSet<>();

    private final Logger logger = LoggerFactory.getLogger(getClass());
    public AllCombFasterAnalysisHandler(InterproceduralCFG<Unit, SootMethod> cfg, TaintPropagationResults preResults, MultiInfoflow multiInfoflow, AndroidSourceSinkManager sourcesSinks){
        super(multiInfoflow, sourcesSinks);
        this.preResults = preResults;
        this.iCfg = cfg;
    }

    @Override
    public void run() {

        int newPatternCount = 0;
        int multiAnalysisCount = 0;
        calculateSourcesSinksSignature();


        long beforeCompute = System.nanoTime() ;
        Pair<Integer, Integer> ret = computeMultiInfo();
        long afterCompute = System.nanoTime() ;

        if (ret != null) {
            newPatternCount = ret.getO1();
            multiAnalysisCount = ret.getO2();
        }

        String summary = "";
        int resCount = 0;
        for(Map.Entry<NoDisPair<String, String>, Integer> entry : resPairToFlagMap.entrySet()) {
            if(entry.getKey().getO1() != entry.getKey().getO2()) {
                //summary +="\n[ "+ (resCount+1) +"] Pair<< " + entry.getKey().getO1() +", " + entry.getKey().getO2() + ">>, Flag : " + flagToString(entry.getValue()) ;
                summary +="\n[ "+ (resCount+1) +"] Pair<< " + entry.getKey().getO1() +", " + entry.getKey().getO2() + ">>";
                resCount++;
            }
        }
        logger.info("MultiInfoFlow took " +  (afterCompute - beforeCompute) / 1E9
                + " seconds");
        logger.info("Each multi-solver took " +  timeList.toString()
                + " seconds");
        logger.info("Avg of each multi-solver took " +  getAverageTime()
                + " seconds");

        logger.info("===== Multi Pattern Found Result : " + resCount);
        logger.info("===== Multi Analysis Count : " + multiAnalysisCount);
        logger.info(summary);

    }

    private double getAverageTime() {
        double sum = 0;
        for(double d : timeList) {
            sum += d;
        }
        return sum / timeList.size();
    }


    protected String flagToString(int flag) {
        switch (flag) {
            case 1 : return "PATHCONTAIN";
            case 2 : return "SAMEOBJECT";
            case 3 : return "SAMESINK";
            case -1 : return "ERRORFLAG";
        }
        return "NONMATCHTYPE";
    }

    protected Pair<Integer, Integer> computeMultiInfo() {

        int newPatternCount = 0;
        int multiAnalysisCount = 0;
        sourceSignatureList.addAll(sourceSinkMap.keySet());
        if(sourceSignatureList.size() <= 1) {
            logger.info("No matched Sources !");
          //  logger.info("Count of matched Sources is less than 2, please add more Sources !");
            return null;
        }
        boolean found = false;
        for(int i = 0; i < sourceSignatureList.size(); i++) {
            String firstSource = sourceSignatureList.get(i);
            Set<Pair<Stmt, Stmt>> fistSourceInfo = sourceSinkMap.get(firstSource);
            for(int j = i + 1; j < sourceSignatureList.size(); j++) {
                String secondSource = sourceSignatureList.get(j);
                if(resPairToFlagMap.containsKey(new NoDisPair<String, String>(firstSource, secondSource))) {
//                        logger.info("Found Pair<< " + firstSource +", " + secondSource + ">>, Flag : " +
//                                resPairToFlagMap.get(new Pair<String, String>(firstSource, secondSource)));
                    found = true;
                    continue;
                }
                Set<Pair<Stmt, Stmt>> secondSourceInfo = sourceSinkMap.get(secondSource);

                int flag = -1;
                    for(Pair<Stmt, Stmt> first : fistSourceInfo) {
                        for(Pair<Stmt, Stmt> second : secondSourceInfo) {
                            if(first.getO2().equals(second.getO2())) {
                                flag = 3;
                                break;
                            }
                        }
                        if(flag != -1)
                            break;
                    }

                if(flag == -1)
                    for(Pair<Stmt, Stmt> first : fistSourceInfo) {
                        for(Pair<Stmt, Stmt> second : secondSourceInfo) {
                            if(visited.contains(new NoDisPair<Stmt, Stmt>(first.getO1(), second.getO1())))
                                break;
                            else {
                                visited.add(new NoDisPair<Stmt, Stmt>(first.getO1(), second.getO1()));
//                                    visited.add(new NoDisPair<Stmt, Stmt>(second.getO1(), first.getO1()));
                            }
                            multiAnalysisCount++;
                            flag =  computeMultiInfo(first, second);

                            if(flag != -1)
                                break;
                        }
                        if(flag != -1)
                            break;
                    }

                if(flag != -1) {
                    resPairToFlagMap.put(new NoDisPair<String, String>(firstSource, secondSource), flag);
//                        resPairToFlagMap.put(new Pair<String, String>(secondSource, firstSource), flag);
//                        logger.info("Found Pair<< " + firstSource +", " + secondSource + ">>, Flag : " + Integer.toString(flag));
                    found = true;
                    newPatternCount++;
                }
            }

        }

        return new Pair<>(newPatternCount, multiAnalysisCount);
    }


    private void calculateSourcesSinksSignature() {

        for(AbstractionAtSink sink : preResults.getResults()) {

            Stmt sourceStmt = sink.getAbstraction().getSourceContext().getStmt();
            Stmt sinkStmt = sink.getSinkStmt();

            String sourceSignature = this.sourceSinkManager.getSourceSignature(sourceStmt, iCfg);
            if(sourceSignature == null)
                continue;

            if(!sourceSinkMap.containsKey(sourceSignature))
                sourceSinkMap.put(sourceSignature, new HashSet<Pair<Stmt, Stmt>>());

            Set<Pair<Stmt, Stmt>> list = sourceSinkMap.get(sourceSignature);
            if(sourceStmt != null && sinkStmt != null)
                list.add(new Pair<Stmt, Stmt>(sourceStmt, sinkStmt));

        }

    }

    protected  int computeMultiInfo(Pair<Stmt, Stmt> firstInfo, Pair<Stmt, Stmt> secondInfo) {

//        logger.info("First : " + firstInfo);
//        logger.info("Second : " + secondInfo);
        CustomPairSourceStmtInfo validSourceStmtInfo = new CustomPairSourceStmtInfo();
        validSourceStmtInfo.calculateSourcesSinks(firstInfo, secondInfo);

        MultiInfoflow.validSourceStmtInfo = validSourceStmtInfo;

        MultiTaintPropagationResults propagationResults = runMultiAnalysis();
        Set<MultiAbstractionAtSink> res =  propagationResults.getResults();

        timeList.add((propagationResults.getTime() / 1E9));

        int flag = -1;
        for(MultiAbstractionAtSink s : res) {
            AbstractionVector resAbs =  s.getAbstractionVector();
            if(resAbs.endflag == 2) {
                return 2;
            } else if (resAbs.getIndexSet().size() == 2) {
                return 1;
            }
        }
        return -1;
    }

}
