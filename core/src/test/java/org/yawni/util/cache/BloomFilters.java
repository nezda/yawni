package org.yawni.util.cache;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.List;
import org.yawni.wn.DictionaryDatabase;
import org.yawni.wn.FileBackedDictionary;
import org.yawni.wn.POS;
import org.yawni.wn.Word;

class BloomFilters {
  public static void main(String[] args) throws Exception {
    final double fpProb = 0.001;
    final DictionaryDatabase dictionary = FileBackedDictionary.getInstance();
    for (final POS pos : POS.CATS) {
      int count = 0;
      for (final Word word : dictionary.words(pos)) {
        count++;
      }
      final BloomFilter<CharSequence> filter = new BloomFilter<CharSequence>(count, fpProb);
      //FIXME need to provide customizable hashCode() since CharSequence's hashCode()
      // is not well defined

//      System.err.println(pos+" "+filter);
      for (final Word word : dictionary.words(pos)) {
        filter.add(word.getLemma());
        assert filter.contains(word.getLemma());
      }
//      int numExceptions = 0;
//      int numExceptionInstances = 0;
//      for (final List<String> exceptions : dictionary.exceptions(pos)) {
//        for (final String exception : exceptions) {
//          filter.add(exception);
//          assert filter.contains(exception);
//          if (null == dictionary.lookupWord(exception, pos)) {
//            numExceptions++;
//            numExceptionInstances += (exceptions.size() - 1);
//          }
//        }
//      }
//      System.err.printf("numExceptions: %,d numExceptionInstances: %,d\n",
//        numExceptions, numExceptionInstances);
      for (final Word word : dictionary.words(pos)) {
        assert filter.contains(word.getLemma());
      }
      System.err.println("XXX "+pos+" "+filter);
      final String fname = pos.name()+".bloom";
      final ObjectOutputStream oos =
        new ObjectOutputStream(
          new BufferedOutputStream(
            new FileOutputStream(fname)));
      oos.writeObject(filter);
      oos.close();

      final ObjectInputStream ois =
        new ObjectInputStream(
          new BufferedInputStream(
            new FileInputStream(fname)));
      BloomFilter<CharSequence> resurrected = (BloomFilter<CharSequence>)ois.readObject();
      System.err.println("equal?: "+resurrected.equals(filter));
//      assert resurrected.equals(filter);
    }
  }
}