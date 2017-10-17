package soot.jimple.multiinfoflow.solver.functions;

import heros.FlowFunction;
import soot.jimple.multiinfoflow.data.AbstractionVector;

import java.util.Set;

/**
 * Created by wanglei.
 */
public abstract class MultiSolverNormalFlowFunction implements FlowFunction<AbstractionVector> {

    public Set<AbstractionVector> computeTargets(AbstractionVector source) {
        return computeTargets(null, source);
    }

    public abstract Set<AbstractionVector> computeTargets(AbstractionVector d1, AbstractionVector d2);
}
