package anarchy.minesky.core.escmenu;

import anarchy.minesky.core.impl.escmenu.flows.PixAmountValidator;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PixAmountValidatorTest {

    @Test
    void rejectsZeroAndNegative() {
        assertFalse(PixAmountValidator.validate(0, 0.01, 1000).valid());
        assertFalse(PixAmountValidator.validate(-5, 0.01, 1000).valid());
        assertEquals("non_positive", PixAmountValidator.validate(0, 0.01, 1000).errorKey());
    }

    @Test
    void rejectsNanAndInfinite() {
        assertFalse(PixAmountValidator.validate(Double.NaN, 0.01, 1000).valid());
        assertFalse(PixAmountValidator.validate(Double.POSITIVE_INFINITY, 0.01, 1000).valid());
    }

    @Test
    void parsesBrazilianStyle() {
        PixAmountValidator.Result result = PixAmountValidator.validate("1.000,50", 0.01, 5000);
        assertTrue(result.valid());
        assertEquals(1000.50, result.amount(), 0.001);
    }

    @Test
    void respectsMinMax() {
        assertFalse(PixAmountValidator.validate(0.001, 0.01, 1000).valid());
        assertFalse(PixAmountValidator.validate(2000, 0.01, 1000).valid());
        assertTrue(PixAmountValidator.validate(50, 0.01, 1000).valid());
    }
}