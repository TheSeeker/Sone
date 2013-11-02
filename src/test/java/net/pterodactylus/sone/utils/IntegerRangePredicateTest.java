package net.pterodactylus.sone.utils;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import org.junit.Test;

/**
 * Unit test for {@link IntegerRangePredicate}.
 *
 * @author <a href="mailto:d.roden@xplosion.de">David Roden</a>
 */
public class IntegerRangePredicateTest {

	private final IntegerRangePredicate integerRangePredicate = new IntegerRangePredicate(-12, 38);

	@Test
	public void negativeValueInRange() {
		assertThat(integerRangePredicate.apply(-4), is(true));
	}

	@Test
	public void positiveValueInRange() {
		assertThat(integerRangePredicate.apply(7), is(true));
	}

	@Test
	public void negativeBoundaryInRange() {
		assertThat(integerRangePredicate.apply(-12), is(true));
	}

	@Test
	public void positiveBoundaryInRange() {
		assertThat(integerRangePredicate.apply(38), is(true));
	}

	@Test
	public void negativeValueOutOfRange() {
		assertThat(integerRangePredicate.apply(-24), is(false));
	}

	@Test
	public void positiveValueOutOfRange() {
		assertThat(integerRangePredicate.apply(40), is(false));
	}

}
