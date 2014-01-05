package picea.nano.memor4j;

import org.junit.Test;
import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.MatcherAssert.assertThat;

public class TestNaAtom {
    @Test
    public void testBasic() {
        assertThat( 1+1, is(equalTo(2)) );
    }
}