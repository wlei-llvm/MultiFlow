package soot.jimple.multiinfoflow.problems;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import soot.SootMethod;
import soot.Unit;
import soot.jimple.CaughtExceptionRef;
import soot.jimple.DefinitionStmt;
import soot.jimple.infoflow.solver.cfg.IInfoflowCFG;
import soot.jimple.multiinfoflow.MultiInfoflowManager;
import soot.jimple.multiinfoflow.data.AbstractionVector;
import soot.jimple.multiinfoflow.solver.IMultiInfoflowSolver;
import soot.jimple.toolkits.ide.DefaultJimpleIFDSTabulationProblem;
import soot.jimple.toolkits.ide.icfg.BiDiInterproceduralCFG;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * @author wanglei
 */
public abstract class AbstractMultiInfoflowProblem extends DefaultJimpleIFDSTabulationProblem<AbstractionVector,
        BiDiInterproceduralCFG<Unit, SootMethod>> {

    protected final MultiInfoflowManager manager;

    protected final Map<Unit, Set<AbstractionVector>> initialSeeds = new HashMap<>();

    protected final Logger logger = LoggerFactory.getLogger(getClass());

    private AbstractionVector zeroValue = null;

    protected IMultiInfoflowSolver solver = null;

    public AbstractMultiInfoflowProblem(MultiInfoflowManager manager) {
        super(manager.getICFG());
        this.manager = manager;

    }

    public void setSolver(IMultiInfoflowSolver solver) {
        this.solver = solver;
    }

    @Override
    public AbstractionVector createZeroValue() {
        //if (zeroValue == null)
        zeroValue = AbstractionVector.getZeroAbstractionVec();
        return zeroValue;
    }

    /**
     * performance improvement: since we start directly at the sources, we do not
     * need to generate additional taints unconditionally
     */
    @Override
    public boolean autoAddZero() {
        return false;
    }

    protected AbstractionVector getZeroValue() {
        return this.zeroValue;
    }

    @Override
    public boolean followReturnsPastSeeds() {
        return true;
    }

    @Override
    public Map<Unit, Set<AbstractionVector>> initialSeeds() {
        return this.initialSeeds;
    }

    public void addInitialSeeds(Unit unit, Set<AbstractionVector> seeds) {
        if(this.initialSeeds.containsKey(unit))
            this.initialSeeds.get(unit).addAll(seeds);
        else
            this.initialSeeds.put(unit, new HashSet<AbstractionVector>(seeds));
    }
    @Override
    public IInfoflowCFG interproceduralCFG() {
        return (IInfoflowCFG) super.interproceduralCFG();
    }


    protected boolean isExceptionHandler(Unit u) {
        if (u instanceof DefinitionStmt) {
            DefinitionStmt defStmt = (DefinitionStmt) u;
            return defStmt.getRightOp() instanceof CaughtExceptionRef;
        }
        return false;
    }

}

