package soot.jimple.multiinfoflow.source;

import heros.InterproceduralCFG;
import soot.SootMethod;
import soot.Unit;
import soot.jimple.Stmt;
import soot.jimple.infoflow.android.source.AndroidSourceSinkManager;
import soot.jimple.infoflow.source.data.SourceSinkDefinition;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * @author wanglei
 */
public class CustomMultiSourceSinkManager extends AndroidSourceSinkManager {

    int globalIndex = 0;


    Map<Stmt, Integer> sourceToIndexMapping = new HashMap<>();
    Map<String, Integer> signatureToIndexMapping = new HashMap<>();

    public CustomMultiSourceSinkManager(Set<SourceSinkDefinition> sources, Set<SourceSinkDefinition> sinks) {
        super(sources, sinks);
    }

    public boolean addSourceInfo(Stmt sCallSite, InterproceduralCFG<Unit, SootMethod> cfg) {
        // Callbacks and UI controls are already properly handled by our parent
        // implementation
        SourceType type = getSourceType(sCallSite, cfg);
        if (type == SourceType.NoSource)
            return false;
//            return null;
        if (type == SourceType.Callback || type == SourceType.UISource)
            return false ;
//            return super.getSourceInfo(sCallSite, type);

        // This is a method-based source, so we need to obtain the correct
        // access path
        final String signature = methodToSignature.getUnchecked(
                sCallSite.getInvokeExpr().getMethod());

        if(!signatureToIndexMapping.containsKey(signature)) {
            signatureToIndexMapping.put(signature, globalIndex);
            globalIndex++;
        }

        sourceToIndexMapping.put(sCallSite, signatureToIndexMapping.get(signature));
        return true;

    }




    public Map<Stmt, Integer> getSourceToIndexMapping() {
        return sourceToIndexMapping;
    }
}
