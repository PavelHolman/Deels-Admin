package cz.deels.admin

import org.publicvalue.multiplatform.oidc.appsupport.AndroidCodeAuthFlowFactory
import org.publicvalue.multiplatform.oidc.appsupport.CodeAuthFlowFactory

actual val flowFactory: CodeAuthFlowFactory = AndroidCodeAuthFlowFactory()
