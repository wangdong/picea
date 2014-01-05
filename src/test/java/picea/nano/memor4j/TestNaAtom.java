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
}