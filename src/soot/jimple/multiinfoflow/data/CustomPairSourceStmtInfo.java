package soot.jimple.multiinfoflow.data;

import heros.solver.Pair;
import soot.jimple.Stmt;

/**
 * @author wanglei
 */
public class CustomPairSourceStmtInfo extends AbstractMultiSourceStmtInfo {


    public CustomPairSourceStmtInfo() {

    }

    public void calculateSourcesSinks(Pair<Stmt, Stmt> firstInfo, Pair<Stmt, Stmt> secondInfo ) {

        validSourceStmtToIndexMap.put(firstInfo.getO1(), 0);
        validSourceStmtToIndexMap.put(secondInfo.getO1(), 1);
        validSinkStmtSet.add(firstInfo.getO2());
        validSinkStmtSet.add(secondInfo.getO2());
    }


}
