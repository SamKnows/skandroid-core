package com.samknows;

public class XCT {

  static public void Assert(boolean value) {
    org.junit.Assert.assertTrue(value);
  }

  static public void AssertTrue(boolean value) {
    org.junit.Assert.assertTrue(value);
  }

  static public void AssertFalse(boolean value) {
    org.junit.Assert.assertFalse(value);
  }

  static public void AssertTrue(boolean value, String message) {
    org.junit.Assert.assertTrue(message, value);
  }

  static public void AssertNotNil(Object object, String message) {
    org.junit.Assert.assertNotNull(message, object);
  }

  static public void AssertNotNil(Object object) {
    org.junit.Assert.assertNotNull(object);
  }
  static public void AssertEqualWithAccuracy(double value, double matchThis, double withRange, String caption) {
    org.junit.Assert.assertEquals(caption, value, matchThis, withRange);
  };
  static public void AssertEqualWithAccuracy(double value, double matchThis, double withRange) {
    org.junit.Assert.assertEquals("Test Error", value, matchThis, withRange);
  };
  static public void AssertDoubleApproxEqual(double value, double matchThis) {
    org.junit.Assert.assertEquals("Test Error", value, matchThis, 0.001);
  };

  static public void AssertEqualWithAccuracy(long value, long matchThis, long withRange) {
    org.junit.Assert.assertEquals(value, matchThis, withRange);
  };

}
