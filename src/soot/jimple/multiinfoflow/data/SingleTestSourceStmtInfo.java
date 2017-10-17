package soot.jimple.multiinfoflow.data;

import soot.jimple.Stmt;

import java.util.Set;

/**
 * @author wanglei
 */
public class SingleTestSourceStmtInfo extends AbstractMultiSourceStmtInfo {


    public SingleTestSourceStmtInfo() {

    }

    public void setValidSrcAndIndexForSingleTest(Set<Stmt> srcSet, Set<Stmt> sinkSet) {
        for(Stmt src : srcSet) {
            validSourceStmtToIndexMap.put(src, 0);
        }
        validSinkStmtSet.addAll(sinkSet);
    }

    public void setValidIndexForAllCombinationTest(Set<Stmt> srcSet, Set<Stmt> sinkSet) {
        int i = 0;
        for(Stmt src : srcSet) {
            validSourceStmtToIndexMap.put(src, i++);
        }
        validSinkStmtSet.addAll(sinkSet);
    }


}
