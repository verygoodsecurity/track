package io.vgs.track.utils;

import java.util.Collection;
import java.util.Comparator;
import java.util.Objects;

public class Utils {

  private static class ComparableComparator implements Comparator<Object> {

    @Override
    public int compare(final Object o1, final Object o2) {
      final Comparable<Object> c1 = (Comparable<Object>) o1;
      return c1.compareTo(o2);
    }
  }

  private static final ComparableComparator COMPARABLE_COMPARATOR = new ComparableComparator();

  private Utils() {
  }

  public static boolean equalsOrCompareEquals(Object oldValue, Object newValue) {
    return Objects.equals(oldValue, newValue) || compareEquals(oldValue, newValue);
  }

  private static boolean compareEquals(Object first, Object second) {
    if (!(first instanceof Comparable) || !(second instanceof Comparable)) {
      return false;
    }
    return Objects.compare(first, second, COMPARABLE_COMPARATOR) == 0;
  }

  //return true when: first = null, second = empty collection or vice versa
  public static boolean collectionsEqual(Collection first, Collection second) {
    return (first == null && second.isEmpty()) || (second == null && first.isEmpty());
  }
}
