package dk.alexandra.fresco.demo;

import dk.alexandra.fresco.framework.Application;
import dk.alexandra.fresco.framework.DRes;
import dk.alexandra.fresco.framework.builder.numeric.Numeric;
import dk.alexandra.fresco.framework.builder.numeric.ProtocolBuilderNumeric;
import dk.alexandra.fresco.framework.value.SInt;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Demo application. Takes a number of inputs and converts them to secret shared inputs by having
 * party 1 input them all.
 *
 * @author Kasper Damgaard
 */
public class InputApplication implements Application<List<SInt>, ProtocolBuilderNumeric> {

  private int value;
  private int length;
  private int myId;
  private int numParties;

  public InputApplication(int value, int myId, int numParties) {
    this.value = value;
    this.myId = myId;
    this.numParties = numParties;
  }

  @Override
  public DRes<List<SInt>> buildComputation(ProtocolBuilderNumeric producer) {
    return 
    producer.par(par -> {
      Numeric numeric = par.numeric();
      List<DRes<SInt>> result = new ArrayList<>(length);
      for (int i = 1; i <= this.numParties; i++) {
        // create wires
    	 
    	/**
    	 * For n parties each party will add its value towards the sum under the position matching its id.
    	 * For example, party 1 will add the first value, party 5 will add the 5th value.
    	 * It seems each party executing this code must identify each party contributing in the same order.
    	 * If this party is not going to contribute a value then it provides a null value locally. 
    	 */
        if (i == myId) {
        	result.add(numeric.input(BigInteger.valueOf(value), i));
        } else {
        	result.add(numeric.input(null, i));
        }
      }
      return () -> result.stream().map(DRes::out).collect(Collectors.toList());
    });
  }
}
