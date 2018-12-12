package dk.alexandra.fresco.tools.mascot.triple;

import dk.alexandra.fresco.framework.builder.numeric.Addable;
import dk.alexandra.fresco.framework.builder.numeric.FieldElement;
import dk.alexandra.fresco.framework.builder.numeric.FieldElementUtils;
import dk.alexandra.fresco.framework.util.StrictBitVector;
import dk.alexandra.fresco.tools.mascot.CustomAsserts;
import dk.alexandra.fresco.tools.mascot.MascotSecurityParameters;
import dk.alexandra.fresco.tools.mascot.MascotTestContext;
import dk.alexandra.fresco.tools.mascot.MascotTestUtils;
import dk.alexandra.fresco.tools.mascot.NetworkedTest;
import dk.alexandra.fresco.tools.mascot.field.MultiplicationTriple;
import dk.alexandra.fresco.tools.mascot.prg.FieldElementPrg;
import dk.alexandra.fresco.tools.mascot.prg.FieldElementPrgImpl;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Callable;
import org.hamcrest.collection.IsCollectionWithSize;
import org.junit.Assert;
import org.junit.Test;

public class TestTripleGeneration extends NetworkedTest {

  private FieldElementPrg getJointPrg(int prgSeedLength) {
    return new FieldElementPrgImpl(new StrictBitVector(prgSeedLength));
  }

  private List<FieldElement> runSinglePartyMult(MascotTestContext ctx, FieldElement macKeyShare,
      List<FieldElement> leftFactorGroups, List<FieldElement> rightFactors) {
    TripleGeneration tripleGen = new TripleGeneration(ctx.getResourcePool(), ctx.getNetwork(),
        getJointPrg(ctx.getPrgSeedLength()), macKeyShare);
    return tripleGen.multiply(leftFactorGroups, rightFactors);
  }

  private List<MultiplicationTriple> runSinglePartyTriple(MascotTestContext ctx,
      FieldElement macKeyShare,
      int numTriples) {
    TripleGeneration tripleGen = new TripleGeneration(ctx.getResourcePool(), ctx.getNetwork(),
        getJointPrg(ctx.getPrgSeedLength()), macKeyShare);
    return tripleGen.triple(numTriples);
  }

  private List<MultiplicationTriple> runSinglePartyTripleRepeated(MascotTestContext ctx,
      FieldElement macKeyShare, int numTriples, int numIterations) {
    TripleGeneration tripleGen = new TripleGeneration(ctx.getResourcePool(), ctx.getNetwork(),
        getJointPrg(ctx.getPrgSeedLength()), macKeyShare);
    List<MultiplicationTriple> triples = new ArrayList<>();
    for (int r = 0; r < numIterations; r++) {
      triples.addAll(tripleGen.triple(numTriples));
    }
    return triples;
  }

  @Test
  public void testTwoPartiesSingleMult() {
    // configure number of left factors
    initContexts(2, new MascotSecurityParameters(getDefaultParameters().getModBitLength(),
        getDefaultParameters().getLambdaSecurityParam(), getDefaultParameters().getPrgSeedLength(), 1));

    // left party mac key share
    FieldElement macKeyShareOne = new FieldElement(new BigInteger("11231"), getModulus());

    // right party mac key share
    FieldElement macKeyShareTwo = new FieldElement(new BigInteger("7719"), getModulus());

    // party one input
    int[] leftArrOne = {12};
    List<FieldElement> leftFactorsOne =
        MascotTestUtils.generateSingleRow(leftArrOne, getModulus());
    int[] rightArrOne = {11};
    List<FieldElement> rightFactorsOne =
        MascotTestUtils.generateSingleRow(rightArrOne, getModulus());

    // party two input
    int[] leftArrTwo = {123};
    List<FieldElement> leftFactorsTwo =
        MascotTestUtils.generateSingleRow(leftArrTwo, getModulus());
    int[] rightArrTwo = {2222};
    List<FieldElement> rightFactorsTwo =
        MascotTestUtils.generateSingleRow(rightArrTwo, getModulus());

    // define task each party will run
    Callable<List<FieldElement>> partyOneTask =
        () -> runSinglePartyMult(contexts.get(1), macKeyShareOne, leftFactorsOne, rightFactorsOne);
    Callable<List<FieldElement>> partyTwoTask =
        () -> runSinglePartyMult(contexts.get(2), macKeyShareTwo, leftFactorsTwo, rightFactorsTwo);

    List<List<FieldElement>> results =
        testRuntime.runPerPartyTasks(Arrays.asList(partyOneTask, partyTwoTask));
    FieldElement left = results.get(0).get(0);
    FieldElement right = results.get(1).get(0);

    // (12 + 123) * (11 + 2222) % 65519
    FieldElement expected = new FieldElement(39379, getModulus());
    FieldElement actual = left.add(right);
    CustomAsserts.assertEquals(expected, actual);
  }

  @Test
  public void testTwoPartiesBatchedMult() {
    // two parties run this
    initContexts(2);

    // left party mac key share
    FieldElement macKeyShareOne = new FieldElement(new BigInteger("11231"), getModulus());

    // right party mac key share
    FieldElement macKeyShareTwo = new FieldElement(new BigInteger("7719"), getModulus());

    // party one input
    int[] leftArrOne = {1, 2, 3, 4, 5, 6};
    List<FieldElement> leftFactorsOne =
        MascotTestUtils.generateSingleRow(leftArrOne, getModulus());
    int[] rightArrOne = {7, 8};
    List<FieldElement> rightFactorsOne =
        MascotTestUtils.generateSingleRow(rightArrOne, getModulus());

    // party two input
    int[] leftArrTwo = {9, 10, 11, 12, 13, 14};
    List<FieldElement> leftFactorsTwo =
        MascotTestUtils.generateSingleRow(leftArrTwo, getModulus());
    int[] rightArrTwo = {15, 16};
    List<FieldElement> rightFactorsTwo =
        MascotTestUtils.generateSingleRow(rightArrTwo, getModulus());

    // define task each party will run
    Callable<List<FieldElement>> partyOneTask =
        () -> runSinglePartyMult(contexts.get(1), macKeyShareOne, leftFactorsOne, rightFactorsOne);
    Callable<List<FieldElement>> partyTwoTask =
        () -> runSinglePartyMult(contexts.get(2), macKeyShareTwo, leftFactorsTwo, rightFactorsTwo);

    List<List<FieldElement>> results =
        testRuntime.runPerPartyTasks(Arrays.asList(partyOneTask, partyTwoTask));

    FieldElementUtils fieldElementUtils = new FieldElementUtils(getModulus());
    // for each input pair of factors the result is (a1 + a2 + ...) * (b1 + b2 + ...)
    List<FieldElement> expectedLeftFactors =
        Addable.sumRows(Arrays.asList(leftFactorsOne, leftFactorsTwo));
    List<FieldElement> expectedRightFactors = fieldElementUtils
        .stretch(Addable.sumRows(Arrays.asList(rightFactorsOne, rightFactorsTwo)), 3);

    List<FieldElement> expected =
        fieldElementUtils.pairWiseMultiply(expectedLeftFactors, expectedRightFactors);

    // actual results, recombined
    List<FieldElement> actual = Addable.sumRows(results);
    CustomAsserts.assertEquals(expected, actual);
  }

  private void testMultiplePartiesTriple(List<FieldElement> macKeyShares, int numTriples) {
    // set up runtime environment and get contexts
    initContexts(macKeyShares.size());

    // define per party task with params
    List<Callable<List<MultiplicationTriple>>> tasks = new ArrayList<>();
    for (int pid = 1; pid <= macKeyShares.size(); pid++) {
      MascotTestContext partyCtx = contexts.get(pid);
      FieldElement macKeyShare = macKeyShares.get(pid - 1);
      Callable<List<MultiplicationTriple>> partyTask =
          () -> runSinglePartyTriple(partyCtx, macKeyShare, numTriples);
      tasks.add(partyTask);
    }

    List<List<MultiplicationTriple>> results = testRuntime.runPerPartyTasks(tasks);
    List<MultiplicationTriple> combined = Addable.sumRows(results);
    Assert.assertThat(combined, IsCollectionWithSize.hasSize(numTriples));
    for (MultiplicationTriple triple : combined) {
      CustomAsserts.assertTripleIsValid(triple, Addable.sum(macKeyShares));
    }
  }

  private void testMultiplePartiesTripleRepeated(List<FieldElement> macKeyShares, int numTriples,
      int numIterations) {
    // set up runtime environment and get contexts
    initContexts(macKeyShares.size());

    // define per party task with params
    List<Callable<List<MultiplicationTriple>>> tasks = new ArrayList<>();
    for (int pid = 1; pid <= macKeyShares.size(); pid++) {
      MascotTestContext partyCtx = contexts.get(pid);
      FieldElement macKeyShare = macKeyShares.get(pid - 1);
      Callable<List<MultiplicationTriple>> partyTask =
          () -> runSinglePartyTripleRepeated(partyCtx, macKeyShare, numTriples, numIterations);
      tasks.add(partyTask);
    }

    List<List<MultiplicationTriple>> results = testRuntime.runPerPartyTasks(tasks);
    List<MultiplicationTriple> combined = Addable.sumRows(results);
    Assert.assertThat(combined, IsCollectionWithSize.hasSize(numTriples * numIterations));
    for (MultiplicationTriple triple : combined) {
      CustomAsserts.assertTripleIsValid(triple, Addable.sum(macKeyShares));
    }
  }

  @Test
  public void testTwoPartiesSingleTriple() {
    FieldElement macKeyShareOne = new FieldElement(11231, getModulus());
    FieldElement macKeyShareTwo = new FieldElement(7719, getModulus());
    testMultiplePartiesTriple(Arrays.asList(macKeyShareOne, macKeyShareTwo), 1
    );
  }

  @Test
  public void testTwoPartiesMultipleTriple() {
    FieldElement macKeyShareOne = new FieldElement(11231, getModulus());
    FieldElement macKeyShareTwo = new FieldElement(7719, getModulus());
    testMultiplePartiesTriple(Arrays.asList(macKeyShareOne, macKeyShareTwo), 10
    );
  }

  @Test
  public void testTwoPartiesMultipleTripleRepeated() {
    FieldElement macKeyShareOne = new FieldElement(11231, getModulus());
    FieldElement macKeyShareTwo = new FieldElement(7719, getModulus());
    int triplesPerBatch = 2;
    int numIterations = 5;
    testMultiplePartiesTripleRepeated(Arrays.asList(macKeyShareOne, macKeyShareTwo),
        triplesPerBatch, numIterations);
  }

  @Test
  public void testThreePartiesSingleTriple() {
    FieldElement macKeyShareOne = new FieldElement(11231, getModulus());
    FieldElement macKeyShareTwo = new FieldElement(7719, getModulus());
    FieldElement macKeyShareThree = new FieldElement(4444, getModulus());
    testMultiplePartiesTriple(Arrays.asList(macKeyShareOne, macKeyShareTwo, macKeyShareThree), 1
    );
  }

  @Test
  public void testThreePartiesMultTriple() {
    FieldElement macKeyShareOne = new FieldElement(11231, getModulus());
    FieldElement macKeyShareTwo = new FieldElement(7719, getModulus());
    FieldElement macKeyShareThree = new FieldElement(4444, getModulus());
    testMultiplePartiesTriple(Arrays.asList(macKeyShareOne, macKeyShareTwo, macKeyShareThree), 3
    );
  }

}
