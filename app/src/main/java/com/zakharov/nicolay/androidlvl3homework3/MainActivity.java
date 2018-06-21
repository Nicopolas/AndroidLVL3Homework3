package com.zakharov.nicolay.androidlvl3homework3;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.ImageView;

import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.nio.ByteBuffer;
import java.util.ArrayList;

import io.reactivex.BackpressureStrategy;
import io.reactivex.Flowable;
import io.reactivex.schedulers.Schedulers;

public class MainActivity extends AppCompatActivity {
    ImageView imageViewJpg;
    ImageView imageViewPng;
    Bitmap bitmap;
    String jpgFilePath;
    String pngFilePath;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        jpgFilePath = getFileStreamPath(getString(R.string.android_logo_jpg_name)).getPath();
        pngFilePath = getFileStreamPath(getString(R.string.android_logo_png_name)).getPath();
        imageViewJpg = findViewById(R.id.android_logo_jpg);
        imageViewPng = findViewById(R.id.android_logo_png);
/*
        //решение без RX
        resImageToFileJpg(R.drawable.android_logo);
        imageViewJpg.setImageBitmap(getBitmapFromFile(jpgFilePath));
        conversionJpgFileToPng(jpgFilePath);
        imageViewPng.setImageBitmap(getBitmapFromFile(pngFilePath));
*/

        resImageToFileJpgRX(R.drawable.android_logo);
        setImageFromFileRX(imageViewJpg, jpgFilePath);
        conversionJpgFileToPngRX(jpgFilePath);
        //setImageFromFileRX(imageViewPng, pngFilePath);
    }

    private void resImageToFileJpgRX(int id) {
        Flowable<Bitmap> flowable = Flowable.create(emitter -> {
            bitmap = BitmapFactory.decodeResource(getResources(), id);
            emitter.onNext(bitmap);
            emitter.onComplete();
        }, BackpressureStrategy.BUFFER);

        flowable
                .observeOn(Schedulers.computation())
                .subscribeOn(Schedulers.io())
                .subscribe(bitmap -> {
                    FileOutputStream fos = new FileOutputStream(jpgFilePath);
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 75, fos);
                    fos.flush();
                    fos.close();
                });
    }

    private void setImageFromFileRX(ImageView imageView, String name) {
        Flowable<Bitmap> flowable = Flowable.create(emitter -> {
            emitter.onNext(BitmapFactory.decodeFile(name));
            emitter.onComplete();
        }, BackpressureStrategy.BUFFER);

        flowable
                .observeOn(Schedulers.computation())
                .subscribeOn(Schedulers.io())
                .subscribe(bitmap -> imageView.setImageBitmap(bitmap));
    }

    private void conversionJpgFileToPngRX(String name) {
        Flowable<Byte> flowable = Flowable.create(emitter -> {
            //получает файл
            Bitmap bmp = BitmapFactory.decodeFile(name);
            int size = bmp.getRowBytes() * bmp.getHeight();

            //конвертируем в png и разбирает на байты
            ByteArrayOutputStream stream = new ByteArrayOutputStream();
            bmp.compress(Bitmap.CompressFormat.PNG, 100, stream);

            ByteBuffer b = ByteBuffer.allocate(size);
            bmp.copyPixelsToBuffer(b);
            bmp.copyPixelsFromBuffer(b);

            //отсылает по байту
            for (int i = 0; i != size; i++) {
                emitter.onNext(b.get(i));
            }
            emitter.onComplete();
        }, BackpressureStrategy.BUFFER);

        flowable
                .observeOn(Schedulers.computation())
                .subscribeOn(Schedulers.io())
                .subscribe(new Subscriber<Byte>() {
                    ArrayList<Byte> mBytes = new ArrayList();

                    @Override
                    public void onSubscribe(Subscription s) {
                        Log.e("Subscriber", "onSubscribe()");
                    }

                    @Override
                    public void onNext(Byte mByte) {
                        //собираем байты в массив
                        mBytes.add(mByte);
                        Log.e("Subscriber", "onNext()");
                    }

                    @Override
                    public void onError(Throwable t) {
                        t.printStackTrace();
                        Log.e("Subscriber", "onError()");
                    }

                    @Override
                    public void onComplete() {
                        Log.e("Subscriber", "onComplete()");
                        byte ArrByte[] = new byte[mBytes.size()];
                        for (int i = 0; i != mBytes.size(); i++) {
                            ArrByte[i] = mBytes.get(i);
                        }
                        //записываем в новый файл
                        try {
                            FileOutputStream fos = new FileOutputStream(pngFilePath);
                            fos.write(ArrByte);
                            fos.flush();
                            fos.close();
                        } catch (Exception e) {
                            Log.e("onComplete", String.valueOf(e));
                            e.printStackTrace();
                        }
                        setImageFromFileRX(imageViewPng, pngFilePath);
                    }
                });
    }

    private void conversionJpgFileToPng(String path) {
        Bitmap bitmap = BitmapFactory.decodeFile(path);
        try {
            FileOutputStream fos = new FileOutputStream(pngFilePath);
            bitmap.compress(Bitmap.CompressFormat.PNG, 75, fos);
            fos.flush();
            fos.close();
        } catch (Exception e) {
            Log.e("conversionJpgFileToPng", e.toString());
        }
    }

    private Bitmap getBitmapFromFile(String name) {
        return BitmapFactory.decodeFile(name);
    }

    private Bitmap getBitmapFromFileFIS(String name) {
        byte b[];
        Bitmap bitmap = null;
        try {
            FileInputStream fileInputStream = new FileInputStream(name);
            b = new byte[fileInputStream.available()];

            int count = fileInputStream.read(b, 0, fileInputStream.available());
            fileInputStream.close();
            bitmap = BitmapFactory.decodeByteArray(b, 0, b.length);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
        return bitmap;
    }

    private void resImageToFileJpg(int id) {
        try {
            bitmap = BitmapFactory.decodeResource(getResources(), id);
            FileOutputStream fos = new FileOutputStream(jpgFilePath);
            bitmap.compress(Bitmap.CompressFormat.JPEG, 75, fos);
            fos.flush();
            fos.close();
        } catch (Exception e) {
            Log.e("resImageToFileJpg", e.toString());
        }
    }
}
