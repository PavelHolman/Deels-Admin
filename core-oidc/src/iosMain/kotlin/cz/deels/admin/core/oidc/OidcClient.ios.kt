package cz.deels.admin.core.oidc

import org.publicvalue.multiplatform.oidc.appsupport.IosCodeAuthFlowFactory
import org.publicvalue.multiplatform.oidc.flows.CodeAuthFlowFactory

public actual val flowFactory: CodeAuthFlowFactory = IosCodeAuthFlowFactory()
