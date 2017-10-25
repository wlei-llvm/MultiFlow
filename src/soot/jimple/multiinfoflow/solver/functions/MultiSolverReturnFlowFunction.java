package soot.jimple.multiinfoflow.solver.functions;

import heros.FlowFunction;
import soot.jimple.multiinfoflow.data.AbstractionVector;

import java.util.Collection;
import java.util.Collections;
import java.util.Set;

/**
 * Created by wanglei .
 */
public abstract class MultiSolverReturnFlowFunction implements FlowFunction<AbstractionVector> {
    @Override
    public Set<AbstractionVector> computeTargets(AbstractionVector source) {
        return computeTargets(source, null, Collections.<AbstractionVector>emptySet());
    }

    /**
     * Computes the abstractions at the return site.
     * @param source The abstraction at the exit node
     * @param calleeD1 The abstraction at the start point of the callee
     * @param callerD1s The abstractions at the start nodes of all methods to
     * which we return (i.e. the contexts to which this flow function will be
     * applied).
     * @return The set of abstractions at the return site.
     */
    public abstract Set<AbstractionVector> computeTargets(AbstractionVector source,
                                                          AbstractionVector calleeD1,
                                                          Collection<AbstractionVector> callerD1s);
}
