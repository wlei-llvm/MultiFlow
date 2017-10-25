package soot.jimple.multiinfoflow.util;

public class NoDisPair<T, U> {
    protected T o1;
	protected U o2;

	protected int hashCode = 0;

	public NoDisPair() {
		o1 = null;
		o2 = null;
	}

	public NoDisPair(T o1, U o2) {
		this.o1 = o1;
		this.o2 = o2;
	}

	@Override
	public int hashCode() {
		if (hashCode != 0)
			return hashCode;
		
		final int prime = 31;
		int result = 1;
		result = result  + ((o1 == null) ? 0 : o1.hashCode());
		result = result  + ((o2 == null) ? 0 : o2.hashCode());
		hashCode = result;
		
		return hashCode;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;

        NoDisPair other = (NoDisPair) obj;

		if(o1 == other.getO1() && o2 == other.getO2() )
			return true;
		if(o2 == other.getO1() && o1 == other.getO2() )
			return true;
		if(o1.equals(other.getO1()) && o2.equals(other.getO2()) )
			return true;
		if(o2.equals(other.getO1()) && o1.equals(other.getO2()) )
			return true;

		return false;
	}

	public String toString() {
		return "Pair " + o1 + "," + o2;
	}

	public T getO1() {
		return o1;
	}

	public U getO2() {
		return o2;
	}

	public void setO1(T no1) {
		o1 = no1;
		hashCode = 0;
	}

	public void setO2(U no2) {
		o2 = no2;
		hashCode = 0;
	}

	public void setPair(T no1, U no2) {
		o1 = no1;
		o2 = no2;
		hashCode = 0;
	}


}
