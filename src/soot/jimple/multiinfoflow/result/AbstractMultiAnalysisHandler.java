package soot.jimple.multiinfoflow.result;

import soot.jimple.infoflow.android.source.AndroidSourceSinkManager;
import soot.jimple.multiinfoflow.MultiInfoflow;

/**
 * @author wanglei
 */
public abstract  class AbstractMultiAnalysisHandler {

    MultiInfoflow multiInfoflow = null;

    AndroidSourceSinkManager sourceSinkManager = null;

    public AbstractMultiAnalysisHandler(MultiInfoflow multiInfoflow, AndroidSourceSinkManager sourcesSinks) {
        this.multiInfoflow = multiInfoflow;
        this.sourceSinkManager = sourcesSinks;

    }

    public abstract void run();



    protected MultiTaintPropagationResults runMultiAnalysis() {

        return multiInfoflow.runMultiAnalysis();
    }

}
