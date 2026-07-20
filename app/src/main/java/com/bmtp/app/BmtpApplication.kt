package com.bmtp.app

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

/**
 * Main application class for the BMTP application.
 * Annotated with [HiltAndroidApp] to trigger Hilt code generation,
 * including a base class for the application that serves as the
 * application-level dependency container.
 */
@HiltAndroidApp
class BmtpApplication : Application()
