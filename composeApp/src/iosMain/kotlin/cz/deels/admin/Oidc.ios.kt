package cz.deels.admin

import org.publicvalue.multiplatform.oidc.appsupport.CodeAuthFlowFactory
import org.publicvalue.multiplatform.oidc.appsupport.IosCodeAuthFlowFactory

actual val flowFactory: CodeAuthFlowFactory = IosCodeAuthFlowFactory()
