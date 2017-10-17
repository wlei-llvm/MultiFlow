package soot.jimple.multiinfoflow.result;

import heros.solver.Pair;
import soot.SootMethod;
import soot.jimple.Stmt;
import soot.jimple.infoflow.collect.MyConcurrentHashMap;
import soot.jimple.infoflow.data.Abstraction;
import soot.jimple.infoflow.data.AbstractionAtSink;
import soot.jimple.infoflow.data.SourceContext;
import soot.jimple.infoflow.problems.TaintPropagationResults;
import soot.jimple.infoflow.util.SystemClassHandler;
import soot.jimple.multiinfoflow.MultiInfoflowManager;
import soot.jimple.multiinfoflow.data.AbstractionVector;
import soot.jimple.multiinfoflow.data.MultiAbstractionAtSink;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Created by wanglei .
 */
public class MultiTaintPropagationResults {

    /**
     * Handler interface that is invoked when new taint propagation results are
     * added to the result object
     */
    public interface OnTaintPropagationResultAdded {

        /**
         * Called when a new abstraction has reached a sink statement
         * @param abs The abstraction at the sink
         * @return True if the data flow analysis shall continue, otherwise false
         */
        public boolean onResultAvailable(AbstractionAtSink abs);

    }

    private final  Map<Pair<SourceContext,Stmt>, Integer> resultToIndex = new HashMap<>();

    private  Map<SootMethod, AbstractionVector> methodEndSummary ;

    protected final MultiInfoflowManager manager;

    public long time = 0;

    protected final MyConcurrentHashMap<AbstractionAtSink, Abstraction> results =
            new MyConcurrentHashMap<AbstractionAtSink, Abstraction>();

    protected final MyConcurrentHashMap<MultiAbstractionAtSink, Abstraction> multiResults =
            new MyConcurrentHashMap<MultiAbstractionAtSink, Abstraction>();

    protected final Set<TaintPropagationResults.OnTaintPropagationResultAdded> resultAddedHandlers = new HashSet<>();

    /**
     * Creates a new instance of the TaintPropagationResults class
     * @param manager A reference to the manager class used during taint
     * propagation
     */
    public MultiTaintPropagationResults(MultiInfoflowManager manager) {
        this.manager = manager;
    }

    public boolean addResult(MultiAbstractionAtSink resultAbsPair) {
        this.multiResults.put(resultAbsPair, Abstraction.getZeroAbstraction(true));
        return true;
    }
    public boolean addResult(AbstractionVector resultAbsPair, Stmt stmt) {
        addResult(new MultiAbstractionAtSink(resultAbsPair, stmt));
        return true;
    }


    /**
     * Adds a new result of the data flow analysis to the collection
     * @param resultAbs The abstraction at the sink instruction
     * @return True if the data flow analysis shall continue, otherwise false
     */
    public boolean addResult(AbstractionAtSink resultAbs) {
        // Check whether we need to filter a result in a system package
        if (manager.getConfig().getIgnoreFlowsInSystemPackages() && SystemClassHandler.isClassInSystemPackage
                (manager.getICFG().getMethodOf(resultAbs.getSinkStmt()).getDeclaringClass().getName()))
            return true;

        // Construct the abstraction at the sink
        Abstraction abs = resultAbs.getAbstraction();
        abs = abs.deriveNewAbstraction(abs.getAccessPath(), resultAbs.getSinkStmt());
        abs.setCorrespondingCallSite(resultAbs.getSinkStmt());

        // Reduce the incoming abstraction
//        IMemoryManager<Abstraction> memoryManager = manager.getForwardSolver().getMemoryManager();
//        if (memoryManager != null) {
//            abs = memoryManager.handleMemoryObject(abs);
//            if (abs == null)
//                return true;
//        }

        // Record the result
        resultAbs = new AbstractionAtSink(abs, resultAbs.getSinkStmt());
        Abstraction newAbs = this.results.putIfAbsentElseGet
                (resultAbs, resultAbs.getAbstraction());
        if (newAbs != resultAbs.getAbstraction())
            newAbs.addNeighbor(resultAbs.getAbstraction());

        // Notify the handlers
        boolean continueAnalysis = true;
        for (TaintPropagationResults.OnTaintPropagationResultAdded handler : resultAddedHandlers)
            if (!handler.onResultAvailable(resultAbs))
                continueAnalysis = false;
        return continueAnalysis;
    }

    /**
     * Checks whether this result object is empty
     * @return True if this result object is empty, i.e., there are no results
     * yet, otherwise false
     * @return
     */
    public boolean isEmpty() {
        return this.results.isEmpty();
    }

    /**
     * Gets all results collected in this data object
     * @return All data flow results collected in this object
     */
//    public Set<AbstractionAtSink> getResults() {
//        return this.results.keySet();
//    }

    public void setMethodEndSummary(Map<SootMethod, AbstractionVector> methodEndSummary) {
        this.methodEndSummary = methodEndSummary;
    }

    public Set<MultiAbstractionAtSink> getResults() {return this.multiResults.keySet();};

    public void setTime(long time) {
        this.time = time;
    }

    public long getTime() {
        return this.time;
    }

    public Map<Integer, Set<Integer>> computeResults() {


        int globalIndex = 1;
        Map<Integer, Set<Integer>> multiResultMap = new HashMap<>();

        for(MultiAbstractionAtSink key : multiResults.keySet()) {
            AbstractionVector resAbsVec = key.getAbstractionVector();
            Stmt sinkStmt = key.getSinkStmt();
            HashSet<Pair<SourceContext, Stmt>> correlationSrcSinkSet = new HashSet<>();
            for(Map.Entry<Integer, Abstraction> entry : resAbsVec.getEntrySet()) {
                Abstraction abs = entry.getValue();
                if(AbstractionVector.isEndAbstraction(abs)) {
                    correlationSrcSinkSet.add(new Pair<SourceContext, Stmt>(abs.getEndAbstraction().getSourceContext(), abs.getEndSinkStmt()));
                }
            }

            //computeZeroSetByDfs(resAbsVec, correlationSrcSinkSet );

            //
            for(Pair<SourceContext, Stmt> srcSink : correlationSrcSinkSet) {
                if(!resultToIndex.containsKey(srcSink)) {
                    resultToIndex.put(srcSink , globalIndex);
                    globalIndex++;
                }
                for(Pair<SourceContext, Stmt> srcSink2 : correlationSrcSinkSet) {

                    if(!resultToIndex.containsKey(srcSink2)) {
                        resultToIndex.put(srcSink2 , globalIndex);
                        globalIndex++;
                    }

                    if(!srcSink.equals(srcSink2)) {
                        int index1 = resultToIndex.get(srcSink);
                        int index2 = resultToIndex.get(srcSink2);
                        if(!multiResultMap.containsKey(index1) || multiResultMap.get(index1) == null) {
                            Set<Integer> tmpSet = new HashSet<>();
                            tmpSet.add(index2);
                            multiResultMap.put(index1, tmpSet);

                        } else {
                            Set<Integer> tmpSet = multiResultMap.get(index1);
                            tmpSet.add(index2);
                        }

                        if(!multiResultMap.containsKey(index2) || multiResultMap.get(index2) == null) {
                            Set<Integer> tmpSet = new HashSet<>();
                            tmpSet.add(index1);
                            multiResultMap.put(index2, tmpSet);

                        } else {
                            Set<Integer> tmpSet = multiResultMap.get(index2);
                            tmpSet.add(index1);
                        }


                    }
                }
            }

        }


        return multiResultMap;

    }

//    private void computeZeroSetByDfs(AbstractionVector absV, Set<Pair<SourceContext, Stmt>> correlationSrcSinkSet ) {
//        if(absV.getZeroGenAbstractionVec() != null) {
//            Set<Pair<SootMethod, AbstractionVector>> zeroGenAbstractionVec = absV.getZeroGenAbstractionVec();
//
//            for(Pair<SootMethod, AbstractionVector> pair : zeroGenAbstractionVec) {
//                AbstractionVector newGenAbsV =  methodEndSummary.get(pair.getO1());
//                if(newGenAbsV != null) {
//                    for(int i = 0; i < AbstractionVector.ABS_VECTOR_SIZE; i++) {
//                        Abstraction abs = newGenAbsV.getAbstractionByIndex(i);
//                        if(AbstractionVector.isEndAbstraction(abs)) {
//                            correlationSrcSinkSet.add(new Pair<SourceContext, Stmt>(abs.getPredecessor().getSourceContext(), abs.getCorrespondingCallSite()));
//                        }
//                    }
//                }
//            }
//        }
//        if(absV.getPredecessor() != null)
//            computeZeroSetByDfs(absV.getPredecessor(), correlationSrcSinkSet);
//
//        if(absV.getNeighbors() != null) {
//            for(AbstractionVector neighbor: absV.getNeighbors()) {
//                computeZeroSetByDfs(neighbor, correlationSrcSinkSet);
//            }
//        }
//
//    }

    public Map<Pair<SourceContext,Stmt>, Integer> getResultToIndex() {
        return this.resultToIndex;
    }


    /**
     * Adds a new handler that is invoked when a new data flow result is added
     * to this data object
     * @param handler The handler implementation to add
     */
    public void addResultAvailableHandler(TaintPropagationResults.OnTaintPropagationResultAdded handler) {
        this.resultAddedHandlers.add(handler);
    }

}
