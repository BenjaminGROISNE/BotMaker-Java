package com.botmaker.opencv;

import org.opencv.core.*;
import org.opencv.imgproc.Imgproc;

import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

import static org.opencv.imgproc.Imgproc.*;



 public class OpencvManager {
    private static final double DEFAULT_CONFIDENCE_THRESHOLD = 0.9;
    public OpencvManager() {

    }

     static public Boolean isRGBA(Mat mat){
        return mat.channels() == 4;
    }

     static public Boolean isRGB(Mat mat){
        return mat.channels() == 3;
    }
     static public Boolean isGray(Mat mat){
        return mat.channels() == 1;
    }

     static void convertToBGR(Mat mat){
        if(isGray(mat)){
            cvtColor(mat, mat, COLOR_GRAY2RGB);
        }
        else if(isRGBA(mat)){
            cvtColor(mat, mat, COLOR_RGBA2RGB);
        }
    }

     static void convertToGray(Mat mat){
        if(isRGB(mat)){
            cvtColor(mat, mat, COLOR_RGB2GRAY);
        }
        else if(isRGBA(mat)){
            cvtColor(mat, mat, COLOR_RGBA2GRAY);
        }
    }

     static public MatchResult findBestMatch(Template template, Template backgroundTemplate,MatType convertType){
        return findBestMatch(template,backgroundTemplate,convertType,DEFAULT_CONFIDENCE_THRESHOLD);
    }

     static public void convertTo(Template template, MatType convertType){
         convertTo(template.mat,convertType);
     }

    static public void convertTo(Mat mat, MatType convertType){
        if(convertType == MatType.COLOR){
            convertToBGR(mat);
        }
        else if(convertType == MatType.GRAY){
            convertToGray(mat);
        }
    }

    static public MatchResult findBestMatch(Template template, Template backgroundTemplate, MatType convertType, double confidenceThreshold){

        convertTo(template,convertType);
        convertTo(backgroundTemplate,convertType);
        // Check for size mismatch
        if (backgroundTemplate.width() < template.width() || backgroundTemplate.height() < template.height()) {
            System.err.println("Error: Template dimensions are larger than the background image.");
            return null;
        }

        int resultCols = backgroundTemplate.cols() - template.cols() + 1;
        int resultRows = backgroundTemplate.rows() - template.rows() + 1;
        Mat resultMat = new Mat(resultRows, resultCols, CvType.CV_32FC1);

        matchTemplate(backgroundTemplate.mat,template.mat,resultMat,TM_CCOEFF_NORMED);
        Core.MinMaxLocResult mmr = Core.minMaxLoc(resultMat);

        double bestScore = mmr.maxVal;
        if (bestScore >= confidenceThreshold) {
            // If it is, create and return the MatchResult object.
            Point bestLocation = mmr.maxLoc;
            Rect rect = new Rect(bestLocation, template.size());
            return new MatchResult(rect, bestScore, confidenceThreshold, template.id, backgroundTemplate.id);
        } else {
            // Otherwise, the match is not significant, so return null.
            return null;
        }
    }


     public static List<MatchResult> findMultipleMatches(Template template, Template backgroundTemplate, MatType convertType){
        return findMultipleMatches(template,backgroundTemplate,convertType,DEFAULT_CONFIDENCE_THRESHOLD);
     }
     public static List<MatchResult> findMultipleMatches(Template template, Template backgroundTemplate, MatType convertType, double confidenceThreshold) {
         if (template.empty() || backgroundTemplate.empty() || backgroundTemplate.width() < template.width() || backgroundTemplate.height() < template.height()) {
             System.err.println("Error: Invalid input images.");
             return null; // Return an empty list on error
         }

         convertTo(template,convertType);
         convertTo(backgroundTemplate,convertType);

         // --- Perform initial template matching ---
         int resultCols = backgroundTemplate.cols() - template.cols() + 1;
         int resultRows = backgroundTemplate.rows() - template.rows() + 1;
         Mat resultMat = new Mat(resultRows, resultCols, CvType.CV_32FC1);
         Imgproc.matchTemplate(backgroundTemplate.mat, template.mat, resultMat, Imgproc.TM_CCOEFF_NORMED);

         List<MatchResult> matches = new ArrayList<MatchResult>();

         // --- Efficiently find all peaks above the threshold ---
         while (true) {
             Core.MinMaxLocResult mmr = Core.minMaxLoc(resultMat);
             Point matchLoc = mmr.maxLoc;
             double matchScore = mmr.maxVal;

             // Break the loop if the best score is below our threshold
             if (matchScore < confidenceThreshold) {
                 break;
             }
             // A valid match was found, add it to the list
             Rect rect = new Rect(matchLoc, template.size());
             matches.add(new MatchResult(rect, matchScore, confidenceThreshold, template.id, backgroundTemplate.id));
             // "Erase" the found match from the result matrix to avoid finding it again.
             // We draw a filled black rectangle over the area of the found match.
             // This prevents finding multiple overlapping matches for the same object.
             Imgproc.rectangle(
                     resultMat,
                     rect,
                     new Scalar(0, 0, 0),
                     -1
             );
         }
         System.out.printf("Found %d matches above the threshold of %.2f.%n", matches.size(), confidenceThreshold);
         return matches;
     }
     public static List<MatchResult> findBestMatchPerTemplate(List<Template> allTemplates, Template backgroundTemplate, MatType convertType){
        return findBestMatchPerTemplate(allTemplates,backgroundTemplate,convertType,DEFAULT_CONFIDENCE_THRESHOLD);
     }
     public static List<MatchResult> findBestMatchPerTemplate(List<Template> allTemplates, Template backgroundTemplate, MatType convertType, double confidenceThreshold){
        return allTemplates
                .parallelStream()
                .map(template -> findBestMatch(template,backgroundTemplate,convertType,confidenceThreshold))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
     }

     public static MatchResult findOneTemplate(List<Template> allTemplates, Template backgroundTemplate,MatType convertType){
        return findOneTemplate(allTemplates,backgroundTemplate,convertType,DEFAULT_CONFIDENCE_THRESHOLD);
     }
     public static MatchResult findOneTemplate(List<Template> allTemplates, Template backgroundTemplate,MatType convertType, double confidenceThreshold){
         return allTemplates
                 .parallelStream()
                 .map(template -> findBestMatch(template, backgroundTemplate, convertType, confidenceThreshold))
                 .filter(Objects::nonNull)
                 .findAny()
                 .orElse(null);
     }


     public static MatchResult findBestOneTemplate(List<Template> allTemplates, Template backgroundTemplate,MatType convertType) {
         return findBestOneTemplate(allTemplates,backgroundTemplate,convertType,DEFAULT_CONFIDENCE_THRESHOLD);
    }

     public static MatchResult findBestOneTemplate(List<Template> allTemplates, Template backgroundTemplate,MatType convertType, double confidenceThreshold){
         return findBestMatchPerTemplate(allTemplates,backgroundTemplate,convertType,confidenceThreshold)
                 .stream()
                 .max(Comparator.comparing(MatchResult::getScore))
                 .orElse(null);
     }

     public static List<MatchResult> findAllMatches(List<Template> allTemplates, Template backgroundTemplate,MatType convertType, double confidenceThreshold){
         return allTemplates
                 .parallelStream()
                 .flatMap(template ->
                         Objects.requireNonNull(findMultipleMatches(template, backgroundTemplate, null, confidenceThreshold)).stream()
                 )
                 .collect(Collectors.toList());
     }

     public static List<MatchResult> findAllMatches(List<Template> allTemplates, List<Template> allBackgrounds,MatType convertType, double confidenceThreshold){
         return allBackgrounds
                 .parallelStream()
                 .flatMap(background -> findAllMatches(allTemplates, background, convertType, confidenceThreshold).stream())
                 .collect(Collectors.toList());
     }


     public static MatchResult findBestInBackgrounds(
             Template template,
             List<Template> allBackgrounds, // Changed to List<Mat> for clarity
             MatType matType,
             double confidenceThreshold)
     {
         return allBackgrounds
                 .parallelStream()
                 .map(backgroundMat -> findBestMatch(template, backgroundMat, matType, confidenceThreshold))
                 .filter(Objects::nonNull)
                 .max(Comparator.comparing(MatchResult::getScore))
                 .orElse(null);
     }


     public static List<MatchResult> findBestPerBackground(
             Template template,
             List<Template> allBackgrounds, // Changed to List<Mat>
             MatType matType,
             double confidenceThreshold)
     {
         // This implementation is already correct for its stated goal.
         return allBackgrounds
                 .parallelStream()
                 .map(backgroundMat -> findBestMatch(template, backgroundMat, matType, confidenceThreshold))
                 .filter(Objects::nonNull)
                 .collect(Collectors.toList());
     }

     public static List<MatchResult> findCompetitiveMatches(
             List<Template> allTemplates,
             Template background,
             MatType matType,
             double confidenceThreshold)
     {
         List<MatchResult> candidates = findAllMatches(allTemplates, background, matType, confidenceThreshold);
         candidates.sort(Comparator.comparing(MatchResult::getScore).reversed());
         List<MatchResult> winners = new ArrayList<>();
         while (!candidates.isEmpty()) {
             MatchResult champion = candidates.getFirst();
             winners.add(champion);
             candidates.removeIf(competitor ->
                     intersects(champion.rectLocation, competitor.rectLocation)
             );
         }
         return winners;
     }

     /**
      * Helper function to check if two rectangles overlap.
      */
     private static boolean intersects(Rect r1, Rect r2) {
         return r1.x < r2.x + r2.width &&
                 r1.x + r1.width > r2.x &&
                 r1.y < r2.y + r2.height &&
                 r1.y + r1.height > r2.y;
     }



    }

