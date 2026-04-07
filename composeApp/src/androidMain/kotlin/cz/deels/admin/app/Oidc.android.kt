package cz.deels.admin.app

import org.publicvalue.multiplatform.oidc.appsupport.AndroidCodeAuthFlowFactory
import org.publicvalue.multiplatform.oidc.flows.CodeAuthFlowFactory

actual val flowFactory: CodeAuthFlowFactory = AndroidCodeAuthFlowFactory()
