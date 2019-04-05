package dk.alexandra.fresco.lib.real.fixed.algorithms;

import dk.alexandra.fresco.framework.DRes;
import dk.alexandra.fresco.framework.builder.Computation;
import dk.alexandra.fresco.framework.builder.numeric.ProtocolBuilderNumeric;
import dk.alexandra.fresco.framework.value.SInt;
import dk.alexandra.fresco.lib.math.integer.binary.Truncate;
import dk.alexandra.fresco.lib.real.SReal;
import dk.alexandra.fresco.lib.real.fixed.SFixed;
import java.math.BigInteger;

public class Reciprocal implements Computation<SReal, ProtocolBuilderNumeric> {

  // Input
  private final DRes<SReal> x;

  /**
   * Compute the reciprocal value of the input iteratively. The input cannot be larger than
   * <i>2<sup>defaultPrecision / 2</sup></i>.
   * 
   * @param input
   */
  public Reciprocal(DRes<SReal> input) {
    this.x = input;
  }

  @Override
  public DRes<SReal> buildComputation(ProtocolBuilderNumeric builder) {

    int iterations = 16;

    return builder.seq(seq -> {
      SFixed input = (SFixed) x.out();

      DRes<SInt> estimate = seq.numeric().known(BigInteger.ONE);
      int initialScale = builder.getRealNumericContext().getPrecision() / 2;

      return new IterationState(1, new SFixed(estimate, initialScale), input);
    }).whileLoop((iterationState) -> iterationState.iteration < iterations,
        (seq, iterationState) -> {

          // We iterate y -> y(2 - xy)

          DRes<SInt> y = iterationState.value.getSInt();

          DRes<SInt> value = seq.numeric().mult(y, iterationState.input.getSInt());

          // Subtracting
          BigInteger two = BigInteger.valueOf(2)
              .shiftLeft(iterationState.input.getPrecision() + iterationState.value.getPrecision());
          value = seq.numeric().sub(two, value);

          value = seq.numeric().mult(y, value);

          int targetPrecision = seq.getRealNumericContext().getPrecision();
          int shifts = 2 * iterationState.value.getPrecision() + iterationState.input.getPrecision()
              - targetPrecision;

          value = new Truncate(value, shifts).buildComputation(seq);

          return new IterationState(iterationState.iteration + 1,
              new SFixed(value, targetPrecision), iterationState.input);
        }).seq((seq, iterationState) -> iterationState.value);
  };

  private static final class IterationState implements DRes<IterationState> {

    private final int iteration;
    private SFixed input, value;

    private IterationState(int iteration, SFixed value, SFixed input) {
      this.iteration = iteration;
      this.value = value;
      this.input = input;
    }

    @Override
    public IterationState out() {
      return this;
    }
  }
}
