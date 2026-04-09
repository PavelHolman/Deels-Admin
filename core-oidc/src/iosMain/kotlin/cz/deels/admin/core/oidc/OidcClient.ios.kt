package cz.deels.admin.core.oidc

import org.publicvalue.multiplatform.oidc.appsupport.IosCodeAuthFlowFactory
import org.publicvalue.multiplatform.oidc.flows.CodeAuthFlowFactory

internal actual val flowFactory: CodeAuthFlowFactory = IosCodeAuthFlowFactory()
