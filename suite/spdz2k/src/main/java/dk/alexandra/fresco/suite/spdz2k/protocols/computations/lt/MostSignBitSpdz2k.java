package dk.alexandra.fresco.suite.spdz2k.protocols.computations.lt;

import dk.alexandra.fresco.framework.DRes;
import dk.alexandra.fresco.framework.builder.Computation;
import dk.alexandra.fresco.framework.builder.numeric.Numeric;
import dk.alexandra.fresco.framework.builder.numeric.ProtocolBuilderNumeric;
import dk.alexandra.fresco.framework.util.Pair;
import dk.alexandra.fresco.framework.value.OInt;
import dk.alexandra.fresco.framework.value.OIntArithmetic;
import dk.alexandra.fresco.framework.value.SInt;
import dk.alexandra.fresco.lib.math.integer.binary.RandomBitMask;
import dk.alexandra.fresco.suite.spdz2k.datatypes.CompUInt;
import dk.alexandra.fresco.suite.spdz2k.datatypes.CompUIntFactory;

/**
 * Extract the value of the most significant bit of value.
 */
public class MostSignBitSpdz2k<PlainT extends CompUInt<?, ?, PlainT>> implements
    Computation<SInt, ProtocolBuilderNumeric> {

  private final DRes<SInt> value;
  private final CompUIntFactory<PlainT> factory;
  private final int k;

  public MostSignBitSpdz2k(DRes<SInt> value, CompUIntFactory<PlainT> factory) {
    this.value = value;
    this.factory = factory;
    this.k = factory.getLowBitLength();
  }

  @Override
  public DRes<SInt> buildComputation(ProtocolBuilderNumeric builder) {
    OIntArithmetic arithmetic = builder.getOIntArithmetic();
    DRes<OInt> twoTo2k1 = arithmetic.twoTo(k - 1);
    return builder.seq(seq -> seq.advancedNumeric().randomBitMask(k - 2))
        .seq((seq, mask) -> {
          Numeric numeric = seq.numeric();
          DRes<SInt> rPrime = mask.getValue();
          DRes<SInt> kthBit = numeric.randomBit();
          DRes<SInt> r = numeric.add(rPrime, numeric.multByOpen(twoTo2k1, kthBit));
          DRes<SInt> c = numeric.add(value, r);
          DRes<OInt> cOpen = numeric.openAsOInt(c);
          final Pair<DRes<OInt>, RandomBitMask> resPair = new Pair<>(cOpen, mask);
          return () -> resPair;
        }).seq((seq, pair) -> {
          Numeric nb = seq.numeric();
          PlainT cOpen = factory.fromOInt(pair.getFirst());
          PlainT cPrime = cOpen.clearAboveBitAt(k - 1);
          RandomBitMask mask = pair.getSecond();
          DRes<SInt> rPrime = mask.getValue();
          DRes<SInt> u = seq.comparison().compareLTBits(cPrime, mask.getBits());
          DRes<SInt> aPrime = nb.add(
              nb.subFromOpen(() -> cPrime, rPrime),
              seq.seq(ignored -> factory.toSpdz2kSIntBoolean(u).asArithmetic().out())
          );
          DRes<SInt> d = nb.sub(value, aPrime);
          DRes<SInt> b = nb.randomBit();
          DRes<SInt> e = nb.add(d, nb.multByOpen(twoTo2k1, b));
          DRes<OInt> eOpen = nb.openAsOInt(e);
          final Pair<DRes<OInt>, DRes<SInt>> resPair = new Pair<>(eOpen, b);
          return () -> resPair;
        }).seq((seq, pair) -> {
          Numeric nb = seq.numeric();
          PlainT eOpen = factory.fromOInt(pair.getFirst());
          DRes<SInt> b = pair.getSecond();
          PlainT eMsb = eOpen.testBitAsUInt(k - 1);
          PlainT eMsbByTwo = eMsb.multiply(factory.two());
          return nb.sub(
              nb.addOpen(eMsb, b),
              nb.multByOpen(eMsbByTwo, b)
          );
        });
  }
}
