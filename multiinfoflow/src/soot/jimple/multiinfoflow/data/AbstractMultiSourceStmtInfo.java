package soot.jimple.multiinfoflow.data;

import soot.jimple.Stmt;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * @author wanglei
 */
public abstract class AbstractMultiSourceStmtInfo {

    //private Set<Stmt> validSourceStmtInfo = null;

    protected Map<Stmt, Integer> validSourceStmtToIndexMap = new HashMap<>();

    protected Set<Stmt> validSinkStmtSet = new HashSet<>();

    public AbstractMultiSourceStmtInfo() {

    }

    public Set<Stmt> getValidSourceStmtInfo() {
        return validSourceStmtToIndexMap.keySet();
    }

    public int getSourceIndex(Stmt sourceStmt) {
        if(!validSourceStmtToIndexMap.containsKey(sourceStmt)) {
            return -1;
        }
        return validSourceStmtToIndexMap.get(sourceStmt);
    }

    public boolean isValidSink(Stmt stmt) {
        return validSinkStmtSet.contains(stmt);
    }

    public void clear() {

        validSourceStmtToIndexMap.clear();
        validSinkStmtSet.clear();
    }



}
