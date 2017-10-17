package soot.jimple.multiinfoflow.result;

import soot.jimple.infoflow.InfoflowConfiguration;
import soot.jimple.infoflow.android.source.AndroidSourceSinkManager;
import soot.jimple.infoflow.problems.TaintPropagationResults;
import soot.jimple.infoflow.solver.cfg.IInfoflowCFG;
import soot.jimple.multiinfoflow.MultiInfoflow;

/**
 * @author wanglei
 */
public class MultiAnalysisHandlerFactory {
    public static  MultiAnalysisHandlerFactory instance = new MultiAnalysisHandlerFactory();

    public static MultiAnalysisHandlerFactory v() {return instance;}

    public AbstractMultiAnalysisHandler createMultiAnalysisHandler(InfoflowConfiguration.MultiTaintMode mode,
                                                                   IInfoflowCFG iCfg,
                                                                   TaintPropagationResults propagationResults,
                                                                   MultiInfoflow multiInfoflow, AndroidSourceSinkManager sourcesSinks) {
        AbstractMultiAnalysisHandler multiInfoFlowHandler = null;

        switch (mode){
            case SoloModeForTest:
                multiInfoFlowHandler = new SingleSourceForTestAnalysisHandler(propagationResults,multiInfoflow) ;
                break;
            case DoubleCombination:
                break;
            case Custom:
                multiInfoFlowHandler = new CustomPairSourceAnalysisHandler(iCfg, propagationResults,multiInfoflow, (AndroidSourceSinkManager) sourcesSinks);
                break;
            case CustomALL:
                multiInfoFlowHandler = new CustomSourceAnalysisHandler(iCfg, propagationResults,multiInfoflow,  (AndroidSourceSinkManager)sourcesSinks);
                break;
            case AllCombination:
                multiInfoFlowHandler = new AllCombFasterAnalysisHandler(iCfg, propagationResults,multiInfoflow, (AndroidSourceSinkManager) sourcesSinks);
                break;
            case AllCombinationForTest:
                multiInfoFlowHandler = new AllCombinationForTestAnalysisHandler(propagationResults, multiInfoflow);
                break;
            case AllCombCallBackPartition:
                multiInfoFlowHandler = new AllCombCallBackPartitionAnalysisHandler(iCfg, propagationResults,multiInfoflow, (AndroidSourceSinkManager) sourcesSinks);

        }
        return multiInfoFlowHandler;
    }

}
