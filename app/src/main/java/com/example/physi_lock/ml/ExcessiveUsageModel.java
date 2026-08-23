package com.example.physi_lock.ml;
public class ExcessiveUsageModel {
    public static double score(double[] input) {
        return -12.789773868557061 + input[0] * 0.17289426849802284 + input[1] * 0.005641149204614058 + input[2] * 0.12886093288947056 + input[3] * 2.523920116271995;
    }
}
