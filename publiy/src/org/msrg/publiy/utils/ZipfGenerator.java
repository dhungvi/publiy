package org.msrg.publiy.utils;

import java.util.Random;

public class ZipfGenerator {
 private Random rnd = new Random(SystemTime.currentTimeMillis());
 private int size;
 private double skew;
 private double bottom = 0;

 public ZipfGenerator(int size, double skew) {
  this.size = size;
  this.skew = skew;

  for(int i=1;i<size; i++) {
  this.bottom += (1/Math.pow(i, this.skew));
  }
 }

 // the next() method returns an rank id. The frequency of returned rank ids are follows Zipf distribution.
 public int next() {
   int rank;
   double friquency = 0;
   double dice;

   rank = rnd.nextInt(size);
   friquency = (1.0d / Math.pow(rank, this.skew)) / this.bottom;
   dice = rnd.nextDouble();

   while(!(dice < friquency)) {
     rank = rnd.nextInt(size);
     friquency = (1.0d / Math.pow(rank, this.skew)) / this.bottom;
     dice = rnd.nextDouble();
   }

   return rank;
 }

 // This method returns a probability that the given rank occurs.
 public double getProbability(int rank) {
   return (1.0d / Math.pow(rank, this.skew)) / this.bottom;
 }

 public static void main(String[] args) {
   if(args.length != 2) {
     System.out.println("usage: ./zipf size skew");
     System.exit(-1);
   }

   int size = Integer.valueOf(args[0]);
   double skew = Double.valueOf(args[1]);
   ZipfGenerator zipf0 = new ZipfGenerator(size, skew + 0);
   ZipfGenerator zipf1 = new ZipfGenerator(size, skew + 0.1);
   ZipfGenerator zipf2 = new ZipfGenerator(size, skew + 0.2);
   
   for(int i=1;i<=size;i++) {
     System.out.print(i);
     System.out.print(" " + zipf0.getProbability(i));
     System.out.print(" " + zipf1.getProbability(i));
     System.out.print(" " + zipf2.getProbability(i));
     System.out.println();
   }
 }
}
