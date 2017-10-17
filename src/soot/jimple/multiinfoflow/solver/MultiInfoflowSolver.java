package soot.jimple.multiinfoflow.solver;

import heros.FlowFunction;
import heros.solver.CountingThreadPoolExecutor;
import heros.solver.Pair;
import heros.solver.PathEdge;
import soot.SootMethod;
import soot.Unit;
import soot.jimple.infoflow.solver.IFollowReturnsPastSeedsHandler;
import soot.jimple.infoflow.solver.fastSolver.IFDSSolver;
import soot.jimple.multiinfoflow.data.AbstractionVector;
import soot.jimple.multiinfoflow.problems.AbstractMultiInfoflowProblem;
import soot.jimple.multiinfoflow.solver.functions.MultiSolverCallFlowFunction;
import soot.jimple.multiinfoflow.solver.functions.MultiSolverCallToReturnFlowFunction;
import soot.jimple.multiinfoflow.solver.functions.MultiSolverNormalFlowFunction;
import soot.jimple.multiinfoflow.solver.functions.MultiSolverReturnFlowFunction;
import soot.jimple.toolkits.ide.icfg.BiDiInterproceduralCFG;

import java.util.Collection;
import java.util.Set;

/**
 * Created by wanglei on 16/10/10.
 */
public class MultiInfoflowSolver extends IFDSSolver<Unit, AbstractionVector, SootMethod, BiDiInterproceduralCFG<Unit, SootMethod>>
        implements IMultiInfoflowSolver {


    public MultiInfoflowSolver(AbstractMultiInfoflowProblem tabulationProblem,
                               CountingThreadPoolExecutor executor) {
        super(tabulationProblem);
        this.executor = executor;
        tabulationProblem.setSolver(this);
    }


    @Override
    protected CountingThreadPoolExecutor getExecutor() {
        return executor;
    }

    @Override
    public boolean processEdge(PathEdge<Unit, AbstractionVector> edge) {
        propagate(edge.factAtSource(), edge.getTarget(), edge.factAtTarget(), null, false, true);
        return false;
    }



    @Override
    public Set<Pair<Unit, AbstractionVector>> endSummary(SootMethod m, AbstractionVector d3) {
        return super.endSummary(m, d3);
    }

    @Override
    public void injectContext(IMultiInfoflowSolver otherSolver, SootMethod callee, AbstractionVector d3, Unit callSite, AbstractionVector d2, AbstractionVector d1) {

    }

    @Override
    public Set<AbstractionVector> computeReturnFlowFunction(
            FlowFunction<AbstractionVector> retFunction,
            AbstractionVector d1,
            AbstractionVector d2,
            Unit callSite,
            Collection<AbstractionVector> callerSideDs) {
        if (retFunction instanceof MultiSolverReturnFlowFunction) {
            // Get the d1s at the start points of the caller
            return ((MultiSolverReturnFlowFunction) retFunction).computeTargets(d2, d1, callerSideDs);
        }
        else
            return retFunction.computeTargets(d2);
    }

    @Override
    protected Set<AbstractionVector> computeNormalFlowFunction
            (FlowFunction<AbstractionVector> flowFunction, AbstractionVector d1, AbstractionVector d2) {
        if (flowFunction instanceof MultiSolverNormalFlowFunction)
            return ((MultiSolverNormalFlowFunction) flowFunction).computeTargets(d1, d2);
        else
            return flowFunction.computeTargets(d2);
    }

    @Override
    protected Set<AbstractionVector> computeCallToReturnFlowFunction
            (FlowFunction<AbstractionVector> flowFunction, AbstractionVector d1, AbstractionVector d2) {
        if (flowFunction instanceof MultiSolverCallToReturnFlowFunction)
            return ((MultiSolverCallToReturnFlowFunction) flowFunction).computeTargets(d1, d2);
        else
            return flowFunction.computeTargets(d2);
    }

    @Override
    protected Set<AbstractionVector> computeCallFlowFunction
            (FlowFunction<AbstractionVector> flowFunction, AbstractionVector d1, AbstractionVector d2) {
        if (flowFunction instanceof MultiSolverCallFlowFunction)
            return ((MultiSolverCallFlowFunction) flowFunction).computeTargets(d1, d2);
        else
            return flowFunction.computeTargets(d2);
    }

    @Override
    public void cleanup() {
        this.jumpFn.clear();
        this.incoming.clear();
        this.endSummary.clear();
    }

    @Override
    public void setFollowReturnsPastSeedsHandler(IFollowReturnsPastSeedsHandler handler) {

    }
}
