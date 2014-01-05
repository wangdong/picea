package picea.nano.memor4j;

public abstract class NaAtomData implements KmoAtomHibernation {
	private int tip = AtomStoreImpl.sharedStoreImpl().tip();
	public final int tip() {
		return tip;
	}
	public void __forceOldest() {
		tip = 0;
	}
	public void hibernate() {
	}
	public void wakeup() {
	}
	public abstract NaAtomData copy();
}

class EmptyAtomData extends NaAtomData {
	public EmptyAtomData() {
		__forceOldest();
	}
	
	@Override
	public NaAtomData copy() {
		return this;
	}
	
}

interface KmoAtomHibernation {
	public void hibernate();
	public void wakeup();
}