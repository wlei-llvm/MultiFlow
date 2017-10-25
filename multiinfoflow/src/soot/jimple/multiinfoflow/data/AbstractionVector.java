package soot.jimple.multiinfoflow.data;

import heros.solver.Pair;
import soot.NullType;
import soot.Unit;
import soot.jimple.Stmt;
import soot.jimple.infoflow.data.Abstraction;
import soot.jimple.infoflow.data.AccessPath;
import soot.jimple.infoflow.data.AccessPathFactory;
import soot.jimple.infoflow.data.SourceContext;
import soot.jimple.infoflow.solver.fastSolver.FastSolverLinkedNode;
import soot.jimple.internal.JimpleLocal;
import soot.jimple.multiinfoflow.MultiInfoflow;
import soot.jimple.multiinfoflow.util.Triplet;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * @author wanglei
 */
public class AbstractionVector implements FastSolverLinkedNode<AbstractionVector, Unit> {


    private final Map<Integer,Abstraction> abstractionMap ;

    private int hashCode = 0;

    public static AccessPath endAP = AccessPathFactory.v().createAccessPath(new JimpleLocal("end", NullType.v()), false);

    public int endflag = 0;

    public AbstractionVector() {

        abstractionMap = new HashMap<>();

    }

    public AbstractionVector(Map<Integer, Abstraction> absMap) {
        abstractionMap = absMap;
    }



    public Set<Integer> getIndexSet() {

        return abstractionMap.keySet();
    }
    public boolean isZeroAbstractionVec() {
        return abstractionMap.size() == 0;
    }

    public Set<Map.Entry<Integer, Abstraction>> getEntrySet() {
        return abstractionMap.entrySet();
    }

//
//    public Abstraction getD1ByIndex(int index) {
//        if(!abstractionMap.containsKey(index))
//            return getZeroAbstractionElement();
//        return abstractionMap.get(index);
//    }

    public Abstraction getAbstractionByIndex(int index) {

        return abstractionMap.get(index);
    }

    public Map<Integer, SourceContext> getSourceContextMap() {
        Map<Integer, SourceContext> sourceContextMap = new HashMap<>();

        for(Map.Entry<Integer, Abstraction> entry : abstractionMap.entrySet())  {
            int i = entry.getKey();
            Abstraction abs = entry.getValue();
            sourceContextMap.put(i, abs.getSourceContext());
        }

        return sourceContextMap;
    }

    public static AbstractionVector getZeroAbstractionVec() {

        AbstractionVector AbsVec = new AbstractionVector();

        return  AbsVec;
    }



    public static Abstraction getEndAbstraction() {
        return  new Abstraction(endAP,
                null, false, false);
    }

    public static boolean isEndAbstraction(Abstraction abs) {return isSpecialAbstraction(abs, "end");}

    public static boolean isZeroAbstraction(Abstraction abs) {
        return isSpecialAbstraction(abs, "zero");
    }


    private static boolean isSpecialAbstraction(Abstraction abs, String specialName) {
        if(abs.getAccessPath() == null)
            return false;
        if(abs.getAccessPath().getBaseType() == null)
            return false;
        if(!abs.getAccessPath().getBaseType().equals(NullType.v())){
            return false;
        }
        if(abs.getAccessPath().getPlainValue().getName() != specialName) {
            return false;
        }
        return true;

    }

    public boolean isAllEndAbs() {

        int count = 0;
        for(Abstraction abs : abstractionMap.values()) {
            if(!isEndAbstraction(abs))
                return false;
            else
                count++;
        }
        if(count == MultiInfoflow.validSourceStmtInfo.getValidSourceStmtInfo().size())
            return true;
        else
            return false;
    }

    public  boolean isSameAccessPath() {
        if(abstractionMap.size() <= 1)
            return false;
        AccessPath ap = null;
        boolean flag = true;
        for(Abstraction abs : abstractionMap.values()) {
            if(flag) {
                ap = abs.getAccessPath();
                flag = false;
            }else if(!abs.getAccessPath().equals(ap)) {
                return false;
            }
        }
        return true;
    }



    public Set<AbstractionVector> deriveNewAbstractionVecSetByAbsSet(Set<Abstraction> absSet, int index) {

        Set<AbstractionVector> result = new HashSet<>();

        for(Abstraction abs: absSet) {
            result.add(deriveNewAbstractionVec(abs, index));
        }

        return result;
    }

    public AbstractionVector deriveEndAbstractionVecByIndexSet(Set<Integer> mapping, Stmt stmt) {

        Map<Integer, Abstraction> absMap = new HashMap<>();

        for(Map.Entry<Integer, Abstraction> entry : abstractionMap.entrySet()) {
            int i = entry.getKey();
            Abstraction abs = entry.getValue();
            if(mapping.contains(i)) {
                Abstraction endAbs = deriveEnd(abs, stmt);
                absMap.put(i, endAbs);
            }else {
                absMap.put(i , abs);
            }
        }

        AbstractionVector newVec = new AbstractionVector(absMap);
        return newVec;
    }

    public AbstractionVector deriveAllEndMeetSameAP(Stmt stmt) {
//        System.out.println("Meet same AP!");
        Map<Integer, Abstraction> absMap = new HashMap<>();

        for(Map.Entry<Integer, Abstraction> entry : abstractionMap.entrySet()) {
            int i = entry.getKey();
            Abstraction abs = entry.getValue();
            Abstraction endAbs = deriveEnd(abs, stmt);
            absMap.put(i, endAbs);
        }

        AbstractionVector newVec = new AbstractionVector(absMap);
        newVec.endflag = 2;
        return newVec;
    }

    public static Abstraction deriveEnd(Abstraction abs, Stmt stmt) {
        Abstraction endAbs = getEndAbstraction();
        endAbs.setEndSinkStmt(stmt);
        endAbs.setEndAbstraction(abs);
        endAbs.setSourceContext(abs.getSourceContext());
        return endAbs;
    }


    public final AbstractionVector deriveNewAbstractionVec(Abstraction newAbs, int toReplaceIndex) {


        Map<Integer, Abstraction> absMap = new HashMap<>();
        for(Map.Entry<Integer, Abstraction> entry : abstractionMap.entrySet()){
            int i = entry.getKey();
            Abstraction abs = entry.getValue();
            absMap.put(i, abs);
        }
        absMap.put(toReplaceIndex, newAbs);

        AbstractionVector newVec = new AbstractionVector(absMap);

        return newVec;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null || getClass() != obj.getClass())
            return false;
        AbstractionVector other = (AbstractionVector) obj;

        // If we have already computed hash codes, we can use them for
        // comparison
        if (this.hashCode != 0
                && other.hashCode != 0
                && this.hashCode != other.hashCode)
            return false;
        if(other.abstractionMap.size() != this.abstractionMap.size())
            return false;
        if(other.abstractionMap.size() == 0)
            return true;
        for(Map.Entry<Integer, Abstraction> entry :abstractionMap.entrySet()) {
            int i = entry.getKey();
            Abstraction abs = entry.getValue();
            if(other.getAbstractionByIndex(i) == null && abs != null)
                return false;
            if(!other.getAbstractionByIndex(i).equals(abs))
                return false;
        }

        for(Map.Entry<Integer, Abstraction> entry :other.getEntrySet()) {
            int i = entry.getKey();
            Abstraction abs = entry.getValue();
            if(this.getAbstractionByIndex(i) == null && abs != null)
                return false;
            if(!this.getAbstractionByIndex(i).equals(abs))
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

        if(abstractionMap.size() == 0)
            return 1;
        // deliberately ignore prevAbs
        for(Map.Entry<Integer, Abstraction> entry :abstractionMap.entrySet())  {
            int i = entry.getKey();
            Abstraction abs = entry.getValue();
            result = result* prime + abs.hashCode() + i;
        }

        this.hashCode = result;

        return this.hashCode;
    }


    @Override
    public String toString(){

        if(abstractionMap.size() == 0)
            return "Zero";

        StringBuilder builder = new StringBuilder();
        for(int i : getIndexSet()) {
            builder.append("{(");
            builder.append(i);
            builder.append("): ");
            builder.append(abstractionMap.get(i));
            builder.append("}        ");
        }
        return builder.toString();
    }



    @Override
    public void setPredecessor(AbstractionVector predecessor) {

    }

    @Override
    public AbstractionVector getPredecessor() {
        return null;
    }

    @Override
    public void setSummaryPredecessor(Pair<Triplet<Unit, Unit, AbstractionVector>, AbstractionVector> summaryPredecessor) {

    }

    @Override
    public Pair<Triplet<Unit, Unit, AbstractionVector>, AbstractionVector> getSummaryPredecessor() {
        return null;
    }

    @Override
    public AbstractionVector clone() {
        return null;
    }

    @Override
    public void addNeighbor(AbstractionVector originalAbstraction) {

    }

    @Override
    public void setCallingContext(AbstractionVector callingContext) {

    }
}
