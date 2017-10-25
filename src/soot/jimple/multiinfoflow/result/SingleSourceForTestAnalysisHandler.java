package soot.jimple.multiinfoflow.result;

import heros.solver.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import soot.jimple.Stmt;
import soot.jimple.infoflow.data.AbstractionAtSink;
import soot.jimple.infoflow.data.SourceContext;
import soot.jimple.infoflow.problems.TaintPropagationResults;
import soot.jimple.multiinfoflow.MultiInfoflow;
import soot.jimple.multiinfoflow.data.MultiAbstractionAtSink;
import soot.jimple.multiinfoflow.data.SingleTestSourceStmtInfo;

import java.util.*;

/**
 * @author wanglei
 */
public class SingleSourceForTestAnalysisHandler extends AbstractMultiAnalysisHandler {

    TaintPropagationResults preResults = null;

    private final Logger logger = LoggerFactory.getLogger(getClass());
    public SingleSourceForTestAnalysisHandler(TaintPropagationResults preResults, MultiInfoflow multiInfoflow){
        super(multiInfoflow, null);
        this.preResults = preResults;
    }

    @Override
    public void run() {

        List<Stmt> sourceSet = new ArrayList<>();
        List<Stmt> sinkSet = new ArrayList<>();
        int resultSize = 0;
        for(AbstractionAtSink atSink :preResults.getResults()) {
            sourceSet.add(atSink.getAbstraction().getSourceContext().getStmt());
            sinkSet.add(atSink.getSinkStmt());
            logger.info("("+ (resultSize+1)+") :" +atSink.getAbstraction().getSourceContext()+ " To "+ sinkSet.get(resultSize)+ "" );
            resultSize++;
        }
//        AbstractionVector.MultiSourceContext.addAll(sourceSet);
//        AbstractionVector.ABS_MAP_MAX_SIZE = AbstractionVector.MultiSourceContext.size();

        System.out.println("Result Size : " + resultSize);


        logger.info("====================== Single Test Start ==============");
        int countSucceed = 0;

        for(int k = 0; k < resultSize; k++) {

            logger.info("--Test (" + (k+1) + ")  ------------");
            SingleTestSourceStmtInfo validSourceStmtInfo = new SingleTestSourceStmtInfo();
            MultiInfoflow.validSourceStmtInfo = validSourceStmtInfo;
            Stmt sourceStmtForOnceTest = sourceSet.get(k);
            Stmt sinkStmtForOnceTest = sinkSet.get(k);

            validSourceStmtInfo.setValidSrcAndIndexForSingleTest(
                    Collections.singleton(sourceStmtForOnceTest),  Collections.singleton(sinkStmtForOnceTest));

            MultiTaintPropagationResults propagationResults = runMultiAnalysis();
            Set<MultiAbstractionAtSink> res = propagationResults.getResults();
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

            boolean flag = false;

            for(int i = 0; i < sortedbyIndexRes.size(); i++) {
                Map.Entry<Pair<SourceContext, Stmt>, Integer> entry = sortedbyIndexRes.get(i);
                int index = entry.getValue();
                Pair<SourceContext, Stmt> srcSink = entry.getKey();
                String tmp = "result:[" + index + "]:" + srcSink.getO1() +" TO " + srcSink.getO2();
                logger.info(tmp);
                if(sourceStmtForOnceTest.equals(srcSink.getO1().getStmt()) && sinkStmtForOnceTest.equals(srcSink.getO2()))
                    flag = true;
            }

            if(flag) {
                logger.info("=====================The "+ (k+1) + " test run successfully!!!============");
                countSucceed++;
            }

//
//        for(Map.Entry<Pair<SourceContext, Stmt>, Integer> entry : resIndex.entrySet()) {
//            int index = entry.getValue();
//            Pair<SourceContext, Stmt> srcSink = entry.getKey();
//            String tmp = "(" + index + "):\n" + srcSink.getO1() +" TO \n" + srcSink.getO2();
//            logger.info(tmp);
//        }
            for(Map.Entry<Integer, Set<Integer>> entry : correlationRes.entrySet()) {
                String tmp = entry.getKey().toString() + ": [";
                for(int j : entry.getValue()) {
                    tmp += j + " ";
                }
                tmp += "]";
                logger.info(tmp);
            }


        }
        logger.info("=====================================================");
        if(countSucceed == resultSize){
            logger.info("=====MultiTest run successfully! ==============");
            logger.info("=====MultiTest Result: " + countSucceed +":" + resultSize);
            logger.info("0");
        }else {
            logger.info("=====MultiTest failed!!! ==============");
            logger.info("=====MultiTest Result: " + countSucceed +":" + resultSize);
            logger.info("1");
        }


    }

}
