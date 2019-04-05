package dk.alexandra.fresco.lib.real.fixed.algorithms;

import dk.alexandra.fresco.framework.DRes;
import dk.alexandra.fresco.framework.builder.Computation;
import dk.alexandra.fresco.framework.builder.numeric.ProtocolBuilderNumeric;
import dk.alexandra.fresco.framework.value.SInt;
import dk.alexandra.fresco.lib.math.integer.binary.Truncate;
import dk.alexandra.fresco.lib.real.SReal;
import dk.alexandra.fresco.lib.real.fixed.SFixed;
import java.math.BigInteger;

public class ReciprocalSquareRoot implements Computation<SReal, ProtocolBuilderNumeric> {

  // Input
  private final DRes<SReal> x;

  /**
   * Compute the reciprocal of the square root of the input. The answer has to be larger than
   * 2^{-defaultPrecision / 2}, so the input cannot be larger than 2^defaultPrecision.
   * 
   * @param input
   */
  public ReciprocalSquareRoot(DRes<SReal> input) {
    this.x = input;
  }

  @Override
  public DRes<SReal> buildComputation(ProtocolBuilderNumeric builder) {

    int iterations = 20;

    return builder.seq(seq -> {
      SFixed cast = (SFixed) x.out();

      int initialPrecision = builder.getRealNumericContext().getPrecision() / 2;
      SFixed estimate = new SFixed(seq.numeric().known(BigInteger.ONE), initialPrecision);
      
      return new IterationState(1, estimate, cast);
    }).whileLoop((iterationState) -> iterationState.iteration < iterations,
        (seq, iterationState) -> {

          /*
           * We iterate y -> y/2 (3 - x y^2) and truncate only at the end of each iteration
           */

          DRes<SInt> y = iterationState.value.out().getSInt();

          // scale = 2*iterationState.scale;
          DRes<SInt> value = seq.numeric().mult(y, y);

          // scale += iterationState.x.getPrecision();
          value = seq.numeric().mult(value, iterationState.input.getSInt());

          BigInteger three = BigInteger.valueOf(3)
              .shiftLeft(2 * iterationState.value.getPrecision() + iterationState.input.getPrecision());
          value = seq.numeric().sub(three, value);

          // scale += iterationState.scale
          value = seq.numeric().mult(y, value);

          int targetPrecision = seq.getRealNumericContext().getPrecision();

          // We aim at the precision defined above and have multiplied x with the iteration state
          // three times and we divide by 2
          int shifts = 3 * iterationState.value.getPrecision() + iterationState.input.getPrecision()
              - targetPrecision + 1;

          value = new Truncate(value, shifts).buildComputation(seq);

          return new IterationState(iterationState.iteration + 1,
              new SFixed(value, targetPrecision),              iterationState.input);
        }).seq((seq, iterationState) -> iterationState.value);
  };

  private static final class IterationState implements DRes<IterationState> {

    private final int iteration;
    private SFixed input, value;

    private IterationState(int iteration, SFixed value, SFixed x) {
      this.iteration = iteration;
      this.value = value;
      this.input = x;
    }

    @Override
    public IterationState out() {
      return this;
    }
  }
}
