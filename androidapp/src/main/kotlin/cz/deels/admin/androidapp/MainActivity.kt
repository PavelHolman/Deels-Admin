package cz.deels.admin.androidapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import cz.deels.admin.app.App
import cz.deels.admin.app.flowFactory
import org.publicvalue.multiplatform.oidc.appsupport.AndroidCodeAuthFlowFactory

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        (flowFactory as AndroidCodeAuthFlowFactory).registerActivity(activity = this)

        setContent {
            App()
        }
    }
}
