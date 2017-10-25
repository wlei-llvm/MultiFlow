package soot.jimple.multiinfoflow;

import heros.solver.CountingThreadPoolExecutor;
import heros.solver.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import soot.*;
import soot.jimple.Stmt;
import soot.jimple.infoflow.AbstractInfoflow;
import soot.jimple.infoflow.InfoflowConfiguration;
import soot.jimple.infoflow.InfoflowManager;
import soot.jimple.infoflow.aliasing.FlowSensitiveAliasStrategy;
import soot.jimple.infoflow.aliasing.IAliasingStrategy;
import soot.jimple.infoflow.aliasing.PtsBasedAliasStrategy;
import soot.jimple.infoflow.android.source.AndroidSourceSinkManager;
import soot.jimple.infoflow.cfg.BiDirICFGFactory;
import soot.jimple.infoflow.codeOptimization.DeadCodeEliminator;
import soot.jimple.infoflow.codeOptimization.ICodeOptimizer;
import soot.jimple.infoflow.collect.ConcurrentHashSet;
import soot.jimple.infoflow.collect.MyConcurrentHashMap;
import soot.jimple.infoflow.data.Abstraction;
import soot.jimple.infoflow.data.AbstractionAtSink;
import soot.jimple.infoflow.data.AccessPathFactory;
import soot.jimple.infoflow.data.pathBuilders.DefaultPathBuilderFactory;
import soot.jimple.infoflow.data.pathBuilders.IPathBuilderFactory;
import soot.jimple.infoflow.entryPointCreators.IEntryPointCreator;
import soot.jimple.infoflow.handlers.ResultsAvailableHandler;
import soot.jimple.infoflow.problems.TaintPropagationResults;
import soot.jimple.infoflow.results.InfoflowResults;
import soot.jimple.infoflow.solver.cfg.BackwardsInfoflowCFG;
import soot.jimple.infoflow.solver.cfg.IInfoflowCFG;
import soot.jimple.infoflow.solver.cfg.InfoflowCFG;
import soot.jimple.infoflow.solver.fastSolver.BackwardsInfoflowSolver;
import soot.jimple.infoflow.solver.fastSolver.InfoflowSolver;
import soot.jimple.infoflow.source.ISourceSinkManager;
import soot.jimple.infoflow.util.SystemClassHandler;
import soot.jimple.multiinfoflow.data.AbstractMultiSourceStmtInfo;
import soot.jimple.multiinfoflow.problems.ComputeMultiPropagationInfoProblem;
import soot.jimple.multiinfoflow.problems.MultiInfoflowProblem;
import soot.jimple.multiinfoflow.problems.PreProcessBackwardsInfoflowProblem;
import soot.jimple.multiinfoflow.problems.PreProcessInfoflowProblem;
import soot.jimple.multiinfoflow.result.*;
import soot.jimple.multiinfoflow.solver.MultiInfoflowSolver;
import soot.jimple.multiinfoflow.util.Triplet;
import soot.jimple.toolkits.callgraph.ReachableMethods;
import soot.jimple.toolkits.ide.icfg.BiDiInterproceduralCFG;
import soot.jimple.toolkits.ide.icfg.JimpleBasedInterproceduralCFG;
import soot.options.Options;

import java.util.*;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 * base class for multi-infoflow
 *
 * @author wanglei
 */
public class MultiInfoflow extends AbstractInfoflow {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private IInfoflowCFG iCfg;

    private long maxMemoryConsumption = -1;
    private Set<ResultsAvailableHandler> onResultsAvailable = new HashSet<ResultsAvailableHandler>();

    private InfoflowResults results = null;

    private int numThreads = 1;

    //Test-Tag
    private boolean isTest = false;

    //Debug mode
    private boolean isDebugMode = true;

    private Set<Stmt> collectedSources = null;
    private Set<Stmt> collectedSinks = null;

    ISourceSinkManager sourcesSinks = null;

    public static Abstraction zeroAbstraction ;

    PreProcessBackwardsInfoflowProblem preProcessbackProblem = null;
    InfoflowSolver preProcessforwardSolver = null;

    private MyConcurrentHashMap<Abstraction, Abstraction> realAbsMap = new MyConcurrentHashMap();
    public static AbstractMultiSourceStmtInfo validSourceStmtInfo = null;

    private Map<Stmt, Set<Abstraction>> seedSourceAbstraction = new HashMap<>();

    private Map<Stmt, Set<Abstraction>> seedSinkAbstraction = new HashMap<>();

    private Set<SootMethod> callBackList = new HashSet<>();

    private final Set<AbsCopy> visitedSet = new ConcurrentHashSet<>();

    public MultiInfoflow() {
        super();
        numThreads = Runtime.getRuntime().availableProcessors();
    }


    public MultiInfoflow(String androidPath, boolean forceAndroidJar, BiDirICFGFactory icfgFactory,
                         IPathBuilderFactory pathBuilderFactory){
        super(icfgFactory, androidPath, forceAndroidJar);
        this.pathBuilderFactory = new DefaultPathBuilderFactory();
        numThreads = Runtime.getRuntime().availableProcessors();
    }


    @Override
    public void computeInfoflow(String appPath, String libPath,
                                IEntryPointCreator entryPointCreator,
                                ISourceSinkManager sourcesSinks) {
        if (sourcesSinks == null) {
            logger.error("Sources are empty!");
            return;
        }

        initializeSoot(appPath, libPath, entryPointCreator.getRequiredClasses());

        // entryPoints are the entryPoints required by Soot to calculate Graph - if there is no main method,
        // we have to create a new main method and use it as entryPoint and store our real entryPoints
        Scene.v().setEntryPoints(Collections.singletonList(entryPointCreator.createDummyMain()));

        // Run the analysis
        runAnalysis(sourcesSinks, null);

    }

    @Override
    public void computeInfoflow(String appPath, String libPath, String entryPoint, ISourceSinkManager sourcesSinks) {

    }

    @Override
    public InfoflowResults getResults() {
        return null;
    }

    @Override
    public boolean isResultAvailable() {
        return false;
    }

    private void  runAnalysis(final ISourceSinkManager sourcesSinks, final Set<String> additionalSeeds) {
        // Clear the data from previous runs
        maxMemoryConsumption = -1;
        results = null;
        numThreads = Runtime.getRuntime().availableProcessors();

        // Some configuration options do not really make sense in combination
        if (config.getEnableStaticFieldTracking()
                && InfoflowConfiguration.getAccessPathLength() == 0)
            throw new RuntimeException("Static field tracking must be disabled "
                    + "if the access path length is zero");
        if (InfoflowConfiguration.getAccessPathLength() < 0)
            throw new RuntimeException("The access path length may not be negative");

        // Clear the base registrations from previous runs
        AccessPathFactory.v().clearBaseRegister();
        // Build the callgraph
        long beforeCallgraph = System.nanoTime();
        constructCallgraph();
        logger.info("Callgraph construction took " + (System.nanoTime() - beforeCallgraph) / 1E9
                + " seconds");

        // Perform constant propagation and remove dead code
        if (config.getCodeEliminationMode() != InfoflowConfiguration.CodeEliminationMode.NoCodeElimination) {
            long currentMillis = System.nanoTime();
            eliminateDeadCode(sourcesSinks);
            logger.info("Dead code elimination took " + (System.nanoTime() - currentMillis) / 1E9
                    + " seconds");
        }

        if (config.getCallgraphAlgorithm() != InfoflowConfiguration.CallgraphAlgorithm.OnDemand)
            logger.info("Callgraph has {} edges", Scene.v().getCallGraph().size());

        if (!config.isTaintAnalysisEnabled()) {
            return;
        }
        logger.info("Starting Multi-sources  Taint Analysis");
        iCfg = icfgFactory.buildBiDirICFG(config.getCallgraphAlgorithm(),
                config.getEnableExceptionTracking());


        runInternalAnalysis(sourcesSinks, additionalSeeds);

    }


    void computMultiInfoflowByCmdForTest(String appPath, String libPath, Collection<String> classes, ISourceSinkManager sourcesSinks) {
        // 1 Config soot: load classes...
        initializeSootForTest(appPath,libPath, classes);

        // 2.1 Build the callgraph
        long beforeCallgraph = System.nanoTime();
        constructSimpleCallGraphForTest();
        logger.info("Callgraph construction took " + (System.nanoTime() - beforeCallgraph) / 1E9
                + " seconds");
        logger.info("Callgraph has {} edges", Scene.v().getCallGraph().size());

        logger.info("Starting Taint Analysis");

        // 2.2 Construct interprocedural CFG
        BiDiInterproceduralCFG<Unit, SootMethod> baseICFG  = new JimpleBasedInterproceduralCFG(true);
        iCfg = new InfoflowCFG(baseICFG) ;

        isTest = true;
        runInternalAnalysis(sourcesSinks , null ) ;
    }

    private void runInternalAnalysis(final ISourceSinkManager sourcesSinks, final Set<String> additionalSeeds ) {
        CountingThreadPoolExecutor executor = createExecutor(numThreads);

        this.sourcesSinks = sourcesSinks;

        // Initialize the memory manager
//        FlowDroidMemoryManager.PathDataErasureMode erasureMode = FlowDroidMemoryManager.PathDataErasureMode.EraseAll;
//        if (pathBuilderFactory.isContextSensitive())
//            erasureMode = FlowDroidMemoryManager.PathDataErasureMode.KeepOnlyContextData;
//        if (pathBuilderFactory.supportsPathReconstruction())
//            erasureMode = FlowDroidMemoryManager.PathDataErasureMode.EraseNothing;
//        IMemoryManager<Abstraction> memoryManager = new FlowDroidMemoryManager(false,
//                erasureMode);

        // Initialize the data flow manager
        InfoflowManager manager = new InfoflowManager(config, null, iCfg, sourcesSinks,
                taintWrapper, hierarchy);


        InfoflowManager backwardsManager = null;
        InfoflowSolver backSolver = null;
        final IAliasingStrategy aliasingStrategy;
//        if(!getConfig().getFlowSensitiveAliasing() ) {
//            getConfig().setAliasingAlgorithm(PtsBased);
//        }
        switch (getConfig().getAliasingAlgorithm()) {
            case FlowSensitive:
                backwardsManager = new InfoflowManager(config, null,
                        new BackwardsInfoflowCFG(iCfg), sourcesSinks, taintWrapper, hierarchy);
                preProcessbackProblem = new PreProcessBackwardsInfoflowProblem(backwardsManager);
                backSolver = new BackwardsInfoflowSolver(preProcessbackProblem, executor);
//              backSolver.setMemoryManager(memoryManager);
                backSolver.setJumpPredecessors(!pathBuilderFactory.supportsPathReconstruction());
//				backSolver.setEnableMergePointChecking(true);

                aliasingStrategy = new FlowSensitiveAliasStrategy(iCfg, backSolver);
                break;
            case PtsBased:
                preProcessbackProblem = null;
                backSolver = null;
                aliasingStrategy = new PtsBasedAliasStrategy(iCfg);
                break;
            default:
                throw new RuntimeException("Unsupported aliasing algorithm");
        }

        // Get the zero fact
        Abstraction zeroValue = preProcessbackProblem != null
                ? preProcessbackProblem.createZeroValue() : null;
        PreProcessInfoflowProblem soloProblem  = new PreProcessInfoflowProblem(manager,
                aliasingStrategy, zeroValue);
        MultiInfoflow.zeroAbstraction = zeroValue;

        // Set the options
        preProcessforwardSolver = new InfoflowSolver(soloProblem, executor);
        aliasingStrategy.setForwardSolver(preProcessforwardSolver);
        manager.setForwardSolver(preProcessforwardSolver);
        if (backwardsManager != null)
            backwardsManager.setForwardSolver(preProcessforwardSolver);

//        forwardSolver.setMemoryManager(memoryManager);
        preProcessforwardSolver.setJumpPredecessors(!pathBuilderFactory.supportsPathReconstruction());
//		forwardSolver.setEnableMergePointChecking(true);

//        forwardProblem.setTaintPropagationHandler(taintPropagationHandler);
        soloProblem.setTaintWrapper(taintWrapper);
        if (nativeCallHandler != null)
            soloProblem.setNativeCallHandler(nativeCallHandler);

        if (preProcessbackProblem != null) {
            preProcessbackProblem.setForwardSolver(preProcessforwardSolver);
//            backProblem.setTaintPropagationHandler(backwardsPropagationHandler);
            preProcessbackProblem.setTaintWrapper(taintWrapper);
            if (nativeCallHandler != null)
                preProcessbackProblem.setNativeCallHandler(nativeCallHandler);
            preProcessbackProblem.setActivationUnitsToCallSites(soloProblem);
        }

        // Print our configuration
        config.printSummary();
        if (config.getFlowSensitiveAliasing() && !aliasingStrategy.isFlowSensitive())
            logger.warn("Trying to use a flow-sensitive aliasing with an "
                    + "aliasing strategy that does not support this feature");

        // We have to look through the complete program to find sources
        // which are then taken as seeds.
        int sinkCount = 0;
        logger.info("Looking for sources and sinks...");


        //callbacks
        callBackList.clear();
        List<SootMethod> eplist = Scene.v().getEntryPoints();
        for(SootMethod ep : eplist) {
            callBackList.addAll(getCallBackList(ep));
        }

        for (SootMethod sm : getMethodsForSeeds(iCfg))
            sinkCount += scanMethodForSourcesSinks(sourcesSinks, soloProblem, sm);

        // We optionally also allow additional seeds to be specified
        if (additionalSeeds != null)
            for (String meth : additionalSeeds) {
                SootMethod m = Scene.v().getMethod(meth);
                if (!m.hasActiveBody()) {
                    logger.warn("Seed method {} has no active body", m);
                    continue;
                }
                soloProblem.addInitialSeeds(m.getActiveBody().getUnits().getFirst(),
                        Collections.singleton(soloProblem.zeroValue()));
            }

        if(config.isDumpSourceEnable())   {
            if(collectedSources.isEmpty()) {
                logger.info("Source : no source");
                return;
            }

            for(Stmt src: collectedSources ) {
                logger.info("Source : "  + src);
            }
            return;
        }

        // Report on the sources and sinks we have found
        if (!soloProblem.hasInitialSeeds()) {
            logger.error("No sources found, aborting analysis");
            return;
        }
        if (sinkCount == 0) {
            logger.error("No sinks found, aborting analysis");
            return;
        }
        logger.info("Source lookup done, found {} sources and {} sinks.", soloProblem.getInitialSeeds().size(),
                sinkCount);

        // Initialize the taint wrapper if we have one
        if (taintWrapper != null)
            taintWrapper.initialize(manager);
        if (nativeCallHandler != null)
            nativeCallHandler.initialize(manager);

        // Register the handler for interim results
        TaintPropagationResults propagationResults = soloProblem.getResults();

        long beforeSolver = System.nanoTime();
        preProcessforwardSolver.solve();
        Set<String> resCount = new HashSet<>();
        for(Abstraction endAbs: propagationResults.getEndAbs()) {
            resCount.add("Solo Result: SRC: " + endAbs.getEndAbstraction().getSourceContext().getStmt() + "SINK: "+ endAbs.getEndSinkStmt());
        }
        logger.info("IFDS problem with {} forward and {} backward edges solved, "
                        + "processing {} results...", preProcessforwardSolver.propagationCount,
                backSolver == null ? 0 : backSolver.propagationCount,
                propagationResults == null ? 0 : resCount.size());

        for(String info : resCount) {
            logger.info(info);
        }

        logger.info("SoloFlowDroidSolver took " + (System.nanoTime() - beforeSolver) / 1E9
                + " seconds");
//        maxMemoryConsumption = Math.max(maxMemoryConsumption, getUsedMemory());

        // Not really nice, but sometimes Heros returns before all
        // executor tasks are actually done. This way, we give it a
        // chance to terminate gracefully before moving on.
//        int terminateTries = 0;
//        while (terminateTries < 10) {
//            if (executor.getActiveCount() != 0 || !executor.isTerminated()) {
//                terminateTries++;
//                try {
//                    Thread.sleep(500);
//                }
//                catch (InterruptedException e) {
//                    logger.error("Could not wait for executor termination", e);
//                }
//            }
//            else
//                break;
//        }
//        if (executor.getActiveCount() != 0 || !executor.isTerminated())
//            logger.error("Executor did not terminate gracefully");
//
//        // Print taint wrapper statistics
//        if (taintWrapper != null) {
//            logger.info("Taint wrapper hits: " + taintWrapper.getWrapperHits());
//            logger.info("Taint wrapper misses: " + taintWrapper.getWrapperMisses());
//        }

        Set<AbstractionAtSink> res = propagationResults.getResults();

        beforeSolver = System.nanoTime();


        computeEndAbs(propagationResults.getEndAbs());
        CountingThreadPoolExecutor resExecutor = createExecutor(numThreads);
        ComputeMultiPropagationInfoProblem computeMultiPropagationInfoProblem = new ComputeMultiPropagationInfoProblem(resExecutor, res , preProcessforwardSolver.getEndAbs());
        computeMultiPropagationInfoProblem.solve();
        logger.info("PreProcessProblem took " + (System.nanoTime() - beforeSolver) / 1E9
                + " seconds");
        seedSourceAbstraction.putAll(computeMultiPropagationInfoProblem.getSeedSourceAbstraction());
        seedSinkAbstraction.putAll(computeMultiPropagationInfoProblem.getSeedSinkAbstraction());


        //multi-infoflow analysis
        //we do multi-infoflow anlaysis while handling results of the solo process, it can be run many times;
        AbstractMultiAnalysisHandler multiInfoFlowHandler =
                MultiAnalysisHandlerFactory.v().createMultiAnalysisHandler(config.getMultiTaintMode(),
                        iCfg,
                        propagationResults,
                        this,
                        (AndroidSourceSinkManager)sourcesSinks);
        multiInfoFlowHandler.run();

        //clean up
        preProcessforwardSolver.cleanup();
        if(backSolver != null) {
            backSolver.cleanup();
            backSolver = null;
            preProcessbackProblem = null;
        }
        preProcessforwardSolver = null;
        soloProblem = null;
        Runtime.getRuntime().gc();
    }

    private void computeEndAbs(Set<Abstraction> endSet) {

        for(Abstraction end : endSet) {

            if (end.getReturnSiteInfo() != null) {
                Set<Triplet<Unit, Unit, Abstraction>> returnInfo = end.getReturnSiteInfo();
                if (end.getReturnFlowMap() == null) {
                    end.setReturnFlowMap(new MyConcurrentHashMap<Pair<Unit, Unit>, Set<Abstraction>>());
                }
                MyConcurrentHashMap<Pair<Unit, Unit>, Set<Abstraction>> returnFlowMap = (MyConcurrentHashMap) end.getReturnFlowMap();

                for (Triplet<Unit, Unit, Abstraction> triple : returnInfo) {

                    Pair<Unit, Unit> key = new Pair<>(triple.getO1(), triple.getO2());
                    Set<Abstraction> abstractionSet = returnFlowMap.putIfAbsentElseGet(key, new ConcurrentHashSet<Abstraction>());
                    abstractionSet.add(end);

                    if(triple.getO3().equals(MultiInfoflow.zeroAbstraction)) {

                        Stmt stmt = (Stmt) triple.getO1();
                        if(!seedSourceAbstraction.containsKey(stmt)) {
                            seedSourceAbstraction.put(stmt, new HashSet<Abstraction>());
                        }
                        Set<Abstraction> seedSet = seedSourceAbstraction.get(stmt);
                        seedSet.add(end);
                    } else {
                        Abstraction d1 = triple.getO3();

                        if (d1.getSummaryFlowMap() == null) {
                            d1.setSummaryFlowMap(new MyConcurrentHashMap<Unit, Set<Pair<Unit, Abstraction>>>());
                        }
                        MyConcurrentHashMap<Unit, Set<Pair<Unit,Abstraction>>> nextAbsMap = (MyConcurrentHashMap) d1.getSummaryFlowMap();
                        Set<Pair<Unit, Abstraction>> tmpSet = nextAbsMap.putIfAbsentElseGet(triple.getO1(), new ConcurrentHashSet<Pair<Unit,Abstraction>>());
                        tmpSet.add(new Pair<>(triple.getO2(), end));

                    }

                }
            }
        }

    }

    public MultiTaintPropagationResults runMultiAnalysis() {

        CountingThreadPoolExecutor multiRunExecutor = createExecutor(this.numThreads);

        MultiInfoflowManager multiInfoFlowManager = new MultiInfoflowManager(config, null ,null, iCfg, sourcesSinks, taintWrapper, hierarchy);

        MultiInfoflowProblem multiForwardProblem = new MultiInfoflowProblem(multiInfoFlowManager);

        MultiInfoflowSolver multiForwardSolver = new MultiInfoflowSolver(multiForwardProblem, multiRunExecutor);
        multiInfoFlowManager.setMultiForwardSolver(multiForwardSolver);
        multiInfoFlowManager.setFirstRunForwardSolver(preProcessforwardSolver);
        multiInfoFlowManager.setCallBackSet(this.callBackList);

        MultiTaintPropagationResults res =  new MultiTaintPropagationResults(multiInfoFlowManager) ;

        //3 Look for sources and sinks And add sources as the initial seeds for IFDS
        for(Map.Entry<Stmt, Set<Abstraction>> entry: seedSourceAbstraction.entrySet()) {
            Stmt stmt = entry.getKey();
            if(!validSourceStmtInfo.getValidSourceStmtInfo().contains(stmt))
                continue;
            int index = validSourceStmtInfo.getSourceIndex(stmt);
            for(Abstraction seed : entry.getValue()) {
               // AbstractionVector absVec =  multiForwardProblem.zeroValue().deriveNewAbstractionVec(seed, index);
                multiForwardProblem.addInitialSeeds(entry.getKey(), Collections.singleton(multiForwardProblem.zeroValue()));
            }
        }

        multiForwardProblem.setResults(res);
        multiForwardProblem.setSeedSourceAbstraction(seedSourceAbstraction);
        multiForwardProblem.setSeedSinkAbstraction(seedSinkAbstraction);


        // 4 IFDS begin : solving the problem on-the-fly
        long beforeSolver = System.nanoTime();
        multiForwardSolver.solve();
        long afterSolver = System.nanoTime();
//        logger.info("Multiple infoflow took " + (System.nanoTime() - beforeSolver) / 1E9
//                + " seconds");

        res.setTime(afterSolver - beforeSolver);
        // 5 Get results
//        MultiTaintPropagationResults propagationResults = multiForwardProblem.getResults();

        System.gc();


        return multiForwardProblem.getResults();


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


    void  initializeSootForTest(String appPath, String libPath , Collection<String> classes ) {
        logger.info("Resetting Soot...");
        soot.G.reset();

        Options.v().set_no_bodies_for_excluded(true);
        Options.v().set_allow_phantom_refs(true);

        Options.v().set_output_format(Options.output_format_none);
        //String path1 = "/Users/wanglei/Downloads/examples/call_graph/src/";
        // Options.v().set_soot_classpath(libPath);

        List<String> processDirs = new LinkedList<String>();
        processDirs.add(appPath);
        logger.info("prcocess dir :" + appPath);
        Options.v().set_process_dir(processDirs);

        Options.v().setPhaseOption("jb.ulp", "off");

        Options.v().set_src_prec(Options.src_prec_java);

        Options.v().set_whole_program(true);
        Options.v().setPhaseOption("cg", "trim-clinit:false");

        Options.v().setPhaseOption("cg.cha", "on");

        // load all entryPoint classes with their bodies
        for (String className : classes)
            Scene.v().addBasicClass(className, SootClass.BODIES);

        Scene.v().loadNecessaryClasses();
        logger.info("Basic class loading done.");

        boolean hasClasses = false;
        for (String className : classes) {
            SootClass c = Scene.v().forceResolve(className, SootClass.BODIES);
            if (c != null){
                c.setApplicationClass();
                if(!c.isPhantomClass() && !c.isPhantom())
                    hasClasses = true;
            }
        }
        if (!hasClasses) {
            System.out.println("Only phantom classes loaded, skipping analysis...");
            return;
        }
        logger.info("InitializeSoot phase done.");

    }

    void constructSimpleCallGraphForTest() {

        PackManager.v().getPack("wjpp").apply();
        PackManager.v().getPack("cg").apply();
        //CallGraph cg = Scene.v().getCallGraph();

        hierarchy = Scene.v().getOrMakeFastHierarchy();

    }

    /**
     * Creates a new executor object for spawning worker threads
     * @param numThreads The number of threads to use
     * @return The generated executor
     */
    private CountingThreadPoolExecutor createExecutor(int numThreads) {
        return new CountingThreadPoolExecutor
                (config.getMaxThreadNum() == -1 ? numThreads
                        : Math.min(config.getMaxThreadNum(), numThreads),
                        Integer.MAX_VALUE, 30, TimeUnit.SECONDS,
                        new LinkedBlockingQueue<Runnable>());
    }


    private Collection<SootMethod> getCallBackList(SootMethod entry) {
        List<SootMethod> seeds = new ArrayList<>();
        if(entry.hasActiveBody()) {
            for(Unit u : entry.getActiveBody().getUnits()) {
                Stmt stmt = (Stmt) u;
                if (stmt.containsInvokeExpr()) {
                    SootMethod sm = stmt.getInvokeExpr().getMethod();
                    seeds.add(sm);
                }
            }
        }
        return seeds;
    }


    private Collection<SootMethod> getMethodsForSeeds(IInfoflowCFG icfg) {
        List<SootMethod> seeds = new LinkedList<SootMethod>();
        // If we have a callgraph, we retrieve the reachable methods. Otherwise,
        // we have no choice but take all application methods as an approximation
        if (Scene.v().hasCallGraph()) {
            List<MethodOrMethodContext> eps = new ArrayList<MethodOrMethodContext>(Scene.v().getEntryPoints());
            ReachableMethods reachableMethods = new ReachableMethods(Scene.v().getCallGraph(), eps.iterator(), null);
            reachableMethods.update();
            for (Iterator<MethodOrMethodContext> iter = reachableMethods.listener(); iter.hasNext();)
                seeds.add(iter.next().method());
        }
        else {
            long beforeSeedMethods = System.nanoTime();
            Set<SootMethod> doneSet = new HashSet<SootMethod>();
            for (SootMethod sm : Scene.v().getEntryPoints())
                getMethodsForSeedsIncremental(sm, doneSet, seeds, icfg);
            logger.info("Collecting seed methods took {} seconds", (System.nanoTime() - beforeSeedMethods) / 1E9);
        }
        return seeds;
    }

    private void getMethodsForSeedsIncremental(SootMethod sm,
                                               Set<SootMethod> doneSet, List<SootMethod> seeds, IInfoflowCFG icfg) {
        assert Scene.v().hasFastHierarchy();
        if (!sm.isConcrete() || !sm.getDeclaringClass().isApplicationClass() || !doneSet.add(sm))
            return;
        seeds.add(sm);
        for (Unit u : sm.retrieveActiveBody().getUnits()) {
            Stmt stmt = (Stmt) u;
            if (stmt.containsInvokeExpr())
                for (SootMethod callee : icfg.getCalleesOfCallAt(stmt))
                    getMethodsForSeedsIncremental(callee, doneSet, seeds, icfg);
        }
    }

    /**
     * Adds a handler that is called when information flow results are available
     * @param handler The handler to add
     */
    public void addResultsAvailableHandler(ResultsAvailableHandler handler) {
        this.onResultsAvailable.add(handler);
    }

    /**
     * Gets the maximum memory consumption during the last analysis run
     * @return The maximum memory consumption during the last analysis run if
     * available, otherwise -1
     */
    public long getMaxMemoryConsumption() {
        return this.maxMemoryConsumption;
    }
    /**
     * Gets the concrete set of sources that have been collected in preparation
     * for the taint analysis. This method will return null if source and sink
     * logging has not been enabled (see InfoflowConfiguration.
     * setLogSourcesAndSinks()),
     * @return The set of sources collected for taint analysis
     */
    public Set<Stmt> getCollectedSources() {
        return this.collectedSources;
    }

    /**
     * Gets the concrete set of sinks that have been collected in preparation
     * for the taint analysis. This method will return null if source and sink
     * logging has not been enabled (see InfoflowConfiguration.
     * setLogSourcesAndSinks()),
     * @return The set of sinks collected for taint analysis
     */
    public Set<Stmt> getCollectedSinks() {
        return this.collectedSinks;
    }

    public static boolean isCustomPairMode (InfoflowConfiguration.MultiTaintMode mode) {

        if(mode == InfoflowConfiguration.MultiTaintMode.AllCombination || mode == InfoflowConfiguration.MultiTaintMode.AllCombCallBackPartition ||
        mode == InfoflowConfiguration.MultiTaintMode.Custom)
            return true;

        return false;
    }


    /**
     * Runs all code optimizers
     * @param sourcesSinks The SourceSinkManager
     */
    private void eliminateDeadCode(ISourceSinkManager sourcesSinks) {
        ICodeOptimizer dce = new DeadCodeEliminator();
        dce.initialize(config);
        dce.run(iCfg, Scene.v().getEntryPoints(), sourcesSinks, taintWrapper);
    }

    /**
     * Scans the given method for sources and sinks contained in it. Sinks are
     * just counted, sources are added to the InfoflowProblem as seeds.
     * @param sourcesSinks The SourceSinkManager to be used for identifying
     * sources and sinks
     * @param forwardProblem The InfoflowProblem in which to register the
     * sources as seeds
     * @param m The method to scan for sources and sinks
     * @return The number of sinks found in this method
     */
    private int scanMethodForSourcesSinks(
            final ISourceSinkManager sourcesSinks,
            PreProcessInfoflowProblem forwardProblem,
            SootMethod m) {
        if (getConfig().getLogSourcesAndSinks() && collectedSources == null) {
            collectedSources = new HashSet<>();
            collectedSinks = new HashSet<>();
        }

        int sinkCount = 0;
        if (m.hasActiveBody()) {
            // Check whether this is a system class we need to ignore
            final String className = m.getDeclaringClass().getName();
            if (config.getIgnoreFlowsInSystemPackages()
                    && SystemClassHandler.isClassInSystemPackage(className))
                return sinkCount;

            // Look for a source in the method. Also look for sinks. If we
            // have no sink in the program, we don't need to perform any
            // analysis
            PatchingChain<Unit> units = m.getActiveBody().getUnits();

            //Multi-Tag : debug test
            //logger.info(m.getActiveBody().toString());

            for (Unit u : units) {
                Stmt s = (Stmt) u;
                if (sourcesSinks.getSourceInfo(s, iCfg) != null) {
                    forwardProblem.addInitialSeeds(u, Collections.singleton(forwardProblem.zeroValue()));
                    if (getConfig().getLogSourcesAndSinks())
                        collectedSources.add(s);
                    logger.debug("Source found: {}", u);
                }
                if (sourcesSinks.isSink(s, iCfg, null)) {
                    sinkCount++;
                    if (getConfig().getLogSourcesAndSinks())
                        collectedSinks.add(s);
                    logger.debug("Sink found: {}", u);
                }
            }

        }
        return sinkCount;
    }

    public Set<SootMethod> getCallBackList() {
        return callBackList;
    }


}
