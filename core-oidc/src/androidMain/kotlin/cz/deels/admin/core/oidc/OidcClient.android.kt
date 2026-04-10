package cz.deels.admin.core.oidc

import org.publicvalue.multiplatform.oidc.appsupport.AndroidCodeAuthFlowFactory
import org.publicvalue.multiplatform.oidc.flows.CodeAuthFlowFactory

public actual val flowFactory: CodeAuthFlowFactory = AndroidCodeAuthFlowFactory()
