package picea.nano.memor4j;

import org.junit.Test;

import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.MatcherAssert.assertThat;

public class TestNaAtom {
    @Test
    public void testSingleton() {
        NaAtomStore store = NaAtomStore.sharedStore();
        assertThat( store, is(notNullValue()) );
    }

    @Test
    public void testBasicUndoRedo() {
        class ExampleData extends NaAtomData {
            protected String msg = "";
            @Override
            public NaAtomData copy() {
                ExampleData cp = new ExampleData();
                cp.msg = this.msg;
                return cp;
            }
        }

        class ExampleAtom extends NaAtom<ExampleData> {
            public ExampleAtom() {
                super(new ExampleData());
            }
            public void setMsg(String msg) {
                beforeWrite();
                rawData().msg = msg;
            }
            public String getMsg() {
                return rawData().msg;
            }
        }

        final NaAtomStore store = NaAtomStore.sharedStore();

        ExampleAtom exam = new ExampleAtom();

        store.start();
        exam.setMsg("hello");
        store.commit();

        store.start();
        exam.setMsg("world");
        store.commit();

        assertThat( exam.getMsg(), is( equalTo("world") ) );

        store.undo();
        assertThat( exam.getMsg(), is( equalTo("hello") ) );

        store.redo();
        assertThat( exam.getMsg(), is( equalTo("world") ) );
    }
}