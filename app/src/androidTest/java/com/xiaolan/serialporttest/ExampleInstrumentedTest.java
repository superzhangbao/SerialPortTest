package com.xiaolan.serialporttest;

import android.support.test.runner.AndroidJUnit4;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.IOException;

import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.plugins.RxJavaPlugins;
import io.reactivex.schedulers.Schedulers;

/**
 * Instrumented test, which will execute on an Android device.
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */
@RunWith(AndroidJUnit4.class)
public class ExampleInstrumentedTest {
    @Test
    public void useAppContext() {
        RxJavaPlugins.setErrorHandler(throwable -> System.out.println(throwable.getMessage()));
        Disposable subscribe = Observable.just(1)
                .subscribeOn(Schedulers.io())
                .onErrorResumeNext(throwable -> {
                    return Observable.empty();
                })
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(integer -> {
                    throw new IOException();
                });
    }
}
