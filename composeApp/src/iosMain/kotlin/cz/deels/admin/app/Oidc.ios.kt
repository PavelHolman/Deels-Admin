package cz.deels.admin.app

import org.publicvalue.multiplatform.oidc.appsupport.IosCodeAuthFlowFactory
import org.publicvalue.multiplatform.oidc.flows.CodeAuthFlowFactory

actual val flowFactory: CodeAuthFlowFactory = IosCodeAuthFlowFactory()
