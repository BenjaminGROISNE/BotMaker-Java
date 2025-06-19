package com.botmaker.opencv;

import org.opencv.core.Rect;


enum MatType {COLOR,GRAY}


public class MatchResult {
    public Rect rectLocation;
    public double score;
    public double confidenceThreshold;
    public String winningTemplateId;
    public String winningBackgroundId;

    public MatchResult(Rect rect, double score, double threshold, String winningTemplateId, String winningBackgroundId) {
        this.rectLocation = rect;
        this.score = score;
        this.confidenceThreshold = threshold;
        this.winningTemplateId = winningTemplateId;
    }

    public double getScore(){
        return score;
    }

    public Boolean isMatch(){
        return score>=confidenceThreshold;
    }

    @Override
    public String toString() {
        // Corrected to use rectLocation
        return String.format("MatchResult [Score: %.4f, Location: %s, Threshold: %.4f, IsSignificant: %b]",
                score, rectLocation.toString(), confidenceThreshold, isMatch());
    }
}
