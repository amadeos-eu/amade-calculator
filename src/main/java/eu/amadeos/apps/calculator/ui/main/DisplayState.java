package eu.amadeos.apps.calculator.ui.main;

import eu.amadeos.apps.calculator.domain.Radix;

import java.math.BigInteger;
import java.util.Map;

/**
 * What the window shows after each key.
 *
 * @param expression   the upper line
 * @param value        the number, or the error text
 * @param error        true if {@code value} is an error text
 * @param memory       true if a number is in the memory (M)
 * @param second       true while 2nd is on
 * @param openBrackets how many brackets are open
 * @param radixValues  Programmer: the value in HEX, DEC, OCT and BIN; empty in the other modes
 * @param bits         Programmer: the bit pattern of the value; null in the other modes
 */
public record DisplayState(String expression, String value, boolean error, boolean memory, boolean second,
                           int openBrackets, Map<Radix, String> radixValues, BigInteger bits) {
    public DisplayState {
        radixValues = Map.copyOf(radixValues);
    }
}
