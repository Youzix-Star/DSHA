package com.youzixstar.dsha

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.youzixstar.dsha.setup.DshaController
import com.youzixstar.dsha.ui.miuix.DshaApp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            val context = LocalContext.current.applicationContext
            // 控制器只创建一次，交给整棵界面树
            val controller = remember { DshaController(context) }
            DshaApp(controller = controller)
        }
    }
}
