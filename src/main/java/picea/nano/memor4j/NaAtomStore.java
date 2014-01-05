package picea.nano.memor4j;

import java.util.ArrayList;
import java.util.LinkedList;

public abstract class NaAtomStore {
	public static final int DEFAULT_MAX_UNDO_STEPS = 20;

	public abstract void reset();

	public abstract int maxSteps();
	public abstract void setMaxSteps(int maxSteps);

	public abstract void snapshot();

	public abstract boolean canUndo();
	public abstract void undo();
	public abstract void start();
	public abstract void discard();
	public abstract boolean isAtomStarted();

	public abstract NaTipStamp getCurrentTipStamp();

	public abstract boolean isTheSameTipStamp(NaTipStamp ts);
	
	/**
	 * @return true, 提交成功
	 * 		false, 提交失败或者事务为空
	 */
	public abstract boolean commit();

	public abstract boolean canRedo();
	public abstract void redo();

	private static NaAtomStore inst;
	public static NaAtomStore sharedStore() {
		if (inst == null)
			inst = new AtomStoreImpl();
		return inst;
	}
	/**
	 * 清空实例，仅供ShutDown使用
	 */
	public static void clearUp() {
		inst = null;
	}
}

final class AtomSnapshot {
	final class AtomImage {
		public NaAtom<? extends NaAtomData> atom;
		public NaAtomData data;
	}
	private final int tip = AtomStoreImpl.sharedStoreImpl().tip();

	private final ArrayList<AtomImage> atomImages = new ArrayList<AtomImage>();

	public void append(NaAtom<? extends NaAtomData> atom, NaAtomData data) {
		AtomImage image = new AtomImage();
		image.atom = atom;
		image.data = data;
		atomImages.add(image);
	}

	public void swap(boolean isUndo) {
		//这里之所以有顺序之分，是因为Redo和Undo的数据方向不一样。
		if (!isUndo) {
			for (AtomImage image : atomImages) {
				image.atom.willSwap(isUndo);
				image.data = image.atom.swapData(image.data);
				image.atom.didFinishSwapping(isUndo);
			}
		}
		else {
			for (int i = atomImages.size() - 1; i >= 0; --i) {
				AtomImage image = atomImages.get(i);
				image.atom.willSwap(isUndo);
				image.data = image.atom.swapData(image.data);
				image.atom.didFinishSwapping(isUndo);
			}
		}
	}

	public void clear() {
		atomImages.clear();
	}

	public boolean isEmpty() {
		return atomImages.isEmpty();
	}
	
	public int tip() {
		return tip;
	}
}

final class AtomSnapshotStack {
	private final LinkedList<AtomSnapshot> snapshots = new LinkedList<AtomSnapshot>();
	private int top = -1;
	private int capability = NaAtomStore.DEFAULT_MAX_UNDO_STEPS;

	public int capability() {
		return capability;
	}

	public void setCapability(int capability) {
		this.capability = capability;
		recapability();
	}

	public void forward() {
		if (snapshotsAhead() == 0)
			snapshots.add(new AtomSnapshot());
		++top;

		recapability();
	}

	public void forwardBySnap(AtomSnapshot snap) {
		if (snapshotsAhead() == 0)
			snapshots.add(snap);

		++top;
		recapability();
	}

	public void backward() {
		if (top > -1)
			--top;
	}

	public int snapshotsAhead() {
		return snapshots.size() - (top + 1);
	}

	public void cleanupAhead() {
		int size = snapshots.size();
		for (int i = top + 1; i < size; ++i)
			snapshots.remove(snapshots.size() - 1);
	}

	public boolean isEmpty() {
		return top == -1;
	}

	public AtomSnapshot top() {
		if (snapshots.isEmpty()) {
			assert false;
			return new AtomSnapshot();
		}
		return snapshots.get(top);
	}

	public void reset() {
		for (int i = 0; i < snapshots.size(); ++i)
			snapshots.get(i).clear();
		snapshots.clear();
		top = -1;
		capability = NaAtomStore.DEFAULT_MAX_UNDO_STEPS;
	}

	private void recapability() {
		if (top >= capability) {
			int count = top - capability + 1;
			for (int i = 0; i < count; ++i) {
				AtomSnapshot snapshot = snapshots.get(0);
				snapshot.clear();
				snapshots.remove(0);
				--top;
			}
		}
	}
}

class TipStampImpl implements NaTipStamp {
	int tip;
	public TipStampImpl(int tip) {
		this.tip = tip;
	}
}

final class AtomStoreImpl extends NaAtomStore {
	private final AtomSnapshotStack snapshotStack = new AtomSnapshotStack();
	//当前工作的快照，当修改正式提交了以后才把快照存入快照列表中。
	private AtomSnapshot workingSnap = null;
	private int tip = 0;
	private volatile int nextTip = 1;

	@Override
	public NaTipStamp getCurrentTipStamp() {
		return new TipStampImpl(tip());
	}

	@Override
	public boolean isTheSameTipStamp(NaTipStamp ts) {
		return ts != null && ((TipStampImpl) ts).tip == tip();
	}
	
	public int tip() {
		return tip;
	}

	@Override
	public int maxSteps() {
		return snapshotStack.capability();
	}

	@Override
	public void setMaxSteps(int maxSteps) {
		if (maxSteps > 0)
			snapshotStack.setCapability(maxSteps);
	}

	@Override
	public void start() {
		tip = nextTip++;
		workingSnap = new AtomSnapshot();
	}

	@Override
	public void snapshot() {
		tip = nextTip++;
		if (canRedo())
			snapshotStack.cleanupAhead();
		snapshotStack.forward();
	}

	@Override
	public void reset() {
		workingSnap = null;
		snapshotStack.reset();
	}

	@Override
	public boolean canUndo() {
		return !snapshotStack.isEmpty();
	}

	/**
	 * Undo的本质就是拿历史状态和当前的工作状态置换
	 *
	 */
	@Override
	public void undo() {
		if (canUndo()) {
			snapshotStack.top().swap(true);
			snapshotStack.backward();
			tip = snapshotStack.isEmpty() ? 0 : snapshotStack.top().tip();
		}
	}

	@Override
	public void discard() {
		workingSnap.swap(true);
		workingSnap = null;
		tip = snapshotStack.isEmpty() ? 0 : snapshotStack.top().tip();
	}

	@Override
	public boolean commit() {
		assert workingSnap != null;
		if (workingSnap.isEmpty())
			return false;

		if (canRedo())
			snapshotStack.cleanupAhead();
		snapshotStack.forwardBySnap(workingSnap);
		workingSnap = null;
		return true;
	}

	@Override
	public boolean canRedo() {
		return snapshotStack.snapshotsAhead() > 0;
	}

	@Override
	public void redo() {
		if (canRedo()) {
			snapshotStack.forward();
			snapshotStack.top().swap(false);
			tip = snapshotStack.top().tip();
		}
	}

	public NaAtomData needCopy(NaAtom<? extends NaAtomData> atom, NaAtomData data) {
		if (data.tip() < tip()) {
			//先添加到工作副本上，等待正式提交以后才加入副本队列
			if (workingSnap != null) {
				workingSnap.append(atom, data);
				data = data.copy();
				return data;
			}
		}
		return data;
	}

	public static AtomStoreImpl sharedStoreImpl() {
		return (AtomStoreImpl) sharedStore();
	}

	@Override
	public boolean isAtomStarted() {
		return workingSnap != null; 
	}
}
