package soot.jimple.multiinfoflow.result;

import heros.InterproceduralCFG;
import heros.solver.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import soot.SootMethod;
import soot.Unit;
import soot.jimple.Stmt;
import soot.jimple.infoflow.android.source.AndroidSourceSinkManager;
import soot.jimple.infoflow.problems.TaintPropagationResults;
import soot.jimple.multiinfoflow.MultiInfoflow;
import soot.jimple.multiinfoflow.util.NoDisPair;

import java.util.*;

/**
 * @author wanglei
 */
public class AllCombCallBackPartitionAnalysisHandler extends AllCombFasterAnalysisHandler {


    private final Logger logger = LoggerFactory.getLogger(getClass());
    public AllCombCallBackPartitionAnalysisHandler(InterproceduralCFG<Unit, SootMethod> cfg, TaintPropagationResults preResults, MultiInfoflow multiInfoflow, AndroidSourceSinkManager sourcesSinks){
        super(cfg, preResults, multiInfoflow, sourcesSinks);
    }

    @Override
    protected Pair<Integer, Integer> computeMultiInfo() {

        Set<SootMethod> callBackList = multiInfoflow.getCallBackList();


        Map<SootMethod, Set<SootMethod>> callBackToMethod = new HashMap<>();
        Map<SootMethod, Set<SootMethod>> methodToCallBack = new HashMap<>();

        for(SootMethod callbackEp : callBackList) {
            Set<SootMethod> doneSet = new HashSet<>();
            setCallBackReachableMethod(callbackEp, callbackEp, callBackToMethod, methodToCallBack, doneSet)  ;
        }

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

                            //different callback
                            if(!isSameCallBack(first.getO2(), second.getO1(), callBackToMethod, methodToCallBack)
                                    && !isSameCallBack(first.getO1(), second.getO2(), callBackToMethod, methodToCallBack) ) {
                                break;
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


    private void setCallBackReachableMethod(SootMethod entry, SootMethod callback, Map<SootMethod,
            Set<SootMethod>> callBackToMethod, Map<SootMethod, Set<SootMethod>> methodToCallBack,
                                            Set<SootMethod> doneSet) {
        if (!entry.isConcrete() || !entry.getDeclaringClass().isApplicationClass() || !doneSet.add(entry))
            return;

        if(entry.hasActiveBody()) {
            for(Unit u : entry.getActiveBody().getUnits()) {
                Stmt stmt = (Stmt) u;
                if (stmt.containsInvokeExpr()) {
                    SootMethod sm = stmt.getInvokeExpr().getMethod();
                    if(sm.hasActiveBody()) {
                        if(!callBackToMethod.containsKey(callback)) {
                            callBackToMethod.put(callback, new HashSet<SootMethod>());
                        }
                        Set<SootMethod> tmpSet = callBackToMethod.get(callback);
                        tmpSet.add(sm);

                        if(!methodToCallBack.containsKey(sm)) {
                            methodToCallBack.put(sm, new HashSet<SootMethod>());
                        }
                        Set<SootMethod> callSet = methodToCallBack.get(sm);
                        callSet.add(callback);

                        setCallBackReachableMethod(sm, callback, callBackToMethod, methodToCallBack, doneSet);
                    }
                }
            }
        }
    }


    private boolean isSameCallBack(Stmt s1, Stmt s2, Map<SootMethod, Set<SootMethod>> callBackToMethod,  Map<SootMethod, Set<SootMethod>> methodToCallBack) {
        SootMethod sm1 = iCfg.getMethodOf(s1);
        SootMethod sm2 = iCfg.getMethodOf(s2);

        Set<SootMethod> set1 = methodToCallBack.get(sm1);
        Set<SootMethod> set2 = methodToCallBack.get(sm2);
        if(set1 == null || set2 == null)
            return true;

        if(set1.size() > set2.size()) {
            Set<SootMethod> tmp = set1;
            set1 = set2;
            set2 = tmp;
        }

        for(SootMethod m : set1) {
            if(set2.contains(m))
                return true;
        }
        return false;

    }

}
