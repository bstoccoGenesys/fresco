package dk.alexandra.fresco.suite.spdz2k.protocols.natives;

import dk.alexandra.fresco.framework.DRes;
import dk.alexandra.fresco.framework.network.Network;
import dk.alexandra.fresco.framework.value.OInt;
import dk.alexandra.fresco.framework.value.SInt;
import dk.alexandra.fresco.suite.spdz2k.datatypes.CompUInt;
import dk.alexandra.fresco.suite.spdz2k.datatypes.CompUIntFactory;
import dk.alexandra.fresco.suite.spdz2k.datatypes.Spdz2kSIntBoolean;
import dk.alexandra.fresco.suite.spdz2k.resource.Spdz2kResourcePool;
import java.util.ArrayList;
import java.util.List;

public class Spdz2kXorKnownBatchedProtocol<PlainT extends CompUInt<?, ?, PlainT>> extends
    Spdz2kNativeProtocol<List<DRes<SInt>>, PlainT> {

  private final DRes<List<OInt>> left;
  private final DRes<List<DRes<SInt>>> right;
  private List<DRes<SInt>> result;

  public Spdz2kXorKnownBatchedProtocol(
      DRes<List<OInt>> left,
      DRes<List<DRes<SInt>>> right) {
    this.left = left;
    this.right = right;
  }

  @Override
  public EvaluationStatus evaluate(int round, Spdz2kResourcePool<PlainT> resourcePool,
      Network network) {
    CompUIntFactory<PlainT> factory = resourcePool.getFactory();
    PlainT secretSharedKey = resourcePool.getDataSupplier().getSecretSharedKey();
    List<OInt> leftOut = left.out();
    List<DRes<SInt>> rightOut = right.out();
    if (leftOut.size() != rightOut.size()) {
      throw new IllegalArgumentException("Lists must be same size");
    }
    this.result = new ArrayList<>(leftOut.size());
    for (int i = 0; i < leftOut.size(); i++) {
      PlainT knownBit = factory.fromOInt(leftOut.get(i)).toBitRep();
      DRes<SInt> secretBit = rightOut.get(i);
      Spdz2kSIntBoolean<PlainT> xoredBit = factory.toSpdz2kSIntBoolean(secretBit)
          .xorOpen(knownBit, secretSharedKey, factory.zero().toBitRep(),
              resourcePool.getMyId() == 1);
      result.add(xoredBit);
    }
    return EvaluationStatus.IS_DONE;
  }

  @Override
  public List<DRes<SInt>> out() {
    return result;
  }

}
