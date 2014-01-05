package picea.nano.memor4j;

public abstract class NaAtom<AtomRawDataType extends NaAtomData> implements NaAtomDataEvent {
	private NaAtomData rawData;

	protected NaAtom(AtomRawDataType rhs) {
		rawData = (NaAtomData) rhs;
	}

	@SuppressWarnings("unchecked")
	protected final AtomRawDataType rawData() {
		return (AtomRawDataType) rawData;
	}

	public final void beforeWrite() {
		rawData = AtomStoreImpl.sharedStoreImpl().needCopy(this, rawData);
	}
	
	public final NaAtomData swapData(NaAtomData rhs) {
		NaAtomData tmp = rawData;
		rawData = rhs;
		return tmp;
	}
	
	public void willSwap(boolean isUndo) {
	}
	public void didFinishSwapping(boolean isUndo) {
	}
}

interface NaAtomDataEvent {
	public void willSwap(boolean isUndo);
	public void didFinishSwapping(boolean isUndo);
}
