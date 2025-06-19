package com.botmaker.opencv;


import org.opencv.core.Mat;
import org.opencv.core.Size;

public class Template {
    public final Mat mat;
    public final String id;

    public Template(Mat mat, String id) {
        this.mat = mat;
        this.id = id;
    }

    public int rows(){
        return mat.rows();
    }
    public int cols(){
        return mat.cols();
    }
    public int channels(){
        return mat.channels();
    }
    public int type(){
        return mat.type();
    }

    public int width(){
        return mat.width();
    }
    public int height(){
        return mat.height();
    }

    public Size size(){
        return mat.size();
    }
    public boolean empty(){
        return mat.empty();
    }
    public Template clone(){
        return new Template(mat.clone(),id);
    }

}
