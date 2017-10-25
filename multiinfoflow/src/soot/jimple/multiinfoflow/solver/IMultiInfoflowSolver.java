package soot.jimple.multiinfoflow.solver;

import heros.solver.Pair;
import heros.solver.PathEdge;
import soot.SootMethod;
import soot.Unit;
import soot.jimple.infoflow.solver.IFollowReturnsPastSeedsHandler;
import soot.jimple.infoflow.solver.IMemoryManager;
import soot.jimple.multiinfoflow.data.AbstractionVector;

import java.util.Set;

/**
 * Created by wanglei on 16/10/13.
 */
public interface IMultiInfoflowSolver {

    /**
     * Schedules the given edge for processing in the solver
     * @param edge The edge to schedule for processing
     * @return True if the edge was scheduled, otherwise (e.g., if the edge has
     * already been processed earlier) false
     */
    public boolean processEdge(PathEdge<Unit, AbstractionVector> edge);

    /**
     * Gets the end summary of the given method for the given incoming
     * abstraction
     * @param m The method for which to get the end summary
     * @param d3 The incoming fact (context) for which to get the end summary
     * @return The end summary of the given method for the given incoming
     * abstraction
     */
    public Set<Pair<Unit, AbstractionVector>> endSummary(SootMethod m, AbstractionVector d3);

    public void injectContext(IMultiInfoflowSolver otherSolver, SootMethod callee, AbstractionVector d3,
                              Unit callSite, AbstractionVector d2, AbstractionVector d1);

    /**
     * Cleans up some unused memory. Results will still be available afterwards,
     * but no intermediate computation values.
     */
    public void cleanup();

    /**
     * Sets a handler that will be called when a followReturnsPastSeeds case
     * happens, i.e., a taint leaves a method for which we have not seen any
     * callers
     * @param handler The handler to be called when a followReturnsPastSeeds
     * case happens
     */
    public void setFollowReturnsPastSeedsHandler(IFollowReturnsPastSeedsHandler handler);

    /**
     * Gets the memory manager used by this solver to reduce memory consumption
     * @return The memory manager registered with this solver
     */
    public IMemoryManager<AbstractionVector> getMemoryManager();
}
