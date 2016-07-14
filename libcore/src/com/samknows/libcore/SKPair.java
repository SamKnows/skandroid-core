package com.samknows.libcore;

//
// We use SKPair, instead of Pair, as it is platform independent.
//
public class SKPair<T,Y>
{
  public SKPair() { first = null; second = null; }
  public SKPair(T first, Y second) { this.first = first;  this.second = second; }

  public T getFirst() { return first; }
  public Y getSecond() { return second; }

  public void setFirst(T newValue) { first = newValue; }
  public void setSecond(Y newValue) { second = newValue; }

  public T first;
  public Y second;
}
