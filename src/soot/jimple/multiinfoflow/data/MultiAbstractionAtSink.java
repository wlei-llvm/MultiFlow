package soot.jimple.multiinfoflow.data;

import soot.jimple.Stmt;
import soot.jimple.infoflow.data.Abstraction;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by wanglei .
 */
public class MultiAbstractionAtSink {


    private final List<Abstraction> abstractionPair = new ArrayList<>();
    private final Stmt sinkStmt;

    private final AbstractionVector abstractionVector;



    public MultiAbstractionAtSink(AbstractionVector absV, Stmt sinkStmt) {
       // this.abstractionPair.addAll(absV.getAbstractionVec());
        this.abstractionVector = absV;
        this.sinkStmt = sinkStmt;
    }

    public AbstractionVector getAbstractionVector() {
        return this.abstractionVector;
    }
    public Stmt getSinkStmt() {
        return this.sinkStmt;
    }

    public String toString() {
        String res = "";
//        res += "\n\t<<<Source:";
//        for(int i = 0; i < AbstractionVector.ABS_VECTOR_SIZE; i++) {
//            String begin = "\n\t<<<NO."+ (i + 1) + " Source: ";
//            Abstraction abs  = abstractionPair.get(i);
//            res += begin;
//            if(AbstractionVector.isEndAbstraction(abs)) {
//                res += abs.getPredecessor().getSourceContext();
//            }else {
//                res += abs.getSourceContext();
//            }
//            //res += "\n";
//        }
//        res += "\n\t>>>Sink:";
//        for(int i = 0; i < AbstractionVector.ABS_VECTOR_SIZE; i++) {
//            String begin = "\n\t>>>NO."+ (i + 1) + " Sink: ";
//            Abstraction abs  = abstractionPair.get(i);
//            res += begin;
//            if(AbstractionVector.isEndAbstraction(abs)) {
//                res += abs.getPredecessor() + " at " + abs.getCorrespondingCallSite();
//            }else {
//                res += abs + " at " + sinkStmt;
//            }
//            //res += "\n";
//        }
//        res += "\n\t=================================================";
//

        return res;
    }
}
