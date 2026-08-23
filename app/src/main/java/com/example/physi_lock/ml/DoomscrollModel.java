package com.example.physi_lock.ml;
public class DoomscrollModel {
    public static double score(double[] input) {
        return -0.6791864448775351 + input[0] * 0.053203962816719325 + input[1] * -0.018163374398353204 + input[2] * -0.06823802864766794;
    }
}
