package id.ac.loopy

import android.app.Application
import id.ac.loopy.di.AppContainer

class LoopyApplication : Application() {
    val container: AppContainer by lazy { AppContainer(this) }
}
