package soot.jimple.multiinfoflow;

import soot.FastHierarchy;
import soot.SootMethod;
import soot.jimple.infoflow.InfoflowConfiguration;
import soot.jimple.infoflow.InfoflowManager;
import soot.jimple.infoflow.solver.IInfoflowSolver;
import soot.jimple.infoflow.solver.cfg.IInfoflowCFG;
import soot.jimple.infoflow.source.ISourceSinkManager;
import soot.jimple.infoflow.taintWrappers.ITaintPropagationWrapper;
import soot.jimple.multiinfoflow.solver.MultiInfoflowSolver;

import java.util.Set;

/**
 * Created by wanglei .
 */
public class MultiInfoflowManager extends InfoflowManager {

//    private final InfoflowConfiguration config;
    private MultiInfoflowSolver multiForwardSolver;

    private Set<SootMethod> callBackSet = null;
//    private final IInfoflowCFG icfg;
//    private final ISourceSinkManager sourceSinkManager;
//    private final ITaintPropagationWrapper taintWrapper;
//    private final TypeUtils typeUtils;
//    private final FastHierarchy hierarchy;

    public MultiInfoflowManager(InfoflowConfiguration config, IInfoflowSolver forwardSolver,
                                MultiInfoflowSolver multiForwardSolver,
                                IInfoflowCFG icfg,
                                ISourceSinkManager sourceSinkManager,
                                ITaintPropagationWrapper taintWrapper,
                                FastHierarchy hierarchy) {
        super(config, forwardSolver, icfg, sourceSinkManager, taintWrapper, hierarchy);
        this.multiForwardSolver = multiForwardSolver;
    }


    /**
     * Gets the IFDS solver that propagates edges forward
     * @return The IFDS solver that propagates edges forward
     */
    public MultiInfoflowSolver getMultiForwardSolver() {
        return this.multiForwardSolver;
    }

    public void setMultiForwardSolver( MultiInfoflowSolver multiForwardSolver ) {

        this.multiForwardSolver = multiForwardSolver;
    }

    /**
     * Gets the IFDS solver that propagates edges forward
     * @return The IFDS solver that propagates edges forward
     */
    public IInfoflowSolver getFirstRunForwardSolver() {
        return this.getForwardSolver();
    }

    public void setFirstRunForwardSolver( IInfoflowSolver forwardSolver ) {

        this.setForwardSolver(forwardSolver);
    }

    public void setCallBackSet(Set<SootMethod> callBackSet) {
        this.callBackSet = callBackSet;
    }
    public Set<SootMethod> getCallBackSet() {
        return this.callBackSet;
    }



}
