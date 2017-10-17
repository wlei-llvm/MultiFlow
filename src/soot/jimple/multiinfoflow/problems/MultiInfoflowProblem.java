package soot.jimple.multiinfoflow.problems;

import heros.DontSynchronize;
import heros.FlowFunction;
import heros.FlowFunctions;
import heros.solver.Pair;
import heros.solver.PathEdge;
import soot.SootMethod;
import soot.Unit;
import soot.jimple.Stmt;
import soot.jimple.infoflow.collect.ConcurrentHashSet;
import soot.jimple.infoflow.data.Abstraction;
import soot.jimple.infoflow.data.SourceContext;
import soot.jimple.multiinfoflow.MultiInfoflow;
import soot.jimple.multiinfoflow.MultiInfoflowManager;
import soot.jimple.multiinfoflow.data.AbstractionVector;
import soot.jimple.multiinfoflow.result.MultiTaintPropagationResults;
import soot.jimple.multiinfoflow.solver.functions.MultiSolverCallFlowFunction;
import soot.jimple.multiinfoflow.solver.functions.MultiSolverCallToReturnFlowFunction;
import soot.jimple.multiinfoflow.solver.functions.MultiSolverNormalFlowFunction;
import soot.jimple.multiinfoflow.solver.functions.MultiSolverReturnFlowFunction;

import java.util.*;

/**
 * @author wanglei
 */
public class MultiInfoflowProblem extends AbstractMultiInfoflowProblem {

    private Map<Stmt, Set<Abstraction>> seedSourceAbstraction = null;

    private Map<Stmt, Set<Abstraction>> seedSinkAbstraction = null;

    private  MultiTaintPropagationResults results;

//    private Set<AbstractionVector> result = new ConcurrentHashSet<AbstractionVector>();

    private  ConcurrentHashSet<SourceContextSet> visitedVectorSourceInfoSet = new ConcurrentHashSet<>();


    public MultiInfoflowProblem(MultiInfoflowManager manager) {
        super(manager);
        stopAnalysis = false;
    }


    public MultiTaintPropagationResults getResults() {
        return this.results;
    }

    public void setResults(MultiTaintPropagationResults results) {
        this.results =  results;
    }

    public void setSeedSourceAbstraction ( Map<Stmt, Set<Abstraction>> seedSourceAbstraction) {
        this.seedSourceAbstraction = seedSourceAbstraction;
    }

    public void setSeedSinkAbstraction(Map<Stmt, Set<Abstraction>> seedSinkAbstraction) {
        this.seedSinkAbstraction = seedSinkAbstraction;
    }

    @DontSynchronize
    boolean stopAnalysis = false;


    private class SourceContextSet {

        private int hashCode = 0;

        Map<Integer, SourceContext> innerMap = new HashMap();

        SourceContextSet(Map<Integer, SourceContext> sourceContextMap) {
            innerMap.putAll(sourceContextMap);
        }

        void put(int index , SourceContext sourceContext) {
            innerMap.put(index, sourceContext);
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj)
                return true;
            if (obj == null || getClass() != obj.getClass())
                return false;
            SourceContextSet other = (SourceContextSet) obj;

            if(other.innerMap.size() != this.innerMap.size())
                return false;
            if(other.innerMap.size() == 0)
                return true;
            for(Map.Entry<Integer, SourceContext> entry :innerMap.entrySet()) {
                int i = entry.getKey();
                SourceContext sourceContext = entry.getValue();
                if(other.innerMap.get(i) == null && sourceContext != null)
                    return false;
                if(!other.innerMap.get(i).equals(sourceContext))
                    return false;
            }

            for(Map.Entry<Integer, SourceContext> entry :other.innerMap.entrySet()) {
                int i = entry.getKey();
                SourceContext sourceContext = entry.getValue();
                if(innerMap.get(i) == null && sourceContext != null)
                    return false;
                if(!this.innerMap.get(i).equals(sourceContext))
                    return false;
            }

            return true;
        }

        @Override
        public int hashCode() {
            if (this.hashCode != 0)
                return hashCode;
            final int prime = 31;
            int result = 1;

            for(Map.Entry<Integer, SourceContext> entry : innerMap.entrySet())  {
                int i = entry.getKey();
                SourceContext sourceContext = entry.getValue();
                result = result +  sourceContext.hashCode() + i * prime;
            }

            this.hashCode = result;

            return this.hashCode;
        }


    }


    @Override
    protected FlowFunctions<Unit, AbstractionVector, SootMethod> createFlowFunctionsFactory() {
        return new FlowFunctions<Unit, AbstractionVector, SootMethod>() {

            abstract class MultiNormalFlowFunction extends MultiSolverNormalFlowFunction {

                protected final Stmt stmt;

                public MultiNormalFlowFunction(Stmt stmt) {
                    this.stmt = stmt;
                }

                public Set<AbstractionVector> computeTargets(AbstractionVector d1, AbstractionVector source) {
                    if(stopAnalysis)
                        return Collections.emptySet();

                    Set<AbstractionVector> res = computeTargetsInternal(d1, source);
                    if(MultiInfoflow.isCustomPairMode(manager.getConfig().getMultiTaintMode())) {
                        for(AbstractionVector absVec : res) {
                            if(absVec.isSameAccessPath()) {
                                AbstractionVector allEnd = absVec.deriveAllEndMeetSameAP(stmt);
                                getResults().addResult(allEnd, stmt);
                                stopAnalysis = true;
                                return Collections.emptySet();
                            }
                            if(absVec.isAllEndAbs() && absVec.getIndexSet().size() == 2)
                                return Collections.emptySet();
                        }
                    }
                    return res;
                }

                public Set<AbstractionVector> computeTargetsInternal(AbstractionVector d1, AbstractionVector source) {

                    Set<AbstractionVector> res = new HashSet<>();
                    res.add(source);
                    for (int index : source.getIndexSet()) {
                        Set<AbstractionVector> inLoopSet = new HashSet<>();
                        inLoopSet.addAll(res);
                        for (AbstractionVector absVec : inLoopSet) {
                            Abstraction abs = absVec.getAbstractionByIndex(index);
                            Set<Abstraction> newAbsSet = computeSingleTarget(abs);
                            if (newAbsSet.contains(abs) && newAbsSet.size() == 1) {
                                res.add(absVec);
                            } else {
                                Set<AbstractionVector> newRes = absVec.deriveNewAbstractionVecSetByAbsSet(newAbsSet, index);
                                res.addAll(newRes);
                            }

                        }
                    }
                    Set<AbstractionVector> sourceGeneratedSet = new HashSet<>();
                    for(AbstractionVector absVec : res) {
                        int sourceIndex = MultiInfoflow.validSourceStmtInfo.getSourceIndex((Stmt) stmt);
                        if(sourceIndex != -1) {
                            Set<Abstraction> absSet = seedSourceAbstraction.get(stmt);
                            Set<AbstractionVector> newAbsVecSet = absVec.deriveNewAbstractionVecSetByAbsSet(absSet, sourceIndex);
                            sourceGeneratedSet.addAll(newAbsVecSet);
                        }
                    }
                    res.addAll(sourceGeneratedSet);
                    res.remove(getZeroValue());

                    Set<AbstractionVector> aliasGeneratedSet = new HashSet<>();
                    aliasGeneratedSet.addAll(res);
                    for (int index : source.getIndexSet()) {
                        Set<AbstractionVector> inLoopSet = new HashSet<>();
                        inLoopSet.addAll(aliasGeneratedSet);
                        for (AbstractionVector absVec : inLoopSet) {
                            Abstraction abs = absVec.getAbstractionByIndex(index);
                            Set<Abstraction> newAbsSet = computeAliases(abs);

                            Set<AbstractionVector> newRes = absVec.deriveNewAbstractionVecSetByAbsSet(newAbsSet, index);
                            aliasGeneratedSet.addAll(newRes);

                        }
                    }
                    res.addAll(aliasGeneratedSet);


                    return res;
                }

                public abstract Set<Abstraction> computeSingleTarget(Abstraction source);
                public abstract Set<Abstraction> computeAliases(Abstraction source);

            }


            @Override
            public FlowFunction<AbstractionVector> getNormalFlowFunction(final Unit curr, Unit succ) {

                return new MultiNormalFlowFunction((Stmt) curr) {

                    @Override
                    public Set<Abstraction> computeSingleTarget(Abstraction source) {

                        Set<Abstraction> res = new HashSet<>();

                        Map<Stmt, Set<Abstraction>> normalMap = source.getNormalFlowMap();
                        if (normalMap != null && normalMap.containsKey(curr)) {
                            res.addAll(normalMap.get(curr));
                        }

                        return res;
                    }
                    @Override
                    public Set<Abstraction> computeAliases(Abstraction source) {

                        Set<Abstraction> res = new HashSet<>();

                        Map<Unit, Set<Abstraction>> aliasMap = source.getAliasFlowMap();
                        if (aliasMap != null && aliasMap.containsKey(curr)) {
                            res.addAll(aliasMap.get(curr));
                        }

                        return res;
                    }

                };
            }

            abstract class MultiCallFlowFunction extends MultiSolverCallFlowFunction {

                protected final Stmt stmt;
                protected final SootMethod dest;

                public MultiCallFlowFunction(Stmt stmt, SootMethod dest) {
                    this.stmt = stmt;
                    this.dest = dest;
                }

                public Set<AbstractionVector> computeTargets(AbstractionVector d1, AbstractionVector source) {
                    if(stopAnalysis)
                        return Collections.emptySet();
                    Set<AbstractionVector> res = computeTargetsInternal(d1, source);
                    if(MultiInfoflow.isCustomPairMode(manager.getConfig().getMultiTaintMode())) {
                        for(AbstractionVector absVec : res) {
                            if(absVec.isSameAccessPath()) {
                                AbstractionVector allEnd = absVec.deriveAllEndMeetSameAP(stmt);
                                getResults().addResult(allEnd, stmt);
                                stopAnalysis = true;
                                return Collections.emptySet();

                            }
                            if(absVec.isAllEndAbs() && absVec.getIndexSet().size() == 2)
                                return Collections.emptySet();

                        }
                    }
                    return res;
                }

                public Set<AbstractionVector> computeTargetsInternal(AbstractionVector d1, AbstractionVector source) {



                    Set<AbstractionVector> res = new HashSet<>();

                    //
                    Map<Integer, Set<Abstraction>> indexToAbsSetMapping = new HashMap<>();

                    for (int index : source.getIndexSet()) {
                        Abstraction abs = source.getAbstractionByIndex(index);

                        Set<Abstraction> newAbsSet = computeSingleTarget(abs);

                        if (newAbsSet != null && !newAbsSet.isEmpty())
                            indexToAbsSetMapping.put(index, newAbsSet);
                    }

                    if(indexToAbsSetMapping.size() == 3) {
                        boolean isFound = false;
                        for(int index : indexToAbsSetMapping.keySet()) {
                            Abstraction abs = source.getAbstractionByIndex(index);
//
//                            Set<Pair<Unit, Abstraction>> summary =  manager.getFirstRunForwardSolver().endSummary(dest , abs);
//
//                            if(summary == null)
//                                continue;

                            Set<Pair<Unit, Abstraction>> newAbsSet = computeSummary(abs);

                            if (newAbsSet != null && !newAbsSet.isEmpty()) {
                                Set<Pair<Unit, AbstractionVector>> endSummary = new HashSet<>();
                                for(Pair<Unit, Abstraction> newAbsPair : newAbsSet) {
                                    AbstractionVector newAbsVec = source.deriveNewAbstractionVec(newAbsPair.getO2(), index);
                                    endSummary.add(new Pair<Unit, AbstractionVector>(newAbsPair.getO1(), newAbsVec));
                                }
                                if(!endSummary.isEmpty())
                                    for(Pair<Unit, AbstractionVector> entry: endSummary) {
                                        isFound = true;
                                        Unit eP = entry.getO1();
                                        AbstractionVector d4 = entry.getO2();
                                        if(d4.isAllEndAbs()) {
                                            getResults().addResult(d4, stmt);
                                            //System.out.println("multi summary END!");
                                            continue;
                                        }
                                        //for each return site
                                        for(Unit retSiteN: interproceduralCFG().getReturnSitesOfCallAt(stmt)) {
//                                            //compute return-flow function
//                                            FlowFunction<AbstractionVector> retFunction = flowFunctions().getReturnFlowFunction(stmt, dest, eP, retSiteN);
//                                            //for each target value of the function
//                                            for(AbstractionVector d5: manager.getMultiForwardSolver().computeReturnFlowFunction(retFunction, source, d4, stmt, Collections.singleton(d1))) {
//                                                manager.getMultiForwardSolver().processEdge(new PathEdge<Unit, AbstractionVector>(d1, retSiteN, d5));
//                                                isFound = true;
//                                            }
                                            manager.getMultiForwardSolver().processEdge(new PathEdge<Unit, AbstractionVector>(d1, retSiteN, d4));

                                        }
                                    }


                            }
//                                indexToAbsSetMapping.put(index, newAbsSet);
                        }
                        if(isFound)
                            return Collections.emptySet();
                    }



                    Set<AbstractionVector> inLoopSet = new HashSet<>();
                    inLoopSet.add(source);

                    for (Map.Entry<Integer, Set<Abstraction>> entry : indexToAbsSetMapping.entrySet()) {
                        //System.out.println("multi  CALL NONO!" + stmt);
                        int index = entry.getKey();
                        Set<Abstraction> absSet = entry.getValue();
                        for (Abstraction abs : absSet) {

                            for (AbstractionVector absVec : inLoopSet) {
                                AbstractionVector newAbs = absVec.deriveNewAbstractionVec(abs, index);
                                if (newAbs != null)
                                    res.add(newAbs);
                            }
                        }
                        inLoopSet.clear();
                        inLoopSet.addAll(res);

                    }


                    return res;
                }

                public abstract Set<Abstraction> computeSingleTarget(Abstraction source);
                public abstract Set<Pair<Unit, Abstraction>> computeSummary(Abstraction source);

            }



            @Override
            public FlowFunction<AbstractionVector> getCallFlowFunction(final Unit callStmt, final SootMethod destinationMethod) {
                return new MultiCallFlowFunction((Stmt) callStmt, destinationMethod) {
                    @Override
                    public Set<Abstraction> computeSingleTarget(Abstraction source) {

                        Set<Abstraction> res = new HashSet<>();

                        Map<Pair<Unit, SootMethod>, Set<Abstraction>> callMap = source.getCallFlowMap();
                        Pair<Unit, SootMethod> key = new Pair<>(callStmt, destinationMethod);
                        if (callMap != null && callMap.containsKey(key)) {
                            res.addAll(callMap.get(key));
                        } else
                            return Collections.emptySet();

                        return res;
                    }

                    @Override
                    public Set<Pair<Unit, Abstraction>> computeSummary(Abstraction source) {

                        Set<Pair<Unit, Abstraction>> res = new HashSet<>();

                        Map<Unit, Set<Pair<Unit, Abstraction>>> summary = source.getSummaryFlowMap();
                        if (summary != null && summary.containsKey(callStmt)) {
                            res.addAll(summary.get(callStmt));
//                            for(Abstraction abs : summary.get(key)) {
//                                MyConcurrentHashMap<Pair<Unit, Unit>, Set<Abstraction>> returnFlowMap = (MyConcurrentHashMap) abs.getReturnFlowMap();
//                                Pair<Unit, Unit> retKey = new Pair<>(callStmt, null);
//                                Set<Abstraction> abstractionSet = returnFlowMap.putIfAbsentElseGet(retKey, new ConcurrentHashSet<Abstraction>());
//                                abstractionSet.add(abs);
//                            }

                        } else
                            return Collections.emptySet();

                        return res;
                    }

                };
            }

             abstract class MultiReturnFlowFunction extends MultiSolverReturnFlowFunction {

                protected final Stmt stmt;

                public MultiReturnFlowFunction(Stmt stmt) {
                    this.stmt = stmt;
                }

                 @Override
                 public Set<AbstractionVector> computeTargets(AbstractionVector source, AbstractionVector calleeD1, Collection<AbstractionVector> callerD1s) {
                     if(stopAnalysis)
                         return Collections.emptySet();
                     Set<AbstractionVector> res = computeTargetsInternal(source, calleeD1, callerD1s);

                     //optimize the same ap in the vector, we directly output the sink.
                     if(MultiInfoflow.isCustomPairMode(manager.getConfig().getMultiTaintMode())) {
                         for(AbstractionVector absVec : res) {
                             if(absVec.isSameAccessPath()) {
                                 AbstractionVector allEnd = absVec.deriveAllEndMeetSameAP(stmt);
                                 getResults().addResult(allEnd, stmt);
                                 stopAnalysis = true;
                                 return Collections.emptySet();
                             }
                             if(absVec.isAllEndAbs() && absVec.getIndexSet().size() == 2)
                                 return Collections.emptySet();

                         }

                     }
                     return res;
                 }


                public Set<AbstractionVector> computeTargetsInternal(AbstractionVector source, AbstractionVector calleeD1, Collection<AbstractionVector> callerD1s) {

                    Set<AbstractionVector> res = new HashSet<>();

                    Map<Integer, Set<Abstraction>> indexToAbsSetMapping = new HashMap<>();

                    for (int index : source.getIndexSet()) {
                        Abstraction abs = source.getAbstractionByIndex(index);

                        Set<Abstraction> newAbsSet = computeSingleTarget(abs);

                        if (newAbsSet != null && !newAbsSet.isEmpty())
                            indexToAbsSetMapping.put(index, newAbsSet);
                    }

                    Set<AbstractionVector> inLoopSet = new HashSet<>();
                    inLoopSet.add(source);

                    for (Map.Entry<Integer, Set<Abstraction>> entry : indexToAbsSetMapping.entrySet()) {
                        int index = entry.getKey();
                        Set<Abstraction> absSet = entry.getValue();
                        for (Abstraction abs : absSet) {
                            for (AbstractionVector absVec : inLoopSet) {
                                AbstractionVector newAbs = absVec.deriveNewAbstractionVec(abs, index);
                                if (newAbs != null)
                                    res.add(newAbs);
                            }
                        }
                        inLoopSet.clear();
                        inLoopSet.addAll(res);

                    }

                    return res;
                }

                public abstract Set<Abstraction> computeSingleTarget(Abstraction source);


             }

             boolean isCallUsed (AbstractionVector absVec, Stmt callStmt) {
                 for(int index : absVec.getIndexSet()) {
                     Abstraction abs = absVec.getAbstractionByIndex(index);
                         for(SootMethod method : manager.getICFG().getCalleesOfCallAt(callStmt)) {
                             Pair<Stmt, SootMethod> key = new Pair<>(callStmt, method);
                             if(abs.getCallFlowMap() != null && abs.getCallFlowMap().containsKey(key)) {
                              return true;
                             }
                         }
                 }
                 return false;
             }

            @Override
            public FlowFunction<AbstractionVector> getReturnFlowFunction(final Unit callSite, final SootMethod calleeMethod, Unit exitStmt, final Unit returnSite) {
                return new MultiReturnFlowFunction((Stmt)callSite) {

                    @Override
                    public Set<Abstraction> computeSingleTarget(Abstraction source) {
                        Set<Abstraction> res = new HashSet<>();

                        if(manager.getConfig().isEnableCallBackEnd() && AbstractionVector.isEndAbstraction(source) ) {
                            if(manager.getCallBackSet().contains(calleeMethod)) {
                                return Collections.emptySet();
                            }else {
                                return Collections.singleton(source);
                            }
                        }

                        Map<Pair<Unit, Unit>, Set<Abstraction>> returnFlowMap = source.getReturnFlowMap();
                        Pair<Unit, Unit> key = new Pair<>(callSite, returnSite);
                        Pair<Unit, Unit> aliasKey = new Pair<>(callSite, null);
                        if (returnFlowMap != null && returnFlowMap.containsKey(key)) {
                            res.addAll(returnFlowMap.get(key));
                        } else if (returnSite != null && returnFlowMap != null && returnFlowMap.containsKey(aliasKey)) {
                            res.addAll(returnFlowMap.get(aliasKey));
                        } else
                            return Collections.emptySet();

                        return res;
                    }

                };
            }


            abstract class MultiCallToReturnFlowFunction extends MultiSolverCallToReturnFlowFunction {

                protected final Stmt stmt;

                public MultiCallToReturnFlowFunction(Stmt stmt) {
                    this.stmt = stmt;
                }

                public Set<AbstractionVector> computeTargets(AbstractionVector d1, AbstractionVector source) {
                    if(stopAnalysis)
                        return Collections.emptySet();

                    Set<AbstractionVector> res = computeTargetsInternal(d1, source);
                    if(MultiInfoflow.isCustomPairMode(manager.getConfig().getMultiTaintMode())) {
                        for(AbstractionVector absVec : res) {
                            if(absVec.isSameAccessPath()) {
                                AbstractionVector allEnd = absVec.deriveAllEndMeetSameAP(stmt);
                                getResults().addResult(allEnd, stmt);
                                stopAnalysis = true;
                                return Collections.emptySet();
                            }
                            if(absVec.isAllEndAbs() && absVec.getIndexSet().size() == 2)
                                return Collections.emptySet();
                        }

                    }
                    return res;
                }

                public Set<AbstractionVector> computeTargetsInternal(AbstractionVector d1, AbstractionVector source) {



                    Set<AbstractionVector> res = new HashSet<>();
                    res.add(source);
                    for (int index : source.getIndexSet()) {
                        Set<AbstractionVector> inLoopSet = new HashSet<>();
                        inLoopSet.addAll(res);
                        //res.clear();
                        for (AbstractionVector absVec : inLoopSet) {
                            Abstraction abs = absVec.getAbstractionByIndex(index);
                            Set<Abstraction> newAbsSet = computeSingleTarget(abs);

                            Set<AbstractionVector> newRes = absVec.deriveNewAbstractionVecSetByAbsSet(newAbsSet, index);
                            res.addAll(newRes);

                        }
                    }

                    boolean isCallUsedFlag = isCallUsed(source, stmt);
                    Set<AbstractionVector> sourceGeneratedSet = new HashSet<>();
                    for(AbstractionVector absVec : res) {
                        Set<Abstraction> absSet = seedSourceAbstraction.get(stmt);
                        if(absSet != null && !absSet.isEmpty()) {
                            Set<AbstractionVector> newAbsVecSet = new HashSet<>();
                            for(Abstraction abs : absSet) {
                                int sourceIndex = MultiInfoflow.validSourceStmtInfo.getSourceIndex(abs.getSourceContext().getStmt());

                                if(sourceIndex != -1){

                                    SourceContextSet sourceContextSet = new SourceContextSet(absVec.getSourceContextMap());

                                    sourceContextSet.put(sourceIndex, abs.getSourceContext());

                                    if(visitedVectorSourceInfoSet.contains(sourceContextSet)) {
                                        continue;
                                    }

                                    if(MultiInfoflow.isCustomPairMode(manager.getConfig().getMultiTaintMode())&&
                                            absVec.getSourceContextMap().containsValue(sourceContextSet))
                                        continue;

                                    //multicheck!!!
                                    if(stmt.equals(abs.getSourceContext().getStmt()) || !isCallUsedFlag ) {
                                        //System.out.println("Multi source generate!" + abs);
                                        newAbsVecSet.add(absVec.deriveNewAbstractionVec(abs, sourceIndex));
                                        visitedVectorSourceInfoSet.add(sourceContextSet);
                                    }
                                }
                            }
                            if (!newAbsVecSet.isEmpty())
                                sourceGeneratedSet.addAll(newAbsVecSet);
                        }

//                        int sourceIndex = MultiInfoflow.validSourceStmtInfo.getSourceIndex((Stmt) stmt);
//                        if(sourceIndex != -1) {
//                            Set<Abstraction> absSet = seedSourceAbstraction.get(stmt);
//                            Set<AbstractionVector> newAbsVecSet = absVec.deriveNewAbstractionVecSetByAbsSet(absSet, sourceIndex);
//                            sourceGeneratedSet.addAll(newAbsVecSet);
//                        }
                    }
                    res.addAll(sourceGeneratedSet);
                    res.remove(getZeroValue());


                    if(MultiInfoflow.validSourceStmtInfo.isValidSink(stmt)) {

                        Set<AbstractionVector> tmpSet = new HashSet<>();
                        tmpSet.addAll(res);
                        for(AbstractionVector absVec : tmpSet) {
                            Set<Integer> endStmtToIndexMapping = new HashSet<>() ;
                            for(int index : absVec.getIndexSet()) {
                                Abstraction abs =  absVec.getAbstractionByIndex(index);
                                if(seedSinkAbstraction.get(stmt).contains(abs)) {
                                    endStmtToIndexMapping.add(index);
                                }
                            }
                            if(!endStmtToIndexMapping.isEmpty()) {
                                res.remove(absVec);
                                AbstractionVector endVec = absVec.deriveEndAbstractionVecByIndexSet(endStmtToIndexMapping, stmt);
                                if(endVec.isAllEndAbs()) {
                                    getResults().addResult(endVec, stmt);
                                } else
                                    res.add(endVec);

                            }

                        }
                    }

                    Set<AbstractionVector> aliasGeneratedSet = new HashSet<>();
                    aliasGeneratedSet.addAll(res);
                    for (int index : source.getIndexSet()) {
                        Set<AbstractionVector> inLoopSet = new HashSet<>();
                        inLoopSet.addAll(aliasGeneratedSet);
                        for (AbstractionVector absVec : inLoopSet) {
                            Abstraction abs = absVec.getAbstractionByIndex(index);
                            Set<Abstraction> newAbsSet = computeAliases(abs);

                            Set<AbstractionVector> newRes = absVec.deriveNewAbstractionVecSetByAbsSet(newAbsSet, index);
                            aliasGeneratedSet.addAll(newRes);

                        }
                    }
                    res.addAll(aliasGeneratedSet);


                    return res;
                }

                public abstract Set<Abstraction> computeSingleTarget(Abstraction source);
                public abstract Set<Abstraction> computeAliases(Abstraction source);

            }



            @Override
            public FlowFunction<AbstractionVector> getCallToReturnFlowFunction(final Unit callSite, Unit returnSite) {
                return new MultiCallToReturnFlowFunction((Stmt) callSite) {

                    @Override
                    public Set<Abstraction> computeSingleTarget(Abstraction source) {

                        Set<Abstraction> res = new HashSet<>();

                        Map<Stmt, Set<Abstraction>> normalMap = source.getNormalFlowMap();
                        if (normalMap != null && normalMap.containsKey(callSite)) {
                            res.addAll(normalMap.get(callSite));
                        } else {
                            //multicheck!!!
                            res.add(source);
                        }


                        return res;
                    }

                    @Override
                    public Set<Abstraction> computeAliases(Abstraction source) {

                        Set<Abstraction> res = new HashSet<>();

                        Map<Unit, Set<Abstraction>> aliasMap = source.getAliasFlowMap();
                        if (aliasMap != null && aliasMap.containsKey(callSite)) {
                            res.addAll(aliasMap.get(callSite));
                        }

                        return res;
                    }

                };
            }
        };
    }
}
