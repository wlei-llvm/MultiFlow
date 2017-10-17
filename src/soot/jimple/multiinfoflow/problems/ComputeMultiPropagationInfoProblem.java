package soot.jimple.multiinfoflow.problems;

import heros.solver.CountingThreadPoolExecutor;
import heros.solver.Pair;
import soot.NullType;
import soot.SootMethod;
import soot.Unit;
import soot.jimple.Stmt;
import soot.jimple.infoflow.collect.ConcurrentHashSet;
import soot.jimple.infoflow.collect.MyConcurrentHashMap;
import soot.jimple.infoflow.data.Abstraction;
import soot.jimple.infoflow.data.AbstractionAtSink;
import soot.jimple.infoflow.data.AccessPathFactory;
import soot.jimple.internal.JimpleLocal;
import soot.jimple.multiinfoflow.data.AbstractionVector;
import soot.jimple.multiinfoflow.util.Triplet;

import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * @author wanglei
 */
public class ComputeMultiPropagationInfoProblem {


    private final CountingThreadPoolExecutor executor;

    private final Set<AbsCopy> visitedSet = new ConcurrentHashSet<>();


    private final MyConcurrentHashMap<Stmt, Set<Abstraction>> seedSourceAbstraction = new MyConcurrentHashMap<>();

    private final MyConcurrentHashMap<Stmt, Set<Abstraction>> seedSinkAbstraction = new MyConcurrentHashMap<>();

    private final Set<AbstractionAtSink> preProcessRes = new HashSet<>();

    private Set<Abstraction> endSet;

    private final MyConcurrentHashMap<Abstraction, Abstraction> realAbsMap = new MyConcurrentHashMap();

    public ComputeMultiPropagationInfoProblem(CountingThreadPoolExecutor executor,
                                              Set<AbstractionAtSink> preProcessRes ,Set<Abstraction> endSet) {
        this.executor = executor;
//        this.seedSourceAbstraction.putAll(seedSourceAbstraction);
//        this.seedSinkAbstraction.putAll(seedSinkAbstraction);
        this.preProcessRes.addAll(preProcessRes);

        this.endSet = endSet;
    }

    public Map<Stmt, Set<Abstraction>> getSeedSourceAbstraction() {
        return seedSourceAbstraction;
    }

    public Map<Stmt, Set<Abstraction>> getSeedSinkAbstraction() {
        return seedSinkAbstraction;
    }

    public void solve() {

        for(AbstractionAtSink abstractionAtSink : preProcessRes) {
            Abstraction abs = abstractionAtSink.getAbstraction();
            Set<Abstraction> resSet = seedSinkAbstraction.putIfAbsentElseGet(abstractionAtSink.getSinkStmt(), new HashSet<Abstraction>());
            resSet.add(abs);
            spawnNewTask(abs, null);
        }

        for(Abstraction end : endSet) {
            spawnNewTask(end, null);
        }

        try {
            executor.awaitCompletion();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        Throwable exception = executor.getException();
        if(exception!=null) {
            throw new RuntimeException("There were exceptions during Function OPT analysis. Exiting.",exception);
        }
        executor.shutdown();

//        // Wait for the executor to be really gone
//        while (!executor.isTerminated()) {
//            try {
//                Thread.sleep(100);
//            } catch (InterruptedException e) {
//                e.printStackTrace();
//            }
//        }


    }

    protected void spawnNewTask(Abstraction abs, Abstraction alias) {


        if(alias != null && !alias.getAliasStmt().equals(abs.getActivationUnit())) {
            return ;
        }
        executor.execute(new ComputeMultiPropagationInfoTask(abs, alias));
    }

    private class AbsCopy {
        private Abstraction abstraction;
        private Abstraction aliasAbs;
        public AbsCopy(Abstraction abstraction, Abstraction alias){
            this.abstraction = abstraction;
            this.aliasAbs = alias;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj)
                return true;
            if (obj == null || getClass() != obj.getClass())
                return false;
            AbsCopy other = (AbsCopy) obj;

            if (abstraction == null) {
                if (other.abstraction != null)
                    return false;
            } else if (abstraction != other.abstraction)
                return false;

            if (aliasAbs == null) {
                if (other.aliasAbs != null)
                    return false;
            } else if (aliasAbs != other.aliasAbs)
                return false;

            return true;
        }

        @Override
        public int hashCode() {

            final int prime = 31;
            int result = 1;

            // deliberately ignore prevAbs
            result = prime * result + ((abstraction == null) ? 0 : abstraction.hashCode());
            result = prime * result + ((aliasAbs == null) ? 0 : aliasAbs.hashCode());
            return result;
        }

    }

    private class ComputeMultiPropagationInfoTask implements Runnable {

        private final Abstraction abstraction;

        private final Abstraction aliasAbstraction;

        public ComputeMultiPropagationInfoTask(Abstraction abstraction, Abstraction alias) {

            this.abstraction = abstraction;
            this.aliasAbstraction = alias;
        }

        public void computeReturnFlow(Set<Triplet<Unit, Unit, Abstraction>> keySet, Abstraction src, Abstraction dest) {

            Abstraction realDestAbstraction = realAbsMap.putIfAbsentElseGet(dest, dest);
            Abstraction realSrcAbstraction = realAbsMap.putIfAbsentElseGet(src, src);

            if (realDestAbstraction.getReturnFlowMap() == null) {
                realDestAbstraction.setReturnFlowMap(new MyConcurrentHashMap<Pair<Unit, Unit>, Set<Abstraction>>());
            }
            MyConcurrentHashMap<Pair<Unit, Unit>, Set<Abstraction>> returnFlowMap = (MyConcurrentHashMap) realDestAbstraction.getReturnFlowMap();
            for (Triplet<Unit, Unit, Abstraction> triple : keySet) {
                Pair<Unit, Unit> key = new Pair<>(triple.getO1(), triple.getO2());
                Set<Abstraction> abstractionSet = returnFlowMap.putIfAbsentElseGet(key, new ConcurrentHashSet<Abstraction>());
                abstractionSet.add(realSrcAbstraction);

                if(AbstractionVector.isZeroAbstraction(triple.getO3())) {
                    Set<Abstraction> seedSet = seedSourceAbstraction.putIfAbsentElseGet(
                            (Stmt) triple.getO1(), new ConcurrentHashSet<Abstraction>());
                    seedSet.add(realSrcAbstraction);
                }

            }

        }

        public void computeAliasFlow(Unit key, Abstraction src, Abstraction dest) {

            Abstraction realDestAbstraction = realAbsMap.putIfAbsentElseGet(dest, dest);
            Abstraction realSrcAbstraction = realAbsMap.putIfAbsentElseGet(src, src);

            if (realDestAbstraction.getAliasFlowMap() == null) {
                realDestAbstraction.setAliasFlowMap(new MyConcurrentHashMap<Unit, Set<Abstraction>>());
            }
            MyConcurrentHashMap<Unit, Set<Abstraction>> nextAbsMap = (MyConcurrentHashMap) realDestAbstraction.getAliasFlowMap();
            Set<Abstraction> abstractionSet = nextAbsMap.putIfAbsentElseGet(key, new ConcurrentHashSet<Abstraction>());
            abstractionSet.add(realSrcAbstraction);

        }

        public void computeCallFlow(Pair<Unit, SootMethod> calleeInfo, Abstraction src, Abstraction dest) {

            Abstraction realDestAbstraction = realAbsMap.putIfAbsentElseGet(dest, dest);
            Abstraction realSrcAbstraction = realAbsMap.putIfAbsentElseGet(src, src);

            if (realDestAbstraction.getCallFlowMap() == null) {
                realDestAbstraction.setCallFlowMap(new MyConcurrentHashMap<Pair<Unit, SootMethod>, Set<Abstraction>>());
            }
            MyConcurrentHashMap<Pair<Unit, SootMethod>, Set<Abstraction>> nextAbsMap = (MyConcurrentHashMap) realDestAbstraction.getCallFlowMap();
            Set<Abstraction> abstractionSet = nextAbsMap.putIfAbsentElseGet(calleeInfo, new ConcurrentHashSet<Abstraction>());
            abstractionSet.add(realSrcAbstraction);

        }
        public void computeSummaryFlow(Pair<Unit, Unit> calleeInfo, Abstraction src, Abstraction dest) {

            Abstraction realDestAbstraction = realAbsMap.putIfAbsentElseGet(dest, dest);
            Abstraction realSrcAbstraction = realAbsMap.putIfAbsentElseGet(src, src);

            if (realDestAbstraction.getSummaryFlowMap() == null) {
                realDestAbstraction.setSummaryFlowMap(new MyConcurrentHashMap<Unit, Set<Pair<Unit, Abstraction>>>());
            }
            MyConcurrentHashMap<Unit, Set<Pair<Unit,Abstraction>>> nextAbsMap = (MyConcurrentHashMap) realDestAbstraction.getSummaryFlowMap();
            Set<Pair<Unit, Abstraction>> abstractionSet = nextAbsMap.putIfAbsentElseGet(calleeInfo.getO1(), new ConcurrentHashSet<Pair<Unit,Abstraction>>());
            abstractionSet.add(new Pair<>(calleeInfo.getO2(), realSrcAbstraction));

        }



        public void computeNormalFlow(Stmt currentStmt, Abstraction src, Abstraction dest) {

            Abstraction realDestAbstraction = realAbsMap.putIfAbsentElseGet(dest, dest);
            Abstraction realSrcAbstraction = realAbsMap.putIfAbsentElseGet(src, src);

            if (realDestAbstraction.getNormalFlowMap() == null) {
                realDestAbstraction.setNormalFlowMap(new MyConcurrentHashMap<Stmt, Set<Abstraction>>());
            }
            MyConcurrentHashMap<Stmt, Set<Abstraction>> nextAbsMap = (MyConcurrentHashMap) realDestAbstraction.getNormalFlowMap();
            Set<Abstraction> abstractionSet = nextAbsMap.putIfAbsentElseGet(currentStmt, new ConcurrentHashSet<Abstraction>());
            abstractionSet.add(realSrcAbstraction);


        }

        protected Abstraction getFakeAbstraction() {
            Abstraction fakeValue = new Abstraction(
                    AccessPathFactory.v().createAccessPath(new JimpleLocal("fake", NullType.v()), false),
                    null,
                    false,
                    false);
            return fakeValue;
        }

        protected Abstraction getRealPredecessor(Abstraction abs) {
            if(abs.getPredecessor() != null && abs.getSummaryPredecessor() == null
                    && abs.getNeighbors() == null && abs.getCurrentStmt() == null
                    && abs.getCorrespondingCallSite() == null && abs.getCalleeInfo() == null && abs.getReturnSiteInfo() == null
                    && abs.getActivationUnit() == null) {
                if(abs.getPredecessor().getPredecessor()!= null && abs.getPredecessor().getPredecessor().getActivationUnit() == null)
                    return abs.getPredecessor();
            }
            return abs;
        }


        @Override
        public void run() {

            Abstraction key2 = aliasAbstraction == null ? null :  realAbsMap.putIfAbsentElseGet(aliasAbstraction, aliasAbstraction);
            AbsCopy absCopy = new AbsCopy(abstraction,key2);
            //AbsCopy absCopy = new AbsCopy(abs, null);

            if (visitedSet.contains(absCopy))
                return;
            else
                visitedSet.add(absCopy);

            final Abstraction predecessor = abstraction.getPredecessor();
            final Pair<Triplet<Unit, Unit, Abstraction>, Abstraction> summaryPred = abstraction.getSummaryPredecessor();
            Abstraction next = predecessor;
            boolean meetAlias = false;



//            if (predecessor != null && summaryPred == null && abstraction.getActivationUnit() == null && predecessor.getActivationUnit() != null) {
            if (predecessor != null && abstraction.getActivationUnit() == null && predecessor.getActivationUnit() != null) {
                //meet alias
                assert aliasAbstraction == null;

                if(summaryPred != null) {

                }

                Abstraction alias = realAbsMap.putIfAbsentElseGet(abstraction, abstraction);
                alias.setAliasStmt(predecessor.getActivationUnit());

                spawnNewTask(predecessor, alias);
                //spawnNewTask(predecessor, null);
                meetAlias = true;


                if (abstraction.getNeighbors() != null) {
                    for (Abstraction neighbor : abstraction.getNeighbors()) {
                        spawnNewTask(neighbor, null);
                    }
                }
                return;

            }

            if (predecessor == null) {
                if(AbstractionVector.isEndAbstraction(abstraction))
                    return ;
                if(aliasAbstraction != null) {
                    System.out.println("COMCOM !ALIAS NULL!!");
                }

                Set<Abstraction> seedSet = seedSourceAbstraction.putIfAbsentElseGet(
                        abstraction.getCurrentStmt(), new ConcurrentHashSet<Abstraction>());
                seedSet.add(realAbsMap.putIfAbsentElseGet(abstraction, abstraction));

            } else if (aliasAbstraction != null) {
                if (summaryPred != null) {

                    // alias call ,  return :

                    //Pair<Unit, Unit> key = new Pair<>(summaryPred.getO1().getO1(), null);
                    Triplet<Unit, Unit, Abstraction> key = new Triplet<>(summaryPred.getO1().getO1(), null, getFakeAbstraction());

                    if(predecessor.getActivationUnit() != null && predecessor.getActivationUnit().equals(aliasAbstraction.getAliasStmt())) {
                       // computeReturnFlow(Collections.singleton(key), aliasAbstraction, aliasAbstraction);
                        spawnNewTask(predecessor, aliasAbstraction);
                    }else {
                        computeReturnFlow(Collections.singleton(key), aliasAbstraction, aliasAbstraction);
                        if(predecessor.getActivationUnit() == null)
                            computeSummaryFlow(new Pair(key.getO1(), key.getO2()), aliasAbstraction, getRealPredecessor(predecessor));

                        spawnNewTask(summaryPred.getO2(), aliasAbstraction);
                    }


//                    if (abstraction.getNeighbors() != null) {
//                        for (Abstraction neighbor : abstraction.getNeighbors()) {
//                            spawnNewTask(neighbor, aliasAbstraction);
//                        }
//                    }
                    return;

                }
                if (abstraction.getCorrespondingCallSite() != null) {

                    Triplet<Unit, Unit, Abstraction> key = new Triplet<>((Unit) abstraction.getCorrespondingCallSite(), null, Abstraction.getZeroAbstraction(true));

                    computeReturnFlow(Collections.singleton(key), aliasAbstraction, aliasAbstraction);
                }

                Unit preCurrentStmt = predecessor.getCurrentStmt();
                if (preCurrentStmt != null && preCurrentStmt.equals(abstraction.getActivationUnit())) {
                    //AliasFlow:

                    computeAliasFlow(predecessor.getCurrentStmt(), aliasAbstraction, getRealPredecessor(predecessor));
                    spawnNewTask(predecessor, null);

                } else if (predecessor.getActivationUnit() != null) {

                    if (abstraction.getActivationUnit().equals(predecessor.getActivationUnit()))
                        spawnNewTask(predecessor, aliasAbstraction);
                } else {
                    spawnNewTask(predecessor, null);
                }

//                if (abstraction.getNeighbors() != null) {
//                        for (Abstraction neighbor : abstraction.getNeighbors()) {
//                            spawnNewTask(neighbor, aliasAbstraction);
//                        }
//                    }
                return;

            } else {
                Stmt correspondingCallSite = abstraction.getCorrespondingCallSite();
                Stmt currentStmt = abstraction.getCurrentStmt();

                if (abstraction.getCalleeInfo() != null) {
                    //CallFlow:
                    if (summaryPred != null)
                        assert false;

                    Pair<Unit, SootMethod> calleeInfo = abstraction.getCalleeInfo();

                    computeCallFlow(calleeInfo, abstraction, getRealPredecessor(predecessor));

                }
                if (abstraction.getReturnSiteInfo() != null) {
                    // unbalance return :
                    if (correspondingCallSite != null) {
                        // return taint :

                        computeReturnFlow(abstraction.getReturnSiteInfo(), abstraction, getRealPredecessor(predecessor));

                    } else {
                        //global taint :
                        computeReturnFlow(abstraction.getReturnSiteInfo(), abstraction, abstraction);

                    }


                }

//                if (currentStmt != null) {
//
//                    if (currentStmt == correspondingCallSite) {
//                        //CallToReturnFlow
//                        computeNormalFlow(currentStmt, abstraction, predecessor);
//
//                    } else {
//
//                        //NormalFlow
//                        computeNormalFlow(currentStmt, abstraction, predecessor);
//
//                    }
//
//
//                } else
                if (summaryPred != null) {
                    //ReturnFlow

                    if (summaryPred != null) {

                        Triplet<Unit, Unit, Abstraction> callStmtAndCalleeAndD1 = summaryPred.getO1();
//                        Triplet<Unit, Unit, Abstraction> callStmtAndCalleeAndD1 =
//                                new Triplet<>(summaryPred.getO1().getO1(), summaryPred.getO1().getO2(), getFakeAbstraction());

                        Abstraction returnSiteAbs = summaryPred.getO2();

                        computeReturnFlow(Collections.singleton(callStmtAndCalleeAndD1), abstraction, getRealPredecessor(returnSiteAbs));

                        computeSummaryFlow(new Pair(callStmtAndCalleeAndD1.getO1(), callStmtAndCalleeAndD1.getO2()), abstraction, getRealPredecessor(predecessor));

                        next = returnSiteAbs;

                    } else {
                        assert false;
                        System.out.print("MultiAnalysis Abnormal!");
                        computeReturnFlow(abstraction.getReturnSiteInfo(), abstraction, abstraction);
                    }

                }else {
                    if(currentStmt != null)
                        computeNormalFlow(currentStmt, abstraction, getRealPredecessor(predecessor));
                   // else
                       // computeNormalFlow(null, abstraction, predecessor);
                }


                spawnNewTask(next, null);
            }

            if (abstraction.getNeighbors() != null) {
                for (Abstraction neighbor : abstraction.getNeighbors()) {
                    spawnNewTask(neighbor, aliasAbstraction);
                }
            }
        }
    }

}
