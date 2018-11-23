package it.sephiroth.android.library.tooltip_demo

import android.app.Application
import timber.log.Timber

//
//  ADOBE CONFIDENTIAL
//  __________________
//
//  Copyright 2017 Adobe
//  All Rights Reserved.
//
//  NOTICE:  All information contained herein is, and remains
//  the property of Adobe and its suppliers, if any. The intellectual
//  and technical concepts contained herein are proprietary to Adobe
//  and its suppliers and are protected by all applicable intellectual
//  property laws, including trade secret and copyright laws.
//  Dissemination of this information or reproduction of this material
//  is strictly forbidden unless prior written permission is obtained
//  from Adobe.
//
class MainApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        Timber.plant(Timber.DebugTree())
    }
}