package soot.jimple.multiinfoflow.util;


/**
 * @author wanglei
 */
public class Triplet<F,S,T> {

    protected F o1;
    protected S o2;
    protected T o3;

    protected int hashCode = 0;

    public Triplet() {
        o1 = null;
        o2 = null;
        o3 = null;
    }

    public Triplet(F o1, S o2, T o3) {
        this.o1 = o1;
        this.o2 = o2;
        this.o3 = o3;
    }

    @Override
    public int hashCode() {
        if (hashCode != 0)
            return hashCode;

        final int prime = 31;
        int result = 1;
        result = prime * result + ((o1 == null) ? 0 : o1.hashCode());
        result = prime * result + ((o2 == null) ? 0 : o2.hashCode());
        result = prime * result + ((o3 == null) ? 0 : o3.hashCode());
        hashCode = result;

        return hashCode;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        @SuppressWarnings("rawtypes")
        Triplet other = (Triplet) obj;
        if (o1 == null) {
            if (other.o1 != null)
                return false;
        } else if (!o1.equals(other.o1))
            return false;
        if (o2 == null) {
            if (other.o2 != null)
                return false;
        } else if (!o2.equals(other.o2))
            return false;
        if (o3 == null) {
            if (other.o3 != null)
                return false;
        } else if (!o3.equals(other.o3))
            return false;
        return true;
    }

    public String toString() {
        return "Pair " + o1 + "," + o2;
    }

    public F getO1() {
        return o1;
    }

    public S getO2() {
        return o2;
    }

    public T getO3() {
        return o3;
    }

    public void setO1(F no1) {
        o1 = no1;
        hashCode = 0;
    }

    public void setO2(S no2) {
        o2 = no2;
        hashCode = 0;
    }

    public void setO3(T no3) {
        o3 = no3;
        hashCode = 0;
    }

    public void setPair(F no1, S no2, T no3) {
        o1 = no1;
        o2 = no2;
        o3 = no3;
        hashCode = 0;
    }

}
