package com.example.visiontools;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.support.v7.widget.AppCompatImageView;

import com.google.api.services.vision.v1.model.Block;
import com.google.api.services.vision.v1.model.BoundingPoly;
import com.google.api.services.vision.v1.model.Page;
import com.google.api.services.vision.v1.model.Paragraph;
import com.google.api.services.vision.v1.model.TextAnnotation;
import com.google.api.services.vision.v1.model.Vertex;
import com.google.api.services.vision.v1.model.Word;

import java.util.List;

public class DrawingImageView extends AppCompatImageView {

    Paint mPaint;
    Bitmap mBitmap;
    Canvas mCanvas;
    Path mPath;
    Paint mBitmapPaint;

    public DrawingImageView(Context context) {
        super(context);
        mPaint = new Paint();
        mPaint.setAntiAlias(true);
        mPaint.setDither(true);
        mPaint.setColor(0xFF00FF00);
        mPaint.setStyle(Paint.Style.STROKE);
        mPaint.setStrokeJoin(Paint.Join.ROUND);
        mPaint.setStrokeCap(Paint.Cap.ROUND);
        mPaint.setStrokeWidth(14);
        mPath = new Path();
        mBitmapPaint = new Paint();
        mBitmapPaint.setAlpha(0);
        mBitmapPaint.setColor(Color.GREEN);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        if(w > 0 && h > 0) {
            mBitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
            mCanvas = new Canvas(mBitmap);
        }
    }

    @Override
    public void draw(Canvas canvas) {
        super.draw(canvas);
        canvas.drawBitmap(mBitmap, 0, 0, mBitmapPaint);
        canvas.drawPath(mPath, mPaint);
    }

    // Highlights detected words with bounding boxes
    public void highlightWords(TextAnnotation textAnnotation, int imageViewWidth, int imageViewHeight, int width, int height){
        mPaint.setColor(0xFF00FF00);
        mBitmapPaint.setColor(Color.GREEN);
        mPaint.setStrokeWidth(6);
        float x, y;
        for(Page page : textAnnotation.getPages()){
            for(Block block : page.getBlocks()) {
                for(Paragraph paragraph : block.getParagraphs()) {
                    for(Word word : paragraph.getWords()) {
                        int i = 0;
                        for(Vertex v : word.getBoundingBox().getVertices()) {
                            if(i == 0) {
                                x = v.getX().floatValue()/width*imageViewWidth;
                                y = v.getY().floatValue()/height*imageViewHeight;
                                mPath.moveTo(x, y);
                            } else {
                                x = v.getX().floatValue()/width*imageViewWidth;
                                y = v.getY().floatValue()/height*imageViewHeight;
                                mPath.lineTo(x, y);
                            }
                            i++;
                        }
                        mPath.close();
                        mCanvas.drawPath(mPath, mPaint);
                        mPath.reset();
                        invalidate();
                    }
                }
            }
        }
    }

    // Highlights detected faces with bounding boxes
    public void highlightFaces(List<BoundingPoly> facePolys, int imageViewWidth, int imageViewHeight, int width, int height){
        mPaint.setColor(0xFF00FF00);
        mBitmapPaint.setColor(Color.GREEN);
        mPaint.setStrokeWidth(6);
        float x, y;
        for(BoundingPoly box : facePolys){
            int i = 0;
            for(Vertex v : box.getVertices()) {
                if(i == 0) {
                    x = v.getX().floatValue()/width*imageViewWidth;
                    y = v.getY().floatValue()/height*imageViewHeight;
                    mPath.moveTo(x, y);
                } else {
                    x = v.getX().floatValue()/width*imageViewWidth;
                    y = v.getY().floatValue()/height*imageViewHeight;
                    mPath.lineTo(x, y);
                }
                i++;
            }
            mPath.close();
            mCanvas.drawPath(mPath, mPaint);
            mPath.reset();
            invalidate();
        }
    }

}