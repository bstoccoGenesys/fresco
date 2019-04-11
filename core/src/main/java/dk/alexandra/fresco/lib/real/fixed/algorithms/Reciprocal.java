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
  private int iterations;

  /**
   * Compute the reciprocal value of the input iteratively. The input cannot be larger than
   * <i>2<sup>defaultPrecision / 2</sup></i>.
   * 
   * This algorithm iterates over x -> x(2 - x * input).
   * 
   * @param input A secret value.
   * @param iterations The number of iterations.
   */
  public Reciprocal(DRes<SReal> input, int iterations) {
    this.x = input;
    this.iterations = iterations;
  }

  /**
   * Compute the reciprocal value of the input iteratively. The input cannot be larger than
   * <i>2<sup>defaultPrecision / 2</sup></i>.
   * 
   * This algorithm iterates over x -> x(2 - x * input). The number of iterations is chosen such
   * that the relative error is < 0.5% for inputs in the interval <i>[2<sup>-8</sup>,
   * 2<sup>8</sup>]</i> for precision at least 16. For smaller precision <i>p < 16</i> the result
   * has to be greater than <i>2<sup>-p/2</sup></i> so the input has to be smaller than
   * <i>2<sup>p/2</sup></i>.
   * 
   * @param input A secret value.
   */
  public Reciprocal(DRes<SReal> input) {
    this(input,  -1);
  }
  
  @Override
  public DRes<SReal> buildComputation(ProtocolBuilderNumeric builder) {

    if (iterations < 0) {
      // The number of iterations has been found numerically
      if (builder.getRealNumericContext().getPrecision() <= 16) {
        this.iterations = 20;
      } else {
        this.iterations = 30;
      }       
    }
    
    return builder.seq(seq -> {
      SFixed input = (SFixed) x.out();
      
      // Starting point is 2^{-p/2}, so the input has to be at least 2^{p/2}
      int initialPrecision = builder.getRealNumericContext().getPrecision() / 2;
      SFixed estimate = new SFixed(seq.numeric().known(BigInteger.ONE), initialPrecision);

      return new IterationState(1, estimate, input);
    }).whileLoop((iterationState) -> iterationState.iteration < iterations,
        (seq, iterationState) -> {

          // We iterate y -> y(2 - xy) and truncate only at the end of each iteration

          DRes<SInt> y = iterationState.value.getSInt();

          DRes<SInt> value = seq.numeric().mult(y, iterationState.input.getSInt());
          
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
    private final SFixed input, value;

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
