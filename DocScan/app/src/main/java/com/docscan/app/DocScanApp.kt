package com.docscan.app

import android.app.Application
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader

class DocScanApp : Application() {
    override fun onCreate() {
        super.onCreate()
        PDFBoxResourceLoader.init(applicationContext)
    }
}
