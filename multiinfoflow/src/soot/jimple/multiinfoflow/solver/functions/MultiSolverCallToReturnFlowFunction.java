package soot.jimple.multiinfoflow.solver.functions;

import heros.FlowFunction;
import soot.jimple.multiinfoflow.data.AbstractionVector;

import java.util.Set;

/**
 * Created by wanglei on 16/10/12.
 */
public abstract class MultiSolverCallToReturnFlowFunction implements FlowFunction<AbstractionVector> {

    @Override
    public Set<AbstractionVector> computeTargets(AbstractionVector source) {
        return computeTargets(null, source);
    }

    /**
     * Computes the abstractions at the return site
     * @param d1 The abstraction at the beginning of the caller, i.e. the
     * context in which the method call is made
     * @param d2 The abstraction at the call site
     * @return The set of abstractions at the first node inside the callee
     */
    public abstract Set<AbstractionVector> computeTargets(AbstractionVector d1, AbstractionVector d2);

}
