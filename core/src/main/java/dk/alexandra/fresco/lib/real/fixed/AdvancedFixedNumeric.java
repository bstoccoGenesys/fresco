package dk.alexandra.fresco.lib.real.fixed;

import dk.alexandra.fresco.framework.DRes;
import dk.alexandra.fresco.framework.builder.numeric.AdvancedNumeric.RandomAdditiveMask;
import dk.alexandra.fresco.framework.builder.numeric.ProtocolBuilderNumeric;
import dk.alexandra.fresco.lib.real.DefaultAdvancedRealNumeric;
import dk.alexandra.fresco.lib.real.SReal;
import dk.alexandra.fresco.lib.real.algorithms.SquareRoot;
import dk.alexandra.fresco.lib.real.fixed.algorithms.Reciprocal;

public class AdvancedFixedNumeric extends DefaultAdvancedRealNumeric {

  public AdvancedFixedNumeric(ProtocolBuilderNumeric builder) {
    super(builder);
  }

  @Override
  public DRes<SReal> sqrt(DRes<SReal> x) {
    return new SquareRoot(x).buildComputation(builder);
  }

  @Override
  public DRes<SReal> random(int bits) {
    return builder.seq(seq -> {
      DRes<RandomAdditiveMask> random = seq.advancedNumeric().additiveMask(bits);
      return random;
    }).seq((seq, random) -> {
      return () -> new SFixed(random.random, bits);
    });
  }

  @Override
  public DRes<SReal> reciprocal(DRes<SReal> x) {
    return new Reciprocal(x).buildComputation(builder);
  }
}
