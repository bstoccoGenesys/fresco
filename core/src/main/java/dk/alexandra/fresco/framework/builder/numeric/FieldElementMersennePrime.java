package dk.alexandra.fresco.framework.builder.numeric;

import java.math.BigInteger;
import java.util.Objects;

public final class FieldElementMersennePrime implements FieldElement {

  private final BigInteger value;
  private final ModulusMersennePrime modulus;

  public FieldElementMersennePrime(BigInteger value, ModulusMersennePrime modulus) {
    this.value = modulus.mod(value);
    this.modulus = modulus;
  }

  public FieldElementMersennePrime(int i, ModulusMersennePrime modulus) {
    this(BigInteger.valueOf(i), modulus);
  }

  public FieldElementMersennePrime(String toString, ModulusMersennePrime modulus) {
    this(new BigInteger(toString), modulus);
  }

  public FieldElementMersennePrime(byte[] bytes, ModulusMersennePrime modulus) {
    this(new BigInteger(bytes), modulus);
  }

  private FieldElementMersennePrime create(BigInteger divide) {
    return new FieldElementMersennePrime(divide, modulus);
  }

  @Override
  public FieldElementMersennePrime subtract(FieldElement operand) {
    return create(value.subtract(operand.convertToBigInteger()));
  }

  @Override
  public FieldElementMersennePrime multiply(FieldElement operand) {
    return create(value.multiply(operand.convertToBigInteger()));
  }

  @Override
  public FieldElementMersennePrime add(FieldElement operand) {
    return create(value.add(operand.convertToBigInteger()));
  }

  @Override
  public BigInteger convertToBigInteger() {
    return value;
  }

  @Override
  public void toByteArray(byte[] bytes, int offset, int byteLength) {
    byte[] byteArray = value.toByteArray();
    System.arraycopy(byteArray, 0, bytes, byteLength - byteArray.length + offset, byteArray.length);
  }

  @Override
  public int compareTo(FieldElement o) {
    return value.compareTo(o.convertToBigInteger());
  }

  @Override
  public String toString() {
    return "FieldElementMersennePrime{" +
        "value=" + value +
        ", modulus =" + modulus +
        '}';
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    FieldElementMersennePrime fieldElementMersennePrime = (FieldElementMersennePrime) o;
    return Objects.equals(modulus, fieldElementMersennePrime.modulus) &&
        Objects.equals(value, fieldElementMersennePrime.value);
  }

  @Override
  public int hashCode() {
    return Objects.hash(modulus, value);
  }
}
