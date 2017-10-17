package soot.jimple.multiinfoflow.data;

import heros.InterproceduralCFG;
import soot.SootMethod;
import soot.Unit;
import soot.jimple.infoflow.android.data.parsers.PermissionMethodParser;
import soot.jimple.infoflow.data.AbstractionAtSink;
import soot.jimple.infoflow.source.data.ISourceSinkDefinitionProvider;
import soot.jimple.infoflow.source.data.SourceSinkDefinition;
import soot.jimple.multiinfoflow.source.CustomMultiSourceSinkManager;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * @author wanglei
 */
public class CustomSourceStmtInfo extends AbstractMultiSourceStmtInfo {


    public CustomSourceStmtInfo() {

    }

    public void calculateSourcesSinks(List<String> sourceSinkFile, Set<AbstractionAtSink> res, InterproceduralCFG<Unit, SootMethod> cfg ) {

        ISourceSinkDefinitionProvider parser = null;

        try {
//            parser = PermissionMethodParser.fromFile(sourceSinkFile);
            parser = PermissionMethodParser.fromStringList(sourceSinkFile);



            CustomMultiSourceSinkManager sourceSinkManager = new CustomMultiSourceSinkManager(parser.getSources(), Collections.<SourceSinkDefinition>emptySet());

            for(AbstractionAtSink sink : res) {

                if(sourceSinkManager.addSourceInfo(sink.getAbstraction().getSourceContext().getStmt(), cfg)) {
                    validSinkStmtSet.add(sink.getSinkStmt());
                }

            }

            validSourceStmtToIndexMap.putAll(sourceSinkManager.getSourceToIndexMapping());
        }
        catch (IOException e) {
            System.err.println("CustomSourcesAndSinks.txt file not found ! ");
            e.printStackTrace();
        }


    }




}
