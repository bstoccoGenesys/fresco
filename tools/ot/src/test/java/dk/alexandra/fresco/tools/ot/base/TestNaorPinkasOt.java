package dk.alexandra.fresco.tools.ot.base;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.math.BigInteger;
import java.security.AlgorithmParameterGenerator;
import java.security.AlgorithmParameters;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidParameterSpecException;
import java.util.Arrays;

import javax.crypto.spec.DHParameterSpec;

import org.junit.Before;
import org.junit.Test;

import dk.alexandra.fresco.framework.network.Network;
import dk.alexandra.fresco.framework.util.AesCtrDrbg;
import dk.alexandra.fresco.framework.util.Drbg;
import dk.alexandra.fresco.framework.util.Drng;
import dk.alexandra.fresco.framework.util.DrngImpl;
import dk.alexandra.fresco.tools.helper.Constants;

public class TestNaorPinkasOt {
  private NaorPinkasOT ot;
  private Drng randNum;

  public static final BigInteger DhGvalue = new BigInteger(
      "1817929693051677794042418360119535939035448877384059423016092223723589389"
          + "89386921540078076694389023214591116103022506752626702949377742490622411"
          + "36154252930934999558878557838951366230121689192613836661801579283976804"
          + "90566221950235571908449465416597162122008963523511429191971262704962062"
          + "23722995544735685829105160578247097947199471860741139749699562917671426"
          + "82888600060270321923905677901250333513320663621356005726499527794262632"
          + "80575136645831734174762968521856711608877942562412558950963899754610266"
          + "97615963606394464455636761856586890950014177457842992286652934126338664"
          + "99748366638338849983708609236396436614761807745");
  public static final BigInteger DhPvalue = new BigInteger(
      "2080109726332741595567900301553712643291061397185326442939225885200811703"
          + "46221477943683854922915625754365585955880683687623164529077074421717622"
          + "55815247681135202838112705300460371527291002353818384380395178484616163"
          + "81789931732016235932408088148285827220196826505807878031275264842308641"
          + "84386700540754381703938109115634660390655677474772619937553430208773150"
          + "06567328507885962926589890627547794887973720401310026543273364787901564"
          + "85827844212318499978829377355564689095172787513731965744913645190518423"
          + "06594567246898679968677700656495114013774368779648395287433119164167454"
          + "67731166272088057888135437754886129005590419051");

  @Before
  public void setup()
      throws NoSuchAlgorithmException, InvalidParameterSpecException {
    Drbg randBit = new AesCtrDrbg(Constants.seedOne);
    randNum = new DrngImpl(randBit);
    // fake network
    Network network = new Network() {
      @Override
      public void send(int partyId, byte[] data) {
      }

      @Override
      public byte[] receive(int partyId) {
        return null;
      }

      @Override
      public int getNoOfParties() {
        return 0;
      }
    };
    DHParameterSpec params = new DHParameterSpec(DhPvalue, DhGvalue);
    this.ot = new NaorPinkasOT(1, 2, randBit, network, params);
  }


  /**** POSITIVE TESTS. ****/
  // Verify that the generation of the Dh parameters is stable
  @Test
  public void testStabilityOfDhParams()
      throws NoSuchAlgorithmException, InvalidParameterSpecException {
    // Make a parameter generator for Diffie-Hellman parameters
    AlgorithmParameterGenerator paramGen = AlgorithmParameterGenerator
        .getInstance("DH");
    SecureRandom commonRand = SecureRandom.getInstance("SHA1PRNG");
    commonRand.setSeed(new byte[] { 0x42 });
    // Construct DH parameters of a 2048 bit group based on the common seed
    paramGen.init(2048, commonRand);
    AlgorithmParameters params = paramGen.generateParameters();
    DHParameterSpec newSpec = params.getParameterSpec(DHParameterSpec.class);
    assertEquals(DhGvalue, newSpec.getG());
    assertEquals(DhPvalue, newSpec.getP());
  }

  @Test
  public void testEncDec() throws NoSuchAlgorithmException {
    BigInteger privateKey = randNum.nextBigInteger(ot.getDhParams().getP());
    BigInteger publicKey = ot.getDhParams().getG().modPow(privateKey,
        ot.getDhParams().getP());
    // We are statically using SHA-256 and thus have 256 bit digests
    byte[] message = new byte[256 / 8];
    BigInteger cipher = ot.encryptMessage(publicKey, message);
    // Sanity check that the byte array gets initialized, i.e. is not the 0-array
    assertFalse(Arrays.equals(new byte[256 / 8], message));
    byte[] decryptedMessage = ot.decryptMessage(cipher, privateKey);
    assertArrayEquals(message, decryptedMessage);
  }

  @Test
  public void testPadMessage()
      throws NoSuchAlgorithmException, IOException, ClassNotFoundException {
    byte[] message = new byte[] { 0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07,
        0x08 };
    byte[] paddedMessage = ot.padMessage(message, 10001, Constants.seedThree);
    byte[] unpaddedMessage = ot.unpadMessage(paddedMessage,
        Constants.seedThree);
    byte[] messageWithZeros = Arrays.copyOf(message, 10001);
    assertArrayEquals(messageWithZeros, unpaddedMessage);
  }

  /**** NEGATIVE TESTS. ****/
  @Test
  public void testFailedEncDec() throws NoSuchAlgorithmException {
    BigInteger privateKey = randNum.nextBigInteger(ot.getDhParams().getP());
    BigInteger publicKey = ot.getDhParams().getG().modPow(privateKey,
        ot.getDhParams().getP());
    // We are statically using SHA-256 and thus have 256 bit digests
    byte[] message = new byte[256 / 8];
    BigInteger cipher = ot.encryptMessage(publicKey, message);
    // Sanity check that the byte array gets initialized, i.e. is not the
    // 0-array
    assertFalse(Arrays.equals(new byte[256 / 8], message));
    message[(256 / 8) - 1] ^= 0x01;
    byte[] decryptedMessage = ot.decryptMessage(cipher, privateKey);
    assertFalse(Arrays.equals(message, decryptedMessage));
  }

  @Test
  public void testFailedPadMessage()
      throws NoSuchAlgorithmException, IOException, ClassNotFoundException {
    byte[] message = new byte[] { 0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07,
        0x08 };
    byte[] paddedMessage = ot.padMessage(message, 10001, Constants.seedThree);
    paddedMessage[10000] ^= 0x01;
    byte[] unpaddedMessage = ot.unpadMessage(paddedMessage,
        Constants.seedThree);
    byte[] messageWithZeros = Arrays.copyOf(message, 10001);
    assertTrue(!Arrays.equals(messageWithZeros, unpaddedMessage));
  }

  @Test
  public void testUnequalLengthMessages() throws NoSuchFieldException,
      SecurityException, IllegalArgumentException, IllegalAccessException,
      NoSuchMethodException, InvocationTargetException {
    Method method = ot.getClass().getDeclaredMethod("recoverTrueMessage",
        byte[].class, byte[].class, byte[].class, boolean.class);
    // Remove private
    method.setAccessible(true);
    boolean thrown = false;
    try {
      method.invoke(ot, new byte[] { 0x42, 0x42 }, new byte[] { 0x42 },
          new byte[] { 0x42 }, true);
    } catch (InvocationTargetException e) {
      assertEquals("The length of the two choice messages is not equal",
          e.getTargetException().getMessage());
      thrown = true;
    }
    assertTrue(thrown);
  }
}
