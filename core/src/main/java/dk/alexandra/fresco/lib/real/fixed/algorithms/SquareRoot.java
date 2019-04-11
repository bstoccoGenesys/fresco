package dk.alexandra.fresco.lib.real.fixed.algorithms;

import dk.alexandra.fresco.framework.DRes;
import dk.alexandra.fresco.framework.builder.Computation;
import dk.alexandra.fresco.framework.builder.numeric.ProtocolBuilderNumeric;
import dk.alexandra.fresco.lib.real.SReal;
import java.math.BigDecimal;

public class SquareRoot implements Computation<SReal, ProtocolBuilderNumeric> {

  // Input
  private final DRes<SReal> x;
  private DRes<SReal> initialValue;
  private int iterations;

  /**
   * Compute the square root iteratively. The initialValue should be close to the target result to
   * minimise the number of needed iterations.
   * 
   * @param x
   */
  public SquareRoot(DRes<SReal> x, DRes<SReal> initialValue, int iterations) {
    this.x = x;
    this.initialValue = initialValue;
    this.iterations = iterations;
  }

  /**
   * Compute the square root of x iteratively.
   * 
   * @param x
   */
  public SquareRoot(DRes<SReal> x) {
    this(x, null, -1);
  }

  @Override
  public DRes<SReal> buildComputation(ProtocolBuilderNumeric builder) {

    return builder.seq(seq -> {

      // Computing the reciprocal of the square rooot is very fast since it does not require
      // division, so we use x * 1/sqrt(x) as our first estimate.
      if (initialValue == null) {

        int reciprocalIterations;
        // These number has been found numerically
        if (builder.getRealNumericContext().getPrecision() < 20) {
          reciprocalIterations = 22;
        } else if (builder.getRealNumericContext().getPrecision() < 28) {
          reciprocalIterations = 32;
        } else {
          reciprocalIterations = 40;
        }

        initialValue = seq.realNumeric().mult(x,
            new ReciprocalSquareRoot(x, reciprocalIterations).buildComputation(seq));

        /*
         * Convergence is quadratic (the number of correct digits rougly doubles on each iteration)
         * but our first guess is very good, so we need just a few iterations to get a good result.
         */
        this.iterations = 2;
      }

      return new IterationState(1, initialValue);
    }).whileLoop((iterationState) -> iterationState.iteration < iterations,
        (seq, iterationState) -> {

          // We iterate y -> 1/2 (y + x/y)
          DRes<SReal> value = seq.realNumeric().add(iterationState.value,
              seq.realNumeric().div(x, iterationState.value));
          value = seq.realNumeric().mult(BigDecimal.valueOf(0.5), value);

          return new IterationState(iterationState.iteration + 1, value);
        }).seq((seq, iterationState) -> iterationState.value);
  };

  private static final class IterationState implements DRes<IterationState> {

    private final int iteration;
    private final DRes<SReal> value;

    private IterationState(int iteration, DRes<SReal> left) {
      this.iteration = iteration;
      this.value = left;
    }

    @Override
    public IterationState out() {
      return this;
    }
  }
}
