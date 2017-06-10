package com.example.alienware.fantasy_image;

import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Matrix;
import android.util.Log;

/**
 * Created by Administrator on 2017/6/10.
 */

//直方图均衡化类
public class histEqualize {
    //传入一个原始图像类Bitmap，传出一个直方图均衡化后的图像类Bitmap
    public Bitmap histEqualize(Bitmap myBitmap){
        boolean is_scale = true; //用于判断bitmap是否需要缩放以避免内存溢出
        Matrix matrix = new Matrix(); //用于进行bitmap放大缩小的矩阵
        matrix.postScale(0.5f,0.5f); //长和宽放大缩小的比例
        if(myBitmap.getWidth() < 1000 && myBitmap.getHeight() < 1000) {
            is_scale = false;
        } //当图片小于1000X1000时不进行放大缩小操作
        if (is_scale) {
            myBitmap = Bitmap.createBitmap(myBitmap, 0, 0, myBitmap.getWidth(), myBitmap.getHeight(), matrix, true);
            //利用bitmap静态方法构建缩小为原有图片1/2的bitmap
        }
        // Create new array
        int width = myBitmap.getWidth(); //获取图片的宽度
        int height = myBitmap.getHeight(); //获取图片的高度
        int[] pix = new int[width * height]; //新建一个数组存储图片所有像素值,每一位包含rgb三通道的值
        int[] imageR = new int[width * height]; //存储图片的像素r值
        int[] imageG = new int[width * height]; //存储图片的像素g值
        int[] imageB = new int[width * height]; //存储图片的像素b 值
        myBitmap.getPixels(pix, 0, width, 0, 0, width, height); //获取图片的rgb值并存于pix中

        //从pix中得到r、g、b值
        for (int i = 0; i < pix.length; i++) {
            int clr = pix[i];
            imageR[i] = (clr & 0x00ff0000) >> 16; // 取高两位，为图片像素r值
            imageG[i] = (clr & 0x0000ff00) >> 8; // 取中两位，为图片像素g值
            imageB[i] = clr & 0x000000ff; // 取低两位，为图片像素b 值
        }

        //调用直方图均衡化函数Equalize进行图片直方图均衡化
        Bitmap result_bitmap = Equalize(imageR, imageG, imageB, width, height);

        //根据之前是否对bitmap进行缩小决定是否对输出bitmap进行放大
        if (is_scale) {
            matrix.postScale(2.0f, 2.0f); //长和宽放大缩小的比例
            result_bitmap = Bitmap.createBitmap(result_bitmap, 0, 0, result_bitmap.getWidth(), result_bitmap.getHeight(), matrix, true);
            //利用bitmap静态方法构建放大为原有图片2倍的bitmap
        }
        return result_bitmap;
    }

    //传入依次为存储r、g、b的int数组、图片宽度、图片高度，返回值为转换后的bitmap
    private Bitmap Equalize(int[] r,int[] g,int[] b, int image_width, int image_height){
        Bitmap bitmap = Bitmap.createBitmap(image_width, image_height, Bitmap.Config.ARGB_8888); //根据传入的图片宽高构建新bitmap
        int sum_num = image_height * image_width; // 像素点总个数
        int[] gray = new int[sum_num]; // 记录对应点的灰度值
        int[] nk = new int[256]; // 记录每一级像素点的个数
        float[] nk_pr = new float[256]; // 记录每个灰度值的概率
        float[] pdf_nk = new float[256]; //记录累计密度函数
        int[] gray_change = new int[256]; // 转换后的灰度

        //对应数组置零
        for (int i = 0; i < 256; i++) {
            nk[i] = 0;
            nk_pr[i] = 0;
            pdf_nk[i] = 0;
            gray_change[i] = 0;
        }

        //计算对应的灰度序列
        for (int i = 0; i < sum_num; i++) {
            gray[i] = (int) (r[i] * 0.3 + g[i] * 0.59 + b[i] * 0.11 + 0.5); //获得灰度值
            nk[gray[i]]++; //对应灰度值个数加一
        }

        //计算每个级别的概率密度
        for (int i = 0; i < 256; i++) {
            nk_pr[i] = (float) nk[i]/sum_num; //计算0-255个灰度值分布概率
        }

        //计算相应的累计密度函数与转换后的灰度值
        pdf_nk[0] = nk_pr[0]; //灰度值为0的累计分布概率
        gray_change[0] = (int) (255*pdf_nk[0] + 0.5); //灰度值为0对应的转换后灰度值
        for(int i = 1; i < 256; i++) {
            pdf_nk[i] = nk_pr[i] + pdf_nk[i - 1]; //累计分布函数
            gray_change[i] = (int) (255*pdf_nk[i] + 0.5); //各灰度值对应的转换后灰度值
            //Log.i("pixel:" + i + ": ", pdf_nk[i] + ", " + gray_change[i]);
        }
        //应用回彩色图像
        for (int i = 0; i < sum_num; i++) {
            double gray_temp = r[i] * 0.3 + g[i] * 0.59 + b[i] * 0.11; //灰度值
            int parse_temp = (int) (gray_temp + 0.5); //gray_change下标

            //判断分母是否为零
            if (gray[i] == 0) {
                r[i] = 0;
                g[i] = 0;
                b[i] = 0;
            } else {
                //判断是否像素值越界
                if ((int) gray_change[parse_temp]/gray_temp * r[i] > 255) {
                    r[i] = 255;
                } else {
                    r[i] = (int) (gray_change[parse_temp]/gray_temp * r[i]); //根据灰度值变化倍数变化rgb值
                }
                if ((int) gray_change[parse_temp]/gray_temp * g[i] > 255) {
                    g[i] = 255;
                } else {
                    g[i] = (int) (gray_change[parse_temp]/gray_temp * g[i]); //根据灰度值变化倍数变化rgb值
                }
                if ((int) gray_change[parse_temp]/gray_temp * b[i] > 255) {
                    b[i] = 255;
                } else {
                    b[i] = (int) (gray_change[parse_temp]/gray_temp * b[i]); //根据灰度值变化倍数变化rgb值
                }
            }
        }

        //将获得到的转换后rgb值填入构建的bitmap中
        for (int i = 0; i < sum_num; i++) {
            //Log.i("rgb", r[i] + " " + g[i] + " " + b[i] + "");
            int color = Color.argb(255, r[i], g[i], b[i]); //透明度为255
            bitmap.setPixel(i % image_width, i / image_width, color); //填入color
        }

        return bitmap;
    }
}
